package org.mitre.fhir.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Base64.Decoder;


public class RSAUtils {
	
	private static final String RSA_PUBLIC_KEY_RESOURCE_PATH = "/rsa_key.pub";
	private static final String RSA_PRIVATE_KEY_RESOURCE_PATH = "/rsa_key.key";
	
	public static RSAPublicKey getRSAPublicKey()
	{
		return getRSAPublicKey(RSA_PUBLIC_KEY_RESOURCE_PATH);
	}
	
	public static RSAPublicKey getRSAPublicKey(String publicKeyResourcePath)
	{

		byte[] publicBytes;
		try {

			InputStream in = RSAUtils.class.getResourceAsStream(publicKeyResourcePath);

			publicBytes = in.readAllBytes();
		 
		
			String temp = new String(publicBytes);
			String publicKeyPEM = temp.replace("-----BEGIN RSA PUBLIC KEY-----\n", "");
		    publicKeyPEM = publicKeyPEM.replace("-----END RSA PUBLIC KEY-----", "");
		    publicKeyPEM = publicKeyPEM.replace("\n", "");
	
			Decoder decoder = Base64.getDecoder();
			byte[] decoded = decoder.decode(publicKeyPEM);
			
			EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decoded);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");		
			RSAPublicKey publicKey = (RSAPublicKey)keyFactory.generatePublic(publicKeySpec);
	
			return publicKey;
		}
		
		catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException("Error getting RSA Public Key", e);
		}		

		

	}
	
	public static RSAPrivateKey getRSAPrivateKey()
	{
		return getRSAPrivateKey(RSA_PRIVATE_KEY_RESOURCE_PATH);
	}

	public static RSAPrivateKey getRSAPrivateKey(String privateKeyResourcePath)
	{
		byte[] privateBytes;
		try {

			InputStream in = RSAUtils.class.getResourceAsStream(privateKeyResourcePath);

			privateBytes = in.readAllBytes();
		 		
			String temp = new String(privateBytes);
			String privateKeyPEM = temp.replace("-----BEGIN RSA PRIVATE KEY-----\n", "");
		    privateKeyPEM = privateKeyPEM.replace("-----END RSA PRIVATE KEY-----", "");
		    privateKeyPEM = privateKeyPEM.replace("\n", "");
	
			Decoder decoder = Base64.getDecoder();
			byte[] decoded = decoder.decode(privateKeyPEM);
			
			EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decoded);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");		
			RSAPrivateKey privateKey = (RSAPrivateKey)keyFactory.generatePrivate(privateKeySpec);
				
			return privateKey;
		}
		
		catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException("Error getting RSA Private Key", e);
		}		

	}

}
