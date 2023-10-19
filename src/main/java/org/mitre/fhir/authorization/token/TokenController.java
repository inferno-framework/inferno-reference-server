package org.mitre.fhir.authorization.token;

import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;
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
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
          "Token " + tokenString + " not found");

    }
  }

  @PostMapping(path = "/introspect", produces = { "application/json" })
  public String introspect(HttpServletRequest request) {
    // TODO: some kind of authorization?
    String tokenKey = request.getParameter("token");
    TokenManager tokenManager = TokenManager.getInstance();
    JSONObject tokenResponse = new JSONObject();
    tokenResponse.put("active", false);
    Token token = null;
    if (tokenKey != null && !tokenKey.isEmpty()) {
      try {
        token = tokenManager.getToken(tokenKey);
        tokenResponse.put("active", token.isActive());
        if (token.isActive()) {
          tokenResponse.put("scope", token.getScopesString());
        }

        if (token.getPatientId() != null) {
          tokenResponse.put("patient", token.getPatientId().toString());
        }

        if (token.getEncounterId() != null) {
          tokenResponse.put("encounter", token.getEncounterId().toString());
        }
      } catch (TokenNotFoundException tokenNotFoundException) {
        // This doesn't feel quit right, but am staying consistent
        tokenNotFoundException.printStackTrace();
      }
    }
    return tokenResponse.toString();
  }

}
