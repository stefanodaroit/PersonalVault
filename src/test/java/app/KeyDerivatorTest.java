package app;

import javax.crypto.spec.SecretKeySpec;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.security.SecureRandom;
import java.util.Random;

import app.core.KeyDerivator;
import app.core.KeyDerivator.InvalidPasswordException;
import app.core.KeyDerivator.InvalidSaltException;
import static app.core.Constants.SALT_LENGTH;

/**
 * Unit test for KeyDerivator class
 */
public class KeyDerivatorTest{

  private KeyDerivator kd = new KeyDerivator();
  private Random gen = new SecureRandom();
  
  @Test
  public void testCorrectPsw() throws Exception {   
    String password = "SecretP@ssword1234";
    kd.setPsw(password);
    byte[] hashedBytes = kd.getMasterKey();
    assertEquals(64, hashedBytes.length);
  }

  @Test
  public void testNullPsw() throws Exception {   
    try {
      kd.setPsw(null);
      Assert.fail("NullPointerException not thrown");
    } catch(NullPointerException e) {}
  }

  @Test
  public void testNullPswValidate() throws Exception {   
    try {
      kd.validatePassword(null);
      Assert.fail("NullPointerException not thrown");
    } catch(NullPointerException e) {}
  }

  @Test
  public void testShortPsw() {     
    try {
      String password = "P@ssword123"; 
      kd.validatePassword(password);
      Assert.fail("InvalidPasswordException not thrown");
    } catch(InvalidPasswordException e) {}
  }

  @Test
  public void testLongPsw() {     
    try {
      String password = "SecretP@ssword1234fffffffffffffffffffffffffffffffffffffffffffffff";
      kd.validatePassword(password);
      Assert.fail("InvalidPasswordException not thrown");
    } catch(InvalidPasswordException e) {}
  }

  @Test
  public void testNoSpecialPsw() {
    try {
      String password = "SecretPassword1234";
      kd.validatePassword(password);
      Assert.fail("InvalidPasswordException not thrown");
    } catch(InvalidPasswordException e) {}
  }

  @Test
  public void testNoUpperCasePsw() {     
    try {
      String password = "secretp@ssword1234";
      kd.validatePassword(password);
      Assert.fail("InvalidPasswordException not thrown");
    } catch(InvalidPasswordException e) {}
  }

  @Test
  public void testNoDigitPsw() {     
    try {
      String password = "SecretP@ssword";
      kd.validatePassword(password);
      Assert.fail("InvalidPasswordException not thrown");
    } catch(InvalidPasswordException e) {}
  }

  @Test
  public void testNoLowerCasePsw() {     
    try {
      String password = "SECRETP@SSWORD1234";
      kd.validatePassword(password);
      Assert.fail("InvalidPasswordException not thrown");
    } catch(InvalidPasswordException e) {}
  }

  @Test
  public void testSalt() throws Exception {     
    try {
      byte[] salt = new byte[SALT_LENGTH - 1];
      gen.nextBytes(salt);
      kd = new KeyDerivator(salt);
      Assert.fail("InvalidSaltException not thrown");
    } catch(InvalidSaltException e) {}
  }

  @Test
  public void testSaltNull() throws Exception {     
    try {
      kd = new KeyDerivator(null);
      Assert.fail("NullPointerException not thrown");
    } catch(NullPointerException e) {}
  }

  @Test
  public void testSameMasterKey() throws Exception {   
    String password = "dhfmn284BBB'''13.";
    kd.setPsw(password);
    byte[] hashedBytes = kd.getMasterKey();

    byte[] salt2 = kd.getSalt();
    KeyDerivator kd2 = new KeyDerivator(salt2);
    kd2.setPsw(password);
    byte[] hashedBytes2 = kd2.getMasterKey();

    assertEquals(new SecretKeySpec(hashedBytes, "AES"), new SecretKeySpec(hashedBytes2, "AES"));
  }
  
}

