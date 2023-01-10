package kr.co.iteyes.fhir.jpa.security.util;

import java.util.Base64;

public class CipherUtils {

	private static final int KEY_BIT_LENGTH = 256;
	private static final int KEY_BYTE_LENGTH = KEY_BIT_LENGTH / 8;
	private static final byte[] bszCTR;

	static {
		bszCTR = new byte[KEY_BYTE_LENGTH];
		bszCTR[KEY_BYTE_LENGTH - 1] = (byte) 0x0FE;
	}

	public static byte[] SEED_CTR_Decrypt(String key, String defaultCipherString) throws Exception {
		byte pbUserKey[] = Base64.getDecoder().decode(key.getBytes());
		byte defaultCipherText[] = Base64.getDecoder().decode(defaultCipherString);
		int PLAINTEXT_LENGTH = defaultCipherText.length;
		return KISA_SEED_CTR.SEED_CTR_Decrypt(pbUserKey, bszCTR, defaultCipherText, 0, PLAINTEXT_LENGTH);
	}

	public static String SEED_CTR_Encrypt(String key, String plainText) {

		byte pbData[] = plainText.getBytes();
		return SEED_CTR_Encrypt(key, pbData);
	}

	public static String SEED_CTR_Encrypt(String key, byte[] pbData) {
		byte pbUserKey[] = Base64.getDecoder().decode(key.getBytes());
		int PLAINTEXT_LENGTH = pbData.length;
		byte[] defaultCipherText = KISA_SEED_CTR.SEED_CTR_Encrypt(pbUserKey, bszCTR, pbData, 0, PLAINTEXT_LENGTH);
		String defaultCipherString = Base64.getEncoder().encodeToString(defaultCipherText);
		return defaultCipherString;
	}
}









