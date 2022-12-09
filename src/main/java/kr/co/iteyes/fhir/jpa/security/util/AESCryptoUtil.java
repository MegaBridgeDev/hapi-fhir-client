package kr.co.iteyes.fhir.jpa.security.util;

import kr.co.iteyes.fhir.jpa.security.config.CryptoConfig;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AESCryptoUtil {

    public static SecretKey getKey() throws Exception{
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        SecretKey secretKey = keyGenerator.generateKey();
        return secretKey;
    }


    public static IvParameterSpec getIv(){
//        byte[] iv = new byte[16];
//        new SecureRandom().nextBytes(iv);

//        return new IvParameterSpec(iv);
        byte[] iv = CryptoConfig.CRYPTO_IV_KEY.substring(0,16).getBytes();
        return new IvParameterSpec(iv);
    }

    public static String encrypt(String specName, SecretKey key, IvParameterSpec iv, String plainText) throws Exception{
        Cipher cipher = Cipher.getInstance(specName);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return DatatypeConverter.printBase64Binary(encrypted);

//        return new String (Base64.getEncoder().encode(encrypted));

    }

    public static String decrypt(String specName, SecretKey key, IvParameterSpec iv,
                                 String cipherText) throws Exception {
        Cipher cipher = Cipher.getInstance(specName);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
//        return DatatypeConverter.printBase64Binary(decrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }


}
