package org.mitre.fhir.authorization;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Patient;
import org.json.JSONObject;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.mitre.fhir.utils.RSAUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.github.dnault.xmlpatch.internal.Log;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

@RestController
public class AuthorizationController {
	
	@PostConstruct
	protected void postConstruct() {
		Log.info("Authorization Controller added");
	}

	/**
	 * Provide a code to get a bearer token for authorization
	 * 
	 * @param code
	 * @return bearer token to be used for authorization
	 */
	@PostMapping(path = "/token", produces = { "application/json" })
	public ResponseEntity<String> getToken(@RequestParam(name = "code", required = false) String code,
			@RequestParam(name = "refresh_token", required = false) String refreshToken,
			@RequestParam(name = "client_id", required = false) String clientId, HttpServletRequest request) {

		Log.info("code is " + code);
		
		String actualCode = null;
		String scopes = "";
		if (code != null)
		{
			//the provided code is actualcode.scopes
			String[] codeAndScopes = code.split("\\."); 
			actualCode = codeAndScopes[0];
			
			//if scope was included*/
			if (codeAndScopes.length >= 2)
			{
				String encodedScopes = codeAndScopes[1];
			    scopes = new String(Base64.getDecoder().decode(encodedScopes));
			}
		}
		
		// if refresh token is provided, then service will return refreshed token
		if (FhirReferenceServerUtils.SAMPLE_REFRESH_TOKEN.equals(refreshToken)) {
			// confirm client id is correct
			if (!FhirReferenceServerUtils.SAMPLE_CLIENT_ID.equals(clientId)) {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid client id");
			}
			return generateBearerTokenResponse(request, scopes);
		}

		// if a code is passed in, return token
		if (FhirReferenceServerUtils.SAMPLE_CODE.equals(actualCode)) {
			return generateBearerTokenResponse(request, scopes);
		}

		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid code");
	}

	private ResponseEntity<String> generateBearerTokenResponse(HttpServletRequest request, String scopes) {
		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl(CacheControl.noStore());
		headers.setPragma("no-cache");

		String tokenJSONString = generateBearerToken(request, scopes);
		ResponseEntity<String> responseEntity = new ResponseEntity<String>(tokenJSONString, headers, HttpStatus.OK);

		return responseEntity;
	}

	/**
	 * Generates Token in Oauth2 expected format
	 * 
	 * @return token JSON String
	 */
	private String generateBearerToken(HttpServletRequest request, String scopes) {

		String fhirServerBaseUrl = FhirReferenceServerUtils.getServerBaseUrl(request) + FhirReferenceServerUtils.FHIR_SERVER_PATH;

		FhirContext fhirContext = FhirContext.forR4();
		IGenericClient client = fhirContext.newRestfulGenericClient(fhirServerBaseUrl);

		// get the first patient in the db
		Bundle patientsBundle = client.search().forResource(Patient.class).returnBundle(Bundle.class)
				.withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
						FhirReferenceServerUtils.AUTHORIZATION_HEADER_VALUE)
				.execute();
		List<BundleEntryComponent> patients = patientsBundle.getEntry();
		Patient patient = null;
		for (BundleEntryComponent bundleEntryComponent : patients) {
			if (bundleEntryComponent.getResource().fhirType().equals("Patient")) {
				patient = (Patient) bundleEntryComponent.getResource();
				break;
			}
		}

		if (patient == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No patients found");
		}

		// get their id
		String patientId = patient.getIdElement().getIdPart();
		
		JSONObject tokenJSON = new JSONObject();
		
		tokenJSON.put("access_token", FhirReferenceServerUtils.SAMPLE_ACCESS_TOKEN);
		tokenJSON.put("token_type", "bearer");
		tokenJSON.put("expires_in", 3600);
		tokenJSON.put("refresh_token", FhirReferenceServerUtils.SAMPLE_REFRESH_TOKEN);
		tokenJSON.put("scope", scopes);
		tokenJSON.put("patient", patientId);
		tokenJSON.put("id_token", generateSampleOpenIdToken(request, patient));
		
		return tokenJSON.toString();
		
		

	}

	/**
	 * Generates a sample open id token
	 * https://openid.net/specs/openid-connect-core-1_0.html
	 * 
	 * @return token JSON String representing the open id token
	 */
	private String generateSampleOpenIdToken(HttpServletRequest request, Patient patient) {
		RSAPublicKey publicKey = RSAUtils.getRSAPublicKey();
		RSAPrivateKey privateKey = RSAUtils.getRSAPrivateKey();

		String patientId = patient.getIdElement().getIdPart();
		
		//for now hard coding as a Patient http://hl7.org/fhir/smart-app-launch/worked_example_id_token/index.html#Encode-them-in-a-JWT
		String fhirUserURL = FhirReferenceServerUtils.getFhirServerBaseUrl(request) + "/Patient/" + patientId;
	
		Algorithm algorithm = Algorithm.RSA256(publicKey, privateKey);
		String token = JWT.create().withIssuer(FhirReferenceServerUtils.getFhirServerBaseUrl(request))
				.withAudience(FhirReferenceServerUtils.SAMPLE_CLIENT_ID).withClaim("fhirUser", fhirUserURL)
				.sign(algorithm);

		return token;
	}

}
