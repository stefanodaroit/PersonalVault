package app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
  
  private static final String DIR = PATH + "/tmpDir", 
                              SUBDIR = "subDir",
                              FILE1 = "file1", 
                              FILE2 = "file2",
                              INVDIR = "/home/app/";

  private static final Path PLOCAL = Path.of("."),
                            PDIR = Path.of(DIR), 
                            PSUBDIR = Path.of(DIR, SUBDIR), 
                            PFILE1 = Path.of(DIR, FILE1), 
                            PFILE2 = Path.of(DIR, SUBDIR, FILE2), 
                            PINV = Path.of(INVDIR);

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
    Files.createDirectories(PSUBDIR);
    Files.createFile(PFILE1);
    Files.createFile(PFILE2);
  }

  public static void deleteDirectory(Path path) throws IOException {
    Files.walk(path)
      .sorted(Comparator.reverseOrder())
      .map(Path::toFile)
      .forEach(File::delete);
  }

  public static void deleteConfig(Vault v) throws IOException {
    if (v == null) { return; }
    
    File vaultDir = v.getStoragePath().toFile(); 
    if (vaultDir != null) {
      deleteDirectory(v.getStoragePath());
      vaultDir.delete(); 
    }
  }

  // @Test
  public void testCreateVault() throws Exception {
    v = new Vault(NAME, PLOCAL, PSW);
    assertNotNull(v);
    deleteConfig(v);
  }

  @Test
  public void testImportVault() throws Exception {
    v = new Vault(new Vault(NAME, PLOCAL, PSW).getVid(), NAME, PLOCAL);
    assertNotNull(v);
    deleteConfig(v);
  }

  @Test
  public void testImportDamagedVault() throws Exception {   
    final char PERIOD = '.';

    try {
      v = new Vault(NAME, PLOCAL, PSW);

      // Tamper header
      Path path = VaultConfiguration.getPath(v.getStoragePath(), v.getVid());
      byte[] token = Files.readAllBytes(path);
      token = Arrays.copyOfRange(token, new String(token).indexOf(PERIOD), token.length);
      Files.write(path, token);

      // Import vault
      v = new Vault(v.getVid(), NAME, PLOCAL);
      
      Assert.fail("InvalidConfigurationException not thrown");
    } catch (InvalidConfigurationException e) {
      deleteConfig(v);
    }

    try {
      v = new Vault(NAME, PLOCAL, PSW);

      // Tamper header
      Path path = VaultConfiguration.getPath(v.getStoragePath(), v.getVid());
      byte[] token = Files.readAllBytes(path);
      swap(token, 0, 1);
      Files.write(path, token);

      // Import vault
      v = new Vault(v.getVid(), NAME, PLOCAL);
      
      Assert.fail("InvalidConfigurationException not thrown");
    } catch (InvalidConfigurationException e) {
      deleteConfig(v);
    }

    try {
      v = new Vault(NAME, PLOCAL, PSW);

      // Tamper serialization
      Path path = VaultConfiguration.getPath(v.getStoragePath(), v.getVid());
      byte[] token = Files.readAllBytes(path);
      int idx = new String(token).indexOf(PERIOD);
      swap(token, idx + 1, idx + 2);
      Files.write(path, token);

      // Import vault
      v = new Vault(v.getVid(), NAME, PLOCAL);
      
      Assert.fail("IOException not thrown");
    } catch (IOException e) {
      deleteConfig(v);
    }
    
    try {
      v = new Vault(NAME, PLOCAL, PSW);

      // Tamper HMAC
      Path path = VaultConfiguration.getPath(v.getStoragePath(), v.getVid());
      byte[] token = Files.readAllBytes(path);
      int idx = new String(token).lastIndexOf(PERIOD);
      swap(token, idx + 1, idx + 2);
      Files.write(path, token);

      // Import vault and unlock
      v = new Vault(v.getVid(), NAME, PLOCAL);
      deleteDirectory(v.unlock(PSW, PLOCAL));
      
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
      new Vault(NAME, PLOCAL, null);
      Assert.fail("NullPointerException not thrown");
    } catch (NullPointerException e) {}

    try {
      new Vault(v.getVid(), NAME, null);
      Assert.fail("NullPointerException not thrown");
    } catch (NullPointerException e) {}
    try {
      UUID vid = null;
      new Vault(vid, NAME, PLOCAL);
      Assert.fail("NullPointerException not thrown");
    } catch (NullPointerException e) {}
  }

  @Test
  public void testInvalidParams() throws Exception {
    try {
      new Vault(NAME, PINV, PSW);
      Assert.fail("IOException not thrown");
    } catch (IOException e) {}

    try {
      v = new Vault(NAME, PLOCAL, PSW);
      new Vault(v.getVid(), NAME, PINV);
      Assert.fail("IOException not thrown");
    } catch (IOException e) {
      deleteConfig(v);
    }

    try {
      new Vault(NAME, PLOCAL, "password");
      Assert.fail("InvalidPasswordException not thrown");
    } catch (InvalidPasswordException e) {}

    try {
      new Vault("../Vault", PLOCAL, "password");
      Assert.fail("IllegalArgumentException not thrown");
    } catch (IllegalArgumentException e) {}

    String name = null;
    deleteConfig(new Vault(name, PLOCAL, PSW));

    deleteConfig(new Vault("", PLOCAL, PSW));
  }

  @Test
  public void testWrongPsw() throws Exception {   
    try {
      v = new Vault(NAME, PLOCAL, PSW);
      deleteDirectory(v.unlock(PSW + "!", PLOCAL));
      Assert.fail("WrongPasswordException not thrown");
    } catch (WrongPasswordException e) {
      deleteConfig(v);
    }
  }

  @Test
  public void testChangePsw() throws Exception {   
    v = new Vault(NAME, PLOCAL, PSW);

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
    v = new Vault(v.getVid(), NAME, PLOCAL);
    v.changePsw(PSW + "!", PSW + "!!");
      
    deleteConfig(v);
  }

  @Test
  public void testFillVault() throws Exception {   
    createTmpDir();
    
    // New vault and add directory and file
    v = new Vault(NAME, PLOCAL, PSW);
    v.addDirectory(PDIR);
    v.addFile(PFILE2);     

    // Import vault and add subdirectory to vault root
    // v = new Vault(v.getVid(), NAME, PLOCAL);
    // deleteDirectory(v.unlock(PSW, PLOCAL));
    // v.addDirectory(PSUBDIR);

    try {
      v.addDirectory(null); 
      Assert.fail("IllegalArgumentException not thrown");
    } catch (IllegalArgumentException e) {}

    try {
      v.addFile(null); 
      Assert.fail("IllegalArgumentException not thrown");
    } catch (IllegalArgumentException e) {}

    deleteConfig(v);
    
    // Try to add non-existing directory
    try {
      v = new Vault(NAME, PLOCAL, PSW);
      v.addDirectory(PINV); 
      Assert.fail("IllegalArgumentException not thrown");
    } catch (IllegalArgumentException e) {
      deleteConfig(v);
    }

    // Try to add already existing directory
    // try {
    //   v = new Vault(NAME, PLOCAL, PSW);
    //   v.addDirectory(PDIR); 
    //   v.addDirectory(PDIR); 
    //   Assert.fail("IOException not thrown");
    // } catch (IOException e) {
    //   deleteConfig(v);
    // }

    // Try to edit a locked vault
    try {
      v = new Vault(new Vault(NAME, PLOCAL, PSW).getVid(), NAME, PLOCAL);
      v.addDirectory(PDIR); 
      Assert.fail("VaultLockedException not thrown");
    } catch (VaultLockedException e) {
      deleteConfig(v);
    }

    deleteDirectory(PDIR);
  }  
}
