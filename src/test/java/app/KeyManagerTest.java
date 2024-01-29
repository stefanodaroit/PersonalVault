package app;

import java.util.Random;
import java.security.SecureRandom;

import org.junit.Assert;
import org.junit.Test;

import static app.core.Constants.SALT_LENGTH;

import app.core.KeyManager;
import app.core.KeyDerivator.InvalidSaltException;

public class KeyManagerTest {

  private KeyManager km = new KeyManager();
  private Random gen = new SecureRandom();
  
  @Test
  public void testSaltNull() throws Exception {     
    try {
      km = new KeyManager(null, null, null);
      Assert.fail("NullPointerException not thrown");
    } catch(NullPointerException e) {}
  }

  @Test
  public void testSaltNotNull() throws Exception {     
    try {
      byte[] salt = new byte[SALT_LENGTH + 1];
      gen.nextBytes(salt);
      km = new KeyManager(salt, salt, salt);
      Assert.fail("InvalidSaltException not thrown");
    } catch(InvalidSaltException e) {}
  }

  @Test
  public void testAuthKeyNull() throws Exception {
    try {
      byte[] salt = new byte[SALT_LENGTH];
      gen.nextBytes(salt);
      km = new KeyManager(salt, null, salt);
      Assert.fail("NullPointerException not thrown");
    } catch(NullPointerException e) {}
  }

  @Test
  public void testEncKeyNull() throws Exception {     
    try {
      byte[] salt = new byte[SALT_LENGTH];
      gen.nextBytes(salt);
      km = new KeyManager(null, salt, salt);
      Assert.fail("NullPointerException not thrown");
    } catch(NullPointerException e) {}
  }

  @Test
  public void testWrapNullPsw() throws Exception {     
    try {
      km.wrapSecretKeys(null);
      Assert.fail("NullPointerException not thrown");
    } catch(NullPointerException e) {}
  }

  @Test
  public void testUnwrapNullPsw() throws Exception {     
    try {
      km.unwrapSecretKeys(null);
      Assert.fail("NullPointerException not thrown");
    } catch(NullPointerException e) {}
  }

}
