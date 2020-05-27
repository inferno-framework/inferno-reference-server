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
  /**
   * Service to revoke a token.
   * 
   * @param tokenString the token value of the token to be revoked
   * @param request the service request
   * @throws InactiveTokenException if token is already inactive
   */
  @PostMapping(path = "/revoke")
  public void revoke(@RequestParam(name = "token", required = true) String tokenString,
      HttpServletRequest request) throws InactiveTokenException {
    TokenManager tokenManager = TokenManager.getInstance();
    try {
      tokenManager.revokeToken(tokenString);
    } catch (TokenNotFoundException tokenNotFoundException) {
      tokenNotFoundException.printStackTrace();
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Token " + tokenString + " not found");

    }
  }

}
