package org.mitre.fhir.bulk;

import ca.uhn.fhir.jpa.bulk.export.job.GroupBulkItemReader;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.storage.ResourcePersistentId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class InfernoGroupBulkItemReader extends GroupBulkItemReader {

  private String[] resourcesWithoutPatientCompartment =
      {"Organization", "Practitioner", "Location", "Medication"};

  @Override
  protected Iterator<ResourcePersistentId> getResourcePidIterator() {
    // check for special case resources
    if (isResourceWithoutPatientCompartment(myResourceType)) {
      // return getAllResourceIds(myResourceType).iterator();
      return getMembers(myResourceType).iterator();
    } else {
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

  private List<ResourcePersistentId> getMembers(String resourceName) {
    SystemRequestDetails requestDetails = SystemRequestDetails.newSystemRequestAllPartitions();
    Set<ResourcePersistentId> ids = myDaoRegistry.getResourceDao(resourceName)
        .searchForIds(new SearchParameterMap(), requestDetails);

    List<ResourcePersistentId> list = new ArrayList<>();
    for (ResourcePersistentId id : ids) {
      list.add(id);
    }

    return list;

  }

}
