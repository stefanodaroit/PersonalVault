package app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import app.core.Vault;
import app.core.VaultConfiguration;

import app.core.Vault;

public class TestMain {
  private static final String PSW = "SecretP@ssword1234";
  private static final String PATH = ".";
  private static final String NAME = "Vault";
  private static final String DIR = PATH + "/tmpDir", SUBDIR = "subDir";
  private static final String FILE1 = "file1", FILE2 = "file2";
  private static final String INVPATH = "/home/app/";
  public static void main(String[] args) {
    String psw = "Password1234!";
    Vault v;
    try {
      // Create vault
      createTmpDir();
    
    // New vault and add directory and file
      v = new Vault(NAME, PATH, PSW);
      
      v.addDirectory(DIR);
      v.addFile(DIR + '/' + SUBDIR + '/' + FILE2);

      // Import vault and add subdirectory to vault root
      v = new Vault(v.getVid(), NAME, PATH);
      v.unlock(PSW);
      v.addDirectory(DIR + '/' + SUBDIR);
      v.unlock(PSW);
      //v.changePsw(psw, "eee");
      //v.unlock(psw);
      //deleteConfig(v);
      //deleteDirectory(DIR);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void createTmpDir() throws IOException {
    Files.createDirectories(Paths.get(DIR, SUBDIR));
    Files.createFile(Paths.get(DIR, FILE1));
    Files.createFile(Paths.get(DIR, SUBDIR, FILE2));
  }

   public static void deleteDirectory(String path) throws IOException {
    Files.walk(Paths.get(path))
      .sorted(Comparator.reverseOrder())
      .map(Path::toFile)
      .forEach(File::delete);
  }

  public static void deleteConfig(Vault v) throws IOException {
    if (v == null) { return; }
    
    File vaultDir = new File(v.getStoragePath()); 
    if (vaultDir != null) {
      deleteDirectory(v.getStoragePath());
      vaultDir.delete(); 
    }
  }
}
