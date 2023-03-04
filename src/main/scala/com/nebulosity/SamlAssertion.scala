package com.nebulosity

import net.shibboleth.utilities.java.support.resolver.{CriteriaSet, Criterion}
import net.shibboleth.utilities.java.support.xml.{BasicParserPool, ParserPool}
import org.apache.xml.security.Init
import org.opensaml.core.config.ConfigurationService
import org.opensaml.core.criterion.EntityIdCriterion
import org.opensaml.core.xml.config.XMLObjectProviderRegistry
import org.opensaml.security.credential.impl.KeyStoreCredentialResolver
import org.opensaml.security.credential.Credential
import org.opensaml.security.x509.impl.KeyStoreX509CredentialAdapter
import org.opensaml.xmlsec.config.impl.JavaCryptoValidationInitializer
import org.opensaml.xmlsec.signature.{Signature, X509Certificate, X509Data}
import org.opensaml.xmlsec.signature.impl.{SignatureBuilder, X509CertificateBuilder, X509DataBuilder}
import org.opensaml.xmlsec.signature.support.SignatureConstants
import org.opensaml.xmlsec.signature.support.SignatureConstants.{ALGO_ID_C14N_EXCL_OMIT_COMMENTS, ALGO_ID_SIGNATURE_RSA_SHA256}

import java.{lang, util}
import java.io.InputStream
import java.lang.Boolean
import java.security.KeyStore
import java.util.{Base64, HashMap, Map}
import javax.xml.namespace.QName
import scala.collection.MapView
import scala.io.BufferedSource
import scala.jdk.javaapi.CollectionConverters
import scala.util.Try

object SamlAssertion:

  Init.init()

  def generate(): Unit = println(certCredentials())

  /**
   * Generates a tuple of base 64 encoded certificate and Credential from a
   * provided keystore. This date is needed for building SAML Signature element.
   * @return
   */
  private def certCredentials(): (String, Credential) =
    val password = "Hiretech786%"
    val entityId = "hiretech-auth-proxy.efx.com"
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
    val stream: InputStream =
      Thread
        .currentThread()
        .getContextClassLoader
        .getResourceAsStream("SenderKeyStore.jks")
    keyStore.load(stream, password.toCharArray)
    val passwordMap = new util.HashMap[String, String]
    passwordMap.put(entityId, password)
    val resolver = new KeyStoreCredentialResolver(keyStore, passwordMap)
    val criterion = new EntityIdCriterion(entityId)
    val criteriaSet = new CriteriaSet
    criteriaSet.add(criterion)
    val credential = resolver.resolveSingle(criteriaSet)
    val keyStoreX509CredentialAdapter = new KeyStoreX509CredentialAdapter(
      keyStore,
      entityId,
      password.toCharArray
    )
    val encodedCertificate = Base64
      .getEncoder
      .encodeToString(
        keyStoreX509CredentialAdapter.getEntityCertificate.getEncoded
      )
    stream.close()
    (encodedCertificate, credential)

  /** Builds a SAML Signature XML element.
   *  @return
   */
  def buildSignature(): Unit =
    val signature = SamlXmlUtils
      .xmlObjectBuilder[SignatureBuilder](Signature.DEFAULT_ELEMENT_NAME)
      .get
      .buildObject()

    signature.setSignatureAlgorithm(
      ALGO_ID_SIGNATURE_RSA_SHA256
    )
    signature.setCanonicalizationAlgorithm(ALGO_ID_C14N_EXCL_OMIT_COMMENTS)

  /** Builds X509Certificate SAML element.
   *  @param certificate base64 encoded certificate text
   *  @return
   */
  private def buildX509Certificate(certificate: String)
    : Option[X509Certificate] =
    SamlXmlUtils
      .xmlObjectBuilder[X509CertificateBuilder](
        X509Certificate.DEFAULT_ELEMENT_NAME
      )
      .map(builder => builder.buildObject())
      .map(x509Certificate => {
        x509Certificate.setValue(certificate)
        x509Certificate
      })

  /** Builds an X509Data SAML element.
   *  @param certificate base64 encoded certificate text
   *  @return
   */
  def buildX509Data(certificate: String): X509Data =
    val x509Certificate: Option[X509Certificate] = buildX509Certificate(certificate)
    val x509Data = SamlXmlUtils
      .xmlObjectBuilder[X509DataBuilder](
        X509Data.DEFAULT_ELEMENT_NAME
      )
      .get
      .buildObject()
    x509Certificate.map(x509Certificate => x509Data.getX509Certificates.add(x509Certificate))
    x509Data
