package app.core;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import app.core.KeyDerivator.InvalidPasswordException;
import app.core.KeyDerivator.InvalidSaltException;

import static app.core.Constants.*;

public class KeyManager {

  private KeyDerivator kd;                    // Key Derivator for generate master key
  private SecureRandom gen;                   // Secure random bytes generator 

  private byte[] wrapEncKey;                  // Wrapped encryption key
  private byte[] wrapAuthKey;                 // Wrapped authentication key
  private SecretKey encryptionKey;            // Encryption key
  private SecretKey authenticationKey;        // Authentication (or MAC) key

  private byte[] masterKey;                   // Master key         

  /**
   * KeyManager class constructor: 
   * generates encryption and authentication secret keys randomly
   */
  public KeyManager(){

    // New KeyDerivator with a random salt
    this.kd = new KeyDerivator();
    this.gen = new SecureRandom();

    // Generate 256 bits for the encryption key
    byte[] encKey = new byte[32];
    gen.nextBytes(encKey);
    // Convert 256 bits of encryption key to secret key
    this.encryptionKey = new SecretKeySpec(encKey, "AES");

    // Generate 256 bits for the authentication key
    byte[] authKey = new byte[32];
    gen.nextBytes(authKey);
    // Convert 256 bits of authentication key to secret key
    this.authenticationKey = new SecretKeySpec(authKey, "AES");
  }

  /**
   * Class constructor specifying stored encryption and authentication keys, and the salt
   * 
   * @param encKey byte[]  stored encryption key
   * @param authKey byte[]  stored authentication key
   * @throws InvalidSaltException if the salt is not 128 bits length
   */
  public KeyManager(byte[] encKey, byte[] authKey, byte[] salt) throws InvalidSaltException{

    // New KeyDerivator with a given salt
    this.kd = new KeyDerivator(salt);

    // If the salt is null, generate an IllegalArgumentException
    if(encKey == null){
      throw new NullPointerException("The encryption key cannot be null");
    }
    // If the salt is null, generate an IllegalArgumentException
    if(authKey == null){
      throw new NullPointerException("The authentication key cannot be null");
    }

    // Store wrapped keys
    this.wrapEncKey = encKey;
    this.wrapAuthKey = authKey;
}

  /**
   * Method to encrypt the two secret kets, i.e. encryption and authentication keys,
   * with the 512 bit master key splitted in two parts and with the algorithm AESWrap of SunJCE provider.
   * 
   * @param psw String  password given as input
   * 
   * @throws NoSuchAlgorithmException
   * @throws NoSuchProviderException
   * @throws NoSuchPaddingException
   * @throws InvalidKeyException
   * @throws IllegalBlockSizeException
   * @throws InvalidPasswordException
   */
  public void wrapSecretKeys(String psw) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, InvalidPasswordException{
      
    // Set the password anche check its validity
    kd.setPsw(psw);

    // Get the master key based on the setted password and salt
    this.masterKey = kd.getMasterKey();

    // Generate the two secret keys
    SecretKey firstKey = getEncKEK(this.masterKey);
    SecretKey secondKey = getAuthKEK(this.masterKey);

    // Wrap the encryption key
    Cipher cipher = Cipher.getInstance(ALG_WRAP_KEYS, PROV_WRAP_KEYS);
    cipher.init(Cipher.WRAP_MODE, firstKey);
    wrapEncKey = cipher.wrap(encryptionKey);

    // Wrap the authentication key
    cipher.init(Cipher.WRAP_MODE, secondKey);
    wrapAuthKey = cipher.wrap(authenticationKey);
  }
  
  /**
   * Method to decrypt the two secret kets, i.e. encryption and authentication keys,
   * with the 512 bit master key splitted in two parts and with the algorithm AESWrap of SunJCE provider.
   * 
   * @param psw String  password given as input
   * @throws InvalidKeyException
   * @throws NoSuchAlgorithmException
   * @throws NoSuchProviderException
   * @throws NoSuchPaddingException
   * @throws InvalidPasswordException
   */
  public void unwrapSecretKeys(String psw) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidPasswordException{

    // Set the password anche check its validity
    kd.setPsw(psw);
    
    // Get the master key based on the setted password and salt
    this.masterKey = kd.getMasterKey();

    // Generate the two secret keys
    SecretKey firstKey = getEncKEK(this.masterKey);
    SecretKey secondKey = getAuthKEK(this.masterKey);

    // Unwrap the encryption key
    Cipher cipher = Cipher.getInstance(ALG_WRAP_KEYS, PROV_WRAP_KEYS);
    cipher.init(Cipher.UNWRAP_MODE, firstKey);
    encryptionKey = (SecretKey) cipher.unwrap(wrapEncKey, "AES", Cipher.SECRET_KEY);

    // Unwrap the authentication key
    cipher.init(Cipher.UNWRAP_MODE, secondKey);
    authenticationKey = (SecretKey) cipher.unwrap(wrapAuthKey, "AES", Cipher.SECRET_KEY);
  }

  /**
   * Generate the 256 bit secret key from the first 256 bit of the 512 bit master key
   * 
   * @param masterKey 512 bit master key
   * @return first 256 bit secret key
   */
  private SecretKey getEncKEK(byte[] masterKey){

    // First 256 bits of the master key
    byte[] first256 = new byte[32];
    System.arraycopy(masterKey, 0, first256, 0, first256.length);

    // Generate the secret key from first 256 bits of the master key
    return new SecretKeySpec(first256, "AES");
  }

  /**
   * Generate the 256 bit secret key from the last 256 bit of the 512 bit master key
   * 
   * @param masterKey 512 bit master key
   * @return last 256 bit secret key
   */
  private SecretKey getAuthKEK(byte[] masterKey){

    // Last 256 bits of the master key
    byte[] second256 = new byte[32];
    System.arraycopy(masterKey, 32, second256, 0, second256.length);

    // Generate the secret key from the last 256 bits of the master key
    return new SecretKeySpec(second256, "AES");
  }

  /**
   * Get the master key
   * 
   * @return SecretKey master key
   */
  public SecretKey getMasterKey() {
    return new SecretKeySpec(this.masterKey, "AES");
  }

  /**
   * Get key derivator salt
   * 
   * @return byte[] salt
   */
  public byte[] getSalt() {
    return this.kd.getSalt();
  }

  /**
   * Get the wrapped authentication key
   * 
   * @return byte[]  wrapped authentication key
   */
  public byte[] getWrapAuthKey() {
    return this.wrapAuthKey;
  }

  /**
   * Get the wrapped encryption key
   * 
   * @return byte[]  wrapped encryption key
   */
  public byte[] getWrapEncKey() {
    return this.wrapEncKey;
  }

  /**
   * Get the unwrapped encryption key
   * 
   * @return byte[]  unwrapped encryption key
   */
  public SecretKey getUnwrapEncKey() {
    return this.encryptionKey;
  }

  /**
   * Get the unwrapped authentication key
   * 
   * @return byte[]  unwrapped authentication key
   */
  public SecretKey getUnwrapAuthKey() {
    return this.authenticationKey;
  }

}
