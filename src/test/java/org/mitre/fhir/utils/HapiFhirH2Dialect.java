package org.mitre.fhir.utils;

import org.hibernate.dialect.H2Dialect;

/**
 * HAPI FHIR dialect for H2 database.
 * This class is cloned into Inferno until we update HAPI to get the real one.
 * Original: 
 * https://github.com/hapifhir/hapi-fhir/blob/d610d33ac361d3fc6e9d1624b40d7768e8371048/hapi-fhir-jpaserver-model/src/main/java/ca/uhn/fhir/jpa/model/dialect/HapiFhirH2Dialect.java
 */
public class HapiFhirH2Dialect extends H2Dialect {

    /**
     * Workaround until this bug is fixed:
     * https://hibernate.atlassian.net/browse/HHH-15002
     */
    @Override
    public String toBooleanValueString(boolean bool) {
        return bool ? "true" : "false";
    }
}