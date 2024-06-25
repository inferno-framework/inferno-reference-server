package org.mitre.fhir.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import org.hl7.fhir.r4.model.IdType;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestScope {

  /**
   * Make sure the Scope class has some set of search params registered before running these tests.
   */
  @BeforeClass
  public static void setup() {
    Scope.registerSearchParams(Map.of("Condition",
        Set.of("code", "identifier", "patient", "abatement-date", "asserter", "body-site",
            "category", "clinical-status", "encounter", "evidence", "evidence-detail", "onset-date",
            "recorded-date", "severity", "stage", "subject", "verification-status",
            "asserted-date")));
  }
  
  @Test
  public void testScopeParsing() {
    Scope s = Scope.fromString("patient/Observation.rs");
    assertEquals("Observation", s.resourceType);
    assertTrue(s.read);
    assertTrue(s.search);
    assertFalse(s.create);
    assertFalse(s.update);
    assertFalse(s.delete);
    assertNull(s.parameters);
    
    s = Scope.fromString("user/*.cruds");
    assertTrue(s.isWildcardResource);
    assertTrue(s.create);
    assertTrue(s.read);
    assertTrue(s.update);
    assertTrue(s.delete);
    assertTrue(s.search);
    assertNull(s.parameters);
    
   
    s = Scope.fromString("patient/Condition.rs?category=http://hl7.org/fhir/us/core/CodeSystem/condition-category|health-concern");
    assertEquals("Condition", s.resourceType);
    assertTrue(s.read);
    assertTrue(s.search);
    assertFalse(s.create);
    assertFalse(s.update);
    assertFalse(s.delete);
    assertNotNull(s.parameters);
    assertEquals(1, s.parameters.size());
    assertEquals("http://hl7.org/fhir/us/core/CodeSystem/condition-category|health-concern", s.parameters.get("category").get(0));
  }

  @Test
  public void testScopeIsValid() {   
    Scope s = new Scope("patient/Observation.rs");
    assertTrue(s.isValid());

    s = new Scope("patient/Condition.rs?category=http://hl7.org/fhir/us/core/CodeSystem/condition-category|health-concern");
    assertTrue(s.isValid());

    s = new Scope("patient/Condition.rs?not_a_real_param=1234");
    assertFalse(s.isValid());
  }
  
  @Test
  public void testScopeApply() {
    RequestDetails requestDetails = new SystemRequestDetails();
    requestDetails.setRestOperationType(RestOperationTypeEnum.READ);
    requestDetails.setResourceName("Observation");
    requestDetails.setId(new IdType("obs123"));
    // the scope applies and allows access to a read of the relevant resource type
    Scope s = Scope.fromString("patient/Observation.rs");
    assertTrue(s.apply(requestDetails, null, null, false));

    // does not apply to a different resource type
    requestDetails.setResourceName("Patient");
    assertFalse(s.apply(requestDetails, null, null, false));

    // does not apply to a different operation
    requestDetails.setResourceName("Observation");
    requestDetails.setRestOperationType(RestOperationTypeEnum.DELETE);
    assertFalse(s.apply(requestDetails, null, null, false));


    s = Scope.fromString("user/*.cruds");
    // wildcard should allow everything
    requestDetails.setResourceName("Patient");
    requestDetails.setRestOperationType(RestOperationTypeEnum.READ);
    assertTrue(s.apply(requestDetails, null, null, false));
    
    requestDetails.setResourceName("Observation");
    requestDetails.setRestOperationType(RestOperationTypeEnum.SEARCH_TYPE);
    assertTrue(s.apply(requestDetails, null, null, false));
    
    requestDetails.setResourceName("Condition");
    requestDetails.setRestOperationType(RestOperationTypeEnum.UPDATE);
    assertTrue(s.apply(requestDetails, null, null, false));
    
    requestDetails.setResourceName("DiagnosticReport");
    requestDetails.setRestOperationType(RestOperationTypeEnum.CREATE);
    assertTrue(s.apply(requestDetails, null, null, false));


    s = Scope.fromString("patient/Condition.rs?category=http://hl7.org/fhir/us/core/CodeSystem/condition-category|health-concern");

    // does not apply to a different resource type
    requestDetails.setResourceName("Patient");
    assertFalse(s.apply(requestDetails, null, null, false));

    // does not apply to a different operation
    requestDetails.setResourceName("Condition");
    requestDetails.setRestOperationType(RestOperationTypeEnum.DELETE);
    assertFalse(s.apply(requestDetails, null, null, false));

    // does apply to the correct resource type
    // for SEARCH the scope params should be added
    requestDetails.setRestOperationType(RestOperationTypeEnum.SEARCH_TYPE);
    Map<String, String> parametersToAdd = new HashMap<>();
    assertTrue(s.apply(requestDetails, parametersToAdd, null, false));

    assertTrue(parametersToAdd.containsKey("category"));
    assertEquals("http://hl7.org/fhir/us/core/CodeSystem/condition-category|health-concern", parametersToAdd.get("category"));

    // for READ the access predicate should be called
    requestDetails.setRestOperationType(RestOperationTypeEnum.READ);
    BiPredicate<String, String> canAccessAllResources = (resourceType, resourceID) -> true;
    assertTrue(s.apply(requestDetails, null, canAccessAllResources, false));

    BiPredicate<String, String> canAccessNoResources = (resourceType, resourceID) -> false;
    assertFalse(s.apply(requestDetails, null, canAccessNoResources, false));


    // test multiple scopes applying on top of each other
    s = Scope.fromString("patient/Condition.rs?category=health-concern");
    requestDetails.setRestOperationType(RestOperationTypeEnum.SEARCH_TYPE);
    parametersToAdd.clear();

    assertTrue(s.apply(requestDetails, parametersToAdd, null, false));
    assertTrue(parametersToAdd.containsKey("category"));
    assertEquals("health-concern", parametersToAdd.get("category"));

    s = Scope.fromString("patient/Condition.rs?category=encounter-diagnosis");

    assertTrue(s.apply(requestDetails, parametersToAdd, null, false));
    assertTrue(parametersToAdd.containsKey("category"));
    assertEquals("health-concern,encounter-diagnosis", parametersToAdd.get("category"));
  }
}
