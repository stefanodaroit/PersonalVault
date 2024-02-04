package app.core;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import static app.core.Constants.*;

public class KeyDerivator {
  private byte[] password;                                              // Byte representation of the input password
  private byte[] salt;                                                  // Password salt

  private Random gen;                                                   // Secure random bytes generator

  /**
   * KeyDerivator class constructor for the first initialization of the salt:
   * set the salt randomly and insert the weak passwords into the list
   */
  public KeyDerivator() {
    this.gen  = new SecureRandom();
    this.salt = getNextSalt();
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
      throw new NullPointerException("The salt cannot be null");
    }

    // If the salt is not 128 bits length, generate an InvalidSaltException
    if (salt.length != SALT_LENGTH) {
      throw new InvalidSaltException();
    }

    this.salt = salt;
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
      throw new NullPointerException("The password cannot be null");
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
   * @throws InvalidSaltException if the salt is not 128 bits length
   */
  public void setSalt(byte[] salt) throws InvalidSaltException{

    // If the salt is null, generate an IllegalArgumentException
    if(salt == null){
      throw new IllegalArgumentException("The salt cannot be null");
    }

    // If the salt is not 128 bits length, generate an InvalidSaltException
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
   * Method to generate a 128 bits salt randomly
   *
   * @return byte[] salt of 128 bits length
   */
  public byte[] getNextSalt() {

    // Initialize a 128 bits salt
		byte[] salt = new byte[SALT_LENGTH];

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
   * @param salt  byte[]  128 bits salt
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
   * @param password String  user-input password
   * @throws InvalidPasswordException if the password is null
   */
  public static void validatePassword(String password) throws InvalidPasswordException {
    if (password == null) {
      throw new NullPointerException("The password cannot be null");
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

    String message = null;

    // If the password is too short
    if (password.length() < MIN_PASSWORD_LENGTH) {
      message = PSW_EXCEPTION[0] + " The password must contain at least " + MIN_PASSWORD_LENGTH + " characters \n";
    }

    // If the password is too long
    if (password.length() > MAX_PASSWORD_LENGTH) {
      message += PSW_EXCEPTION[1] + " The password must contain a maximum of " + MAX_PASSWORD_LENGTH + " characters \n";
    }

    // If the password has not a special character
    if (!hasSpecial.find()) {
      message += PSW_EXCEPTION[2] + " The password must contain at least one special character \n";
    }

    // If the password has not an upper case character
    if (!hasUpperCase.find()) {
      message += PSW_EXCEPTION[3] + " The password must contain at least one upper case character \n";
    }

    // If the password has not a lower case character
    if (!hasLowerCase.find()) {
      message += PSW_EXCEPTION[4] + " The password must contain at least one lower case character\n";
    }

    // If the password has not a number
    if (!hasDigit.find()) {
      message += PSW_EXCEPTION[5] + " The password must contain at least one number\n";
    }

    // If the password is in the list of weak passwords
    if (message == null && isBreached(password)) {
      message += "The password must not appear in previous data breaches";
    }

    if (message != null) {
      throw new InvalidPasswordException(message);
    }
  }

  /**
   * If an Internet connection is working, check if the password appears in some data breaches
   * via the API provided by "Have I Been Pwned" utility <br>
   * Reference: <a href="https://haveibeenpwned.com/API/v3#PwnedPasswords">HIBP Pwned Passwords</a>
   *
   * @param password user password
   * @return true if the password already appeared in previous data breaches, false otherwise
   */
  public static boolean isBreached(String password) {
    try {
        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        String hash = new BigInteger(1, crypt.digest(password.getBytes(StandardCharsets.UTF_8))).toString(16).toUpperCase();

        String hashPrefix = hash.substring(0, 5);
        String hashSuffix = hash.substring(5);

        System.out.println("Checking password breach");
        String urlString = "https://api.pwnedpasswords.com/range/" + hashPrefix;
        URL url = new URL(urlString);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("GET");

        int statusCode = http.getResponseCode();
        if (statusCode == 200) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(http.getInputStream()))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    // first part is suffix of every hash beginning with the specified prefix,
                    // followed by a count of how many times it appears in the data set
                    String[] s = line.split(":");

                    if (s[0].equals(hashSuffix)) {
                      System.out.println("The password is breached. Watch out!");
                      return true;
                    }
                }
            }
        }

        return false;
    } catch (Exception e) {
        return false;
    }
  }

  public class InvalidSaltException extends Exception {
    public InvalidSaltException() {
      super("The salt must be " + SALT_LENGTH + " bytes long");
    }
  }

  public static class InvalidPasswordException extends Exception {
    public InvalidPasswordException(final String message) {
      super(message);
    }
  }
}



  
