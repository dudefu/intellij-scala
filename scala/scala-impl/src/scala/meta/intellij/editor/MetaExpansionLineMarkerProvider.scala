package scala.meta.intellij.editor

import com.intellij.notification.{NotificationGroup, NotificationType}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.plugin.scala.util.MacroExpansion
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.macros.expansion.MacroExpansionLineMarkerProvider
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScAnnotation, ScBlock}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScAnnotationsHolder
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory

import scala.meta.intellij.MetaExpansionsManager
import scala.meta.intellij.MetaExpansionsManager.isUpToDate

class MetaExpansionLineMarkerProvider extends MacroExpansionLineMarkerProvider {

  private val LOG = Logger.getInstance(getClass)

  override protected def getExpandMarker(element: PsiElement): Option[Marker] = {
    import scala.meta.intellij.psiExt._

    val maybeAnnotation = element.getParent match {
      case holder: ScAnnotationsHolder => holder.annotations.find(_.isMetaMacro)
      case _ => None
    }
    maybeAnnotation.map { annot =>
      MetaExpansionsManager.getCompiledMetaAnnotClass(annot) match {
        case Some(clazz) if isUpToDate(annot, clazz) => createExpandMarker(annot.getFirstChild)(_=>expandMetaAnnotation(annot))
        case _ => createNotCompiledLineMarker(annot.getFirstChild, annot)
      }
    }
  }

  override protected def getUndoMarker(element: PsiElement): Option[Marker] = {
    element.getParent.getCopyableUserData(EXPANDED_KEY) match {
      case null => None
      case _ => Some(createUndoMarker(element))
    }
  }

  def expandMetaAnnotation(annot: ScAnnotation): Unit = {
    import scala.meta._
    val result = MetaExpansionsManager.runMetaAnnotation(annot)
    result match {
      case Right(tree) =>
        val removeCompanionObject = tree match {
          case Term.Block(Seq(Defn.Class(_, Type.Name(value1), _, _, _), Defn.Object(_, Term.Name(value2), _))) =>
            value1 == value2
          case Term.Block(Seq(Defn.Trait(_, Type.Name(value1), _, _, _), Defn.Object(_, Term.Name(value2), _))) =>
            value1 == value2
          case _ => false
        }
        inWriteCommandAction(annot.getProject) {
          expandAnnotation(annot, MacroExpansion(null, tree.toString.trim, "", removeCompanionObject))
        }
      case Left(errorMsg) =>
        messageGroup.createNotification(
          s"Macro expansion failed: $errorMsg", NotificationType.ERROR
        ).notify(annot.getProject)
    }
  }

  def expandAnnotation(place: ScAnnotation, expansion: MacroExpansion): Unit = {
    import place.projectContext

    def filter(elt: PsiElement) = elt.isInstanceOf[LeafPsiElement]
    // we can only macro-annotate scala code
    place.getParent.getParent match {
      case holder: ScAnnotationsHolder =>
        val body = expansion.body
        val newPsi = ScalaPsiElementFactory.createBlockExpressionWithoutBracesFromText(body)
        CodeEditUtil.setNodeGeneratedRecursively(newPsi.getNode, true)
        newPsi.firstChild match {
          case Some(block: ScBlock) => // insert content of block expression(annotation can generate >1 expression)
            val children = block.getChildren.dropWhile(filter).reverse.dropWhile(filter).reverse
            val savedCompanion = if (expansion.removeCompanionObject) {
              val companion = holder match {
                case td: ScTypeDefinition => td.baseCompanionModule
                case _ => None
              }
              companion.map { o =>
                o.getParent.getNode.removeChild(o.getNode)
                o.getText
              }
            } else None
            block.children
              .find(_.isInstanceOf[ScalaPsiElement])
              .foreach { p =>
                p.putCopyableUserData(EXPANDED_KEY, UndoExpansionData(holder.getText, savedCompanion))
              }
            holder.getParent.addRangeAfter(children.head, children.last, holder)
            holder.getParent.getNode.removeChild(holder.getNode)
            reformatCode(newPsi)
          case Some(psi: PsiElement) => // defns/method bodies/etc...
            psi.putCopyableUserData(EXPANDED_KEY, UndoExpansionData(holder.getText))
            holder.getParent.getNode.replaceChild(holder.getNode, psi.getNode)
            reformatCode(newPsi)
          case None => LOG.warn(s"Failed to parse expansion: $body")
        }
      case other => LOG.warn(s"Unexpected annotated element: $other at ${other.getText}")
    }
  }

}
