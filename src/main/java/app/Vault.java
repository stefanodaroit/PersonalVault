package app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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

public class Vault {

  private final String ALG_HMAC_TOK= "HmacSHA512";
  private final char   PERIOD = '.';

  // TO DO: UUID
  private static int counter = 0;
  
  public String storagePath;
  public VaultConfiguration conf;

  private KeyManager km;
  
  Vault(String storagePath, String psw) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException {
    this.storagePath = storagePath;
    
    // Create and wrap secret keys
    this.km = new KeyManager();
    this.km.wrapSecretKeys(psw);
    
    // Create and save vault configuration
    this.conf = new VaultConfiguration(counter++, storagePath, this.km.getSalt(), this.km.getWrapEncKey(), this.km.getWrapAuthKey());
    writeConfiguration();
  }

 /**
  * Saves the vault configuration on the file system
  */
  public void writeConfiguration() {
    System.out.print("Writing configuration file... ");
    
    try {
      // Serialize the VaultConfiguration object 
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bos);  
      out.writeObject(this.conf);
      byte[] serializedConf = bos.toByteArray();      
      
      // Generate a token with MAC and save it in a file in the vault root
      byte[] token = createToken(serializedConf);
      Files.write(Paths.get(this.conf.getPath()), token);
      
      out.close();
      bos.close();
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
  private byte[] createToken(byte[] payload) {
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
