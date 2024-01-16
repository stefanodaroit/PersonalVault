package app.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import app.core.KeyDerivator.InvalidPasswordException;
import app.core.KeyDerivator.InvalidSaltException;

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
    if (storagePath == null || psw == null) {
      throw new IllegalArgumentException("Invalid vault parameters");
    }
    
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
    if (storagePath == null) {
      throw new IllegalArgumentException("Invalid vault parameters");
    }
    
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
      byte[] encodedConf = encodeToken(serializedConf);
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
   * Change the password and set the new configuration
   * 
   * @param oldPsw String  Old password
   * @param newPsw String  New password
   * @throws InvalidKeyException
   * @throws NoSuchAlgorithmException
   * @throws NoSuchProviderException
   * @throws NoSuchPaddingException
   * @throws InvalidPasswordException
   * @throws InternalException
   * @throws IllegalBlockSizeException
   * @throws IOException
   * @throws WrongPasswordException
   */
  public void changePsw(String oldPsw, String newPsw) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidPasswordException, InternalException, IllegalBlockSizeException, IOException, WrongPasswordException{
    try {
      // Unwrap secret keys through input password
      this.km.unwrapSecretKeys(oldPsw);
    } catch (InvalidPasswordException | InvalidKeyException e) {
      throw new WrongPasswordException();
    } 

    try {
      // Create and wrap secret keys
      this.km = new KeyManager();
      this.km.wrapSecretKeys(newPsw);  
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
   * Read the vault configuration from the file system
   * 
   * @throws InvalidConfigurationException
   * @throws IOException
   */
  protected void readConfiguration() throws InvalidConfigurationException, IOException {    
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
  protected void writeConfiguration() throws IOException, InternalException {
    System.out.print("Writing configuration file... ");
    
    try {
      // Serialize the VaultConfiguration object 
      byte[] serializedConf = VaultConfiguration.serialize(this.conf);
      
      // Generate a token with MAC and save it in the vault root
      byte[] token = encodeSignedToken(serializedConf);
      Files.write(Paths.get(VaultConfiguration.getPath(this.storagePath, this.vid)), token);
    } catch (IOException e) {
      System.out.println("Error while saving configuration file");
      throw e;
    }
    
    System.out.println("DONE");
  }


  /**
   * Encodes header and payload in base64
   * 
   * @param payload 
   * 
   * @return byte[] base64 encoded token (header.payload)
   */
  private byte[] encodeToken(byte[] payload) {
    // Encode header in base64
    byte[] encodedHeader = Base64.getEncoder().withoutPadding().encode(ALG_HMAC_TOK.getBytes());
    
    // Encode payload in base64
    byte[] encodedPayload = Base64.getEncoder().withoutPadding().encode(payload);

    // Generate token joining encoded payload and encoded MAC
    byte[] token = new byte[encodedHeader.length + 1 + encodedPayload.length];
    System.arraycopy(encodedHeader, 0, token, 0, encodedHeader.length);
    token[encodedHeader.length] = (byte) PERIOD;
    System.arraycopy(encodedPayload,  0, token, encodedHeader.length + 1, encodedPayload.length);

    return token;
  }

   /**
   * Encodes a base64 signed token to save on file system
   * 
   * @param payload byte[]: the content to save as array of bytes
   * 
   * @return byte[] base64 signed token as array of bytes (header.payload.MAC)
   * @throws InternalException
   */
  private byte[] encodeSignedToken(byte[] payload) throws InternalException {
    byte[] token = encodeToken(payload);

    // Generate MAC from encoded payload and encode MAC
    this.confMac = getHmac(ALG_HMAC_TOK, this.km.getMasterKey(), token);
    byte[] encodedMac = Base64.getEncoder().withoutPadding().encode(this.confMac);

    // Generate token joining encoded payload and encoded MAC
    byte[] signedToken = new byte[token.length + 1 + encodedMac.length];
    System.arraycopy(token, 0, signedToken, 0, token.length);
    signedToken[token.length] = (byte) PERIOD;
    System.arraycopy(encodedMac,  0, signedToken, token.length + 1, encodedMac.length);

    return signedToken;
  }

  /**
   * Extract the payload from a base64 signed token and store the MAC
   * 
   * @param token byte[]: the token
   * 
   * @return byte[] payload: the content of the token
   * 
   * @throws InvalidConfigurationException
   */
  private byte[] decodeToken(byte[] token) throws InvalidConfigurationException {
    String tmp = new String(token);
    
    // Check if token has 3 fields (header + payload + MAC)
    String tok[] = tmp.split("\\" + PERIOD);
    if (tok.length != 3) {
      throw new InvalidConfigurationException();
    }

    // Decode header and check if the algorithm is correct (only HMACSHA512 at the moment)
    byte[] header  = Base64.getDecoder().decode(tok[0]);
    if (!Arrays.equals(header, ALG_HMAC_TOK.getBytes())) {
      throw new InvalidConfigurationException();
    }

    // Decode payload and MAC
    byte[] payload = Base64.getDecoder().decode(tok[1]);
    this.confMac   = Base64.getDecoder().decode(tok[2]);

    return payload;
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

  public int getVid() {
    return this.vid;
  }

  public String getStoragePath() {
    return this.storagePath;
  }

  public VaultConfiguration getVaultConfiguration() {
    return this.conf;
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