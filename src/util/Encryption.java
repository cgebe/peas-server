package util;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class Encryption {

	/**
	 * Encrypt the plain text using symmetrical key.
	 * 
	 * @param text
	 *            : original plain text
	 * @param key
	 *            :The public key
	 * @return Encrypted bytes
	 * @throws java.lang.Exception
	 */
	public static byte[] AESencrypt(byte[] toEncrypt, SecretKey key, IvParameterSpec iv) {
		byte[] cipherBytes = null;
		try {
			// get an RSA cipher object and print the provider
			final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			// encrypt the plain text using the public key
			cipher.init(Cipher.ENCRYPT_MODE, key, iv);
			cipherBytes = cipher.doFinal(toEncrypt);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cipherBytes;
	}

	/**
	 * Decrypt text using symmetrical key.
	 * 
	 * @param text
	 *            :encrypted text
	 * @param key
	 *            :The private key
	 * @return Decrypted bytes
	 * @throws java.lang.Exception
	 */
	public static byte[] AESdecrypt(byte[] toDecrypt, SecretKey key, IvParameterSpec iv) {
		byte[] decryptedBytes = null;
		try {
			// get an RSA cipher object and print the provider
			final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

			// decrypt the text using the private key
			cipher.init(Cipher.DECRYPT_MODE, key, iv);
			decryptedBytes = cipher.doFinal(toDecrypt);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return decryptedBytes;
	}
	
	
    public static SecretKey generateNewKey() throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(128);
        SecretKey key = keygen.generateKey();
        //AEScipher.init(Cipher.DECRYPT_MODE, key, iv);
        return key;
    }
    
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
