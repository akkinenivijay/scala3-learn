package com.nebulosity

import net.shibboleth.utilities.java.support.resolver.CriteriaSet

import scala.collection.MapView
import scala.io.BufferedSource
import scala.jdk.javaapi.CollectionConverters
import scala.util.Try

import java._
import java.io.InputStream
import java.lang.Boolean
import java.security.KeyStore
import javax.xml.namespace.QName
import org.apache.xml.security.Init
import org.opensaml.core.config.ConfigurationService
import org.opensaml.core.criterion.EntityIdCriterion
import org.opensaml.core.xml.config.XMLObjectProviderRegistry
import org.opensaml.security.credential.Credential
import org.opensaml.security.credential.impl.KeyStoreCredentialResolver
import org.opensaml.security.x509.impl.KeyStoreX509CredentialAdapter
import org.opensaml.xmlsec.config.impl.JavaCryptoValidationInitializer
import org.opensaml.xmlsec.signature.support.SignatureConstants

import java.util.Base64
import org.opensaml.xmlsec.signature.impl.SignatureBuilder
import org.opensaml.xmlsec.signature.Signature
import org.opensaml.xmlsec.signature.X509Certificate
import org.opensaml.xmlsec.signature.impl.X509CertificateBuilder
import org.opensaml.xmlsec.signature.X509Data
import org.opensaml.xmlsec.signature.impl.X509DataBuilder
import org.opensaml.xmlsec.signature.KeyInfo
import org.opensaml.xmlsec.signature.impl.KeyInfoBuilder
import org.opensaml.xmlsec.signature.support.Signer

object SamlAssertion:

  Init.init()

  def generate(): Unit = println(certCredentials())

  /** Generates a tuple of base 64 encoded certificate and Credential from a
    * provided keystore. This date is needed for building SAML Signature
    * element.
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
    val encodedCertificate = Base64.getEncoder
      .encodeToString(
        keyStoreX509CredentialAdapter.getEntityCertificate.getEncoded
      )
    stream.close()
    (encodedCertificate, credential)

  /** Builds a SAML Signature XML element.
    * @return
    */
  def buildSignature(): Signature =
    val signature = SamlXmlUtils
      .xmlObjectBuilder[SignatureBuilder](Signature.DEFAULT_ELEMENT_NAME)
      .get
      .buildObject()

    signature.setSignatureAlgorithm(
      SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256
    )
    signature.setCanonicalizationAlgorithm(
      SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS
    )

    val (certificate, credential) = certCredentials()
    signature.setKeyInfo(buildKeyInfo(certificate))
    signature.setSigningCredential(credential)
    signature

  /** Builds X509Certificate SAML element.
    * @param certificate
    *   base64 encoded certificate text
    * @return
    */
  private def buildX509Certificate(certificate: String): X509Certificate =
    SamlXmlUtils
      .xmlObjectBuilder[X509CertificateBuilder](
        X509Certificate.DEFAULT_ELEMENT_NAME
      )
      .map(builder => builder.buildObject())
      .map(x509Certificate => {
        x509Certificate.setValue(certificate)
        x509Certificate
      })
      .get

  /** Builds an X509Data SAML element.
    * @param certificate
    *   base64 encoded certificate text
    * @return
    */
  def buildX509Data(certificate: String): X509Data =
    val x509Certificate: X509Certificate = buildX509Certificate(certificate)
    val x509Data = SamlXmlUtils
      .xmlObjectBuilder[X509DataBuilder](X509Data.DEFAULT_ELEMENT_NAME)
      .map(x509DataBuilder => x509DataBuilder.buildObject())
      .get
    x509Data.getX509Certificates.add(x509Certificate)
    x509Data

  /** Builds a KeyInfo SAML element */
  def buildKeyInfo(certificate: String): KeyInfo =
    val keyInfo: KeyInfo = SamlXmlUtils
      .xmlObjectBuilder[KeyInfoBuilder](KeyInfo.DEFAULT_ELEMENT_NAME)
      .map(keyInfoBuilder => keyInfoBuilder.buildObject())
      .get
    val x509Data = buildX509Data(certificate)
    keyInfo.getX509Datas.add(x509Data)
    keyInfo
