package ca.uhn.fhir.rest.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class OAuthClientTest {

    FhirContext ctx = FhirContext.forR4();

//    IGenericClient client = ctx.newRestfulGenericClient("http://101.79.14.160:8080/fhir");
    IGenericClient client = ctx.newRestfulGenericClient("http://localhost:8080/fhir");
    //    {"error":"invalid_token","error_description":"Token was not recognised"} 발생할 경우 신규 토큰 발급되어야합니다. syhan@iteyes.co.kr 로 문의부탁드리겠습니다.
    String token = "488ee80b-2482-45b9-af71-3e9850d09557"; //user
    BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(token);


    @Test
    public void getPatientCountUsingOAuth(){
        client.registerInterceptor(authInterceptor);

        Bundle results = client
                .search()
                .forResource(Patient.class)
//                .where(Patient.FAMILY.matches().values("Test")) // 검색 조건문
                .returnBundle(Bundle.class)
                .execute();

        System.out.println("Found " + results.getEntry().size() + " patients named 'Test'");
    }

    @Test
    public void getPatientInfo(){
        client.registerInterceptor(authInterceptor);

        Bundle results = client
                .search()
                .forResource(Patient.class)
//                .where(Patient.FAMILY.matches().values("Test")) //검색 조건문
                .returnBundle(Bundle.class)
                .execute();
        List<IBaseResource> patients = new ArrayList<>();
        patients.addAll(BundleUtil.toListOfResources(ctx, results));


        while (results.getLink(IBaseBundle.LINK_NEXT) != null) {
            results = client
                    .loadPage()
                    .next(results)
                    .execute();
            patients.addAll(BundleUtil.toListOfResources(ctx, results));
        }
        Patient patient0 = (Patient) patients.get(0);
        System.out.println("PatientInfo " + patient0.getName().get(0).getFamily()  + " Name!");
        System.out.println("PatientInfo " + patient0.getIdentifier().get(0).getSystem()  + " Identifier!");

    }


    @Test
    public void createPatientWithOAuth2(){

        client.registerInterceptor(authInterceptor);


        // Create a patient object
        Patient patient = new Patient();
        patient.addIdentifier()
                .setSystem("http://acme.org/mrns")
                .setValue("12345");
        patient.addName()
                .setFamily("Test01")
                .addGiven("Test2")
                .addGiven("Test2");
        patient.setGender(Enumerations.AdministrativeGender.MALE);

        patient.setId(IdType.newRandomUuid());




        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);

        bundle.addEntry()
                .setFullUrl(patient.getIdElement().getValue())
                .setResource(patient)
                .getRequest()
                .setUrl("Patient")
                .setMethod(Bundle.HTTPVerb.POST);


        Bundle resp = client.transaction().withBundle(bundle).execute();

        System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(resp));

    }

    @Test
    public void createPatientAndOperationDataTransactionWithOAuth2(){

        client.registerInterceptor(authInterceptor);

        // Create a patient object
        Patient patient = new Patient();
        patient.addIdentifier()
                .setSystem("http://acme.org/mrns")
                .setValue("12345");
        patient.addName()
                .setFamily("Test01")
                .addGiven("Test2")
                .addGiven("Test2");
        patient.setGender(Enumerations.AdministrativeGender.MALE);

        patient.setId(IdType.newRandomUuid());

        Observation observation = new Observation();
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
        observation.setSubject(new Reference(patient.getIdElement().getValue()));

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);

        bundle.addEntry()
                .setFullUrl(patient.getIdElement().getValue())
                .setResource(patient)
                .getRequest()
                .setUrl("Patient")
                .setMethod(Bundle.HTTPVerb.POST);

        bundle.addEntry()
                .setResource(observation)
                .getRequest()
                .setUrl("Observation")
                .setMethod(Bundle.HTTPVerb.POST);

        // Log the request

        Bundle resp = client.transaction().withBundle(bundle).execute();

        // Log the response
        System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(resp));

    }

}
