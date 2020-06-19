package org.jetbrains.plugins.scala.lang.references


import com.intellij.model.psi.{PsiSymbolReference, PsiSymbolReferenceService}
import com.intellij.openapi.paths.{UrlReference, WebReference}
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.scala.base.ScalaLightCodeInsightFixtureTestAdapter
import org.jetbrains.plugins.scala.lang.psi.api.ScalaRecursiveElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral
import org.jetbrains.plugins.scala.util.MultilineStringUtil.{MultilineQuotes => quotes}
import org.junit.Assert._

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.collection.mutable

class ScalaReferenceContributorTest extends ScalaLightCodeInsightFixtureTestAdapter {

  def testUrlReference(): Unit = {
    val testUrl = "https://www.site.com/"

    def assertUrlReference(extraMessage: String)(literalText: String): Unit = {
      val text = s"""val value = $literalText"""
      val psiFile = configureFromFileText(text)

      val urlReferenceExpected = testUrl
      val urlReferencesActual  = extractReferences(psiFile).collect {
        case w: WebReference => w.getUrl
        case u: UrlReference => u.getUrl
      }

      assertEquals(1, urlReferencesActual.size)
      assertEquals(
        s"should detect url reference $extraMessage",
        urlReferenceExpected,
        urlReferencesActual.head
      )
    }

    assertUrlReference("in string literal")(s""""$testUrl"""")
    assertUrlReference("in string literal with leading space")(s"""" $testUrl"""")
    assertUrlReference("in string literal with closing space")(s""""$testUrl """")

    assertUrlReference("in multiline string literal")(s"""$quotes$testUrl$quotes""")
    assertUrlReference("in interpolated string literal")(s"""s"$testUrl"""")
    assertUrlReference("in multiline interpolated string literal")(s"""s$quotes$testUrl$quotes""")
  }

  //noinspection UnstableApiUsage
  private def extractReferences(file: PsiFile): Seq[PsiSymbolReference] = {
    val found: mutable.Buffer[PsiSymbolReference] = mutable.Buffer()

    val visitor = new ScalaRecursiveElementVisitor {
      override def visitLiteral(literal: ScLiteral): Unit = {
        if (literal.isString) {
          found ++= PsiSymbolReferenceService.getService.getReferences(literal).asScala
        }
        super.visitLiteral(literal)
      }
    }
    visitor.visitFile(file)

    found
  }

}