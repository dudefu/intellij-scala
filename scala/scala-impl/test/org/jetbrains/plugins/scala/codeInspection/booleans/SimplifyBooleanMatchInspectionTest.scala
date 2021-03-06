package org.jetbrains.plugins.scala.codeInspection.booleans

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.testFramework.EditorTestUtil
import org.jetbrains.plugins.scala.codeInspection.ScalaQuickFixTestBase

class SimplifyBooleanMatchInspectionTest extends ScalaQuickFixTestBase {

  import EditorTestUtil.{SELECTION_END_TAG => END, SELECTION_START_TAG => START}

  override protected val classOfInspection: Class[_ <: LocalInspectionTool] = classOf[SimplifyBooleanMatchInspection]
  override protected val description = "Trivial match can be simplified"

  private val hint = "Simplify match to if statement"

  def test_SingleTrueWithParenthesis_lessPatternSimpleBranches() {
    val selectedText =
      s"""
         |val a = true
         |val b = a ${START}match$END {
         |  case true => 1
         |}
       """.stripMargin
    checkTextHasError(selectedText)

    val text =
      """
        |val a = true
        |val b = a match {
        | case true => 1
        | }
      """.stripMargin
    val result =
      """
        |val a = true
        |val b = if (a) {
        |  1
        |}
      """.stripMargin

    testQuickFix(text, result, hint)
  }

  def test_SingleTrueWithParenthesis_lessPatternSimpleBranchesBracesBlock() {
    val selectedText =
      s"""
         |val a = true
         |val b = a ${START}match$END {
         |  case true => {
         |    1
         |  }
         |}
       """.stripMargin
    checkTextHasError(selectedText)

    val text =
      """
        |val a = true
        |val b = a match {
        | case true => {
        |     1
        |   }
        | }
      """.stripMargin
    val result =
      """
        |val a = true
        |val b = if (a) {
        |  1
        |}
      """.stripMargin

    testQuickFix(text, result, hint)
  }

  def test_SingleFalseWithParenthesis_lessPatternSimpleBranches() {
    val selectedText =
      s"""
         |val a = true
         |val b = a ${START}match$END {
         |  case false => 1
         |}
       """.stripMargin
    checkTextHasError(selectedText)

    val text =
      """
        |val a = true
        |val b = a match {
        | case false => 1
        | }
      """.stripMargin
    val result =
      """
        |val a = true
        |val b = if (!a) {
        |  1
        |}
      """.stripMargin

    testQuickFix(text, result, hint)
  }

  def test_TrueFalseWithParenthesis_lessPatternSimpleBranches() {
    val selectedText =
      s"""
         |val a = true
         |val b = a ${START}match$END {
         |  case false => 1
         |  case true => 4
         |}
       """.stripMargin
    checkTextHasError(selectedText)

    val text =
      """
        |val a = true
        |val b = a match {
        | case false => 1
        | case true => 4
        | }
      """.stripMargin
    val result =
      """
        |val a = true
        |val b = if (a) {
        |  4
        |} else {
        |  1
        |}
      """.stripMargin

    testQuickFix(text, result, hint)
  }

  def test_TrueWildcardWithParenthesis_lessPatternSimpleBranches() {
    val selectedText =
      s"""
         |val a = true
         |val b = a ${START}match$END {
         |  case true => 1
         |  case _ => 4
         |}
       """.stripMargin
    checkTextHasError(selectedText)

    val text =
      """
        |val a = true
        |val b = a match {
        | case true => 1
        | case _ => 4
        | }
      """.stripMargin
    val result =
      """
        |val a = true
        |val b = if (a) {
        |  1
        |} else {
        |  4
        |}
      """.stripMargin

    testQuickFix(text, result, hint)
  }

  def test_FalseWildcardWithParenthesis_lessPatternSimpleBranches() {
    val selectedText =
      s"""
         |val a = true
         |val b = a ${START}match$END {
         |  case false => 1
         |  case _ => 4
         |}
       """.stripMargin
    checkTextHasError(selectedText)

    val text =
      """
        |val a = true
        |val b = a match {
        | case false => 1
        | case _ => 4
        | }
      """.stripMargin
    val result =
      """
        |val a = true
        |val b = if (a) {
        |  4
        |} else {
        |  1
        |}
      """.stripMargin

    testQuickFix(text, result, hint)
  }

  def test_TrueFalseWithParenthesis_lessPatternBlockBranches() {
    val selectedText =
      s"""
         |val a = true
         |val b = a ${START}match$END {
         |  case true => {
         |    1
         |  }
         |  case false => {
         |    val t = 1
         |    t + 2
         |  }
         |}
       """.stripMargin
    checkTextHasError(selectedText)

    val text =
      s"""
         |val a = true
         |val b = a match {
         |  case true => {
         |    1
         |  }
         |  case false => {
         |    val t = 1
         |    t + 2
         |  }
         |}
       """.stripMargin
    val result =
      """
        |val a = true
        |val b = if (a) {
        |  1
        |} else {
        |  val t = 1
        |  t + 2
        |}
      """.stripMargin

    testQuickFix(text, result, hint)
  }

  def test_SingleTrueWithParenthesisedPatternSimpleBranches() {
    val selectedText =
      s"""
         |val b = 1 + 2 == 3 ${START}match$END {
         |  case true => 1
         |}
       """.stripMargin
    checkTextHasError(selectedText)

    val text =
      """
        |val b = 1 + 2 == 3 match {
        | case true => 1
        | }
      """.stripMargin
    val result =
      """
        |val b = if (1 + 2 == 3) {
        |  1
        |}
      """.stripMargin

    testQuickFix(text, result, hint)
  }

  def test_SingleFalseWithParenthesisedPatternSimpleBranches() {
    val selectedText =
      s"""
         |val b = 1 + 2 == 3 ${START}match$END {
         |  case false => 1
         |}
       """.stripMargin
    checkTextHasError(selectedText)

    val text =
      """
        |val b = 1 + 2 == 3 match {
        | case false => 1
        | }
      """.stripMargin
    val result =
      """
        |val b = if (!(1 + 2 == 3)) {
        |  1
        |}
      """.stripMargin

    testQuickFix(text, result, hint)
  }

  def test_SingleComplexBranch() {
    val text =
      s"""
         |val a = true
         |val b = a match {
         |  case a + 2 == 3 => {
         |    1
         |  }
         |}
       """.stripMargin

    checkTextHasNoErrors(text)
  }

  def test_ThreeBranches() {
    val text =
      s"""
         |val a = 1
         |val b = a match {
         |  case a + 2 == 3 => {
         |    1
         |  }
         |  case a + 2 == 4 => {
         |    2
         |  }
         |  case _ =>
         |}
       """.stripMargin

    checkTextHasNoErrors(text)
  }

  def test_NotBooleanExpr() {
    val text =
      s"""
         |val a = 1 + 2
         |val b = a match {
         |  case a + 2 == 3 => {
         |    1
         |  }
         |}
       """.stripMargin

    checkTextHasNoErrors(text)
  }

  def test_OnlyWildcardExpr() {
    val text =
      s"""
         |val a = true
         |val b = a match {
         |  case _ => {
         |    1
         |  }
         |}
       """.stripMargin

    checkTextHasNoErrors(text)
  }

  def test_TwoWildcatdsExpr() {
    val text =
      s"""
         |val a = true
         |val b = a match {
         |  case _ => {
         |    1
         |  }
         |  case _ => {
         |    2
         |  }
         |}
       """.stripMargin

    checkTextHasNoErrors(text)
  }

  def test_MatchWithGuard() {
    val text =
      s"""
         |val a = true
         |val c = false
         |val b = a match {
         |  case true if c => 1
         |  case false => 4
         |}
       """.stripMargin

    checkTextHasNoErrors(text)
  }
}
