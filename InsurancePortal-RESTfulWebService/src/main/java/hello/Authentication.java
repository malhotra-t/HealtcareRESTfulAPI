package hello;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.tomcat.util.codec.binary.Base64;

public class Authentication {
	
	public static String encrypt(String key, String origText){
		try{
			byte[] keyBytes = new String(key).getBytes("UTF-8");
			
			SecretKeySpec myDesKey = new SecretKeySpec(Arrays.copyOf(keyBytes, 16), "AES"); 
		    Cipher desCipher;

		    // Create the cipher
		    desCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

		    // Initialize the cipher for encryption
		    desCipher.init(Cipher.ENCRYPT_MODE, myDesKey);

		    //sensitive information
		    byte[] text = origText.getBytes("UTF-8");

		    System.out.println("Text [Byte Format] : " + text);
		    System.out.println("Text : " + new String(text));

		    // Encrypt the text
		    String textEncrypted = Base64.encodeBase64String(desCipher.doFinal(text));

		    System.out.println("Text Encryted : " + textEncrypted);
		    return new String(""+textEncrypted);

		}catch(NoSuchAlgorithmException e){
			e.printStackTrace();
		}catch(NoSuchPaddingException e){
			e.printStackTrace();
		}catch(InvalidKeyException e){
			e.printStackTrace();
		}catch(IllegalBlockSizeException e){
			e.printStackTrace();
		}catch(BadPaddingException e){
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";

		
	}
	
	
	public static String decrypt(String key, String encryptedVal){
		try{

			byte[] keyBytes = new String(key).getBytes("UTF-8");
			SecretKeySpec myDesKey = new SecretKeySpec(Arrays.copyOf(keyBytes, 16), "AES"); 
		    Cipher desCipher;

		    // Create the cipher
		    desCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

		    // Initialize the same cipher for decryption
		    desCipher.init(Cipher.DECRYPT_MODE, myDesKey);

		    // Decrypt the text
		    byte[] textDecrypted = desCipher.doFinal(Base64.decodeBase64(encryptedVal));

		    System.out.println("Text Decryted : " + new String(textDecrypted));
		    return new String(textDecrypted);

		}catch(NoSuchAlgorithmException e){
			e.printStackTrace();
		}catch(NoSuchPaddingException e){
			e.printStackTrace();
		}catch(InvalidKeyException e){
			e.printStackTrace();
		}catch(IllegalBlockSizeException e){
			e.printStackTrace();
		}catch(BadPaddingException e){
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";

		
	}
}
