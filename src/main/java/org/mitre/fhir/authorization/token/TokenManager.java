package org.mitre.fhir.authorization.token;

import java.util.HashMap;
import java.util.Map;

public class TokenManager {
	
	private static TokenManager instance;
		
	private Map<String, Token> tokenMap = new HashMap<String, Token>();
	
	private Token serverToken;
	
	private TokenManager()
	{
		
	}
	
	public static TokenManager getInstance()
	{
		if (instance == null)
		{
			instance = new TokenManager();
		}
		
		return instance;
	}
	
	public Token createToken()
	{
		Token token = new Token();
		tokenMap.put(token.getTokenValue(), token);
		return token;
	}
	
	public void revokeToken(String tokenValue) throws TokenNotFoundException
	{
		Token token = tokenMap.get(tokenValue);
		
		if (token != null)
		{
			token.revokeToken();
		}
		
		else
		{
			throw new TokenNotFoundException(tokenValue);
		}
	}
	
	public boolean authenticateToken(String tokenValue) throws TokenNotFoundException
	{
		Token token = tokenMap.get(tokenValue);
		
		if (token != null)
		{
			return token.isActive();
		}
		
		throw new TokenNotFoundException(tokenValue);


	}
	
	
	public void clearAllTokens()
	{
		tokenMap.clear();
	}

	public Token getServerToken() {
		if (serverToken == null)
		{
			serverToken = createToken();
		}
		
		return serverToken;
	}
	
	

}
