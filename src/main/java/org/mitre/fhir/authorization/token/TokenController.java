package org.mitre.fhir.authorization.token;


import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/token")
public class TokenController {	
	@PostMapping(path = "/revoke")
	public void revoke(@RequestParam(name = "token", required = true) String tokenString, HttpServletRequest request)
	{
		TokenManager tokenManager = TokenManager.getInstance();
		try
		{
			tokenManager.revokeToken(tokenString);
		}
		catch (TokenNotFoundException tokenNotFoundException)
		{
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Token " + tokenString + " not found");

		}
	}

}
