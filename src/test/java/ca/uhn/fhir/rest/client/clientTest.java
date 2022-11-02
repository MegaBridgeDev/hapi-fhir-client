package ca.uhn.fhir.rest.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r5.model.*;
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
                .where(Patient.FAMILY.matches().values("Test"))
                .returnBundle(Bundle.class)
                .execute();


        System.out.println("Found " + results.getEntry().size() + " patients named 'Test'");

    }

    @Test
    public void test2(){
        // Create a patient object
        Patient patient = new Patient();
        patient.addIdentifier()
                .setSystem("http://acme.org/mrns")
                .setValue("12345");
        patient.addName()
                .setFamily("Test02")
                .addGiven("Test")
                .addGiven("Test");
        patient.setGender(Enumerations.AdministrativeGender.MALE);

// Give the patient a temporary UUID so that other resources in
// the transaction can refer to it
        patient.setId(IdType.newRandomUuid());

// Create an observation object
        Observation observation = new Observation();
        observation.setStatus(Enumerations.ObservationStatus.FINAL);
        observation
                .getCode()
                .addCoding()
                .setSystem("http://loinc.org")
                .setCode("789-8")
                .setDisplay("Erythrocytes [#/volume] in Blood by Automated count");
        observation.setValue(
                new Quantity()
                        .setValue(4.12)
                        .setUnit("10 trillion/L")
                        .setSystem("http://unitsofmeasure.org")
                        .setCode("10*12/L"));

// The observation refers to the patient using the ID, which is already
// set to a temporary UUID
        observation.setSubject(new Reference(patient.getIdElement().getValue()));

// Create a bundle that will be used as a transaction
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);

// Add the patient as an entry. This entry is a POST with an
// If-None-Exist header (conditional create) meaning that it
// will only be created if there isn't already a Patient with
// the identifier 12345
        bundle.addEntry()
                .setFullUrl(patient.getIdElement().getValue())
                .setResource(patient)
                .getRequest()
                .setUrl("Patient")
//                .setIfNoneExist("identifier=http://acme.org/mrns|12345")
                .setMethod(Bundle.HTTPVerb.POST);

// Add the observation. This entry is a POST with no header
// (normal create) meaning that it will be created even if
// a similar resource already exists.
        bundle.addEntry()
                .setResource(observation)
                .getRequest()
                .setUrl("Observation")
                .setMethod(Bundle.HTTPVerb.POST);

// Log the request
        FhirContext ctx = FhirContext.forR5();
        System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle));

// Create a client and post the transaction to the server
//        IGenericClient client = ctx.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
        IGenericClient client = ctx.newRestfulGenericClient("http://localhost:8080/fhir");

        Bundle resp = client.transaction().withBundle(bundle).execute();

// Log the response
        System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(resp));

//        Bundle response = client.search()
//                .forResource(Patient.class)
//                .where(Patient.BIRTHDATE.beforeOrEquals().day("2011-01-01"))
//                .and(Patient.GENERAL_PRACTITIONER.hasChainedProperty(Organization.NAME.matches().value("Smith")))
//                .returnBundle(Bundle.class)
//                .execute();
    }
}
