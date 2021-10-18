package org.mitre.fhir.bulk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.svc.ISearchCoordinatorSvc;
import ca.uhn.fhir.jpa.bulk.export.job.GroupBulkItemReader;
import ca.uhn.fhir.jpa.dao.ISearchBuilder;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.storage.ResourcePersistentId;


public class InfernoGroupBulkItemReader extends GroupBulkItemReader {
  
  @Autowired
  private ISearchCoordinatorSvc mySearchCoordinatorSvc;
  
  private String[] resourcesWithoutPatientCompartment = {
      "Organization",
      "Practitioner",
      "Location",
      "Medication"
  };
   
  @Override
  protected Iterator<ResourcePersistentId> getResourcePidIterator() {


    // check for special case resources
    if (isResourceWithoutPatientCompartment(myResourceType)) {
      return getAllResourceIds(myResourceType).iterator();
      //return new ArrayList<ResourcePersistentId>().iterator();
    }

    else {
      // else do normal behaviour
      return super.getResourcePidIterator();


    }
  }
  
  private boolean isResourceWithoutPatientCompartment(String resourceName)
  {
    for (String resourceWithoutPatientCompartmentName : resourcesWithoutPatientCompartment)
    {
      if (resourceWithoutPatientCompartmentName.equals(resourceName))
      {
        return true;
      }
    }
    
    return false;
  }
  
  
  //@Transactional( propagation = Propagation.SUPPORTS,readOnly = true )
  private List<ResourcePersistentId> getAllResourceIds(String resourceName)
  {
    
    System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!START");
    System.out.println("RESOURCE IS " + resourceName);
    System.out.println("::::::::::::::::::::::SEARCH-START");

    IBundleProvider provider = myDaoRegistry.getResourceDao(resourceName).search(new SearchParameterMap());
    System.out.println("::::::::::::::::::::::SEARCH-END");

    System.out.println("::::::::::::::::::::::PROVIDER GET ALL RESOURCESTARTS");

    
    System.out.println(provider.getClass());
    
    
    //fails sometimes
    List<IBaseResource> resources = provider.getAllResources();
    
    
    
    
    System.out.println("::::::::::::::::::::::PROVIDER GET ALL RESOURCEENDS");

    List<ResourcePersistentId> resourceIds = new ArrayList<>();
    for (IBaseResource baseResource : resources)
    {
      resourceIds.add(new ResourcePersistentId(baseResource.getIdElement().getIdPartAsLong()));
    }
    System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!END");

    return resourceIds;
  }
  
  /*private List<ResourcePersistentId> getAllResourceIds2(String resourceName)
  {
    Class<? extends IBaseResource> resourceType = myContext.getResourceDefinition(resourceName).getImplementingClass();

    IFhirResourceDao<?> dao = myDaoRegistry.getResourceDao(resourceName);

    final ISearchBuilder sb = mySearchBuilderFactory.newSearchBuilder(dao, resourceName, resourceType);
    


    

  }*/
  
  
}
