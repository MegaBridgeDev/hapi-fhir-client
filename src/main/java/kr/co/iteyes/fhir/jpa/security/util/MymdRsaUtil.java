package kr.co.iteyes.fhir.jpa.security.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import lombok.extern.slf4j.Slf4j;

/**
 * RSA util 클래스.
 * 
 * @author iteyes-hskim
 *
 */
@Slf4j
public class MymdRsaUtil {

  private static final String ALGORITHM = "RSA";
  private static final String ENCRYPTION_ALGORITHM = "RSA/ECB/OAEPWithSHA1AndMGF1Padding";
  private static final String DECRYPTION_ALGORITHM = "RSA/ECB/OAEPPadding";
  private static final int KEY_SIZE = 2048;
  private static final int CIPER_BLOCK_SIZE = KEY_SIZE / 8;
  private static final int ENCRYPTION_BLOCK_SIZE = 214;
  private static final int DECRYPTION_BLOCK_SIZE = CIPER_BLOCK_SIZE;

  /**
   * RSA 기반 알고리즘의 공개키와 개인키를 생성하여 문자열로 변환
   * 
   * @return 문자열의 공개키 개인키 Pair
   */
  public static HashMap<String, String> createKeyPair() {
    HashMap<String, String> keyPairMap = new HashMap<String, String>();
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
      keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());

      KeyPair keyPair = keyPairGenerator.genKeyPair();
      PublicKey publicKey = keyPair.getPublic();
      PrivateKey privateKey = keyPair.getPrivate();

      String stringPubicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
      String stringPrivateKey = Base64.getEncoder().encodeToString(privateKey.getEncoded());

      keyPairMap.put("publicKey", stringPubicKey);
      keyPairMap.put("privateKey", stringPrivateKey);
    } catch (NoSuchAlgorithmException e) {
      log.error(e.getLocalizedMessage());
    }
    return keyPairMap;
  }


  /**
   * 공개키를 이용하여 평문 byte 배열을 암호화
   * 
   * @param bytePlainData 암호화할 평문 byte 배열
   * @param stringPublicKey 공개키 문자열
   * @return encryptedData 암호화된 byte 배열
   */
  public static byte[] encrypt(final byte[] bytePlainData, final String stringPublicKey) {
    try {
      // 암호화
      KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
      byte[] bytePublicKey = Base64.getDecoder().decode(stringPublicKey.getBytes());

      X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytePublicKey);
      PublicKey publicKey = keyFactory.generatePublic(keySpec);

      Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, publicKey);

      int length = bytePlainData.length;
      final int encryptedLength = ((length + CIPER_BLOCK_SIZE - 1) / CIPER_BLOCK_SIZE) * DECRYPTION_BLOCK_SIZE;
      ByteArrayOutputStream byteEncrytedStream = new ByteArrayOutputStream(encryptedLength);

      for (int offset = 0; length > 0; offset += ENCRYPTION_BLOCK_SIZE, length -= ENCRYPTION_BLOCK_SIZE) {
        int len = length > ENCRYPTION_BLOCK_SIZE ? ENCRYPTION_BLOCK_SIZE : length;
        byte[] encryted = cipher.doFinal(bytePlainData, offset, len);

        byteEncrytedStream.write(encryted);
      }
      byteEncrytedStream.flush();
      byteEncrytedStream.close();

      return byteEncrytedStream.toByteArray();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | IllegalBlockSizeException
        | BadPaddingException | InvalidKeyException | IOException e) {
      log.error(e.getMessage());
    }

    return null;
  }

  /**
   * 공개키를 이용하여 평문을 암호화
   * 
   * @param plainData 암호화할 평문
   * @param stringPublicKey 공개키 문자열
   * @return encryptedData 암호화된 평문
   */
  public static String encrypt(final String plainData, final String stringPublicKey) {
    byte[] encryptedData = encrypt(plainData.getBytes(), stringPublicKey);

    return encryptedData != null ? Base64.getEncoder().encodeToString(encryptedData) : null;
  }

  /**
   * 암호화한 byte 배열을 개인키를 이용하여 평문으로 복호화
   * 
   * @param encryptedData 암호화된 byte 배열
   * @param stringPrivateKey 개인키 문자열
   * @return 복호화된 평문 byte 배열
   */
  public static byte[] decrypt(final byte[] byteEncryptedData, final String stringPrivateKey) {
    try {
      // 문자열로 전달받은 개인키를 개인키 객체화
      byte[] bytePrivateKey = Base64.getDecoder().decode(stringPrivateKey.getBytes());
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytePrivateKey);

      KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
      PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

      // 만들어진 개인키 객체를 기반으로 암호화모드 설정
      Cipher cipher = Cipher.getInstance(DECRYPTION_ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, privateKey);

      // 암호화문을 평문으로 복호화
      int length = byteEncryptedData.length;
      final int decryptedLength = ((length + CIPER_BLOCK_SIZE - 1) / CIPER_BLOCK_SIZE) * ENCRYPTION_BLOCK_SIZE;
      ByteArrayOutputStream byteDecrytedStream = new ByteArrayOutputStream(decryptedLength);

      for (int offset = 0; length > 0; offset += DECRYPTION_BLOCK_SIZE, length -= DECRYPTION_BLOCK_SIZE) {
        int len = length > DECRYPTION_BLOCK_SIZE ? DECRYPTION_BLOCK_SIZE : length;
        byte[] decrypted = cipher.doFinal(byteEncryptedData, offset, len);

        byteDecrytedStream.write(decrypted);
      }
      byteDecrytedStream.flush();
      byteDecrytedStream.close();

      return byteDecrytedStream.toByteArray();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException
        | IllegalBlockSizeException | BadPaddingException | IOException e) {
      log.error(e.getMessage());
    }

    return null;
  }

  /**
   * 암호화한 문자열을 개인키를 이용하여 평문으로 복호화
   * 
   * @param encryptedData 암호화된 문자열
   * @param stringPrivateKey 개인키 문자열
   * @return 복호화된 평문 데이터
   */
  public static String decrypt(final String encryptedData, final String stringPrivateKey) {
    byte[] byteEncryptedData = Base64.getDecoder().decode(encryptedData.getBytes());
    byte[] decryptedData = decrypt(byteEncryptedData, stringPrivateKey);

    return decryptedData != null ? new String(decryptedData) : null;
  }
}
