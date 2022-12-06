package ca.uhn.fhir.rest.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IBasicClient;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import org.junit.jupiter.api.Test;

public class CustomClientTest {
    public interface IPatientClient extends IBasicClient {
        // nothing yet
    }

    FhirContext ctx = FhirContext.forR4();
    String serverBase = "http://101.79.14.160:8080/fhir";



    IGenericClient client = ctx.newRestfulGenericClient(serverBase);
    IRestfulClientFactory clientFactory = ctx.getRestfulClientFactory();

    @Test
    public void fhirServerCheck(){
//        String token = "831cdd9d-4d62-497b-a385-e7a858023192"; //admin
        String token = "28b60875-ec2d-4181-8e50-ecf9bc8f02b9"; //user

        BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(token);

// Register the interceptor with your client (either style)
        CustomClientTest.IPatientClient annotationClient = ctx.newRestfulClient(CustomClientTest.IPatientClient.class, "http://101.79.14.160:8080/fhir");
        annotationClient.registerInterceptor(authInterceptor);
//
        IGenericClient genericClient = ctx.newRestfulGenericClient("http://101.79.14.160:8080/fhir");
        annotationClient.registerInterceptor(authInterceptor);

    }
}
