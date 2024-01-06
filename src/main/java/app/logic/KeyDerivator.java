package app.logic;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.crypto.spec.PBEKeySpec;
import javax.crypto.*;

import java.io.*;
import java.util.regex.*;

public class KeyDerivator {
  private byte[] password;
  private byte[] salt;

  private static final Random GEN = new SecureRandom();

  private final int ITERATIONS;
  private final int DERIVED_KEY_LENGTH = 512; 

  private final int MIN_MASTER_PASSWORD_LENGTH = 12;
  private final int MAX_MASTER_PASSWORD_LENGTH = 64;
  private final int SALT_LENGTH = 128;

  List<String> weakPasswords = new ArrayList<String>();
  Path path = Paths.get("src/main/resources/WeakPasswords.txt");

  public KeyDerivator(){ 
    this.salt = getNextSalt();
    this.ITERATIONS = 210000;

    path = path.toAbsolutePath();
    String absolutePath = path.toString();

    try{
      BufferedReader bf = new BufferedReader(new FileReader(absolutePath));
      String line = bf.readLine();
        
      while (line != null) {
          weakPasswords.add(line);
          line = bf.readLine();
      }
      bf.close();
    } catch (IOException ex) {
      System.out.println(ex.getMessage());
    }    
  }


  public KeyDerivator(byte[] salt, int iter) throws InvalidSaltException, IOException { 
    String stringSalt = new String(salt, StandardCharsets.UTF_8);

    if (stringSalt.length() != SALT_LENGTH) {
        throw new InvalidSaltException("The salt must be " + SALT_LENGTH + " long");
    }

    this.ITERATIONS = iter;
    this.salt = salt;
    
    path = path.toAbsolutePath();
    String absolutePath = path.toString();

    try{
      BufferedReader bf = new BufferedReader(new FileReader(absolutePath));
      String line = bf.readLine();
        
      while (line != null) {
          weakPasswords.add(line);
          line = bf.readLine();
      }
      bf.close();
    } catch (IOException ex) {
      System.out.println(ex.getMessage());
    }    
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

  public void validateMasterPassword(String masterPassword) throws InvalidPasswordException {
    Pattern lowerCase = Pattern.compile("[a-z]");
    Pattern upperCase = Pattern.compile("[A-Z]");
    Pattern digit = Pattern.compile("[0-9]");
    Pattern special = Pattern.compile ("[?= !\\\"#$%&'()*+,-./:;<=>?@[\\\\^_`{|}~]]");
    
    Matcher hasLowerCase = lowerCase.matcher(masterPassword);
    Matcher hasUpperCase = upperCase.matcher(masterPassword);
    Matcher hasDigit = digit.matcher(masterPassword);
    Matcher hasSpecial = special.matcher(masterPassword);

    if (masterPassword.length() < MIN_MASTER_PASSWORD_LENGTH) {
      throw new InvalidPasswordException("The password must contain at least "+ MIN_MASTER_PASSWORD_LENGTH + " characters");    
    }
    else if (masterPassword.length() > MAX_MASTER_PASSWORD_LENGTH) {
      throw new InvalidPasswordException("The password must contain a maximum of "+ MAX_MASTER_PASSWORD_LENGTH + " characters");   
    }
    else if (!hasSpecial.find()) {
      throw new InvalidPasswordException("The password must contain at least one special character");  
    }
    else if (!hasUpperCase.find()) {
      throw new InvalidPasswordException("The password must contain at least one upper case character");
    }
    else if (!hasDigit.find()) {
      throw new InvalidPasswordException("The password must contain at least one number");  
    }
    else if (!hasLowerCase.find()) {
      throw new InvalidPasswordException("The password must contain at least one lower case character");  
    }
    else if(weakPasswords.contains(masterPassword) ? true : false){
      throw new InvalidPasswordException("The password must not be in the list of weak passwords");  
    }
  }

  public class InvalidSaltException extends Exception { 

    public InvalidSaltException(String message) 
    { 
        super(message); 
    } 
  }

  public class InvalidPasswordException extends Exception { 

    public InvalidPasswordException(final String message){ 
        super(message); 
    }
}  
  
}



  
