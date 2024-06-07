package org.mitre.fhir.authorization;

import org.junit.Assert;
import org.junit.Test;
import org.mitre.fhir.authorization.FakeOauth2AuthorizationInterceptorAdaptor.Scope;

public class ScopeTests {

  @Test
  public void testScopeParsing() {
    Scope s = Scope.fromString("patient/Observation.rs");
    
    Assert.assertEquals("Observation", s.resourceType);
    
    Assert.assertTrue(s.read);
    Assert.assertTrue(s.search);
    
    Assert.assertFalse(s.create);
    Assert.assertFalse(s.update);
    Assert.assertFalse(s.delete);
    
    Assert.assertNull(s.parameters);
    
    
    s = Scope.fromString("user/*.cruds");
    Assert.assertTrue(s.isWildcardResource);
    
    Assert.assertTrue(s.create);
    Assert.assertTrue(s.read);
    Assert.assertTrue(s.update);
    Assert.assertTrue(s.delete);
    Assert.assertTrue(s.search);
    
    Assert.assertNull(s.parameters);
    
    
    s = Scope.fromString("patient/Condition.rs?category=http://hl7.org/fhir/us/core/CodeSystem/condition-category|health-concern");
    
    Assert.assertEquals("Condition", s.resourceType);
    
    Assert.assertTrue(s.read);
    Assert.assertTrue(s.search);
    
    Assert.assertFalse(s.create);
    Assert.assertFalse(s.update);
    Assert.assertFalse(s.delete);
    
    Assert.assertNotNull(s.parameters);
    Assert.assertEquals(1, s.parameters.size());
    Assert.assertEquals("http://hl7.org/fhir/us/core/CodeSystem/condition-category|health-concern", s.parameters.get("category").get(0));
  }
  
}
