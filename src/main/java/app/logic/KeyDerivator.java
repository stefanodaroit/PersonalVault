package app.logic;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

import javax.crypto.spec.PBEKeySpec;


import javax.crypto.*;

public class KeyDerivator {
  private byte[] password;
  private byte[] salt;

  private static final Random GEN = new SecureRandom();

  private final int ITERATIONS;
  private final int DERIVED_KEY_LENGTH = 512; 

  private final int MIN_MASTER_PASSWORD_LENGTH = 12;
  private final int MAX_MASTER_PASSWORD_LENGTH = 64;
  private final int MIN_SALT_LENGTH = 128;
  private final int MAX_SALT_LENGTH = 256;

  public KeyDerivator() { 
    this.salt = getNextSalt();
    this.ITERATIONS = 210000;
  }

  public KeyDerivator(byte[] salt, int iter) throws InvalidSaltException { 
    this.salt = salt;
    
    String stringSalt = new String(salt, StandardCharsets.UTF_8);

    if (stringSalt.length() < MIN_SALT_LENGTH || stringSalt.length() > MAX_SALT_LENGTH) {
        throw new InvalidSaltException("Salt length must be at least " + MIN_SALT_LENGTH + " and mist be at most " + MAX_SALT_LENGTH);
    }
    this.ITERATIONS = iter;
   }

  public void setPsw(String psw){
   
      try {
        validateMasterPassword(psw);
        this.password = psw.getBytes(StandardCharsets. UTF_8);

      } catch (InvalidPasswordException e) {
        // TODO Auto-generated catch block
        System.out.println(e.getMessage());
      }


  }

  public void changePsw(String oldPsw, String newPsw){



  }

  public static byte[] getNextSalt() {
		byte[] salt = new byte[128];
		GEN.nextBytes(salt);
		return salt;
	}

  public byte[] getMasterKey(){
      char[] passwordChars = new String(password, StandardCharsets.UTF_8).toCharArray();

      return hashPassword(passwordChars, salt, ITERATIONS, DERIVED_KEY_LENGTH);
  }

  private static byte[] hashPassword( final char[] password, final byte[] salt, final int iterations, final int keyLength ) {

        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance( "PBKDF2WithHmacSHA512");
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKey key = skf.generateSecret(spec);
            byte[] res = key.getEncoded( );
            return res;
        } catch ( NoSuchAlgorithmException | InvalidKeySpecException e ) {
            throw new RuntimeException( e );
        }
    }

  protected void validateMasterPassword(String masterPassword) throws InvalidPasswordException {
    if (masterPassword.length() < MIN_MASTER_PASSWORD_LENGTH) {
      throw new InvalidPasswordException("Too few characters");
        
    }
    else if (masterPassword.length() > MAX_MASTER_PASSWORD_LENGTH) {
      throw new InvalidPasswordException("Too many characters");
        
    }
    else if (masterPassword.matches("^(?=.*[A-Z])(?= !\"#$%&'()*+,-./:;<=>?@[\\^_`{|}~])(?=.*[0-9])(?=.*[a-z])")) {
      throw new InvalidPasswordException("Invalid password");  
    }
  }

  class InvalidPasswordException extends Exception { 

    public InvalidPasswordException(String message) 
    { 
        super(message); 
    } 
  }  

  class InvalidSaltException extends Exception { 

    public InvalidSaltException(String message) 
    { 
        super(message); 
    } 
  }
  
}
