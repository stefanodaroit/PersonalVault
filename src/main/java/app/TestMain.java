package app;

import app.core.Directory;
import app.core.File;
import app.core.Vault;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.file.Path;

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
      keygen.init(256); // bits
      SecretKey encKey = keygen.generateKey();

//      File fe = new File("./", "README.md");
//      String encFilename = fe.encrypt(encKey, Path.of("."));
//      System.out.println(encFilename);
//
//      File fd = new File("./", encFilename);
//      String originalFilename = fd.decrypt(encKey, Path.of("./output")); // folder must exists beforehand
//      System.out.println(originalFilename);

      Path dstBaseFolderPath = Path.of("./output");

      Directory doe = new Directory("src/main");
      String encFilename = doe.encrypt(encKey, dstBaseFolderPath);

      Directory dod = new Directory(Path.of(dstBaseFolderPath.toString(), encFilename).toString());
      String originalFilename = dod.decrypt(encKey, Path.of("./output")); // destination base folder must exists beforehand
      System.out.println(originalFilename);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
