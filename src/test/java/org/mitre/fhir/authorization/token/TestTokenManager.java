package org.mitre.fhir.authorization.token;

import java.lang.reflect.Field;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mitre.fhir.authorization.exception.InvalidBearerTokenException;
import uk.org.webcompere.systemstubs.SystemStubs;

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
  public void testCreateToken() throws TokenNotFoundException {
    TokenManager tokenManager = TokenManager.getInstance();
    Token token = tokenManager.createToken("");

    Assert.assertTrue(tokenManager.authenticateBearerToken(token.getTokenValue()));

    Assert.assertNotNull(tokenManager.getToken(token.getTokenValue()));

    Token refreshToken = tokenManager.getCorrespondingRefreshToken(token.getTokenValue());

    Assert.assertNotNull(tokenManager.getRefreshToken(refreshToken.getTokenValue()));

    Assert.assertTrue(tokenManager.authenticateRefreshToken(refreshToken.getTokenValue()));
  }

  @Test(expected = TokenNotFoundException.class)
  public void testTokenNotFound() throws TokenNotFoundException {
    TokenManager tokenManager = TokenManager.getInstance();
    tokenManager.authenticateBearerToken("INVALID_VALUE");

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

    Assert.assertTrue(tokenManager.authenticateBearerToken(token.getTokenValue()));
    Assert.assertTrue(tokenManager.authenticateRefreshToken(refreshToken.getTokenValue()));

    tokenManager.revokeToken(token.getTokenValue());

    // should fail because token was revoked
    Assert.assertThrows(InvalidBearerTokenException.class, () -> {
      tokenManager.authenticateBearerToken(token.getTokenValue());
    });

    Assert.assertThrows(InvalidBearerTokenException.class, () -> {
      tokenManager.authenticateRefreshToken(refreshToken.getTokenValue());
    });
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

  @Test
  public void testCustomBearerToken() throws Exception {
    /*
     * Unfortunately due to it being a singleton, TokenManager if used previously will have an
     * existing instance in memory, thus it won't read any env variable added after the first time
     * getInstance() is called. Therefore, this test needs to use reflect to set the static field
     * "instance" back to null
     */
    Field singletonInstanceField = TokenManager.class.getDeclaredField("instance");
    singletonInstanceField.setAccessible(true);
    singletonInstanceField.set(null, null); // set the static variable to null

    String customTokenValue = "MY-CUSTOM-BEARER-TOKEN";

    SystemStubs.withEnvironmentVariable("CUSTOM_BEARER_TOKEN", customTokenValue)
        .execute(() -> Assert
            .assertTrue(TokenManager.getInstance().authenticateBearerToken(customTokenValue)));

  }
}
