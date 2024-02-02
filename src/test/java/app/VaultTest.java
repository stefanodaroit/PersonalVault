package app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import app.core.Vault;
import app.core.VaultConfiguration;
import app.core.KeyDerivator.InvalidPasswordException;
import app.core.Vault.InvalidConfigurationException;
import app.core.Vault.VaultLockedException;
import app.core.Vault.WrongPasswordException;
import junit.framework.TestCase;

public class VaultTest extends TestCase{
  
  private Vault v;
  private static final String PSW = "SecretP@ssword1234";
  private static final String PATH = ".";
  private static final String NAME = "Vault";
  private static final String DIR = PATH + "/tmpDir", SUBDIR = "subDir";
  private static final String FILE1 = "file1", FILE2 = "file2";
  private static final String INVPATH = "/home/app/";

  public static void swap(byte[] array, int idx1, int idx2) {
    if (array[idx1] == array[idx2]) {
      if (array[idx1] != '0')
        array[idx1] = '0';
      else 
        array[idx1] = '1';
    }
    
    byte tmp = array[idx1];
    array[idx1] = array[idx2];
    array[idx2] = tmp;
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

  @Test
  public void testCreateVault() throws Exception {
    v = new Vault(NAME, PATH, PSW);
    assertNotNull(v);
    deleteConfig(v);
  }

  @Test
  public void testImportVault() throws Exception {
    v = new Vault(new Vault(NAME, PATH, PSW).getVid(), NAME, PATH);
    assertNotNull(v);
    deleteConfig(v);
  }

  @Test
  public void testImportDamagedVault() throws Exception {   
    final char PERIOD = '.';

    try {
      v = new Vault(NAME, PATH, PSW);

      // Tamper header
      Path path = VaultConfiguration.getPath(v.getStoragePath(), v.getVid());
      byte[] token = Files.readAllBytes(path);
      token = Arrays.copyOfRange(token, new String(token).indexOf(PERIOD), token.length);
      Files.write(path, token);

      // Import vault
      v = new Vault(v.getVid(), NAME, PATH);
      
      Assert.fail("InvalidConfigurationException not thrown");
    } catch (InvalidConfigurationException e) {
      deleteConfig(v);
    }

    try {
      v = new Vault(NAME, PATH, PSW);

      // Tamper header
      Path path = VaultConfiguration.getPath(v.getStoragePath(), v.getVid());
      byte[] token = Files.readAllBytes(path);
      swap(token, 0, 1);
      Files.write(path, token);

      // Import vault
      v = new Vault(v.getVid(), NAME, PATH);
      
      Assert.fail("InvalidConfigurationException not thrown");
    } catch (InvalidConfigurationException e) {
      deleteConfig(v);
    }

    try {
      v = new Vault(NAME, PATH, PSW);

      // Tamper serialization
      Path path = VaultConfiguration.getPath(v.getStoragePath(), v.getVid());
      byte[] token = Files.readAllBytes(path);
      int idx = new String(token).indexOf(PERIOD);
      swap(token, idx + 1, idx + 2);
      Files.write(path, token);

      // Import vault
      v = new Vault(v.getVid(), NAME, PATH);
      
      Assert.fail("IOException not thrown");
    } catch (IOException e) {
      deleteConfig(v);
    }
    
    try {
      v = new Vault(NAME, PATH, PSW);

      // Tamper HMAC
      Path path = VaultConfiguration.getPath(v.getStoragePath(), v.getVid());
      byte[] token = Files.readAllBytes(path);
      int idx = new String(token).lastIndexOf(PERIOD);
      swap(token, idx + 1, idx + 2);
      Files.write(path, token);

      // Import vault and unlock
      v = new Vault(v.getVid(), NAME, PATH);
      v.unlock(PSW);
      
      Assert.fail("InvalidConfigurationException not thrown");
    } catch (InvalidConfigurationException e) {
      deleteConfig(v);
    }
  }

  @Test
  public void testNullArgument() throws Exception {
    try {
      new Vault(NAME, null, PSW);
      Assert.fail("NullPointerException not thrown");
    } catch (NullPointerException e) {}
    
    try {
      new Vault(NAME, PATH, null);
      Assert.fail("NullPointerException not thrown");
    } catch (NullPointerException e) {}

    try {
      new Vault(v.getVid(), NAME, null);
      Assert.fail("NullPointerException not thrown");
    } catch (NullPointerException e) {}
    try {
      UUID vid = null;
      new Vault(vid, NAME, PATH);
      Assert.fail("NullPointerException not thrown");
    } catch (NullPointerException e) {}
  }

  @Test
  public void testInvalidParams() throws Exception {
    try {
      new Vault(NAME, INVPATH, PSW);
      Assert.fail("IOException not thrown");
    } catch (IOException e) {}

    try {
      v = new Vault(NAME, PATH, PSW);
      new Vault(v.getVid(), NAME, INVPATH);
      Assert.fail("IOException not thrown");
    } catch (IOException e) {
      deleteConfig(v);
    }

    try {
      new Vault(NAME, PATH, "password");
      Assert.fail("InvalidPasswordException not thrown");
    } catch (InvalidPasswordException e) {}

    try {
      new Vault("../Vault", PATH, "password");
      Assert.fail("IllegalArgumentException not thrown");
    } catch (IllegalArgumentException e) {}

    String name = null;
    deleteConfig(new Vault(name, PATH, PSW));

    deleteConfig(new Vault("", PATH, PSW));
  }

  @Test
  public void testWrongPsw() throws Exception {   
    try {
      v = new Vault(NAME, PATH, PSW);
      v.unlock(PSW + "!");
      Assert.fail("WrongPasswordException not thrown");
    } catch (WrongPasswordException e) {
      deleteConfig(v);
    }
  }

  @Test
  public void testChangePsw() throws Exception {   
    v = new Vault(NAME, PATH, PSW);

    // Wrong old password
    try {
      v.changePsw(PSW + "!", PSW + "!");
      Assert.fail("WrongPasswordException not thrown");
    } catch (WrongPasswordException e) {}

    // Invalid new password
    try { 
      v.changePsw(PSW, "password");
      Assert.fail("InvalidPasswordException not thrown");
    } catch (InvalidPasswordException e) {}
    
    // Valid change
    v.changePsw(PSW, PSW + "!");

    // Import and change psw
    v = new Vault(v.getVid(), NAME, PATH);
    v.changePsw(PSW + "!", PSW + "!!");
      
    deleteConfig(v);
  }

  @Test
  public void testFillVault() throws Exception {   
    createTmpDir();
    
    // New vault and add directory and file
    v = new Vault(NAME, PATH, PSW);
    v.addDirectory(DIR);
    v.addFile(DIR + '/' + SUBDIR + '/' + FILE2);     

    // Import vault and add subdirectory to vault root
    v = new Vault(v.getVid(), NAME, PATH);
    v.unlock(PSW);
    v.addDirectory(DIR + '/' + SUBDIR);

    try {
      v.addDirectory(null); 
      Assert.fail("NullPointerException not thrown");
    } catch (NullPointerException e) {}

    try {
      v.addFile(null); 
      Assert.fail("NullPointerException not thrown");
    } catch (NullPointerException e) {}

    deleteConfig(v);
    
    // Try to add non-existing directory
    try {
      v = new Vault(NAME, PATH, PSW);
      v.addDirectory(INVPATH); 
      Assert.fail("IOException not thrown");
    } catch (IOException e) {
      deleteConfig(v);
    }

    // Try to add already existing directory
    try {
      v = new Vault(NAME, PATH, PSW);
      v.addDirectory(DIR); 
      v.addDirectory(DIR); 
      Assert.fail("IOException not thrown");
    } catch (IOException e) {
      deleteConfig(v);
    }

    // Try to edit a locked vault
    try {
      v = new Vault(new Vault(NAME, PATH, PSW).getVid(), NAME, PATH);
      v.addDirectory(DIR); 
      Assert.fail("VaultLockedException not thrown");
    } catch (VaultLockedException e) {
      deleteConfig(v);
    }

    deleteDirectory(DIR);
  }  
}
