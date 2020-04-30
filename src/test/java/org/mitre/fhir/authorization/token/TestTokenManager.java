package org.mitre.fhir.authorization.token;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mitre.fhir.authorization.token.Token;
import org.mitre.fhir.authorization.token.TokenManager;
import org.mitre.fhir.authorization.token.TokenNotFoundException;

public class TestTokenManager {

	@Before
	public void before() {
		TokenManager tokenManager = TokenManager.getInstance();
		tokenManager.clearAllTokens();
	}
	
	@After
	public void after()
	{
		TokenManager tokenManager = TokenManager.getInstance();
		tokenManager.clearAllTokens();
	}
	
	@Test
	public void testCreateToken() throws TokenNotFoundException
	{
		TokenManager tokenManager = TokenManager.getInstance();
		Token token = tokenManager.createToken();
		
		Assert.assertTrue(tokenManager.authenticateToken(token.getTokenValue()));
	}
	
	@Test(expected = TokenNotFoundException.class)
	public void testTokenNotFound() throws TokenNotFoundException
	{
		TokenManager tokenManager = TokenManager.getInstance();
		tokenManager.authenticateToken("INVALID_VALUE");
	}
	
	@Test
	public void testRevokeToken() throws TokenNotFoundException, InactiveTokenException
	{
		TokenManager tokenManager = TokenManager.getInstance();
		Token token = tokenManager.createToken();
		
		Assert.assertTrue(tokenManager.authenticateToken(token.getTokenValue()));
		
		tokenManager.revokeToken(token.getTokenValue());
		
		Assert.assertFalse(tokenManager.authenticateToken(token.getTokenValue()));	//should fail because token was revoked
		
	}
	
}
