package org.mitre.fhir.bulk;

import java.util.Iterator;
import ca.uhn.fhir.jpa.bulk.export.job.GroupBulkItemReader;
import ca.uhn.fhir.rest.api.server.storage.ResourcePersistentId;

public class InfernoGroupBulkItemReader extends GroupBulkItemReader {

  @Override
  protected Iterator<ResourcePersistentId> getResourcePidIterator() {
    
    //check for special case resources
    
    //else do normal behaviour
    Iterator<ResourcePersistentId> iterator = super.getResourcePidIterator();

    
    return iterator;
  }
}
