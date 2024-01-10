package app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import app.KeyDerivator.InvalidPasswordException;
import app.KeyDerivator.InvalidSaltException;

public class Vault {

  private static final String ALG_HMAC_TOK= "HmacSHA512";
  private static final char   PERIOD = '.';

  // TO DO: UUID
  private static int counter = 0;
  private int vid;
  
  private String storagePath;
  private VaultConfiguration conf;
  private byte[] confMac;

  private KeyManager km;
  
  /**
   * Create a new vault in "path" using "password" for keys derivation
   * @param storagePath  The path in which store the vault
   * @param psw          The password used for key derivation
   * 
   * @throws IOException
   * @throws InternalException
   * @throws InvalidPasswordException
   */
  public Vault(String storagePath, String psw) throws IOException, InternalException, InvalidPasswordException {
    this.vid = counter ++;
    this.storagePath = storagePath;
    
    try {
      // Create and wrap secret keys
      this.km = new KeyManager();
      this.km.wrapSecretKeys(psw);  
    } catch (InvalidPasswordException e) { 
      throw e;
    } catch (Exception e) {
      throw new InternalException();
    }
    
    // Create and save vault configuration
    this.conf = new VaultConfiguration(this.vid, this.km.getSalt(), this.km.getWrapEncKey(), this.km.getWrapAuthKey());
    writeConfiguration();
  }

  /**
   * Import an existing vault
   * 
   * @param vid int: vault ID
   * @param storagePath String: vault storage path
   * 
   * @throws InvalidConfigurationException
   * @throws IOException
   */
  public Vault(int vid, String storagePath) throws InvalidConfigurationException, IOException {
    this.vid = vid;
    this.storagePath = storagePath;
    
    try {
      // Read vault configuration and init key manager
      readConfiguration();
      this.km = new KeyManager(this.conf.getEncKey(), this.conf.getAuthKey(), this.conf.getSalt());
    } catch (InvalidSaltException e) {
      throw new InvalidConfigurationException();
    }
  }

  public void lock() {}
  
  /**
   * Unlock the files in the vault
   * 
   * @param psw String: password used for keys derivation
   * 
   * @throws InvalidConfigurationException
   * @throws WrongPasswordException
   * @throws InternalException
   */
  public void unlock(String psw) throws InvalidConfigurationException, WrongPasswordException, InternalException {
    try {
      // Unwrap secret keys through input password
      this.km.unwrapSecretKeys(psw);
    } catch (InvalidPasswordException | InvalidKeyException e) {
      throw new WrongPasswordException();
    } catch (Exception e) {
      throw new InternalException();
    }
    
    // Check configuration file integrity
    try {
      // Recompute HMAC of the current configuration
      byte[] serializedConf = VaultConfiguration.serialize(this.conf);
      byte[] encodedConf = Base64.getEncoder().withoutPadding().encode(serializedConf);
      byte[] mac = getHmac(ALG_HMAC_TOK, this.km.getMasterKey(), encodedConf);
      
      // Check the two MACs; if not equal the configuration have been tampered
      if (!Arrays.equals(this.confMac, mac)) {
        System.out.println("Configuration integrity check failed");
        throw new InvalidConfigurationException();
      }
    } catch (IOException e) {
      System.out.println("Error while checking configuration integrity");
      throw new InvalidConfigurationException();
    }
  }

  /**
   * Read the vault configuration from the file system
   * 
   * @throws InvalidConfigurationException
   * @throws IOException
   */
  public void readConfiguration() throws InvalidConfigurationException, IOException {    
    System.out.print("Reading configuration file... ");
    
    try {
      // Read and decode token from the vault root
      byte[] token = Files.readAllBytes(Paths.get(VaultConfiguration.getPath(this.storagePath, this.vid)));
      byte[] serializedConf = decodeToken(token); 
      
      // Deserialize VaultConfiguration object
      this.conf = VaultConfiguration.deserialize(serializedConf);
    } catch (IOException e) {
      System.out.println("Error while reading configuration file");
      throw e;
    } catch (ClassNotFoundException e) {
      System.out.println("Error while deserializing configuration file");
      throw new InvalidConfigurationException();
    }
    
    System.out.println("DONE");
  }

 /**
  * Saves the vault configuration on the file system
  *
  * @throws IOException
 * @throws InternalException
  */
  public void writeConfiguration() throws IOException, InternalException {
    System.out.print("Writing configuration file... ");
    
    try {
      // Serialize the VaultConfiguration object 
      byte[] serializedConf = VaultConfiguration.serialize(this.conf);
      
      // Generate a token with MAC and save it in the vault root
      byte[] token = encodeToken(serializedConf);
      Files.write(Paths.get(VaultConfiguration.getPath(this.storagePath, this.vid)), token);
    } catch (IOException e) {
      System.out.println("Error while saving configuration file");
      throw e;
    }
    
    System.out.println("DONE");
  }

   /**
   * Generates a base64 encoded token to save on file system including a MAC
   * 
   * @param payload byte[]: the content to save as array of bytes
   * 
   * @return byte[] base64 encoded token as array of bytes
   * @throws InternalException
   */
  private byte[] encodeToken(byte[] payload) throws InternalException {
    // Encode payload in base64
    byte[] encodedPayload = Base64.getEncoder().withoutPadding().encode(payload);

    // Generate MAC from encoded payload and encode MAC
    byte[] macResult = getHmac(ALG_HMAC_TOK, this.km.getMasterKey(), encodedPayload);
    byte[] encodedMac = Base64.getEncoder().withoutPadding().encode(macResult);

    // Generate token joining encoded payload and encoded MAC
    byte[] token = new byte[encodedPayload.length + 1 + encodedMac.length];
    System.arraycopy(encodedPayload, 0, token, 0, encodedPayload.length);
    token[encodedPayload.length] = (byte) PERIOD;
    System.arraycopy(encodedMac,  0, token, encodedPayload.length + 1, encodedMac.length);

    return token;
  }

  /**
   * Extract the payload from a base64 encoded token and store the MAC
   * 
   * @param token byte[]: the token
   * 
   * @return byte[] payload: the content of the token
   */
  private byte[] decodeToken(byte[] token) {
    String tmp = new String(token);
    String tok[] = tmp.split("\\" + PERIOD);

    byte[] decodedPayload = Base64.getDecoder().decode(tok[0].getBytes());
    this.confMac = Base64.getDecoder().decode(tok[1].getBytes());

    return decodedPayload;
  }

   /**
   * Computes MAC for an array of bytes and sign it with a key
   * 
   * @param alg   String:    MAC algorithm
   * @param key   SecretKey: key for MAC signature
   * @param bytes byte[]:    bytes for which compute the MAC
   * 
   * @return byte[] MAC array of bytes
   * @throws InternalException
   */
  private static byte[] getHmac(String alg, SecretKey key, byte[] bytes) throws InternalException {
    byte[] macResult = null;
    
    try {
      Mac mac = Mac.getInstance(alg);
      mac.init(key);
      macResult = mac.doFinal(bytes);
    } catch (Exception e) {   
      System.out.println("Error while computing HMAC");
      throw new InternalException();
    }

    return macResult;
  }

  public static class InvalidConfigurationException extends Exception { 
    public InvalidConfigurationException() { 
      super("Invalid configuration file"); 
    }
  }

  public static class InternalException extends Exception { 
    public InternalException() { 
      super("Internal error"); 
    }
  }

  public static class WrongPasswordException extends Exception { 
    public WrongPasswordException() { 
      super("The password is wrong"); 
    }
  }
} 