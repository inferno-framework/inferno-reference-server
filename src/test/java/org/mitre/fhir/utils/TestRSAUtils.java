package org.mitre.fhir.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import net.sf.ehcache.util.concurrent.ThreadLocalRandom;
import org.junit.Assert;
import org.junit.Test;
import org.mitre.fhir.utils.exception.RsaKeyException;

public class TestRSAUtils {

  private static final String INCORRECT_TEST_PUBLIC_KEY_PATH = "/incorrect_test_key.pub";

  @Test
  public void testRSAPublicAndPrivateKey()
      throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, RsaKeyException {
    // create a challenge
    byte[] challenge = new byte[10000];
    ThreadLocalRandom.current().nextBytes(challenge);

    // sign using the private key
    Signature signature = Signature.getInstance("SHA256withRSA");
    signature.initSign(RsaUtils.getRsaPrivateKey());
    signature.update(challenge);
    byte[] signatureByteArray = signature.sign();

    // verify signature using the public key
    signature.initVerify(RsaUtils.getRsaPublicKey());
    signature.update(challenge);

    Assert.assertTrue(signature.verify(signatureByteArray));

  }

  @Test
  public void testReadingWithPublicKey() throws RsaKeyException {

    RSAPublicKey publicKey = RsaUtils.getRsaPublicKey();
    Algorithm algorithm = Algorithm.RSA256(publicKey, RsaUtils.getRsaPrivateKey());
    String token = JWT.create().withIssuer("issuer").sign(algorithm);

    Algorithm algorithm2 = Algorithm.RSA256(publicKey, null);

    DecodedJWT jwt = JWT.require(algorithm2).build().verify(token);
    Assert.assertNotNull(jwt);
  }

  @Test(expected = SignatureVerificationException.class)
  public void testWrongPublicKey() throws RsaKeyException {
    RSAPublicKey publicKey = RsaUtils.getRsaPublicKey();
    Algorithm algorithm = Algorithm.RSA256(publicKey, RsaUtils.getRsaPrivateKey());
    String token = JWT.create().withIssuer("issuer").sign(algorithm);

    RSAPublicKey newKey = RsaUtils.getRsaPublicKey(INCORRECT_TEST_PUBLIC_KEY_PATH);

    Algorithm algorithm2 = Algorithm.RSA256(newKey, null);

    JWT.require(algorithm2).build().verify(token);

  }

}
