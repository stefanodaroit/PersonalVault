package app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Base64;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import app.KeyDerivator.InvalidSaltException;

public class Vault {

  private final String ALG_HMAC_TOK= "HmacSHA512";
  private final char   PERIOD = '.';

  // TO DO: UUID
  private static int counter = 0;
  private int vid;
  
  private String storagePath;
  private VaultConfiguration conf;
  private byte[] confMac;

  private KeyManager km;
  
  public Vault(String storagePath, String psw) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException {
    this.vid = counter ++;
    this.storagePath = storagePath;
    
    // Create and wrap secret keys
    this.km = new KeyManager();
    this.km.wrapSecretKeys(psw);
    
    // Create and save vault configuration
    this.conf = new VaultConfiguration(this.vid, this.km.getSalt(), this.km.getWrapEncKey(), this.km.getWrapAuthKey());
    writeConfiguration();
  }

  public Vault(int vid, String storagePath) throws InvalidSaltException {
    this.vid = vid;
    this.storagePath = storagePath;
    
    // Read vault configuration and init key manager
    readConfiguration();
    this.km = new KeyManager(this.conf.getEncKey(), this.conf.getAuthKey(), this.conf.getSalt());
  }


  public void lock() {}
  
  public void unlock(String psw) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException {
    this.km.unwrapSecretKeys(psw);
    
    // try {
    //   byte[] serializedConf = VaultConfiguration.serialize(this.conf);
    //   byte[] encodedConf = Base64.getEncoder().withoutPadding().encode(serializedConf);
    //   byte[] mac = getHmac(ALG_HMAC_TOK, this.km.getMasterKey(), encodedConf);
    //   System.out.println(mac.equals(confMac));
    // } catch (IOException e) {
    //   e.printStackTrace();
    // }
  }

  /*
   * Read the vault configuration from the file system
   */
  public void readConfiguration() {    
    System.out.print("Reading configuration file... ");
    
    try {
      // Read and decode token from the vault root
      byte[] token = Files.readAllBytes(Paths.get(VaultConfiguration.getPath(this.storagePath, this.vid)));
      byte[] serializedConf = decodeToken(token); 
      
      // Deserialize VaultConfiguration object
      this.conf = VaultConfiguration.deserialize(serializedConf);
    } catch (Exception e) {
      System.out.println("Error while reading configuration file");
    }
    
    System.out.println("DONE");
  }

 /**
  * Saves the vault configuration on the file system
  */
  public void writeConfiguration() {
    System.out.print("Writing configuration file... ");
    
    try {
      // Serialize the VaultConfiguration object 
      byte[] serializedConf = VaultConfiguration.serialize(this.conf);
      
      // Generate a token with MAC and save it in the vault root
      byte[] token = encodeToken(serializedConf);
      Files.write(Paths.get(VaultConfiguration.getPath(this.storagePath, this.vid)), token);
    } catch (IOException e) {
      System.out.println("Error while saving configuration file");
    }
    
    System.out.println("DONE");
  }

   /**
   * Generates a base64 encoded token to save on file system including a MAC
   * 
   * @param payload byte[]: the content to save as array of bytes
   * 
   * @return byte[] base64 encoded token as array of bytes
   */
  private byte[] encodeToken(byte[] payload) {
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
   */
  private static byte[] getHmac(String alg, SecretKey key, byte[] bytes) {
    byte[] macResult = null;
    
    try {
      Mac mac = Mac.getInstance(alg);
      mac.init(key);
      macResult = mac.doFinal(bytes);
    } catch (InvalidKeyException e) {   
      System.out.println("Invalid Key for MAC");
    } catch (NoSuchAlgorithmException e) {
      System.out.println("Invalid Algorithm for MAC");
    }  

    return macResult;
  }

}
