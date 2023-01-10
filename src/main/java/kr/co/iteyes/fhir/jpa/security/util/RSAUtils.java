package kr.co.iteyes.fhir.jpa.security.util;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;

@Slf4j
public class RSAUtils {
    private static final String Algorithm = "RSA";
    /**
     * 공개키로 RSA 암호화 수행
     * @param plainText 암호화할 문자열
     * @param publicKey 공개키
     * @return String 암호화된 문자열
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public static String encryptRSA(String plainText, PublicKey publicKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] bytePlain = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(bytePlain);
    }

    public static String encryptRSA(String plainText, String base64PublicKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException {
        PublicKey publicKey = toPublicKeyFromBase64(base64PublicKey);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] bytePlain = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(bytePlain);
    }

    /**
     * 개인키로 RSA 복호화 수행
     * @param encrypted 암호화된 문자열
     * @param privateKey 개인키
     * @return String 복호화된 문자열
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws UnsupportedEncodingException
     */
    public static String decryptRSA(String encrypted, PrivateKey privateKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] byteEncrypted = Base64.getDecoder().decode(encrypted.getBytes());
        byte[] bytePlain = cipher.doFinal(byteEncrypted);
        return new String(bytePlain, "utf-8");
    }


    /**
     * Base64로 인코딩된 공개키를 publicKey로 변환
     * @param base64PublicKey Base64로 인코딩된 공개키
     * @return PublicKey
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    public static PublicKey toPublicKeyFromBase64(String base64PublicKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] decodedBase64PubKey = Base64.getDecoder().decode(base64PublicKey);
        return KeyFactory.getInstance(Algorithm)
                .generatePublic(new X509EncodedKeySpec(decodedBase64PubKey));

    }

    /**
     * Base64로 인코딩된 개인키를 privateKey로 변환
     * @param base64PrivateKey Base64로 인코딩된 개인키
     * @return PrivateKey
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    public static PrivateKey toPrivateKeyFromBase64(String base64PrivateKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] decodedBase64PrivateKey = Base64.getDecoder().decode(base64PrivateKey);
        return KeyFactory.getInstance(Algorithm)
                .generatePrivate(new PKCS8EncodedKeySpec(decodedBase64PrivateKey));
    }


    public static PublicKey toPublicKeyFromXml(String xmlPublicKey)
            throws SAXException, IOException, ParserConfigurationException, InvalidKeySpecException, NoSuchAlgorithmException {
        Document doc = CommonUtils.xmlStringToDoc(xmlPublicKey);
        String strModulus = doc.getElementsByTagName("Modulus").item(0).getTextContent();
        String strExponent = doc.getElementsByTagName("Exponent").item(0).getTextContent();

        byte[] modulus = Base64.getDecoder().decode(strModulus);
        byte[] exponent = Base64.getDecoder().decode(strExponent);

        return toPublicKey(modulus, exponent);
    }

    public static PublicKey toPublicKey(byte[] modulus, byte[] exponent) throws InvalidKeySpecException, NoSuchAlgorithmException {
        BigInteger bigModulus = new BigInteger(1, modulus);
        BigInteger bigExponent = new BigInteger(1, exponent);
        RSAPublicKeySpec spec = new RSAPublicKeySpec(bigModulus, bigExponent);
        return KeyFactory.getInstance(Algorithm).generatePublic(spec);
    }

    public static RSAPublicKeySpec toPublicKeySpec(PublicKey publicKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
        return KeyFactory.getInstance(Algorithm).getKeySpec(publicKey, RSAPublicKeySpec.class);
    }

    public static HashMap<String, String> genKeypairAsString() {
        HashMap<String, String> stringKeypair = new HashMap<>();

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048, new SecureRandom());
            KeyPair keyPair = keyPairGenerator.genKeyPair();

            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();

            String stringPublicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            String stringPrivateKey = Base64.getEncoder().encodeToString(privateKey.getEncoded());

            stringKeypair.put("publicKey", stringPublicKey);
            stringKeypair.put("privateKey", stringPrivateKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringKeypair;
    }

    public static String toXmlFromBase64(String base64PublicKey) {
        try {
            PublicKey publicKey;
            publicKey = toPublicKeyFromBase64(base64PublicKey);
            RSAPublicKeySpec spec = toPublicKeySpec(publicKey);
            byte[] modulus = removeSigPaddingOfBigInteger(spec.getModulus().toByteArray());
            byte[] exponent = spec.getPublicExponent().toByteArray();

            String strModulus = Base64.getEncoder().encodeToString(modulus);
            String strExponent = Base64.getEncoder().encodeToString(exponent);

            StringBuilder publicKeyASxml = new StringBuilder("<RSAKeyValue><Modulus>");
            publicKeyASxml.append(strModulus);
            publicKeyASxml.append("</Modulus><Exponent>");
            publicKeyASxml.append(strExponent);
            publicKeyASxml.append("</Exponent></RSAKeyValue>");

            return publicKeyASxml.toString();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
            return "<RSAKeyValue><Modulus></Modulus><Exponent></Exponent></RSAKeyValue>";
        }
    }

    private static byte[] removeSigPaddingOfBigInteger(byte[] a) {
        if(a[0] == 0) {
            byte[] tmp = new byte[a.length - 1];
            System.arraycopy(a, 1, tmp, 0, tmp.length);
            return tmp;
        }
        return a;
    }
}
