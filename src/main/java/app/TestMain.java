package app;

import app.core.Constants;
import app.core.File;
import app.core.Vault;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TestMain {
  public static void main(String[] args) {
    String psw = "Password1234!";
    Vault v;
    try {
      // Create vault
//      v = new Vault("test", ".", psw);

//      v.changePsw(psw, "eee");
//       v.unlock(psw);
//      byte[] ek = v.getVaultConfiguration().getEncKey();
//      System.out.println(ek.length);

//      SecretKey s = new SecretKeySpec(ek, "AES");


      KeyGenerator keygen = KeyGenerator.getInstance("AES");
      keygen.init(128);
      SecretKey encKey = keygen.generateKey();

      File fe = new File("./", "README.md");
      byte[] encOutput = fe.encryptHeader(encKey);
      String filename = new String(encOutput, StandardCharsets.UTF_8);
      System.out.println(filename);
      fe.encrypt();

      File fd = new File("./", "README.md.enc");
      fd.decryptHeader(encKey, encOutput);
      fd.decrypt();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
