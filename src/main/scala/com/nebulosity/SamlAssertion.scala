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
import org.opensaml.saml.saml2.core.impl.SubjectConfirmationDataBuilder
import org.opensaml.saml.saml2.core.SubjectConfirmationData
import java.time.Instant
import org.opensaml.saml.saml2.core.impl.SubjectConfirmationBuilder
import org.opensaml.saml.saml2.core.SubjectConfirmation
import org.opensaml.saml.saml2.core.impl.SubjectBuilder
import org.opensaml.saml.saml2.core.impl.NameIDBuilder
import org.opensaml.saml.saml2.core.NameID
import org.opensaml.saml.saml2.core.NameIDType
import org.opensaml.saml.saml2.core.Subject
import org.opensaml.saml.saml2.core.impl.AuthnContextClassRefBuilder
import org.opensaml.saml.saml2.core.AuthnContextClassRef
import org.opensaml.saml.saml2.core.impl.AuthnStatementBuilder
import org.opensaml.saml.saml2.core.AuthnStatement
import org.opensaml.saml.saml2.core.impl.AuthnContextBuilder
import org.opensaml.saml.saml2.core.AuthnContext
import java.util.concurrent.TimeUnit
import java.time.temporal.ChronoUnit
import org.opensaml.saml.saml2.core.impl.AttributeStatementBuilder
import org.opensaml.saml.saml2.core.AttributeStatement
import org.opensaml.saml.saml2.core.impl.AttributeBuilder
import org.opensaml.saml.saml2.core.Attribute
import org.opensaml.saml.saml2.core.impl.AttributeValueBuilder
import org.opensaml.saml.saml2.core.AttributeValue
import org.opensaml.core.xml.schema.impl.XSStringBuilder
import org.opensaml.core.xml.schema.XSString
import org.opensaml.saml.saml2.core.impl.OneTimeUseBuilder
import org.opensaml.saml.saml2.core.OneTimeUse
import org.opensaml.saml.saml2.core.impl.AudienceRestrictionBuilder
import org.opensaml.saml.saml2.core.AudienceRestriction
import org.opensaml.saml.saml2.core.impl.AudienceBuilder
import org.opensaml.saml.saml2.core.Audience
import org.opensaml.saml.saml2.core.impl.ConditionsBuilder
import org.opensaml.saml.saml2.core.Conditions
import org.opensaml.saml.saml2.core.impl.IssuerBuilder
import org.opensaml.saml.saml2.core.Issuer
import org.opensaml.saml.saml2.core.impl.AssertionBuilder
import org.opensaml.saml.saml2.core.Assertion
import org.opensaml.saml.common.SAMLVersion
import org.opensaml.xmlsec.signature.impl.SignatureMarshaller
import org.opensaml.saml.saml2.core.impl.AssertionMarshaller
import _root_.net.shibboleth.utilities.java.support.xml.SerializeSupport
import java.util.UUID

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
  def signature(): Signature =
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
    signature.setKeyInfo(keyInfo(certificate))
    signature.setSigningCredential(credential)
    signature

  /** Builds X509Certificate SAML element.
    * @param certificate
    *   base64 encoded certificate text
    * @return
    */
  private def x509Certificate(certificate: String): X509Certificate =
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
  def x509Data(certificate: String): X509Data =
    val cert: X509Certificate = x509Certificate(certificate)
    val x509Data = SamlXmlUtils
      .xmlObjectBuilder[X509DataBuilder](X509Data.DEFAULT_ELEMENT_NAME)
      .map(x509DataBuilder => x509DataBuilder.buildObject())
      .get
    x509Data.getX509Certificates.add(cert)
    x509Data

  /** Builds a KeyInfo SAML element */
  def keyInfo(certificate: String): KeyInfo =
    val keyInfo: KeyInfo = SamlXmlUtils
      .xmlObjectBuilder[KeyInfoBuilder](KeyInfo.DEFAULT_ELEMENT_NAME)
      .map(keyInfoBuilder => keyInfoBuilder.buildObject())
      .get
    val data = x509Data(certificate)
    keyInfo.getX509Datas.add(data)
    keyInfo

  /** SubjectConfirmationData Xml Element.
    *
    * @param recipient
    * @return
    */
  def subjectConfirmationData(recipient: String) =
    val subjectConfirmationData: SubjectConfirmationData = SamlXmlUtils
      .xmlObjectBuilder[SubjectConfirmationDataBuilder](
        SubjectConfirmationData.DEFAULT_ELEMENT_NAME
      )
      .get
      .buildObject()
    val now = Instant.now()
    subjectConfirmationData.setNotBefore(now)
    subjectConfirmationData.setNotOnOrAfter(now.plusSeconds(300))
    subjectConfirmationData.setRecipient(recipient)
    subjectConfirmationData

    /** SubjectConfirmation Xml Element.
      *
      * @return
      */
  def subjectConfirmation(recipient: String) =
    val subjectConfirmation =
      SamlXmlUtils
        .xmlObjectBuilder[SubjectConfirmationBuilder](
          SubjectConfirmation.DEFAULT_ELEMENT_NAME
        )
        .get
        .buildObject()
    subjectConfirmation.setSubjectConfirmationData(
      subjectConfirmationData(recipient)
    )
    subjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER)
    subjectConfirmation

  /** NameID.
    *
    * @return
    */
  def nameId(name: String) =
    val nameId = SamlXmlUtils
      .xmlObjectBuilder[NameIDBuilder](NameID.DEFAULT_ELEMENT_NAME)
      .get
      .buildObject()
    nameId.setValue(name)
    nameId.setFormat(NameIDType.UNSPECIFIED)
    nameId

  /** SAML Subject.
    *
    * @return
    */
  def subject(name: String, recipient: String) =
    val subject = SamlXmlUtils
      .xmlObjectBuilder[SubjectBuilder](Subject.DEFAULT_ELEMENT_NAME)
      .get
      .buildObject()

    subject.setNameID(nameId(name))
    subject.getSubjectConfirmations().add(subjectConfirmation(recipient))
    subject

  def authnContextClassRef() =
    val authnContextClassRef = SamlXmlUtils
      .xmlObjectBuilder[AuthnContextClassRefBuilder](
        AuthnContextClassRef.DEFAULT_ELEMENT_NAME
      )
      .get
      .buildObject()
    authnContextClassRef.setURI(
      "urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified"
    )
    authnContextClassRef

  /** Authentication Context element.
    *
    * @return
    */
  def authnContext() =
    val authnContext = SamlXmlUtils
      .xmlObjectBuilder[AuthnContextBuilder](AuthnContext.DEFAULT_ELEMENT_NAME)
      .get
      .buildObject()
    authnContext.setAuthnContextClassRef(authnContextClassRef())
    authnContext

  /** Authnetication Statement.
    */
  def authnStatement(sessionIndex: String) = {
    val authnStatement = SamlXmlUtils
      .xmlObjectBuilder[AuthnStatementBuilder](
        AuthnStatement.DEFAULT_ELEMENT_NAME
      )
      .get
      .buildObject()
    val now = Instant.now()
    authnStatement.setAuthnInstant(now)
    authnStatement.setSessionIndex(sessionIndex)
    authnStatement.setSessionNotOnOrAfter(now.plus(5, ChronoUnit.MINUTES))
    authnStatement.setAuthnContext(authnContext())
    authnStatement
  }

  /** Attribute Statement Xml Element.
    *
    * @param attributes
    * @return
    */
  def attributeStatement(attributes: Map[String, String]) =
    val attributeStatement = SamlXmlUtils
      .xmlObjectBuilder[AttributeStatementBuilder](
        AttributeStatement.DEFAULT_ELEMENT_NAME
      )
      .get
      .buildObject()
    for ((key, value) <- attributes) {
      attributeStatement
        .getAttributes()
        .add(attribute(key, value))
    }
    attributeStatement

  /** Attribute Xml Element.
    *
    * @param key
    * @param value
    * @return
    */
  def attribute(key: String, value: String): Attribute =
    val attribute = SamlXmlUtils
      .xmlObjectBuilder[AttributeBuilder](Attribute.DEFAULT_ELEMENT_NAME)
      .get
      .buildObject()
    attribute.setName(key)

    val attributeValue = SamlXmlUtils
      .xmlObjectBuilder[XSStringBuilder](
        XSString.TYPE_NAME
      )
      .get
      .buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME)
    attributeValue.setValue(value)
    attribute.getAttributeValues().add(attributeValue)
    attribute

  /** Saml assertion is for one time use only by the Relying Party.
    *
    * @return
    */
  def oneTimeUse() =
    SamlXmlUtils
      .xmlObjectBuilder[OneTimeUseBuilder](
        OneTimeUse.DEFAULT_ELEMENT_NAME
      )
      .get
      .buildObject()

  /** Audience restrictions.
    *
    * @param aud
    * @return
    */
  def audienceRestriction(aud: String): AudienceRestriction = {
    val restriction = SamlXmlUtils
      .xmlObjectBuilder[AudienceRestrictionBuilder](
        AudienceRestriction.DEFAULT_ELEMENT_NAME
      )
      .get
      .buildObject()
    val audience = SamlXmlUtils
      .xmlObjectBuilder[AudienceBuilder](Audience.DEFAULT_ELEMENT_NAME)
      .get
      .buildObject()
    audience.setURI(aud)
    restriction.getAudiences().add(audience)
    restriction
  }

  /** Saml Conditions.
    *
    * @return
    */
  def conditions(audience: String) = {
    val conds = SamlXmlUtils
      .xmlObjectBuilder[ConditionsBuilder](Conditions.DEFAULT_ELEMENT_NAME)
      .get
      .buildObject()
    val now = Instant.now()
    conds.setNotBefore(now)
    conds.setNotOnOrAfter(now.plus(5, ChronoUnit.MINUTES))
    conds.getConditions().add(oneTimeUse())
    conds.getAudienceRestrictions().add(audienceRestriction(audience))
    conds
  }

  /** SAML Issuer.
    *
    * @param value
    */
  def issuer(value: String) = {
    val issuer = SamlXmlUtils
      .xmlObjectBuilder[IssuerBuilder](Issuer.DEFAULT_ELEMENT_NAME)
      .get
      .buildObject()
    issuer.setValue(value)
    issuer
  }

  /** Generates a SAML Assertion and Signs it using the provided jks keystore.
    *
    * @return
    */
  def assertion() = {
    val assertion = SamlXmlUtils
      .xmlObjectBuilder[AssertionBuilder](Assertion.DEFAULT_ELEMENT_NAME)
      .get
      .buildObject()
    assertion.setIssuer(issuer("https://hiretech-auth-proxy.efx.com"))
    val now = Instant.now()
    val sig = signature()
    val uuid = UUID.randomUUID()
    assertion.setIssueInstant(now)
    assertion.setID(uuid.toString())
    assertion.setVersion(SAMLVersion.VERSION_20)
    assertion.setSignature(sig)
    assertion.getAuthnStatements().add(authnStatement(uuid.toString()))
    assertion.setSubject(
      subject(
        "akkineni.vijay@gmail.com",
        "https://dev-10052401.okta.com/sso/saml2/0oa8hl63lvaQwvrDP5d7"
      )
    )
    assertion
      .getAttributeStatements()
      .add(
        attributeStatement(
          Map(
            "FirstName" -> "Vijay",
            "LastName" -> "Akkineni",
            "Locality" -> "USA",
            "mobilePhone" -> "8888888888",
            "Email" -> "akkineni.vijay@gmail.com"
          )
        )
      )
    assertion.setConditions(
      conditions(
        "https://www.okta.com/saml2/service-provider/speyapkkuqlzzhisybyc"
      )
    )

    SamlXmlUtils.marshall[AssertionMarshaller, Assertion](
      new AssertionMarshaller,
      assertion
    )
    Signer.signObject(sig)

    val node = SamlXmlUtils.marshall[AssertionMarshaller, Assertion](
      new AssertionMarshaller,
      assertion
    )
    Base64.getEncoder().encodeToString(SerializeSupport.nodeToString(node).getBytes())
  }
