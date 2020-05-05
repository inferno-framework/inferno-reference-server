package org.mitre.fhir.authorization.token;

import java.util.HashMap;
import java.util.Map;

public class TokenManager {
	
	private static TokenManager instance;
		
	private final Map<String, Token> tokenMap = new HashMap<>();
	private final Map<String, String> tokenToCorrespondingRefreshToken = new HashMap<>();
	private final Map<String, Token> refreshTokenMap = new HashMap<>();
	
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
		
		Token refreshToken = new Token();
		tokenToCorrespondingRefreshToken.put(token.getTokenValue(), refreshToken.getTokenValue());
		refreshTokenMap.put(refreshToken.getTokenValue(), refreshToken);
		
		return token;
	}
	
	public Token getCorrespondingRefreshToken(String tokenValue) throws TokenNotFoundException
	{
		//confirm we were passed a valid token value
		if (!tokenMap.containsKey(tokenValue))
		{
			throw new TokenNotFoundException(tokenValue);
		}
		
		String refreshTokenValue = tokenToCorrespondingRefreshToken.get(tokenValue);
		
		Token refreshToken = refreshTokenMap.get(refreshTokenValue);
		
		return refreshToken;
		
	}
	
	public void revokeToken(String tokenValue) throws TokenNotFoundException, InactiveTokenException
	{
		Token token = tokenMap.get(tokenValue);
		
		
		if (token != null)
		{
			if (token.isActive())
			{
				token.revokeToken();
				Token refreshToken = getCorrespondingRefreshToken(tokenValue); //revoke the refresh token
				refreshToken.revokeToken();
			}
			
			else
			{
				throw new InactiveTokenException(token);
			}
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
	
	public boolean authenticateRefreshToken(String refreshTokenValue) throws TokenNotFoundException
	{
		Token refreshToken = refreshTokenMap.get(refreshTokenValue);
		
		if (refreshToken != null)
		{
			return refreshToken.isActive();
		}

		throw new TokenNotFoundException(refreshTokenValue);
	}
	
	
	public void clearAllTokens()
	{
		tokenMap.clear();
		serverToken = null;
	}

	public Token getServerToken() {
		
		if (serverToken == null)
		{
			serverToken = createToken();
		}
		
		return serverToken;
	}
	
	

}
