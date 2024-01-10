package app;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class KeyDerivator {
  private byte[] password;                                              // Byte representation of the input password
  private byte[] salt;                                                  // Password salt

  private Random gen;                                                   // Secure random bytes generator 

  private final int ITERATIONS = 210000;                                // Number of iterations
  private final int DERIVED_KEY_LENGTH = 512;                           // Master key length

  private final int MIN_PASSWORD_LENGTH = 12;                           // Minimum password length
  private final int MAX_PASSWORD_LENGTH = 64;                           // Maximum password length
  private final int SALT_LENGTH = 128;                                  // Salt length

  List<String> weakPasswords = new ArrayList<String>();                 // List of weak passwords
  Path path = Paths.get("src/main/resources/WeakPasswords.txt");        // Path for the file with weak passwords 

  /**
   * KeyDerivator class constructor for the first initialization of the salt:
   * set the salt randomly and insert the weak passwords into the list
   */
  public KeyDerivator(){ 

    this.gen = new SecureRandom();
    // Set the salt randomly
    this.salt = getNextSalt();

    // Read the weakPasswords.txt file and inserts the weak passwords into the list
    readFile();
  }

  /**
   * KeyDerivator class constructor specifying stored salt
   *
   * @param salt byte[]  stored salt
   * @throws InvalidSaltException if the salt is null
   */
  public KeyDerivator(byte[] salt) throws InvalidSaltException { 

    // If the salt is null, generate an IllegalArgumentException
    if(salt == null){
      throw new IllegalArgumentException("The salt cannot be null");
    }

    // If the salt is not 128 bytes length, generate an InvalidSaltException
    if (salt.length != SALT_LENGTH) {
      throw new InvalidSaltException();
    }

    this.salt = salt;

    // Read the weakPasswords.txt file and inserts the weak passwords into the list
    readFile();
   }

  /**
   * Method for initializing the new password entered, verifying its security
   * 
   * @param psw password
   * @throws InvalidPasswordException
   */
  public void setPsw(String psw) throws InvalidPasswordException {

    // If the password is null, generate an IllegalArgumentException
    if(psw == null){
      throw new IllegalArgumentException("The password cannot be null");
    }
   
    // Validate the inserted password
    validatePassword(psw);

    // Convert the entered password to bytes[]
    this.password = psw.getBytes(StandardCharsets. UTF_8);
  }

  /**
   * Method for setting the salt
   * 
   * @param salt byte[]  salt
   * @throws InvalidSaltException if the salt is not 128 byte length
   */
  public void setSalt(byte[] salt) throws InvalidSaltException{

    // If the salt is null, generate an IllegalArgumentException
    if(salt == null){
      throw new IllegalArgumentException("The salt cannot be null");
    }

    // If the salt is not 128 bytes length, generate an InvalidSaltException
    if (salt.length != SALT_LENGTH) {
      throw new InvalidSaltException();
    }
   
    this.salt = salt;
  }

  /**
   * Method to retrieve the salt
   * 
   * @return byte[]  salt
   */
  public byte[] getSalt(){
    return this.salt;
  }

  /**
   * Method to generate a 128 byte salt randomly
   * 
   * @return byte[] salt of 128 bytes length
   */
  public byte[] getNextSalt() {

    // Initialize a 128 byte salt
		byte[] salt = new byte[128];

    // Generate the random salt with the secure random generator
		gen.nextBytes(salt);

		return salt;
	}

  /**
   * Method to retrieve a 512 bit master key from the password and salt
   * 
   * @return byte[] 512 bit master key
   */
  public byte[] getMasterKey(){
    
    // If the password is null, generate an IllegalArgumentException
    if(this.password == null){
      throw new IllegalArgumentException("The password cannot be null");
    }

    // Convert password from byte[] to char[]
    char[] passwordChars = new String(password, StandardCharsets.UTF_8).toCharArray();

    // Method for hashing the password
    return hashPassword(passwordChars, this.salt, ITERATIONS, DERIVED_KEY_LENGTH);
  }

  /**
   * Method for compute the PBKDF2 hash of a password: with the pseudorandom function 
   * "PBKDF2WithHmacSHA512", 210000 iterations and with a derived key of 512 bit
   * 
   * @param password char[]  password in an array of chars
   * @param salt  byte[]  128 byte salt
   * @param iterations final int number of iteration for PBKDF2 (210000) 
   * @param keyLength final int  bit-length of the derived key (512 bit)
   * 
   * @return byte[]  512 bit master key
   */
  private byte[] hashPassword( final char[] password, final byte[] salt, final int iterations, final int keyLength ) {

    try {

      // Generate a new factory for secret keys
      SecretKeyFactory skf = SecretKeyFactory.getInstance( "PBKDF2WithHmacSHA512");

      // Convert the password characters to a PBE key by creating an instance of the appropriate secret-key factory
      PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);

      // Generate a SecretKey object from the provided key specification
      SecretKey key = skf.generateSecret(spec);

      // Convert SecretKey key to byte[]
      byte[] res = key.getEncoded( );
      return res;

    } catch ( NoSuchAlgorithmException | InvalidKeySpecException e ) {
      throw new RuntimeException( e );
    }
  }

  /**
   * Method to check the validity of the password:
      - must have at least 12 characters and a maximum of 64
      - must have at least one lower case character
      - must have at least one upper case character
      - must have at least one special character
      - must have at least one number
      - must not be in the list of weak passwords
   * 
   * @param masterPassword String  user-input password
   * @throws InvalidPasswordException if the password is null
   */
  public void validatePassword(String password) throws InvalidPasswordException {
    
    // If the password is null, generate an IllegalArgumentException
    if(password == null){
      throw new IllegalArgumentException("The password cannot be null");
    }
    
    // Generate regex patter for rules
    Pattern lowerCase = Pattern.compile("[a-z]");
    Pattern upperCase = Pattern.compile("[A-Z]");
    Pattern digit = Pattern.compile("[0-9]");
    Pattern special = Pattern.compile ("[?= !\\\"#$%&'()*+,-./:;<=>?@[\\\\^_`{|}~]]");
    
    // Generate the matcher for rules
    Matcher hasLowerCase = lowerCase.matcher(password);
    Matcher hasUpperCase = upperCase.matcher(password);
    Matcher hasDigit = digit.matcher(password);
    Matcher hasSpecial = special.matcher(password);

    // If the password is too short, generate an InvalidPasswordException
    if (password.length() < MIN_PASSWORD_LENGTH) {
      throw new InvalidPasswordException("The password must contain at least " + MIN_PASSWORD_LENGTH + " characters");    
    }

    // If the password is too long, generate an InvalidPasswordException
    else if (password.length() > MAX_PASSWORD_LENGTH) {
      throw new InvalidPasswordException("The password must contain a maximum of " + MAX_PASSWORD_LENGTH + " characters");   
    }

    // If the password has not a special character, generate an InvalidPasswordException
    else if (!hasSpecial.find()) {
      throw new InvalidPasswordException("The password must contain at least one special character");  
    }

    // If the password has not an upper case character, generate an InvalidPasswordException
    else if (!hasUpperCase.find()) {
      throw new InvalidPasswordException("The password must contain at least one upper case character");
    }

    // If the password has not a number, generate an InvalidPasswordException
    else if (!hasDigit.find()) {
      throw new InvalidPasswordException("The password must contain at least one number");  
    }

    // If the password has not a lower case character, generate an InvalidPasswordException
    else if (!hasLowerCase.find()) {
      throw new InvalidPasswordException("The password must contain at least one lower case character");  
    }

    // If the password is in the list of weak passwords, generate an InvalidPasswordException
    else if(weakPasswords.contains(password) ? true : false){
      throw new InvalidPasswordException("The password must not be in the list of weak passwords");  
    }
  }

  /**
   * Method to read the WeakPasswords.txt file and store 
   * the file's passwords in the weak passwords list
   */
  private void readFile() {
    // Convert the path in an absolute path
    path = path.toAbsolutePath();
    // Convert type path in string
    String absolutePath = path.toString();

    // Read the weakPasswords.txt file
    try{

      // Initialize a new buffer reader
      BufferedReader bf = new BufferedReader(new FileReader(absolutePath));
      String line = bf.readLine();
      
      // Add all lines of the weakPasswords.txt file in the weak passwords list
      while (line != null) {
        weakPasswords.add(line);
        line = bf.readLine();
      }

      // Close the buffer reader
      bf.close();
      
    } catch (IOException ex) {
      System.out.println(ex.getMessage());
    }    
  }

  public class InvalidSaltException extends Exception { 
    public InvalidSaltException() { 
      super("The salt must be " + SALT_LENGTH + " bytes long"); 
    } 
  }

  public class InvalidPasswordException extends Exception { 
    public InvalidPasswordException(final String message) { 
      super(message); 
    }
  }  
}



  
