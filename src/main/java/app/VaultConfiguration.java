package app;

import java.io.Serializable;

public class VaultConfiguration implements Serializable {
  
  private final int vid;    // Vault ID
  private String path;      // Configuration file path
  
  private byte[] salt;      // Salt used for key derivation
  private byte[] encKey;    // Encrypted symmetric key for files' header
  private byte[] authKey;   // Encrypted symmetric key for HMAC
  
  public VaultConfiguration(int vid, String path, byte[] salt, byte[] encKey, byte[] authKey) {
    this.vid = vid;
    this.path = path;
    this.salt = salt;
    this.encKey = encKey;
    this.authKey = authKey;
  }

  public String getPath() {
    return this.path + "/" + vid + "-config.vault";
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

}
