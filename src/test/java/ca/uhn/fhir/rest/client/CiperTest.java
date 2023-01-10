package ca.uhn.fhir.rest.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.google.common.primitives.Bytes;
import kr.co.iteyes.fhir.jpa.security.interceptor.RequestChangeInterceptor;
import kr.co.iteyes.fhir.jpa.security.util.*;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

public class CiperTest {
    public RSAUtils rsaUtils = new RSAUtils();

    public CipherUtils cipherUtils = new CipherUtils();

    public Lz4Utils lz4Utils = new Lz4Utils();

    FhirContext ctx = FhirContext.forR4();

    String serverBase = "http://localhost:8080/fhir";

    IGenericClient client = ctx.newRestfulGenericClient(serverBase);

    RequestChangeInterceptor requestChangeInterceptor = new RequestChangeInterceptor();

    MymdLz4Util mymdLz4Util = new MymdLz4Util();

    MymdRsaUtil mymdRsaUtil = new MymdRsaUtil();

    MymdSeedCtrUtil mymdSeedCtrUtil = new MymdSeedCtrUtil();

    static String CHARSET = "utf-8";
//    static String PBUserKey = "kics2019Hwang!@#"; //16Byte로 설정
    static String PBUserKey = "kics2019Hwang!@#12312151515151"; //16Byte로 설정
    static String DEFAULT_IV = "1234567890123456123214151515"; //16Byte로 설정
//    static String DEFAULT_IV = "1234567890123456"; //16Byte로 설정

    /*
    public static byte pbUserKey[] = { (byte) 0x2c, (byte) 0x11, (byte) 0x19, (byte) 0x1d, (byte) 0x1f, (byte) 0x16, (byte) 0x12,
            (byte) 0x12, (byte) 0x11, (byte) 0x19, (byte) 0x1d, (byte) 0x1f, (byte) 0x10, (byte) 0x14, (byte) 0x1b,
            (byte) 0x16 };
    */
    public static byte[] pbUserKey = PBUserKey.getBytes();

    /*
    public static byte bszIV[] = { (byte) 0x27, (byte) 0x28, (byte) 0x27, (byte) 0x6d, (byte) 0x2d, (byte) 0xd5, (byte) 0x4e,
            (byte) 0x29, (byte) 0x2c, (byte) 0x56, (byte) 0xf4, (byte) 0x2a, (byte) 0x65, (byte) 0x2a, (byte) 0xae,
            (byte) 0x08 };
    */
    public static byte[] bszIV = DEFAULT_IV.getBytes();


    String privateKey="MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCmifpuS65O8DyWhe8675lQL3tT/Rx9JvPP+bx60FqIvBNBpJWGek+lC5DJvpEbaUf7AZUVCpq4uoeVv2PFTIAgeQ3631PRCNuSSRa1WWDvhHf3bZS00f2CNyQWwogZQuEOLbF8u0nLoKDRThmRepIQqjRm453DU28EayXy6NAPa5bAzDhMFQW0W3TExqMxRzMO/k82Y7vI8ieLQ8JPZ1sYmjcqnkgKGC+ZRRudZSZW4+ZXEm0zTJQJSV4V8ceaT/3G8wg27rTBjUUybOheFK1lrWNsiM9C59gcD/rBLfu6gez5W16z4CIk1WXOl31mxGVql6Z55Vh6NTh5v82AML9hAgMBAAECggEAAO0t83tHadutLGPd6eEiK2+V0RgrYlR8Z6NY7DHW6SwfIH0HGIRtFZh9ZPmhr/3Lug0pPTv8ruJnzmWcZo44UlS9oRhmvjqEdeXNwATSYmsHuU5wPTMPpPJAZlTBYrReeEz7cSBr+k54iFd7xrVW0YAG1GGhRNUPeX5fssifAhEEteRhr1QIrc+pU0hhZF9E2ekRh/9z3TIYC2zOrcGZz2ccFEwI6K3Wt5X9L5s1iN+XGRJx5+6p0SDinN2iezTj3OrnlXufoS2PZeKD78gyMLuv1MzF8tz8yiw2QrqxKHgTqkTbizQHXQYKvDZcsJWsAxp4QpnGKdZ6/eSeVI+BQQKBgQDbvAolyTcgqFx20ltYAXEm3wJy4/dRFXEBO4GXH9U19Ga3amgbybmU0WV/Hpq/EzurFnHoQ0N9XVBm/SJgjHBG543/24SMgGoCLeLClVQyy7rTa+9TYqc1Chc7UEA3GBFOFQ3+ynExSlkYWSC4otNADWh+Q/n+1box+vFEPlaA/QKBgQDCBmJAlsbL7hxO/m6rMQlN1HK8Dejsy2g/3/W+PgqzJ+yba9+B4YxHSlehuHQzF0gicjPosM5V9bMjvdMV8ypT7Q4HkJtOmJ4f1OfyCDdyxXaWUWq4XEQ1mpQPww/c1jVWZ7tn+6AcHbzcrPhEhElAEtVqjb4Vd9GdK4zYUZ+nNQKBgH1RnDP6sz8j5kA6LrBdeiwiKiJeU5Hh+aYrSvhmxlHURrS6sg+PGBGA7zL1wGnTTUeBMIu3uQkJrC5gljecQPifXUQb7Ve4cT028Enroq6ptK6Zs0/KRvSgAanpVgZV6qCur2GuEap77Z5OTrQe4P555yuEF1M4j82rgeha6Tj9AoGAdyESTJQHBYlyijIAY04dZ1MtCzgDLvkJTkbFjQRs72lxBlCqkAWbat5DhgFKH2CJItU5+AIu/mp4jlZr98swzwz7Ezv/j7d3RxYjP+E5oleJo2vj+cR1APCjPjZHVBGV+DKCx6qguQKtRlVRrkgG7bdioCTddDL4Wec2eE9Nlo0CgYBHPwl5AdtgPnnEwDYzeCXFVd8m0zgWbq/DUJrVel2w6gZaGCirS23hLhvzsgygub/cf3Kyq0g0iY0oKGQTzBlC5XdWmkUxTu4fdZ1RsrZSPPZ0/bzQfqOcaMW/zzzAP2RVc/KhfzsksZW4A5vgJ3WS2jUAt0CoSfnKxXdzdM89RA==";
    String publicKey="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApon6bkuuTvA8loXvOu+ZUC97U/0cfSbzz/m8etBaiLwTQaSVhnpPpQuQyb6RG2lH+wGVFQqauLqHlb9jxUyAIHkN+t9T0QjbkkkWtVlg74R3922UtNH9gjckFsKIGULhDi2xfLtJy6Cg0U4ZkXqSEKo0ZuOdw1NvBGsl8ujQD2uWwMw4TBUFtFt0xMajMUczDv5PNmO7yPIni0PCT2dbGJo3Kp5IChgvmUUbnWUmVuPmVxJtM0yUCUleFfHHmk/9xvMINu60wY1FMmzoXhStZa1jbIjPQufYHA/6wS37uoHs+Vtes+AiJNVlzpd9ZsRlapemeeVYejU4eb/NgDC/YQIDAQAB";

    public static String byteArrayToHexaString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();

        for (byte data : bytes) {
            builder.append(String.format("%02X ", data));
        }

        return builder.toString();
    }

    @Test
    public void rsaDecryptTest(){
        String encryptedText = "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";
        System.out.println("encryptedText = " + encryptedText);
        String decryptedText = MymdRsaUtil.decrypt(encryptedText, privateKey);
        System.out.println("decryptedText = " + decryptedText);
        byte[] decryptedByte = Base64.getDecoder().decode(decryptedText);
        byte[] decompressedData = lz4Utils.decompress(decryptedByte);

        System.out.println("new String (decompressedData) = " + new String(decompressedData));

    }

    @Test
    public void rsaUtilTest(){
        Map<String, String> genKey =  RSAUtils.genKeypairAsString();

        System.out.println("genKey = " + genKey);
        System.out.println("privateKey = " + privateKey);
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
//        String sampleBundle = "{\n" +
//                "  \"resourceType\": \"Patient\",\n" +
//                "  \"id\": \"1\",\n" +
//                "  \"meta\": {\n" +
//                "    \"versionId\": \"1\",\n" +
//                "    \"lastUpdated\": \"2022-11-14T11:35:02.877+09:00\",\n" +
//                "    \"source\": \"#s49WAqX0NScNTG5W\"\n" +
//                "  }" +
//                "}";
        String sampleBundle = "{\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":[{\"fullUrl\":\"urn:uuid:cccf060b-6b2e-4436-9275-6dce28cd7565\",\"resource\":{\"resourceType\":\"Patient\",\"identifier\":[{\"system\":\"http://acme.org/mrns\",\"value\":\"12345\"}],\"name\":[{\"family\":\"Test01\",\"given\":[\"Test2\",\"Test2\"]}],\"gender\":\"male\"},\"request\":{\"method\":\"POST\",\"url\":\"Patient\"}}]}";
//        String sampleBundle = "{\"resourceType\":\"Bundle\"}";
        System.out.println("sampleBundle = " + sampleBundle);
        byte[] compressedBundle = mymdLz4Util.compress(sampleBundle.getBytes((StandardCharsets.UTF_8)));
        System.out.println("compressedBundle = " + compressedBundle);
        String strCompressedBundle = DatatypeConverter.printBase64Binary(compressedBundle);
        System.out.println("strCompressedBundle = " + strCompressedBundle);
//        byte[] decompressBundle = lz4Utils.decompress(Base64.getDecoder().decode(strCompressedBundle));
//        System.out.println("decompressBundle = " + new String(decompressBundle));


        String encryptedText = MymdRsaUtil.encrypt(strCompressedBundle, publicKey);
//            String encryptedText = RSAUtils.encryptRSA(new String(compressedBundle), publicKey.substring(0,8));
        System.out.println("encryptedText = " + encryptedText);
        String decryptedText = MymdRsaUtil.decrypt(encryptedText, privateKey);
        System.out.println("decryptedText = " + decryptedText);
        byte[] decryptedByte = Base64.getDecoder().decode(decryptedText);
        byte[] decompressedData = lz4Utils.decompress(decryptedByte);

        System.out.println("new String (decompressedData) = " + new String(decompressedData));





        System.out.println("sampleBundle.toString() = " + sampleBundle.toString());
        // Log the request
//        Bundle resp = client.transaction().withBundle(bundle).execute();

        client.registerInterceptor(requestChangeInterceptor);



        String resp = client.transaction().withBundle(sampleBundle).execute();
        // Log the response
        System.out.println("resp : " +resp);
    }



    @Test
    public void seedUtilTest(){

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

        String sampleBundle = "{\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":[{\"fullUrl\":\"urn:uuid:cccf060b-6b2e-4436-9275-6dce28cd7565\",\"resource\":{\"resourceType\":\"Patient\",\"identifier\":[{\"system\":\"http://acme.org/mrns\",\"value\":\"12345\"}],\"name\":[{\"family\":\"Test01\",\"given\":[\"Test2\",\"Test2\"]}],\"gender\":\"male\"},\"request\":{\"method\":\"POST\",\"url\":\"Patient\"}}]}";
//        String sampleBundle = "{\"resourceType\":\"Bundle\"}";
        System.out.println("sampleBundle = " + sampleBundle);

        byte[] compressedBundle = lz4Utils.compress(sampleBundle.getBytes((StandardCharsets.UTF_8)));
        System.out.println("compressedBundle = " + compressedBundle);


        String strCompressedBundle = DatatypeConverter.printBase64Binary(compressedBundle);
//        System.out.println("strCompressedBundle = " + strCompressedBundle);
//        byte[] decompressBundle = lz4Utils.decompress(Base64.getDecoder().decode(strCompressedBundle));
//        System.out.println("decompressBundle = " + new String(decompressBundle));
        try {

//            byte[] compressedBundle222 = Base64.getDecoder().decode(strCompressedBundle);
//            byte[] encyptedData = KISA_SEED_CTR.SEED_CTR_Encrypt(pbUserKey, bszIV, strCompressedBundle.getBytes("utf-8"), 0, strCompressedBundle.getBytes(CHARSET).length);
            byte[] encyptedData = encrypt(strCompressedBundle);
            System.out.println("encryptedText = " + encyptedData);

            String decryptedData = decrypt(encyptedData);
            System.out.println("decryptedData = " + decryptedData);
//            KISA_SEED_CTR.SEED_CTR_Encrypt(publicKey.getBytes(),)
//            String encryptedText = CipherUtils.SEED_CTR_Encrypt(publicKey, strCompressedBundle);
//            String encryptedText = RSAUtils.encryptRSA(new String(compressedBundle), publicKey.substring(0,8));


//            byte[] decryptedData = KISA_SEED_CTR.SEED_CTR_Decrypt(pbUserKey, bszIV, encyptedData, 0, encyptedData.length);

//            byte[] decryptedByte =CipherUtils.SEED_CTR_Decrypt(publicKey, encryptedText) ;
//                    RSAUtils.decryptRSA(encryptedText, RSAUtils.toPrivateKeyFromBase64(privateKey));
//            System.out.println("decryptedByte = " + );
//            String decryptedStr = new String(decryptedData, "utf-8");
//            byte[] decryptedByte = Base64.getDecoder().decode(decryptedText);
            byte[] decompressedData = lz4Utils.decompress(Base64.getDecoder().decode(decryptedData));

            System.out.println("new String (decompressedData) = " + new String(decompressedData));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("sampleBundle.toString() = " + sampleBundle.toString());
        // Log the request
//        Bundle resp = client.transaction().withBundle(bundle).execute();

        client.registerInterceptor(requestChangeInterceptor);
        String resp = client.transaction().withBundle(sampleBundle).execute();
        // Log the response
        System.out.println("resp : " +resp);
    }


    @Test
    public void seedDecryptTest(){

        // Create a patient object


        String sampleBundle = "dDVWYW1ydXkveU1oc0lpTTY1QVNWL1lUVnBpZkszQk9qVlhZWnIwZ2pwOWJxa3BiR0d2dU1GdktxTzlNYVNIalFNUVFCN1lTTWJSYnBMMXljN2FYdGFHWENQcndidEQ3cDMwREN1VnFWbTdacGg4MDZDekMzcTZpK3RncU9pakd4c2YrN04ybGxqVFhsYUs1b2s2TFpTakRSTkRBN2JRRVNYekFhYWxLYlJNSzJibkQ3YnNVU0J6aVJUa2JWMldiUXkyS0pCTyt6VkMyS1FGeXM0QjdOU01vTE9pZSsvaUx5T0tkM1Zla3lxOXFTVHNvWVJqbkJhRmJEUWpuc0plMVM3cHkzMXhPWSttZmxJY3FpVWgxSUV6QTVRMk5MTmxqRVlyNUV1alozVFo0THpiSUVCYUxGRmloNGJpNmk1dzN3RnlNNUZpdmlZS0NZaWhsK0kreVpnSXRwLzRET0V1WmtLZWsxQlpSWTU2QnVqYXF5RUlVRWwwSy9jaXE0d2hYLzFXY0QxZkd3d0tRVGQxd2orZ2hxVDZUbEZRanJEYVVUck9YakxlM2xTWitjTGJmeHlZaTRaa25ZSnlqY0I2NWxjUmxOQnlDYVlLeFVQYmpFZEt1cllVKy8wYmFQYWc4V2FzV0FlaXhCY0hML0VyM2xQNUFOY2NIMDN5QVowaEpkQXdpYzhtRFJHY3lDMmR0Szh4dG1kUUhLTGdwc1pqcXNKbk9HS3g1S25nWStzZnhSRXN1Mk9MMVlJVFlVSzRWSWdUS1pJcHVtUHBpNkFncWdiUVVQbVdVeWNUaFh5RWZ2eG5XVis1YnpHbzhCd3lVMHdTQk4xQkxXaWNycy82c1BUZ0l5ZkZVSUNUUXZtQ1lWMW56bVVOcVduODNzRnd4ak4xYmVxdWNzc3B4dGlTYXRwcWFKNXNQWHlWaE50cWw1UVRsZzllaFhrYWpkSUlLK1BCa2RJQ2pTQnc5Z21nZ2tlY1EwbXI4K2Y0R05sVS84ZU9aUUZQd1hvdlo0YnM3MVJtYldEQWNxbEQxd0R1a1ZWclFsSERyMmFPMlVBZVBTQTE3Y0U3aHVMYVZLWGJvRCtnM2J4WXFjWnFISDNwMHpDczBrcUgyWndPWmVRMlRKc1RBYUNxbkM4QTJLWldzenBNSnl3PT0=";

        try {
            byte[] byteBundle = Base64.getDecoder().decode(sampleBundle);
            String decryptedData = decrypt(byteBundle);
            System.out.println("decryptedData = " + decryptedData);
            byte[] decompressedData = lz4Utils.decompress(Base64.getDecoder().decode(decryptedData));

            System.out.println("new String (decompressedData) = " + new String(decompressedData));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("sampleBundle.toString() = " + sampleBundle.toString());
        // Log the request
//        Bundle resp = client.transaction().withBundle(bundle).execute();
    }

    @Test
    public void compressTest(){
        String sampleBundle = "{\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":[{\"fullUrl\":\"urn:uuid:cccf060b-6b2e-4436-9275-6dce28cd7565\",\"resource\":{\"resourceType\":\"Patient\",\"identifier\":[{\"system\":\"http://acme.org/mrns\",\"value\":\"12345\"}],\"name\":[{\"family\":\"Test01\",\"given\":[\"Test2\",\"Test2\"]}],\"gender\":\"male\"},\"request\":{\"method\":\"POST\",\"url\":\"Patient\"}}]}";
//        String sampleBundle = "{\"resourceType\":\"Bundle\"}";
        System.out.println("sampleBundle = " + sampleBundle);

        byte[] compressedBundle = mymdLz4Util.compress(sampleBundle.getBytes((StandardCharsets.UTF_8)));
        System.out.println("compressedBundle = " + compressedBundle);


        String strCompressedBundle = DatatypeConverter.printBase64Binary(compressedBundle);
        System.out.println("strCompressedBundle = " + strCompressedBundle);



        byte[] decompressBundle = mymdLz4Util.decompress(Base64.getDecoder().decode(strCompressedBundle));
        System.out.println("decompressBundle11111 = " + new String(decompressBundle));





    }


    public static byte[] encrypt(String str) {

        byte[] enc = null;
        try {

            //암호화 함수 호출
            enc = KISA_SEED_CTR.SEED_CTR_Encrypt(pbUserKey, bszIV, str.getBytes(CHARSET), 0, str.getBytes(CHARSET).length);
            //enc = KISA_SEED_ECB.SEED_ECB_Encrypt(pbUserKey, str.getBytes(CHARSET),  0, str.getBytes(CHARSET).length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        /**JDK1.8 일 때 사용*/
         Base64.Encoder encoder = Base64.getEncoder();
         byte[] encArray = encoder.encode(enc);

//        byte[] encArray  = Base64.encode(enc);

        return encArray;
    }

    public static String decrypt(byte[] str) {

        /**JDK1.8 일 때 사용*/
         Base64.Decoder decoder = Base64.getDecoder();
         byte[] enc = decoder.decode(str);

//        byte[] enc  = Base64.decode(str);

        String result = "";
        byte[] dec = null;

        try {
            //복호화 함수 호출
            dec = KISA_SEED_CTR.SEED_CTR_Decrypt(pbUserKey, bszIV, enc, 0, enc.length);
            //dec = KISA_SEED_ECB.SEED_ECB_Decrypt(pbUserKey, enc, 0, enc.length);
            result = new String(dec, "utf-8");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

}
