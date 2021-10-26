package org.mitre.fhir.bulk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import ca.uhn.fhir.jpa.bulk.export.job.GroupBulkItemReader;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.storage.ResourcePersistentId;


public class InfernoGroupBulkItemReader extends GroupBulkItemReader {

  private String[] resourcesWithoutPatientCompartment =
      {"Organization", "Practitioner", "Location", "Medication"};

  @Override
  protected Iterator<ResourcePersistentId> getResourcePidIterator() {
    // check for special case resources
    if (isResourceWithoutPatientCompartment(myResourceType)) {
      return getAllResourceIds(myResourceType).iterator();
    }

    else {
      // else do normal behavior
      return super.getResourcePidIterator();
    }
  }

  private boolean isResourceWithoutPatientCompartment(String resourceName) {
    for (String resourceWithoutPatientCompartmentName : resourcesWithoutPatientCompartment) {
      if (resourceWithoutPatientCompartmentName.equals(resourceName)) {
        return true;
      }
    }
    return false;
  }

  private List<ResourcePersistentId> getAllResourceIds(String resourceName) {

    IBundleProvider provider =
        myDaoRegistry.getResourceDao(resourceName).search(new SearchParameterMap());

    // TODO: if run immediately after another transaction, sometimes fails
    List<IBaseResource> resources = provider.getAllResources();

    List<ResourcePersistentId> resourceIds = new ArrayList<>();
    for (IBaseResource baseResource : resources) {
      resourceIds.add(new ResourcePersistentId(baseResource.getIdElement().getIdPartAsLong()));
    }

    return resourceIds;
  }
}
