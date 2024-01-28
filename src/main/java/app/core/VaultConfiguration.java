package app.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class VaultConfiguration implements Serializable {
  
  private final int vid;    // Vault ID
  
  private byte[] salt;      // Salt used for key derivation
  private byte[] encKey;    // Encrypted symmetric key for files' header
  private byte[] authKey;   // Encrypted symmetric key for HMAC
  
  /**
   * Constructor used to initialize the vault configuration with the provided 
   * vault ID, salt, encrypted file header key, and encrypted HMAC key
   * 
   * @param vid int  vault ID
   * @param salt byte[]  salt used for key derivation
   * @param encKey byte[]  encrypted symmetric key for files' header
   * @param authKey byte[]  encrypted symmetric key for HMAC
   */
  public VaultConfiguration(int vid, byte[] salt, byte[] encKey, byte[] authKey) {
    this.vid = vid;
    this.salt = salt;
    this.encKey = encKey;
    this.authKey = authKey;
  }

  /**
   * Method used to retrieve the salt
   * 
   * @return byte[]  salt used for key derivation
   */
  public byte[] getSalt() {
    return this.salt;
  }

  /**
   * Method used to retrieve the encrypted symmetric key for files' header
   * 
   * @return byte[]  Encrypted symmetric key for files' header
   */
  public byte[] getEncKey() {
    return this.encKey;
  }

  /**
   * Method used to retrieve the encrypted symmetric key for HMAC
   * 
   * @return byte[]  Encrypted symmetric key for HMAC
   */
  public byte[] getAuthKey() {
    return this.authKey;
  }

  /**
   * Method used to the set the salt used for key derivation
   * 
   * @param salt byte[]  salt used for key derivation
   */
  public void setSalt(byte[] salt) {
    this.salt = salt;
  }

  /**
   * Method used to set the encrypted symmetric key for files' header
   * 
   * @param encKey byte[]  Encrypted symmetric key for files' header
   */
  public void setEncKey(byte[] encKey) {
    this.encKey = encKey;
  }

  /**
   * Method used to set the encrypted symmetric key for HMAC
   * 
   * @param authKey byte[]  Encrypted symmetric key for HMAC
   */
  public void setAuthKey(byte[] authKey) {
    this.authKey = authKey;
  }

  @Override
  public String toString() {
    return 
    "{ \n" +
      "\tvid: "     + vid + "\n" +
      "\tsalt: "    + new String(salt)    + "\n" +
      "\tencKey: "  + new String(encKey)  + "\n" +
      "\tauthKey: " + new String(authKey) + "\n" + 
    "}";
  }

  /**
   * Method used to generate a path for storing the serialized configuration file 
   * based on a given directory path and vault ID.
   * 
   * @param path String  Directory path
   * @param vid int  Vault ID
   * @return
   */
  public static String getPath(String path, int vid) {
    return path + "/" + vid + "-config.vault";
  }

  /**
   * Static method that serializes a VaultConfiguration object into a byte array.
   * 
   * @param conf VaultConfiguration  Vault configuration with the provided vault ID, salt, encrypted file header key, and encrypted HMAC key
   * @return byte[]  byte array of VaultConfiguration
   * @throws IOException
   */
  public static byte[] serialize(VaultConfiguration conf) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(bos);  
    out.writeObject(conf);

    byte[] serialized = bos.toByteArray();

    out.close();
    bos.close();
    
    return serialized; 
  }

  /**
   * Static method that deserializes a byte array into a VaultConfiguration object.
   * 
   * @param serialized byte[]  byte array of serialized VaultConfiguration
   * @return VaultConfiguration  Vault configuration with the provided vault ID, salt, encrypted file header key, and encrypted HMAC key
   * @throws IOException
   * @throws ClassNotFoundException
   */
  public static VaultConfiguration deserialize(byte[] serialized) throws IOException, ClassNotFoundException {
    ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
    ObjectInputStream in = new ObjectInputStream(bis);
    VaultConfiguration conf = (VaultConfiguration) in.readObject();
      
    in.close();
    bis.close();
    
    return conf; 
  }

}
