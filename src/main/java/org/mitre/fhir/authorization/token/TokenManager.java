
package org.mitre.fhir.authorization.token;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mitre.fhir.utils.FhirReferenceServerUtils;

public class TokenManager {

  private static final String SKIP_TOKEN_AUTHENTICATION = "SKIP_TOKEN_AUTHENTICATION";

  private static TokenManager instance;

  private final Map<String, Token> tokenMap = new HashMap<>();
  private final Map<String, String> tokenToCorrespondingRefreshToken = new HashMap<>();
  private final Map<String, Token> refreshTokenMap = new HashMap<>();

  private Token serverToken;

  private TokenManager() {

  }

  /**
   * Gets instance of the TokenManager singleton.
   * 
   * @return
   */
  public static TokenManager getInstance() {
    if (instance == null) {
      instance = new TokenManager();
    }

    return instance;
  }

  /**
   * Creates a token.
   * 
   * @return the created token
   */
  public Token createToken(String scopesString) {
    List<String> scopes = FhirReferenceServerUtils.getScopesListByScopeString(scopesString);
    return createToken(scopes);
  }

  /**
   * Creates a token.
   * 
   * @return the created token
   */
  public Token createToken(List<String> scopes) {
    Token token = new Token(scopes);
    tokenMap.put(token.getTokenValue(), token);

    Token refreshToken = new Token(scopes);
    tokenToCorrespondingRefreshToken.put(token.getTokenValue(), refreshToken.getTokenValue());
    refreshTokenMap.put(refreshToken.getTokenValue(), refreshToken);

    return token;
  }

  /**
   * Gets a Token based on the token value.
   * 
   * @param tokenValue the token's key value
   * @return the corresponding token
   * @throws TokenNotFoundException if no token with tokenValue exists
   */
  public Token getToken(String tokenValue) throws TokenNotFoundException {
    // confirm we were passed a valid token value
    if (!tokenMap.containsKey(tokenValue)) {
      throw new TokenNotFoundException(tokenValue);
    }

    return tokenMap.get(tokenValue);
  }

  /**
   * Get the corresponding Token for a refresh token.
   * 
   * @param refreshTokenValue the refresh token's key value
   * @return the corresponding refresh token
   * @throws TokenNotFoundException if no refresh token with refreshTokenValue exists
   */
  public Token getRefreshToken(String refreshTokenValue) throws TokenNotFoundException {
    // confirm we were passed a valid token value
    if (!refreshTokenMap.containsKey(refreshTokenValue)) {
      throw new TokenNotFoundException(refreshTokenValue);
    }

    return refreshTokenMap.get(refreshTokenValue);

  }

  /**
   * Get the refresh token corresponding to a token.
   * 
   * @param tokenValue the relevant token value
   * 
   * @return refresh token corresponding to the provided token.
   * @throws TokenNotFoundException if the provided token value is not found.
   */
  public Token getCorrespondingRefreshToken(String tokenValue) throws TokenNotFoundException {
    // confirm we were passed a valid token value
    if (!tokenMap.containsKey(tokenValue)) {
      throw new TokenNotFoundException(tokenValue);
    }

    String refreshTokenValue = tokenToCorrespondingRefreshToken.get(tokenValue);

    Token refreshToken = refreshTokenMap.get(refreshTokenValue);

    return refreshToken;

  }

  /**
   * revokes a token.
   * 
   * @param tokenValue the token to be revoked.
   * @throws TokenNotFoundException if the provided token value is not found.
   * @throws InactiveTokenException the token has already been revoked.
   */
  public void revokeToken(String tokenValue) throws TokenNotFoundException, InactiveTokenException {

    Token token = tokenMap.get(tokenValue);

    if (token != null) {
      if (token.isActive()) {
        token.revokeToken();
        // revoke the refresh token
        Token refreshToken = getCorrespondingRefreshToken(tokenValue);
        refreshToken.revokeToken();
      } else {
        throw new InactiveTokenException(token);
      }
    } else {
      throw new TokenNotFoundException(tokenValue);
    }
  }

  /**
   * authenticates if the token is active.
   * 
   * @param tokenValue the token to authenticate
   * @return it token is active
   * @throws TokenNotFoundException if the supplied token is not found.
   */
  public boolean authenticateToken(String tokenValue) throws TokenNotFoundException {

    Token token = tokenMap.get(tokenValue);

    if (token != null) {
      return token.isActive();
    }

    throw new TokenNotFoundException(tokenValue);
  }

  /**
   * Authenticates a refresh token.
   * 
   * @param refreshTokenValue the refresh token to be authenticated
   * @return boolean indicating whether the refresh token is active
   * @throws TokenNotFoundException if the provided token is not found
   */
  public boolean authenticateRefreshToken(String refreshTokenValue) throws TokenNotFoundException {
    Token refreshToken = refreshTokenMap.get(refreshTokenValue);

    if (refreshToken != null) {
      return refreshToken.isActive();
    }

    throw new TokenNotFoundException(refreshTokenValue);
  }

  public void clearAllTokens() {
    tokenMap.clear();
    serverToken = null;
  }

  /**
   * Gets a preadded token for calls in the java code.
   * 
   * @return a Token
   */
  public Token getServerToken() {

    if (serverToken == null) {
      serverToken = createToken(FhirReferenceServerUtils.DEFAULT_SCOPE);
    }

    return serverToken;
  }

  /** 
   * Determine if the SKIP_TOKEN_AUTHENTICATION environment variable is set.
   * 
   * @return if the environment variable is set
   */
  public boolean shouldSkipTokenAuthentication() {
    String disableTokenAuthString = System.getenv().get(SKIP_TOKEN_AUTHENTICATION);

    boolean disableTokenAuth = "true".equals(disableTokenAuthString);

    return disableTokenAuth;
  }

}
