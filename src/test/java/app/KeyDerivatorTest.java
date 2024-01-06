package app;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.commons.codec.binary.BinaryCodec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import app.logic.KeyDerivator;
import app.logic.KeyDerivator.InvalidPasswordException;

/**
 * Unit test for KeyDerivator class
 */
public class KeyDerivatorTest{

    private KeyDerivator kd = new KeyDerivator();
    
    @Test
    public void testCorrectPsw()
    {   
        String password = "dhfmn284BBB'''13.";
        kd.setPsw(password);
        byte[] hashedBytes = kd.getMasterKey();

        assertEquals(64, hashedBytes.length);
    }

    @Test
    public void testShortPws()
    {     
        String exceptionMessage = "";
        try{
            String password = "d";
            kd.validateMasterPassword(password);;
        } catch(KeyDerivator.InvalidPasswordException e) {
            exceptionMessage = e.getMessage();
        }
    
        assertEquals("The password must contain at least 12 characters", exceptionMessage);
    }

    @Test
    public void testLongPws()
    {     
        String exceptionMessage = "";
        try{
            String password = "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
            kd.validateMasterPassword(password);;
        } catch(KeyDerivator.InvalidPasswordException e) {
            exceptionMessage = e.getMessage();
        }
    
        assertEquals("The password must contain a maximum of 64 characters", exceptionMessage);
    }

    @Test
    public void testNoSpecialPws()
    {     
        String exceptionMessage = "";
        try{
            String password = "ffffffffffffffff";
            kd.validateMasterPassword(password);
        } catch(KeyDerivator.InvalidPasswordException e) {
            exceptionMessage = e.getMessage();
        }
    
        assertEquals("The password must contain at least one special character", exceptionMessage);
    }

    @Test
    public void testNoUpperCasePws()
    {     
        String exceptionMessage = "";
        try{
            String password = "ffffffffffffffff?";
            kd.validateMasterPassword(password);
        } catch(KeyDerivator.InvalidPasswordException e) {
            exceptionMessage = e.getMessage();
        }
    
        assertEquals("The password must contain at least one upper case character", exceptionMessage);
    }

    @Test
    public void testNoDigitPws()
    {     
        String exceptionMessage = "";
        try{
            String password = "ffffffffffffffff?F";
            kd.validateMasterPassword(password);
        } catch(KeyDerivator.InvalidPasswordException e) {
            exceptionMessage = e.getMessage();
        }
    
        assertEquals("The password must contain at least one number", exceptionMessage);
    }

    @Test
    public void testNoLowerCasePws()
    {     
        String exceptionMessage = "";
        try{
            String password = "FFFFFFFFFFFFF?1";
            kd.validateMasterPassword(password);
        } catch(KeyDerivator.InvalidPasswordException e) {
            exceptionMessage = e.getMessage();
        }
    
        assertEquals("The password must contain at least one lower case character", exceptionMessage);
    }

    @Test
    public void testSalt() throws IOException
    {     
        String exceptionMessage = "";
        try{
            byte[] salt = new BinaryCodec().toByteArray("1000000111010000");
            kd = new KeyDerivator(salt, 1000);
        } catch(KeyDerivator.InvalidSaltException e) {
            exceptionMessage = e.getMessage();
        }
    
        assertEquals("The salt must be 128 long", exceptionMessage);
    }
   
}

