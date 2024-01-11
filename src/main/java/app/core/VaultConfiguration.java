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
  
  public VaultConfiguration(int vid, byte[] salt, byte[] encKey, byte[] authKey) {
    this.vid = vid;
    this.salt = salt;
    this.encKey = encKey;
    this.authKey = authKey;
  }

  public byte[] getSalt() {
    return this.salt;
  }

  public byte[] getEncKey() {
    return this.encKey;
  }

  public byte[] getAuthKey() {
    return this.authKey;
  }

  public void setSalt(byte[] salt) {
    this.salt = salt;
  }

  public void setEncKey(byte[] encKey) {
    this.encKey = encKey;
  }

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

  public static String getPath(String path, int vid) {
    return path + "/" + vid + "-config.vault";
  }

  public static byte[] serialize(VaultConfiguration conf) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(bos);  
    out.writeObject(conf);

    byte[] serialized = bos.toByteArray();

    out.close();
    bos.close();
    
    return serialized; 
  }

  public static VaultConfiguration deserialize(byte[] serialized) throws IOException, ClassNotFoundException {
    ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
    ObjectInputStream in = new ObjectInputStream(bis);
    VaultConfiguration conf = (VaultConfiguration) in.readObject();
      
    in.close();
    bis.close();
    
    return conf; 
  }

}
