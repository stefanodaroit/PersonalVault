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

public class Directory {

    private final Path folderPath; // "./dir/dir2/"
    private final SecureRandom gen; // random bytes generator
    private final Cipher c;
    private byte[] headerIV; // Initialization Vector of the header
    private String encName; // updated by encryptHeader, it is the encrypted directory name

    /**
     * Instantiate a directory operation
     *
     * @param folderPath base directory
     * @throws IOException              The path is not defined
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    public Directory(String folderPath) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException {
        if (folderPath == null) throw new IOException("path cannot be null");
        this.folderPath = Path.of(folderPath).normalize();

        this.gen = new SecureRandom();
        this.headerIV = new byte[IVLEN];
        this.c = Cipher.getInstance("AES/GCM/NoPadding");
        this.encName = "";
    }

    /**
     * Public method to encrypt the directory name
     * @param encKey key used to encrypt the header
     * @param dstBaseFolderPath destination folder path of output
     * @return the encrypted directory name
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException encryption key cannot be null
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws IOException destination folder path cannot be null or other IO exceptions
     */
    public String encrypt(SecretKey encKey, Path dstBaseFolderPath) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        if (encKey == null) throw new InvalidKeyException("encryption key cannot be null");
        if (dstBaseFolderPath == null) throw new IOException("destination folder path cannot be null");

        byte[] encHeader = this.encryptHeader(encKey);

        String encDestinationStr = this.encName;
        dstBaseFolderPath = dstBaseFolderPath.normalize(); // remove redundant elements
        Path dstFolderPath = Path.of(dstBaseFolderPath.toString(), encDestinationStr);
        Files.createDirectory(dstFolderPath);

        OutputStream encryptedOutput = Files.newOutputStream(Path.of(dstFolderPath.toString(), encDestinationStr + ".dir")); // encrypted file output
        encryptedOutput.write(encHeader);
        encryptedOutput.close();

        return encDestinationStr;
    }

    /**
     * Perform the encryption of the directory name and the building of the output file
     * @param encKey key used to encrypt the header
     * @return the output bytes to be written as a file
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException directory name length is more than the allowed one
     * @throws BadPaddingException
     * @throws InvalidAlgorithmParameterException
     */
    private byte[] encryptHeader(SecretKey encKey) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        this.gen.nextBytes(this.headerIV);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LEN_BITS, headerIV);
        this.c.init(Cipher.ENCRYPT_MODE, encKey, spec, this.gen);

        byte[] filenameBytes = this.folderPath.getFileName().toString().getBytes();
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
     * @param encKey key used to decrypt the header
     * @param dstBaseFolderPath destination folder path of output
     * @return the decrypted directory name
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException encryption key cannot be null
     * @throws IOException destination folder path cannot be null or other IO exceptions
     */
    public String decrypt(SecretKey encKey, Path dstBaseFolderPath) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, IOException {
        if (encKey == null) throw new InvalidKeyException("encryption key cannot be null");
        if (dstBaseFolderPath == null) throw new IOException("destination folder path cannot be null");

        Path directoryName = this.folderPath;
        Path directoryFile = Path.of(directoryName.toString(), directoryName.getFileName() + ".dir");

        int dstFileSize = (int) Files.size(directoryFile); // MAX 2.14 GB !!!
        InputStream inputData = Files.newInputStream(directoryFile); // input file stream
        String originalName = this.decryptHeader(encKey, inputData, dstFileSize);

        dstBaseFolderPath = dstBaseFolderPath.normalize(); // remove redundant elements
        Path dstFolderPath = Path.of(dstBaseFolderPath.toString(), originalName).normalize();
        Files.createDirectory(dstFolderPath);

        return dstFolderPath.getFileName().toString();
    }

    /**
     * Perform the encryption of the directory name and the building of the output file
     * @param encKey key used to encrypt the header
     * @param inputData stream of the encrypted file
     * @param inputSize size of the encrypted file
     * @return the original plaintext directory name
     * @throws IllegalArgumentException the file is not a directory header file; the size is not as expected
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
        int _inputBytesRead = inputData.read(encrypted);

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


}
