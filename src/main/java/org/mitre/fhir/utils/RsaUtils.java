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

public class RsaUtils {

  private static final String RSA_PUBLIC_KEY_RESOURCE_PATH = "/rsa_key.pub";
  private static final String RSA_PRIVATE_KEY_RESOURCE_PATH = "/rsa_key.key";
  private static final String ALGORITHM = "RSA";

  /**
   * Returns the RSA public key.
   * @return the RSA public key
   * @throws RSAKeyException error with generating the public key
   */
  public static RSAPublicKey getRsaPublicKey() throws RSAKeyException {
    return getRsaPublicKey(RSA_PUBLIC_KEY_RESOURCE_PATH);
  }

  /**
   * Returns the RSA public key from the provided file.
   * @param publicKeyResourcePath the location of the public key
   * @return the RSA public key
   * @throws RSAKeyException  error with generating the public key
   */
  public static RSAPublicKey getRsaPublicKey(String publicKeyResourcePath) throws RSAKeyException {
    byte[] publicBytes;
    try {

      InputStream in = RsaUtils.class.getResourceAsStream(publicKeyResourcePath);

      publicBytes = in.readAllBytes();

      String temp = new String(publicBytes);
      String publicKeyPem = temp.replace("-----BEGIN RSA PUBLIC KEY-----", "");
      publicKeyPem = publicKeyPem.replace("-----END RSA PUBLIC KEY-----", "");

      publicKeyPem = stripNewlineCharacters(publicKeyPem);

      Decoder decoder = Base64.getDecoder();
      byte[] decoded = decoder.decode(publicKeyPem);

      EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decoded);
      KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
      RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

      return publicKey;
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RSAKeyException("Error getting RSA Public Key", e);
    }
  }

  /**
   * Returns the RSA private key.
   * @return the RSA private key
   * @throws RSAKeyException error with generating the private key
   */
  public static RSAPrivateKey getRsaPrivateKey() throws RSAKeyException {
    return getRsaPrivateKey(RSA_PRIVATE_KEY_RESOURCE_PATH);
  }

  /**
   * Returns the RSA private key from the provided file.
   * @param privateKeyResourcePath the location of the private key
   * @return the RSA private key
   * @throws RSAKeyException error with generating the private key
   */
  public static RSAPrivateKey getRsaPrivateKey(String privateKeyResourcePath)
      throws RSAKeyException {
    byte[] privateBytes;
    try {

      InputStream in = RsaUtils.class.getResourceAsStream(privateKeyResourcePath);

      privateBytes = in.readAllBytes();

      String temp = new String(privateBytes);
      String privateKeyPem = temp.replace("-----BEGIN RSA PRIVATE KEY-----", "");
      privateKeyPem = privateKeyPem.replace("-----END RSA PRIVATE KEY-----", "");

      privateKeyPem = stripNewlineCharacters(privateKeyPem);

      Decoder decoder = Base64.getDecoder();
      byte[] decoded = decoder.decode(privateKeyPem);

      EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decoded);
      KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
      RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);

      return privateKey;
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RSAKeyException("Error getting RSA Private Key", e);
    }

  }

  /**
   * Strips newline characters. This will usually be \r\n in Windows and \n in UNIX
   *
   * @param s - string to strip
   * @return
   */
  private static String stripNewlineCharacters(String s) {
    String result = s.replaceAll("\n", "");
    result = result.replaceAll("\r", "");
    return result;
  }


}
