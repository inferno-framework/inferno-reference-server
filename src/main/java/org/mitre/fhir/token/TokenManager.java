package org.mitre.fhir.token;

import java.util.HashMap;
import java.util.Map;

public class TokenManager {
	
	private static Map<String, Token> tokenMap = new HashMap<String, Token>();
	
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
		
		throw new TokenNotFoundException(tokenValue);
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
	
	

}
