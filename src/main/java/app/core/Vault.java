package app.core;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import app.core.KeyDerivator.InvalidPasswordException;
import app.core.KeyDerivator.InvalidSaltException;

import static app.core.Constants.*;

public class Vault {

  private final UUID vid;
  private String name;
  private Path storagePath;
  
  private byte[] confMac;
  private boolean locked = true;

  private KeyManager km;
  private VaultConfiguration conf;

  private Path treeChecksumFile;

  private List<Path> files;
  
  /**
   * Create a new vault in "path" using "password" for keys derivation
   * @param name         The name of the vault (can be null); if null name = vid
   * @param storagePath  The path in which store the vault
   * @param psw          The password used for key derivation
   * 
   * @throws IOException
   * @throws InternalException
   * @throws InvalidPasswordException
   */
  public Vault(String name, String storagePath, String psw) throws IOException, InternalException, InvalidPasswordException {
    if (storagePath == null || psw == null) {
      throw new NullPointerException("Null vault parameter");
    }

    if (!(name == null || name.length() == 0 || name.matches(VAULT_NAME_RGX))) {
      throw new IllegalArgumentException("Invalid vault name");
    }
    
    this.files = new ArrayList<>();
    this.vid = UUID.randomUUID();
    this.name = (name != null && name.length() != 0) ? name : this.vid.toString();
    this.locked = false;
    
    try {
      // Create and wrap secret keys
      this.km = new KeyManager();
      this.km.wrapSecretKeys(psw);  
    } catch (InvalidPasswordException e) { 
      throw e;
    } catch (Exception e) {
      throw new InternalException();
    }

    this.storagePath = Paths.get(storagePath, this.name);
    Files.createDirectory(this.storagePath);

    this.treeChecksumFile = Paths.get(this.storagePath.toString() + "/" + this.vid + FILE_CHECKSUM_EXTENSION);
    
    // Create and save vault configuration
    this.conf = new VaultConfiguration(this.vid, this.km.getSalt(), this.km.getWrapEncKey(), this.km.getWrapAuthKey());

    writeConfiguration();
  }

  /**
   * Import an existing vault
   * 
   * @param vid         vault id
   * @param name        vault name; if null name = vid
   * @param storagePath vault storage path
   * 
   * @throws InvalidConfigurationException
   * @throws IOException
   */
  public Vault(UUID vid, String name, String storagePath) throws InvalidConfigurationException, IOException {
    if (storagePath == null || vid == null) {
      throw new NullPointerException("Invalid vault parameters");
    }

    if (!(name == null || name.length() == 0 || name.matches(VAULT_NAME_RGX))) {
      throw new IllegalArgumentException("Invalid vault name");
    }
    
    this.vid = vid;
    this.name = (name != null && name.length() != 0) ? name : this.vid.toString();
    this.storagePath = Paths.get(storagePath, this.name);
    this.treeChecksumFile = Paths.get(this.storagePath.toString() + "/" + this.vid + FILE_CHECKSUM_EXTENSION);

    this.files = new ArrayList<>();

    // Add all files to the list of files
    for (Path file : Files.walk(this.storagePath).filter(Files::isRegularFile).toList()) {
      if(!file.getFileName().toString().contains(".vault") && !file.getFileName().toString().contains(FILE_CHECKSUM_EXTENSION)){
        files.add(file);
      }
    }
    
    try {
      // Read vault configuration and init key manager
      readConfiguration();
      this.km = new KeyManager(this.conf.getEncKey(), this.conf.getAuthKey(), this.conf.getSalt());
    } catch (InvalidSaltException e) {
      throw new InvalidConfigurationException();
    }
  }

  /**
   * Add the directory and its content to the vault including all subdirectories
   * 
   * @param path The directory path
   * 
   * @throws IOException If something in the copy does not work
   * @throws VaultLockedException If the vault is still locked 
   * @throws InternalException 
   */
  public void addDirectory(String path) throws IOException, VaultLockedException, InternalException {
    if (path == null) {
      throw new NullPointerException("Path cannot be null");
    }

    // Loop over the source directory content
    Path srcDir = Paths.get(path);
    for (Path file : Files.walk(srcDir).toList()) {
      // Copy file considering the subdirectories
      Path dest = file.subpath(srcDir.getNameCount() - 1, file.getNameCount());
      addFile(file, dest);
    }
    
  }

  /**
   * Add the file to the vault
   * 
   * @param path The directory path
   * 
   * @throws IOException If something in the copy does not work
   * @throws VaultLockedException If the vault is still locked 
   * @throws InternalException 
   */
  public void addFile(String path) throws IOException, VaultLockedException, InternalException {
    if (path == null) {
      throw new NullPointerException("Path cannot be null");
    }
    // Copy file directly in the vault (without subdirectories)
    addFile(Paths.get(path), null);
  }

  /**
   * Copy file in srcPath to storagePath/destPath; if destPath is null copies to storagePath/srcFilename
   * 
   * @param srcPath The source file path
   * @param dstPath The destination path
   * 
   * @throws IOException If something in the copy does not work
   * @throws VaultLockedException If the vault is still locked 
   * @throws InternalException 
   */
  private void addFile(Path srcPath, Path dstPath) throws IOException, VaultLockedException, InternalException {
    if (srcPath == null) {
      throw new NullPointerException("Path cannot be null");
    }

    if (this.locked) {
      throw new VaultLockedException();
    }
    
    // Copy file; if no subdirectories are included use the filename
    Path dst = this.storagePath.resolve(dstPath == null ? srcPath.getFileName() : dstPath);
    Files.copy(srcPath, dst, StandardCopyOption.COPY_ATTRIBUTES);

    // Add the file to the tree checksum and to the list of files
    if(!isDir(dst)){
      files.add(dst);
      createTreeChecksum(dst);
    }

    // TODO add encryption part
  }

  /**
   * Unlock the files in the vault
   * 
   * @param psw String: password used for keys derivation
   * 
   * @throws InvalidConfigurationException
   * @throws WrongPasswordException
   * @throws InternalException
   * @throws IOException 
   * @throws InvalidFilesException 
   */
  public void unlock(String psw) throws InvalidConfigurationException, WrongPasswordException, InternalException, IOException, InvalidFilesException {
    try {
      // Unwrap secret keys through input password
      this.km.unwrapSecretKeys(psw);
    } catch (InvalidPasswordException | InvalidKeyException e) {
      throw new WrongPasswordException();
    } catch (Exception e) {
      throw new InternalException();
    }
    
    // Check configuration file integrity
    try {
      // Recompute HMAC of the current configuration
      byte[] serializedConf = VaultConfiguration.serialize(this.conf);
      byte[] encodedConf = encodeToken(serializedConf);
      byte[] mac = getHmac(ALG_HMAC_TOK, this.km.getMasterKey(), encodedConf);
      
      // Check the two MACs; if not equal the configuration have been tampered
      if (!MessageDigest.isEqual(this.confMac, mac)) {
        System.out.println("Configuration integrity check failed");
        throw new InvalidConfigurationException();
      }
    } catch (IOException e) {
      System.out.println("Error while checking configuration integrity");
      throw new InvalidConfigurationException();
    }

    checkFilesIntegrity();
    
    this.locked = false;

    // TODO add decryption part
  }

  /**
   * Method used to check the files integrity
   * 
   * @throws InvalidFilesException
   * @throws InternalException
   */
  private void checkFilesIntegrity() throws InvalidFilesException, InternalException{

    List<String> pathWithMac = readTreeChecksum();
    
    for(String fileMac: pathWithMac){
      String path = fileMac.split(";")[0];
      String macString = fileMac.split(";")[1];
      byte[] mac = macString.getBytes(StandardCharsets. UTF_8);
      
      for(Path p: files){
        Path newPath = p.subpath(storagePath.getNameCount(), p.getNameCount());
        if(newPath.toString().equals(path)){

          // Recompute HMAC and path
          byte[] newMac = getHmac(ALG_HMAC_TOK, this.km.getMasterKey(), newPath.toString().getBytes(StandardCharsets. UTF_8));
          byte[] mac2 = getHmac(ALG_HMAC_TOK, this.km.getMasterKey(), path.toString().getBytes(StandardCharsets. UTF_8));
          //!newPath.equals(path) || 

          /*if(!MessageDigest.isEqual(mac, newMac)){
            System.out.println("Files integrity check failed");
            throw new InvalidFilesException();
          }*/
        }
      }
    }
  }

  /**
   * Change the password and set the new configuration
   * 
   * @param oldPsw String  Old password
   * @param newPsw String  New password
   * 
   * @throws WrongPasswordException
   * @throws InternalException
   * @throws IOException
   * @throws InvalidPasswordException
   */
  public void changePsw(String oldPsw, String newPsw) throws WrongPasswordException, InternalException, IOException, InvalidPasswordException {
    if (oldPsw == null || newPsw == null) {
      throw new NullPointerException("Invalid passwords");
    }
    
    try {
      // Unwrap secret keys through old password
      this.km.unwrapSecretKeys(oldPsw);
    } catch (InvalidPasswordException | InvalidKeyException e) {
      throw new WrongPasswordException();
    } catch (Exception e) {
      throw new InternalException();
    } 

    try {
      // Wrap secret keys with new psw
      this.km.wrapSecretKeys(newPsw);  
    } catch (InvalidPasswordException e) { 
      throw e;
    } catch (Exception e) {
      throw new InternalException();
    }
    
    // Edit and save vault configuration
    this.conf.setSalt(this.km.getSalt());
    this.conf.setEncKey(this.km.getWrapEncKey());
    this.conf.setAuthKey(this.km.getWrapAuthKey());
    writeConfiguration();
  }

  /**
   * Read the vault configuration from the file system
   * 
   * @throws InvalidConfigurationException
   * @throws IOException
   */
  protected void readConfiguration() throws InvalidConfigurationException, IOException {    
    System.out.print("Reading configuration file... ");
    
    try {
      // Read and decode token from the vault root
      Path confPath = VaultConfiguration.getPath(this.storagePath, this.vid);
      FileChannel channel = FileChannel.open(confPath);

      // Check if configuration file is too large
      if (channel.size() > MAX_TOKEN_SIZE) {
        System.out.println("Vault configuration file is too large");
        throw new InvalidConfigurationException();
      }
      
      byte[] token = Files.readAllBytes(confPath);
      byte[] serializedConf = decodeToken(token); 
      
      // Deserialize VaultConfiguration object
      this.conf = VaultConfiguration.deserialize(serializedConf);
    } catch (IOException e) {
      System.out.println("Error while reading configuration file");
      throw e;
    } catch (ClassNotFoundException e) {
      System.out.println("Error while deserializing configuration file");
      throw new InvalidConfigurationException();
    }
    
    System.out.println("DONE");
  }

 /**
  * Saves the vault configuration on the file system
  *
  * @throws IOException
  * @throws InternalException
  */
  protected void writeConfiguration() throws IOException, InternalException {
    System.out.print("Writing configuration file... ");
    
    try {
      // Serialize the VaultConfiguration object 
      byte[] serializedConf = VaultConfiguration.serialize(this.conf);
      
      // Generate a token with MAC and save it in the vault root
      byte[] token = encodeSignedToken(serializedConf);
      Files.write(VaultConfiguration.getPath(this.storagePath, this.vid), token);
    } catch (IOException e) {
      System.out.println("Error while saving configuration file");
      throw e;
    }
    
    System.out.println("DONE");
  }


  /**
   * Encodes header and payload in base64
   * 
   * @param payload 
   * 
   * @return byte[] base64 encoded token (header.payload)
   */
  private byte[] encodeToken(byte[] payload) {
    // Encode header in base64
    byte[] encodedHeader = Base64.getEncoder().withoutPadding().encode(ALG_HMAC_TOK.getBytes());
    
    // Encode payload in base64
    byte[] encodedPayload = Base64.getEncoder().withoutPadding().encode(payload);

    // Generate token joining encoded payload and encoded MAC
    byte[] token = new byte[encodedHeader.length + 1 + encodedPayload.length];
    System.arraycopy(encodedHeader, 0, token, 0, encodedHeader.length);
    token[encodedHeader.length] = (byte) PERIOD;
    System.arraycopy(encodedPayload,  0, token, encodedHeader.length + 1, encodedPayload.length);

    return token;
  }

   /**
   * Encodes a base64 signed token to save on file system
   * 
   * @param payload byte[]: the content to save as array of bytes
   * 
   * @return byte[] base64 signed token as array of bytes (header.payload.MAC)
   * @throws InternalException
   */
  private byte[] encodeSignedToken(byte[] payload) throws InternalException {
    byte[] token = encodeToken(payload);

    // Generate MAC from encoded payload and encode MAC
    this.confMac = getHmac(ALG_HMAC_TOK, this.km.getMasterKey(), token);
    byte[] encodedMac = Base64.getEncoder().withoutPadding().encode(this.confMac);

    // Generate token joining encoded payload and encoded MAC
    byte[] signedToken = new byte[token.length + 1 + encodedMac.length];
    System.arraycopy(token, 0, signedToken, 0, token.length);
    signedToken[token.length] = (byte) PERIOD;
    System.arraycopy(encodedMac,  0, signedToken, token.length + 1, encodedMac.length);

    return signedToken;
  }

  /**
   * Extract the payload from a base64 signed token and store the MAC
   * 
   * @param token byte[]: the token
   * 
   * @return byte[] payload: the content of the token
   * 
   * @throws InvalidConfigurationException
   */
  private byte[] decodeToken(byte[] token) throws InvalidConfigurationException {
    String tmp = new String(token);
    
    // Check if token has 3 fields (header + payload + MAC)
    String tok[] = tmp.split("\\" + PERIOD);
    if (tok.length != 3) {
      throw new InvalidConfigurationException();
    }

    // Decode header and check if the algorithm is correct (only HMACSHA512 at the moment)
    byte[] header  = Base64.getDecoder().decode(tok[0]);
    if (!MessageDigest.isEqual(header, ALG_HMAC_TOK.getBytes())) {
      throw new InvalidConfigurationException();
    }

    // Decode payload and MAC
    byte[] payload = Base64.getDecoder().decode(tok[1]);
    this.confMac   = Base64.getDecoder().decode(tok[2]);

    return payload;
  }

   /**
   * Computes MAC for an array of bytes and sign it with a key
   * 
   * @param alg   String:    MAC algorithm
   * @param key   SecretKey: key for MAC signature
   * @param bytes byte[]:    bytes for which compute the MAC
   * 
   * @return byte[] MAC array of bytes
   * @throws InternalException
   */
  private static byte[] getHmac(String alg, SecretKey key, byte[] bytes) throws InternalException {
    byte[] macResult = null;
    
    try {
      Mac mac = Mac.getInstance(alg);
      mac.init(key);
      macResult = mac.doFinal(bytes);
    } catch (Exception e) {   
      System.out.println("Error while computing HMAC");
      throw new InternalException();
    }

    return macResult;
  }

  private static Boolean isDir(Path path) {
    if (path == null || !Files.exists(path)) return false;
    else return Files.isDirectory(path);
  }

  /**
   * Add the file in the tree checksum: path file;HMAC
   * 
   * @param file Path  path of the file to add
   * @throws IOException
   * @throws VaultLockedException
   * @throws InternalException
   */
  private void createTreeChecksum(Path file) throws IOException, VaultLockedException, InternalException{
  
    // String Joiner for the line
    StringJoiner pathWithMac = new StringJoiner(";", "", "\n");
    // Create the dest path without the name of the vault
    Path dest = file.subpath(storagePath.getNameCount(), file.getNameCount());
    // Create the MAC of the dest path with the master key
    byte[] mac = getHmac(ALG_HMAC_TOK, this.km.getMasterKey(), dest.toString().getBytes(StandardCharsets. UTF_8));

    // Add to the StringJoiner the dest path and MAC
    pathWithMac.add(dest.toString());
    pathWithMac.add(mac.toString());

    // Write the StringJoiner to the tree checksum file
    writeTreeChecksumToFile(pathWithMac);
    
  }

  /**
   * Method to add a StringJoiner in a file
   * 
   * @param sj StringJoiner  String to add to the file
   */
  private void writeTreeChecksumToFile(StringJoiner sj){

    // Open the file in append mode
    try(FileWriter fw = new FileWriter(treeChecksumFile.toString(), true);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw)){
          
          // Add the StringJoiner to the file
          out.print(sj.toString());
    } catch (IOException e) {
      System.err.println("Cannot write tree checksum file");
    }
  }

  /**
   * Method to read the tree checksum file
   * 
   * @return List<String>  list of path;HMAC
   */
  public List<String> readTreeChecksum(){
    // List of path;HMAC
    List<String> pathWithMac = new ArrayList<>();

    try {
      // Read all the lines of the file
      Scanner scan = new Scanner(treeChecksumFile.toFile());
      while (scan.hasNextLine()) {
        pathWithMac.add(scan.nextLine());
      }
      // Close the scan
      scan.close();
    } catch (FileNotFoundException e) {
      System.out.println("Tree checksum file absent");
    }

    return pathWithMac;
  }
  
  public static Vault importVault(File dir) throws InvalidConfigurationException, IOException {
    // Check if a vault configuration file is present
    List<Path> path = Files.find(Paths.get(dir.getAbsolutePath()), 1, (p, attr) -> p.getFileName().toString().endsWith(CONF_FILE_EXT)).toList();
    
    // If absent or multiple files throw error
    if (path.size() != 1) {
      throw new InvalidConfigurationException();
    }   
    
    // Get UUID from vault
    String vaultFilename = path.get(0).getFileName().toString();
    vaultFilename = vaultFilename.substring(0, vaultFilename.length() - CONF_FILE_EXT.length());

    // Create vault with obtained parameters and add to the list view
    return new Vault(UUID.fromString(vaultFilename), dir.getName(), dir.getParent());
  }

  // Overriding equals() to compare two Vault objects
  @Override
  public boolean equals(Object obj) {
    if (!(obj != null && obj instanceof Vault)) {
      return false;
    }

    return this.getStoragePath().equals(((Vault) obj).getStoragePath());
  }

  public UUID getVid() {
    return this.vid;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name){
    this.name = name;
  }

  public String getStoragePath() {
    return this.storagePath.toString();
  }

  public VaultConfiguration getVaultConfiguration() {
    return this.conf;
  }

  public boolean isLocked() {
    return this.locked;
  }

  @Override
  public String toString() {
    return this.name + '\n' + this.storagePath.toString();
  }

  public static class InvalidConfigurationException extends Exception { 
    public InvalidConfigurationException() { 
      super("Invalid configuration file"); 
    }
  }

  public static class InvalidFilesException extends Exception { 
    public InvalidFilesException() { 
      super("Invalid file"); 
    }
  }

  public static class InternalException extends Exception { 
    public InternalException() { 
      super("Internal error"); 
    }
  }

  public static class WrongPasswordException extends Exception { 
    public WrongPasswordException() { 
      super("The password is wrong"); 
    }
  }

  public static class VaultLockedException extends Exception { 
    public VaultLockedException() { 
      super("Cannot modify the vault - Vault Locked!"); 
    }
  }
  
} 