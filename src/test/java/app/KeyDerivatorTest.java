package app;

import java.io.IOException;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.apache.commons.codec.binary.BinaryCodec;

import app.core.KeyDerivator;
import app.core.KeyDerivator.InvalidPasswordException;
import app.core.KeyDerivator.InvalidSaltException;

/**
 * Unit test for KeyDerivator class
 */
public class KeyDerivatorTest{

  private KeyDerivator kd = new KeyDerivator();
  
  @Test
  public void testCorrectPsw() throws InvalidPasswordException
  {   
    String password = "dhfmn284BBB'''13.";
    kd.setPsw(password);
    byte[] hashedBytes = kd.getMasterKey();

    assertEquals(64, hashedBytes.length);
  }

  @Test
  public void testNullPsw() throws InvalidPasswordException
  {   
    String exceptionMessage = "";
    try {
      kd.setPsw(null);
    } catch(IllegalArgumentException e) {
      exceptionMessage = e.getMessage();
    }

    assertEquals("The password cannot be null", exceptionMessage);
  }

  @Test
  public void testNullPswValidate() throws InvalidPasswordException
  {   
    String exceptionMessage = "";
    try {
      kd.validatePassword(null);
    } catch(IllegalArgumentException e) {
      exceptionMessage = e.getMessage();
    }

    assertEquals("The password cannot be null", exceptionMessage);
  }

  @Test
  public void testShortPws()
  {     
    String exceptionMessage = "";
    try {
      String password = "d";
      kd.validatePassword(password);;
    } catch(KeyDerivator.InvalidPasswordException e) {
      exceptionMessage = e.getMessage();
    }

    assertEquals("The password must contain at least 12 characters", exceptionMessage);
  }

  @Test
  public void testLongPws()
  {     
    String exceptionMessage = "";
    try {
      String password = "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
      kd.validatePassword(password);;
    } catch(KeyDerivator.InvalidPasswordException e) {
      exceptionMessage = e.getMessage();
    }

    assertEquals("The password must contain a maximum of 64 characters", exceptionMessage);
  }

  @Test
  public void testNoSpecialPws()
  {     
    String exceptionMessage = "";
    try {
      String password = "ffffffffffffffff";
      kd.validatePassword(password);
    } catch(KeyDerivator.InvalidPasswordException e) {
      exceptionMessage = e.getMessage();
    }

    assertEquals("The password must contain at least one special character", exceptionMessage);
  }

  @Test
  public void testNoUpperCasePws()
  {     
    String exceptionMessage = "";
    try {
      String password = "ffffffffffffffff?";
      kd.validatePassword(password);
    } catch(KeyDerivator.InvalidPasswordException e) {
      exceptionMessage = e.getMessage();
    }

    assertEquals("The password must contain at least one upper case character", exceptionMessage);
  }

  @Test
  public void testNoDigitPws()
  {     
    String exceptionMessage = "";
    try {
      String password = "ffffffffffffffff?F";
      kd.validatePassword(password);
    } catch(KeyDerivator.InvalidPasswordException e) {
      exceptionMessage = e.getMessage();
    }

    assertEquals("The password must contain at least one number", exceptionMessage);
  }

  @Test
  public void testNoLowerCasePws()
  {     
    String exceptionMessage = "";
    try {
      String password = "FFFFFFFFFFFFF?1";
      kd.validatePassword(password);
    } catch(KeyDerivator.InvalidPasswordException e) {
      exceptionMessage = e.getMessage();
    }

    assertEquals("The password must contain at least one lower case character", exceptionMessage);
  }

  @Test
  public void testSalt() throws IOException
  {     
    String exceptionMessage = "";
    try {
      byte[] salt = new BinaryCodec().toByteArray("1000000111010000");
      kd = new KeyDerivator(salt);
    } catch(KeyDerivator.InvalidSaltException e) {
      exceptionMessage = e.getMessage();
    }

    assertEquals("The salt must be 128 bytes long", exceptionMessage);
  }

  @Test
  public void testSaltNull() throws IOException, InvalidSaltException
  {     
    String exceptionMessage = "";
    try {
      kd = new KeyDerivator(null);
    } catch(IllegalArgumentException e) {
      exceptionMessage = e.getMessage();
    }

    assertEquals("The salt cannot be null", exceptionMessage);
  }

  @Test
  public void testSameMasterKey() throws InvalidSaltException, InvalidPasswordException
  {   
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

