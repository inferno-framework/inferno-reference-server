package org.mitre.fhir.authorization.token;

import com.github.stefanbirkner.systemlambda.SystemLambda;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestTokenManager {

  @Before
  public void before() {
    TokenManager tokenManager = TokenManager.getInstance();
    tokenManager.clearAllTokens();
  }

  @After
  public void after() {
    TokenManager tokenManager = TokenManager.getInstance();
    tokenManager.clearAllTokens();
  }

  @Test
  public void testSkipTokenAuthenticationEnvironmentVariable() throws Exception {
    boolean auth = SystemLambda.withEnvironmentVariable("SKIP_TOKEN_AUTHENTICATION", "true")
        .execute(() -> TokenManager.getInstance().authenticateToken(null));
    Assert.assertTrue(auth);
  }
  


  @Test
  public void testCreateToken() throws TokenNotFoundException {
    TokenManager tokenManager = TokenManager.getInstance();
    Token token = tokenManager.createToken("");

    Assert.assertTrue(tokenManager.authenticateToken(token.getTokenValue()));
    
    Assert.assertNotNull(tokenManager.getToken(token.getTokenValue()));

    Token refreshToken = tokenManager.getCorrespondingRefreshToken(token.getTokenValue());
    
    Assert.assertNotNull(tokenManager.getRefreshToken(refreshToken.getTokenValue()));

    Assert.assertTrue(tokenManager.authenticateRefreshToken(refreshToken.getTokenValue()));
  }

  @Test(expected = TokenNotFoundException.class)
  public void testTokenNotFound() throws TokenNotFoundException {
    TokenManager tokenManager = TokenManager.getInstance();
    tokenManager.authenticateToken("INVALID_VALUE");

    tokenManager.authenticateRefreshToken("INVALID_VALUE");
  }

  @Test(expected = TokenNotFoundException.class)
  public void testRefreshTokenNotFound() throws TokenNotFoundException {
    TokenManager tokenManager = TokenManager.getInstance();

    tokenManager.authenticateRefreshToken("INVALID_VALUE");
  }

  @Test
  public void testRevokeToken() throws TokenNotFoundException, InactiveTokenException {
    TokenManager tokenManager = TokenManager.getInstance();
    Token token = tokenManager.createToken("");

    Token refreshToken = tokenManager.getCorrespondingRefreshToken(token.getTokenValue());

    Assert.assertTrue(tokenManager.authenticateToken(token.getTokenValue()));
    Assert.assertTrue(tokenManager.authenticateRefreshToken(refreshToken.getTokenValue()));

    tokenManager.revokeToken(token.getTokenValue());

    // should fail because token was revoked
    Assert.assertFalse(tokenManager.authenticateToken(token.getTokenValue()));
    Assert.assertFalse(tokenManager.authenticateRefreshToken(refreshToken.getTokenValue()));
  }

  @Test(expected = TokenNotFoundException.class)
  public void testRevokeTokenWithInvalidToken()
      throws TokenNotFoundException, InactiveTokenException {
    TokenManager tokenManager = TokenManager.getInstance();

    tokenManager.revokeToken("INVALID_TOKEN");
  }

  @Test(expected = InactiveTokenException.class)
  public void testRevokingInactiveToken() throws TokenNotFoundException, InactiveTokenException {
    TokenManager tokenManager = TokenManager.getInstance();
    Token token = tokenManager.createToken("");

    tokenManager.revokeToken(token.getTokenValue());
    // token should be revoked, so inactive
    tokenManager.revokeToken(token.getTokenValue());

  }

}
