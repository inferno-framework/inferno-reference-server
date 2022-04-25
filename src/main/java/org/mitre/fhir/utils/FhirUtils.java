package org.mitre.fhir.utils;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.mitre.fhir.authorization.token.Token;
import org.mitre.fhir.authorization.token.TokenManager;

public class FhirUtils {

  public static List<BundleEntryComponent> getAllPatients(IGenericClient client) {
    return getAllResources(client, "Patient");
  }

  public static Bundle getPatientsBundle(IGenericClient client) {
    return getAllResourcesBundle(client, "Patient");
  }

  public static List<BundleEntryComponent> getAllEncounters(IGenericClient client) {
    return getAllResources(client, "Encounter");
  }

  public static Bundle getEncountersBundle(IGenericClient client) {
    return getAllResourcesBundle(client, "Encounter");
  }

  public static List<BundleEntryComponent> getAllGroups(IGenericClient client) {
    return getAllResources(client, "Group");
  }

  public static Bundle getGroupsBundle(IGenericClient client) {
    return getAllResourcesBundle(client, "Group");
  }
  
  public static List<BundleEntryComponent> getAllOrganizations(IGenericClient client) {
    return getAllResources(client, "Organization");
  }

  public static Bundle getOrganzationsBundle(IGenericClient client) {
    return getAllResourcesBundle(client, "Organization");
  }
  
  private static List<BundleEntryComponent> getAllResources(IGenericClient client,
      String resourceName) {

    Bundle bundle = getAllResourcesBundle(client, resourceName);

    List<BundleEntryComponent> resources = new ArrayList<BundleEntryComponent>();

    while (bundle != null) {
      resources.addAll(bundle.getEntry());

      if (bundle.getLink(Bundle.LINK_NEXT) != null) {
        bundle = client.loadPage().next(bundle).execute();
      } else {
        bundle = null;
      }
    }

    return resources;

  }

  private static Bundle getAllResourcesBundle(IGenericClient client, String resourceName) {
    CacheControlDirective cacheControlDirective = new CacheControlDirective();
    cacheControlDirective.setNoCache(true);

    Token token = TokenManager.getInstance().getServerToken();

    Bundle bundle = client.search().forResource(resourceName).returnBundle(Bundle.class).count(1000)
        .cacheControl(cacheControlDirective)
        .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(token.getTokenValue()))
        .execute();

    return bundle;
  }

}
