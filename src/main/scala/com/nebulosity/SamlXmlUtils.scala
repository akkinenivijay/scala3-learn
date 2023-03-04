package com.nebulosity

import net.shibboleth.utilities.java.support.xml.{
  BasicParserPool,
  ParserPool,
  SerializeSupport
}
import org.opensaml.core.config.{ConfigurationService, InitializationService}
import org.opensaml.core.xml.{
  AbstractXMLObjectBuilder,
  XMLObject,
  XMLObjectBuilder,
  XMLObjectBuilderFactory
}
import org.opensaml.core.xml.config.{
  XMLObjectProviderRegistry,
  XMLObjectProviderRegistrySupport
}
import org.opensaml.core.xml.io.Marshaller
import org.opensaml.xmlsec.config.impl.JavaCryptoValidationInitializer

import java.lang.Boolean
import java.util
import java.util.HashMap
import javax.xml.namespace.QName
import scala.jdk.javaapi.CollectionConverters
import scala.util.{Failure, Success, Try}

object SamlXmlUtils:

  /** Applies the provided marshaller's marshall method to xmlObject
   *  @param marshaller XML Marshaller
   *  @param xmlObject XMLObject instance type
   *  @tparam T Marshaller
   *  @tparam S XMLObject
   *  @return serialized xml
   */
  def marshall[T <: Marshaller, S <: XMLObject](
      marshaller: T,
      xmlObject: S
  ): String =
    val element = marshaller.marshall(xmlObject)
    SerializeSupport.nodeToString(element)

  /** Builds an xmlObjectBuilder.
   *  @param qName
   *  @tparam S
   *  @return
   */
  def xmlObjectBuilder[S <: XMLObjectBuilder[?]](
      qName: QName
  ): Option[S] =
    xmlBuilderFactory match
      case Failure(exception) => None
      case Success(value) =>
        val x: S = value
          .getBuilder(qName)
          .asInstanceOf[S]
        Some(x)

  /** Returns an XMLObjectBuilderFactory
   *  @return
   */
  private val xmlBuilderFactory: Try[XMLObjectBuilderFactory] =
    Try {
      val javaCryptoValidationInitializer = new JavaCryptoValidationInitializer
      javaCryptoValidationInitializer.init()
      val registry = new XMLObjectProviderRegistry
      ConfigurationService.register(
        classOf[XMLObjectProviderRegistry],
        registry
      )
      parserPool().map(parserPool => registry.setParserPool(parserPool))
      val factory: XMLObjectBuilderFactory =
        XMLObjectProviderRegistrySupport.getBuilderFactory
      InitializationService.initialize()
      factory
    }

  /** Returns a pool of xml parsers
   *
   *  @return ParserPool
   */
  private def parserPool(): Try[ParserPool] =
    Try {
      val parserPool = new BasicParserPool
      parserPool.setMaxPoolSize(100)
      parserPool.setCoalescing(true)
      parserPool.setIgnoreComments(true)
      parserPool.setIgnoreElementContentWhitespace(true)
      parserPool.setNamespaceAware(true)
      parserPool.setExpandEntityReferences(false)
      parserPool.setXincludeAware(false)

      val features = Map(
        "http://xml.org/sax/features/external-general-entities" ->
          Boolean.FALSE,
        "http://xml.org/sax/features/external-parameter-entities" ->
          Boolean.FALSE,
        "http://apache.org/xml/features/disallow-doctype-decl" -> Boolean.FALSE,
        "http://apache.org/xml/features/validation/schema/normalized-value"
          -> Boolean.FALSE,
        "http://javax.xml.XMLConstants/feature/secure-processing" -> Boolean.TRUE
      )
      parserPool.setBuilderFeatures(CollectionConverters.asJava(features))
      parserPool.setBuilderAttributes(new util.HashMap[String, AnyRef])
      parserPool.initialize()
      parserPool
    }
