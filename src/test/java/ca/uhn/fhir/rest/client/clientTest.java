package ca.uhn.fhir.rest.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Patient;
import org.junit.jupiter.api.Test;



public class clientTest {

    FhirContext ctx = FhirContext.forR5();
    String serverBase = "http://localhost:8080/fhir";



    IGenericClient client = ctx.newRestfulGenericClient(serverBase);
    @Test
    public void test3(){

        Bundle results = client
                .search()
                .forResource(Patient.class)
                .where(Patient.FAMILY.matches().values("duck"))
                .returnBundle(Bundle.class)
                .execute();


        System.out.println("Found " + results.getEntry().size() + " patients named 'duck'");

    }

    @Test
    public void test2(){
//        Bundle response = client.search()
//                .forResource(Patient.class)
//                .where(Patient.BIRTHDATE.beforeOrEquals().day("2011-01-01"))
//                .and(Patient.GENERAL_PRACTITIONER.hasChainedProperty(Organization.NAME.matches().value("Smith")))
//                .returnBundle(Bundle.class)
//                .execute();
    }
}
