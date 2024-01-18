package app;

import java.io.IOException;
import java.util.Random;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import org.junit.Test;

import static app.core.Constants.SALT_LENGTH;
import static org.junit.Assert.assertEquals;

import app.core.KeyManager;
import app.core.KeyDerivator.InvalidPasswordException;
import app.core.KeyDerivator.InvalidSaltException;

public class KeyManagerTest {

  private KeyManager km = new KeyManager();
  private Random gen = new SecureRandom();
  
  @Test
  public void testSaltNull() throws IOException, InvalidSaltException
  {     
    String exceptionMessage = "";
    try {
      km = new KeyManager(null, null, null);
    } catch(IllegalArgumentException e) {
      exceptionMessage = e.getMessage();
    }

    assertEquals("The salt cannot be null", exceptionMessage);
  }

  @Test
  public void testSaltNotNull() throws Exception {     
    try {
      byte[] salt = new String("1000000111010000").getBytes();
      km = new KeyManager(salt, salt, salt);
    } catch(InvalidSaltException e) {}
  }

  @Test
  public void testAuthKeyNull() throws IOException, InvalidSaltException
  {     
    String exceptionMessage = "";
    try {
      byte[] salt = new byte[SALT_LENGTH];
      gen.nextBytes(salt);
      km = new KeyManager(new String("1000000111010000").getBytes(), null, salt);
    } catch(IllegalArgumentException e) {
      exceptionMessage = e.getMessage();
    }

    assertEquals("The authentication key cannot be null", exceptionMessage);
  }

  @Test
  public void testEncKeyNull() throws IOException, InvalidSaltException
  {     
    String exceptionMessage = "";
    try {
      byte[] salt = new byte[SALT_LENGTH];
      gen.nextBytes(salt);
      km = new KeyManager(null, new String("1000000111010000").getBytes(), salt);
    } catch(IllegalArgumentException e) {
      exceptionMessage = e.getMessage();
    }

    assertEquals("The encryption key cannot be null", exceptionMessage);
  }

  @Test
  public void testWrapNullPsw() throws IOException, InvalidSaltException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException, InvalidPasswordException
  {     
    String exceptionMessage = "";
    try {
      km.wrapSecretKeys(null);
    } catch(IllegalArgumentException e) {
      exceptionMessage = e.getMessage();
    }

    assertEquals("The password cannot be null", exceptionMessage);
  }

  @Test
  public void testUnwrapNullPsw() throws IOException, InvalidSaltException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException, InvalidPasswordException
  {     
    String exceptionMessage = "";
    try {
      km.unwrapSecretKeys(null);
    } catch(IllegalArgumentException e) {
      exceptionMessage = e.getMessage();
    }

    assertEquals("The password cannot be null", exceptionMessage);
  }

}
