package org.mitre.fhir.authorization;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;

import org.junit.Assert;
import org.junit.Test;
import org.mitre.fhir.utils.RSAUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import net.sf.ehcache.util.concurrent.ThreadLocalRandom;

public class TestRSAUtils {

	private static final String INCORRECT_TEST_PUBLIC_KEY_PATH = "/incorrect_test_key.pub";

	@Test
	public void testRSAPublicAndPrivateKey() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		// create a challeng
		byte[] challenge = new byte[10000];
		ThreadLocalRandom.current().nextBytes(challenge);

		// sign using the private key
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(RSAUtils.getRSAPrivateKey());
		signature.update(challenge);
		byte[] signatureByteArray = signature.sign();

		// verify signature using the public key
		signature.initVerify(RSAUtils.getRSAPublicKey());
		signature.update(challenge);

		Assert.assertTrue(signature.verify(signatureByteArray));

	}

	@Test
	public void testReadingWithPublicKey() {

		RSAPublicKey publicKey = RSAUtils.getRSAPublicKey();
		Algorithm algorithm = Algorithm.RSA256(publicKey, RSAUtils.getRSAPrivateKey());
		String token = JWT.create().withIssuer("issuer").sign(algorithm);

		Algorithm algorithm2 = Algorithm.RSA256(publicKey, null);

		DecodedJWT jwt = JWT.require(algorithm2).build().verify(token);
		Assert.assertNotNull(jwt);
	}

	@Test(expected = SignatureVerificationException.class)
	public void testWrongPublicKey() {
		RSAPublicKey publicKey = RSAUtils.getRSAPublicKey();
		Algorithm algorithm = Algorithm.RSA256(publicKey, RSAUtils.getRSAPrivateKey());
		String token = JWT.create().withIssuer("issuer").sign(algorithm);

		RSAPublicKey newKey = RSAUtils.getRSAPublicKey(INCORRECT_TEST_PUBLIC_KEY_PATH);

		Algorithm algorithm2 = Algorithm.RSA256(newKey, null);

		JWT.require(algorithm2).build().verify(token);

	}

}
