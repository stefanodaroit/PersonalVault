package app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import app.core.Vault;
import app.core.VaultConfiguration;
import app.core.KeyDerivator.InvalidPasswordException;
import app.core.Vault.InvalidConfigurationException;
import app.core.Vault.WrongPasswordException;
import junit.framework.TestCase;

public class VaultTest extends TestCase{
  
  private Vault v;
  private final String PSW = "SecretP@ssword1234";
  private final String PATH = ".";

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

  public static void deleteConfig(Vault v) {
    if (v == null) { return; }
    
    File conf = new File(VaultConfiguration.getPath(v.getStoragePath(), v.getVid())); 
    if (conf != null) { conf.delete(); }
  }

  @Test
  public void testCreateVault() throws Exception {
    v = new Vault(PATH, PSW, null);
    assertNotNull(v);
    deleteConfig(v);
  }

  @Test
  public void testImportVault() throws Exception {
    v = new Vault(new Vault(PATH, PSW, null).getVid().toString(), PATH);
    assertNotNull(v);
    deleteConfig(v);
  }

  @Test
  public void testImportDamagedVault() throws Exception {   
    final char PERIOD = '.';

    try {
      v = new Vault(PATH, PSW, null);

      // Tamper header
      Path path = Paths.get(VaultConfiguration.getPath(v.getStoragePath(), v.getVid()));
      byte[] token = Files.readAllBytes(path);
      token = Arrays.copyOfRange(token, new String(token).indexOf(PERIOD), token.length);
      Files.write(path, token);

      // Import vault
      v = new Vault(v.getVid().toString(), PATH);
      
      Assert.fail("InvalidConfigurationException not thrown");
    } catch (InvalidConfigurationException e) {
      deleteConfig(v);
    }

    try {
      v = new Vault(PATH, PSW, null);

      // Tamper header
      Path path = Paths.get(VaultConfiguration.getPath(v.getStoragePath(), v.getVid()));
      byte[] token = Files.readAllBytes(path);
      swap(token, 0, 1);
      Files.write(path, token);

      // Import vault
      v = new Vault(v.getVid().toString(), PATH);
      
      Assert.fail("InvalidConfigurationException not thrown");
    } catch (InvalidConfigurationException e) {
      deleteConfig(v);
    }

    try {
      v = new Vault(PATH, PSW, null);

      // Tamper serialization
      Path path = Paths.get(VaultConfiguration.getPath(v.getStoragePath(), v.getVid()));
      byte[] token = Files.readAllBytes(path);
      int idx = new String(token).indexOf(PERIOD);
      swap(token, idx + 1, idx + 2);
      Files.write(path, token);

      // Import vault
      v = new Vault(v.getVid().toString(), PATH);
      
      Assert.fail("IOException not thrown");
    } catch (IOException e) {
      deleteConfig(v);
    }
    
    try {
      v = new Vault(PATH, PSW, null);

      // Tamper HMAC
      Path path = Paths.get(VaultConfiguration.getPath(v.getStoragePath(), v.getVid()));
      byte[] token = Files.readAllBytes(path);
      int idx = new String(token).lastIndexOf(PERIOD);
      swap(token, idx + 1, idx + 2);
      Files.write(path, token);

      // Import vault and unlock
      v = new Vault(v.getVid().toString(), PATH);
      v.unlock(PSW);
      
      Assert.fail("InvalidConfigurationException not thrown");
    } catch (InvalidConfigurationException e) {
      deleteConfig(v);
    }
  }

  @Test
  public void testIllegalArgument() throws Exception {
    try {
      new Vault(null, PSW, null);
      Assert.fail("IllegalArgumentException not thrown");
    } catch (IllegalArgumentException e) {}
    
    try {
      new Vault(PATH, null, null);
      Assert.fail("IllegalArgumentException not thrown");
    } catch (IllegalArgumentException e) {}

    try {
      new Vault("0", null);
      Assert.fail("IllegalArgumentException not thrown");
    } catch (IllegalArgumentException e) {}
    try {
      new Vault(null, PATH);
      Assert.fail("IllegalArgumentException not thrown");
    } catch (IllegalArgumentException e) {}
  }

  @Test
  public void testInvalidPath() throws Exception {
    final String invalidPath = "/home/app/";
    v = new Vault(PATH, PSW, null);
    
    try {
      new Vault(invalidPath, PSW, null);
      Assert.fail("IOException not thrown");
    } catch (IOException e) {}

    try {
      new Vault(v.getVid().toString(), invalidPath);
      Assert.fail("IOException not thrown");
    } catch (IOException e) {}
  }

  @Test
  public void testInvalidPsw() throws Exception {    
    try {
      new Vault(PATH, "password", null);
      Assert.fail("InvalidPasswordException not thrown");
    } catch (InvalidPasswordException e) {}
  }

  @Test
  public void testWrongPsw() throws Exception {   
    try {
      v = new Vault(PATH, PSW, null);
      v.unlock(PSW + "!");
      Assert.fail("WrongPasswordException not thrown");
    } catch (WrongPasswordException e) {
      deleteConfig(v);
    }
  }

  @Test
  public void testChangePsw() throws Exception {   
    v = new Vault(PATH, PSW, null);

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
    v = new Vault(v.getVid().toString(), PATH);
    v.changePsw(PSW + "!", PSW + "!!");
      
    deleteConfig(v);
  }

 
    
}
