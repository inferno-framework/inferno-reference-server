package org.mitre.fhir.token;

import java.util.UUID;

public class Token {
	
	private boolean active = true;
	
	private String tokenValue;
	
	public Token()
	{
		UUID uuid = UUID.randomUUID();
		this.tokenValue = uuid.toString();
	}
	
	public void revokeToken()
	{
		active = false;
	}
	
	public boolean isActive()
	{
		return active;
	}
	
	public String getTokenValue()
	{
		return tokenValue;
	}
	
	

}
