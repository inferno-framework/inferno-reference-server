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

import org.mitre.fhir.utils.exception.RSAKeyException;

public class RSAUtils {
	
	private static final String RSA_PUBLIC_KEY_RESOURCE_PATH = "/rsa_key.pub";
	private static final String RSA_PRIVATE_KEY_RESOURCE_PATH = "/rsa_key.key";
	private static final String ALGORITHM = "RSA";
	
	public static RSAPublicKey getRSAPublicKey() throws RSAKeyException
	{
		return getRSAPublicKey(RSA_PUBLIC_KEY_RESOURCE_PATH);
	}
	
	public static RSAPublicKey getRSAPublicKey(String publicKeyResourcePath) throws RSAKeyException
	{
		byte[] publicBytes;
		try {

			InputStream in = RSAUtils.class.getResourceAsStream(publicKeyResourcePath);

			publicBytes = in.readAllBytes();
		 		
			String temp = new String(publicBytes);
			String publicKeyPEM = temp.replace("-----BEGIN RSA PUBLIC KEY-----", "");
		    publicKeyPEM = publicKeyPEM.replace("-----END RSA PUBLIC KEY-----", "");
		    
		    publicKeyPEM = stripNewlineCharacters(publicKeyPEM);	    
		    
			Decoder decoder = Base64.getDecoder();
			byte[] decoded = decoder.decode(publicKeyPEM);
			
			EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decoded);
			KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);		
			RSAPublicKey publicKey = (RSAPublicKey)keyFactory.generatePublic(publicKeySpec);
	
			return publicKey;
		}
		
		catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RSAKeyException("Error getting RSA Public Key", e);
		}		
	}
	
	public static RSAPrivateKey getRSAPrivateKey() throws RSAKeyException
	{
		return getRSAPrivateKey(RSA_PRIVATE_KEY_RESOURCE_PATH);
	}

	public static RSAPrivateKey getRSAPrivateKey(String privateKeyResourcePath) throws RSAKeyException
	{
		byte[] privateBytes;
		try {

			InputStream in = RSAUtils.class.getResourceAsStream(privateKeyResourcePath);

			privateBytes = in.readAllBytes();
		 		
			String temp = new String(privateBytes);
			String privateKeyPEM = temp.replace("-----BEGIN RSA PRIVATE KEY-----", "");
		    privateKeyPEM = privateKeyPEM.replace("-----END RSA PRIVATE KEY-----", "");
		    		    
		    privateKeyPEM = stripNewlineCharacters(privateKeyPEM);
		
			Decoder decoder = Base64.getDecoder();
			byte[] decoded = decoder.decode(privateKeyPEM);
			
			EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decoded);
			KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);		
			RSAPrivateKey privateKey = (RSAPrivateKey)keyFactory.generatePrivate(privateKeySpec);
				
			return privateKey;
		}
		
		catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RSAKeyException("Error getting RSA Private Key", e);
		}		

	}
	
	/***
	 * Strips newline characters. This will usually be \r\n in Windows and \n in UNIX
	 * 
	 * @param s - string to strip
	 * @return
	 */
	private static String stripNewlineCharacters(String s)
	{
		String result = s.replaceAll("\n", "");		 
	    result = result.replaceAll("\r", "");
	    return result;
	}
	
	

}
