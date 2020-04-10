package org.mitre.fhir.authorization;

import org.junit.Assert;
import org.junit.Test;
import org.mitre.fhir.utils.FhirReferenceServerUtils;


public class TestFhirReferenceServerUtils {
	
	
	@Test
	public void testGetServerBaseUrlWithNestedUrl()
	{
		String baseUrl = FhirReferenceServerUtils.getServerBaseUrl();
		Assert.assertEquals("http://localhost:1234", baseUrl);
	}

}
