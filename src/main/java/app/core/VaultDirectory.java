package app.core;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import static app.core.Constants.*;

public class VaultDirectory implements VaultItem{
    
    private final SecureRandom gen; // random bytes generator
    private final Cipher c;
    private byte[] headerIV; // Initialization Vector of the header
    
    private Path folderPath; // "/dir"
    private Path folderNamePath; // "/dir/dir2"
    private Path encDirFile; // file ".dir" that stores the metadata of the parent folder

    private String encName; // updated by encryptHeader, it is the encrypted directory name
    private String folderName; // "dir2"

    /**
     * Instantiate a directory operation
     *
     * @param folderNamePath base directory
     * @param encrypted if true instance of Self decryption, if false instance of Self encryption
     * @throws IOException              The path is not defined
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    public VaultDirectory(Path folderNamePath, boolean encrypted) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException {
        if (folderNamePath == null) throw new IOException("path cannot be null");
        
        this.folderNamePath = folderNamePath.normalize();
        this.folderPath =  this.folderNamePath.getParent() != null ? this.folderNamePath.getParent() : Path.of(".");;
        
        if (encrypted) {
            this.encName = this.folderNamePath.getFileName().toString();
            this.encDirFile = this.folderNamePath.resolve(this.encName + ".dir");
        } else {
            this.folderName = this.folderNamePath.getFileName().toString();
        }        

        this.gen = new SecureRandom();
        this.headerIV = new byte[IVLEN];
        this.c = Cipher.getInstance("AES/GCM/NoPadding");
    }

    /**
     * Public method to encrypt the directory name
     *
     * @param srcPath
     * @param encKey    key used to encrypt the header
     * @return the encrypted directory name
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException                encryption key cannot be null
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws IOException
     */
    public String encrypt(Path srcPath, SecretKey encKey) throws InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        if (encKey == null) throw new InvalidKeyException("encryption key cannot be null");
//        if (srcPath == null) throw new IOException("destination folder path cannot be null");

        byte[] encHeader = this.encryptHeader(encKey);

        String encDestinationStr = this.encName;
        Path dstFolderPath = this.folderPath.resolve(encDestinationStr);
        Files.createDirectory(dstFolderPath);

        this.folderNamePath = this.folderNamePath.getParent().resolve(encDestinationStr);
        this.encDirFile = Path.of(dstFolderPath.toString(), encDestinationStr + ".dir");
        
        OutputStream encryptedOutput = Files.newOutputStream(this.encDirFile); // encrypted file output
        encryptedOutput.write(encHeader);
        encryptedOutput.close();

        return encDestinationStr;
    }

    /**
     * Perform the encryption of the directory name and the building of the output file
     *
     * @param encKey key used to encrypt the header
     * @return the output bytes to be written as a file
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException          directory name length is more than the allowed one
     * @throws BadPaddingException
     * @throws InvalidAlgorithmParameterException
     */
    private byte[] encryptHeader(SecretKey encKey) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        this.gen.nextBytes(this.headerIV);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LEN_BITS, headerIV);
        this.c.init(Cipher.ENCRYPT_MODE, encKey, spec, this.gen);

        byte[] filenameBytes = this.folderName.getBytes();
        if (filenameBytes.length > FILENAME_MAX_SIZE) {
            throw new IllegalBlockSizeException("filename should be <= " + FILENAME_MAX_SIZE + " bytes, instead it is" + filenameBytes.length + " bytes long");
        }
        byte[] toEnc = new byte[1 + FILENAME_MAX_SIZE];

        // first part is the plain filename length expressed in a single byte
        toEnc[0] = (byte) filenameBytes.length;

        // third part is the filename value
        System.arraycopy(filenameBytes, 0, toEnc, 1, filenameBytes.length);

        byte[] encHeader = this.c.doFinal(toEnc);

        byte[] output = new byte[IVLEN + encHeader.length];
        System.arraycopy(headerIV, 0, output, 0, IVLEN);
        System.arraycopy(encHeader, 0, output, IVLEN, encHeader.length);

        String tempEncName = Base64.getUrlEncoder().encodeToString(encHeader);
        tempEncName = tempEncName.substring(0, Math.min(tempEncName.length(), 15));
        this.encName = tempEncName;

        return output;
    }


    /**
     * Public method to decrypt the file
     *
     * @param dstBaseFolderPath destination folder path of output
     * @param encKey            key used to decrypt the header
     * @return the decrypted directory name
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException                encryption key cannot be null
     * @throws IOException                        destination folder path cannot be null or other IO exceptions
     */
    public String decrypt(Path dstBaseFolderPath, SecretKey encKey) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, IOException {
        if (encKey == null) throw new InvalidKeyException("encryption key cannot be null");
        if (dstBaseFolderPath == null) throw new IOException("destination folder path cannot be null");

        Path directoryName = this.folderNamePath;
        Path directoryFile = Path.of(directoryName.toString(), directoryName.getFileName() + ".dir");

        if (Files.size(directoryFile) >= Integer.MAX_VALUE) {
            throw new IOException("File '" + directoryFile + "' is too large");
        }
        int dstFileSize = (int) Files.size(directoryFile); // MAX 2.14 GB !!!
        InputStream inputData = Files.newInputStream(directoryFile); // input file stream
        this.folderName = this.decryptHeader(encKey, inputData, dstFileSize);

        dstBaseFolderPath = dstBaseFolderPath.normalize(); // remove redundant elements
        Path dstFolderPath = Path.of(dstBaseFolderPath.toString(), this.folderName).normalize();

        // if a directory with the same name already exists, we append an index to the new one to not overwrite the previous one
        if (dstFolderPath.toFile().exists()) {
            int index = 0;
            do {
                dstFolderPath = Path.of(dstBaseFolderPath.toString(), index + "-" + this.folderName).normalize();
                index++;
            } while (dstFolderPath.toFile().exists());
        }

        Files.createDirectory(dstFolderPath);
        return dstFolderPath.getFileName().toString();
    }

    /**
     * Perform the encryption of the directory name and the building of the output file
     *
     * @param encKey    key used to encrypt the header
     * @param inputData stream of the encrypted file
     * @param inputSize size of the encrypted file
     * @return the original plaintext directory name
     * @throws IllegalArgumentException           the file is not a directory header file; the size is not as expected
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws IOException
     */
    private String decryptHeader(SecretKey encKey, InputStream inputData, int inputSize) throws IllegalArgumentException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        int headerFullSize = IVLEN + 1 + FILENAME_MAX_SIZE + TAG_LEN; // last TAG_LEN bytes are GCM authentication tag
        if (inputSize != headerFullSize)
            throw new IllegalArgumentException("file size " + inputSize + "B is not long as expected " + headerFullSize + "B");

        byte[] encrypted = new byte[headerFullSize];
        inputData.read(encrypted);

        // first part of the full header data is the IV
        System.arraycopy(encrypted, 0, this.headerIV, 0, IVLEN);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LEN_BITS, this.headerIV);
        this.c.init(Cipher.DECRYPT_MODE, encKey, spec, this.gen);

        byte[] ciphertext = new byte[headerFullSize - IVLEN];
        // second part of the full header data is the full ciphertext
        System.arraycopy(encrypted, IVLEN, ciphertext, 0, ciphertext.length);

        byte[] headerContent = this.c.doFinal(ciphertext);

        // first part is the plain folder name length expressed in a byte
        int folderNameSize = headerContent[0];

        // second part is the folder name value
        byte[] folderName = new byte[folderNameSize];
        System.arraycopy(headerContent, 1, folderName, 0, folderNameSize);

        String folderStr = new String(folderName, StandardCharsets.UTF_8);
        return folderStr;
    }
    
    @Override
    public Path getRelPath(Path vaultPath) {
        return this.folderNamePath.subpath(vaultPath.normalize().getNameCount(), this.folderNamePath.getNameCount());
    }

    @Override
    public Path getAbsPath() {
        return this.folderNamePath;
    }

    @Override
    public String getName() {
        return this.folderName;
    }

    @Override
    public String getEncName() {
        return this.encName;
    }
    
    public Path getEncDirFile() {
        return this.encDirFile;
    }
}