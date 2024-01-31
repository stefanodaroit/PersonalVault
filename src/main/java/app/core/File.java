package app.core;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class File {

    private final Path path; // "./dir/dir2/"
    private final String filename; // "file.txt"
    private final Path filenamePath; // "./dir/dir2/file.txt"
    private final SecureRandom gen; // random bytes generator
    private boolean encrypted; // flag
    private SecretKey fileKey; // used to encrypt the content
    private byte[] headerIV; // Initialization Vector of the header
    private final Cipher c;

    private static final int IVLEN = 12; // bytes
    private static final int CHUNK_SIZE = 65536; // bytes 2^16
    private static final int KEY_SIZE_BITS = 256; // bits
    private static final int KEY_SIZE = KEY_SIZE_BITS / 8; // bytes
    private static final int FILENAME_MAX_SIZE = 256; // bytes
    private static final String KEY_GEN_ALGO = "AES";

    // TODO: Delete files and keep encrypted/decrypted only

    /**
     * Instantiate a file operation
     * @param path base directory
     * @param filename name of the file to open
     * @throws IOException The path/filename is not a file or the file is not found
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    public File(String path, String filename) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException {
        if (path == null || filename == null) {
            throw new IOException("path and filename cannot be null");
        }
        this.path = Path.of(path).normalize();
        this.filename = Path.of(filename).normalize().getFileName().toString();

        this.filenamePath = Path.of(this.path.toString(), this.filename);
        if (!(new java.io.File(this.filenamePath.toString()).isFile())) {
            throw new IOException("Path '" + this.filenamePath + "' is not a file or not found");
        }

        this.gen = new SecureRandom();
        this.headerIV = new byte[IVLEN];
        this.c = Cipher.getInstance("AES/GCM/NoPadding");
    }

    /**
     * Public method to encrypt the file
     * @param encKey key to use to encrypt the header
     * @return the filename of the encrypted file
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws IOException
     */
    public String encrypt(SecretKey encKey) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        if (encKey == null) {
            throw new InvalidKeyException("encryption key cannot be null");
        }
        StringBuilder encFilename = new StringBuilder();

        byte[] encHeader = this.encryptHeader(encKey, encFilename);
        byte[] encContent = this.encryptContent();

        String encFilenameStr = Path.of(encFilename.toString()).normalize().getFileName().toString();
        OutputStream encryptedOutput = Files.newOutputStream(Path.of(this.path.toString(), encFilenameStr)); // encrypted file output

        encryptedOutput.write(encHeader);
        encryptedOutput.write(encContent);

        encryptedOutput.close();
        this.encrypted = true;

        return encFilenameStr;
    }

    /**
     * function to encrypt the header (called in encrypt())
     * @param encKey key to use to encrypt the header
     * @param encFilename instance of StringBuilder variable, at the end of this method it will contain the encrypted filename
     * @return encrypted bytes of full header
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidAlgorithmParameterException
     */
    private byte[] encryptHeader(SecretKey encKey, StringBuilder encFilename) throws NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        KeyGenerator keygen = KeyGenerator.getInstance(KEY_GEN_ALGO);
        keygen.init(KEY_SIZE_BITS, this.gen); // bits
        this.fileKey = keygen.generateKey(); // used to encrypt content later
        byte[] encodedKey = this.fileKey.getEncoded();
        if (encodedKey.length != KEY_SIZE) {
            throw new InvalidKeyException("encodedKey should be " + KEY_SIZE + ", not " + encodedKey.length + "?");
        }

        this.gen.nextBytes(this.headerIV);
        GCMParameterSpec spec = new GCMParameterSpec(128, headerIV);
        this.c.init(Cipher.ENCRYPT_MODE, encKey, spec, this.gen);

        byte[] toEnc = new byte[KEY_SIZE + 1 + FILENAME_MAX_SIZE];
        // first part is the fileKey, needed to decrypt content
        System.arraycopy(encodedKey, 0, toEnc, 0, KEY_SIZE);

        byte[] filenameBytes = this.filename.getBytes();
        if (filenameBytes.length > FILENAME_MAX_SIZE) {
            throw new IllegalBlockSizeException("filename should be <= " + FILENAME_MAX_SIZE + " bytes, instead it is" + filenameBytes.length + " bytes long");
        }
        // second part is the plain filename length expressed in a single byte
        toEnc[KEY_SIZE] = (byte) filenameBytes.length;

        // third part is the filename value
        System.arraycopy(filenameBytes, 0, toEnc, KEY_SIZE + 1, filenameBytes.length);

        byte[] encHeader = this.c.doFinal(toEnc);

        byte[] output = new byte[IVLEN + encHeader.length];
        System.arraycopy(headerIV, 0, output, 0, IVLEN);
        System.arraycopy(encHeader, 0, output, IVLEN, encHeader.length);

        String tempEncFilename = Base64.getUrlEncoder().encodeToString(encHeader);
        tempEncFilename = tempEncFilename.substring(0, Math.min(tempEncFilename.length(), 15));
        encFilename.append(tempEncFilename); // param as reference

        return output;
    }


    /**
     * function to encrypt the content (called in encrypt())
     * @return encrypted bytes of the content
     * @throws IOException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private byte[] encryptContent() throws IOException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] iv = new byte[IVLEN];

        InputStream is = Files.newInputStream(this.filenamePath); // input file stream
        ByteArrayOutputStream temp = new ByteArrayOutputStream(); // temporary output stream

        byte[] buffer = new byte[CHUNK_SIZE];
        int bytesRead;
        int chunkIndex = 0;
        while ((bytesRead = is.read(buffer)) != -1) {
            this.gen.nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            this.c.init(Cipher.ENCRYPT_MODE, this.fileKey, spec, this.gen);

            this.c.updateAAD(String.format("%d", chunkIndex).getBytes());
            this.c.updateAAD(this.headerIV);

            byte[] encryptedChunkContent = this.c.doFinal(buffer, 0, bytesRead);
            temp.write(iv); // first part of chuck is iv
            temp.write(encryptedChunkContent); // second part of chuck is the encrypted content

            chunkIndex++;
        }

        byte[] output = temp.toByteArray();

        is.close();
        temp.close();

        return output;
    }


    /**
     * Public method to decrypt the file
     * @param encKey key to use to decrypt the header
     * @return the original plaintext filename
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws IOException
     */
    public String decrypt(SecretKey encKey) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, IOException {
        if (encKey == null) {
            throw new InvalidKeyException("encryption key cannot be null");
        }
        Path inputFilePath = this.filenamePath;
        int dstFileSize = (int)Files.size(inputFilePath); // MAX 2.14 GB !!!
        InputStream inputData = Files.newInputStream(inputFilePath); // input file stream

        String originalFilename = this.decryptHeader(encKey, inputData, dstFileSize);
        try {
            // TODO: filename should be only the originalFilename
            this.decryptContent("decrypted_" + originalFilename, inputData, dstFileSize);
        } catch (Exception e) {
            Files.deleteIfExists(Path.of(this.path.toString(), "decrypted_" + originalFilename));
            throw e;
        }

        this.encrypted = false;

        return "decrypted_" + originalFilename;
    }

    /**
     * function to decrypt the header (called in decrypt())
     * @param encKey key to use to encrypt the header
     * @param inputData stream of the encrypted file
     * @param inputSize size of the encrypted file
     * @return the original plaintext filename
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws IOException
     */
    private String decryptHeader(SecretKey encKey, InputStream inputData, int inputSize) throws InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        int headerFullSize = IVLEN + KEY_SIZE + 1 + FILENAME_MAX_SIZE + 16; // last 16 bytes are GCM authentication tag
        byte[] encrypted = new byte[headerFullSize];
        int _inputBytesRead = inputData.read(encrypted);

        // first part of the full header data is the IV
        System.arraycopy(encrypted, 0, this.headerIV, 0, IVLEN);
        GCMParameterSpec spec = new GCMParameterSpec(128, this.headerIV);
        this.c.init(Cipher.DECRYPT_MODE, encKey, spec, this.gen);

        byte[] ciphertext = new byte[headerFullSize - IVLEN];
        // second part of the full header data is the full ciphertext
        System.arraycopy(encrypted, IVLEN, ciphertext, 0, ciphertext.length);

        byte[] headerContent = this.c.doFinal(ciphertext);

        byte[] fKey = new byte[KEY_SIZE];
        // first part of ciphertext is the fileKey
        System.arraycopy(headerContent, 0, fKey, 0, KEY_SIZE);
        this.fileKey = new SecretKeySpec(fKey, 0, fKey.length, KEY_GEN_ALGO);

        // second part is the plain filename length expressed in a byte
        int filenameSize = headerContent[KEY_SIZE];

        // third part is the filename value
        byte[] filename = new byte[filenameSize];
        System.arraycopy(headerContent, KEY_SIZE + 1, filename, 0, filenameSize);

        String filenameStr = new String(filename, StandardCharsets.UTF_8);
        return filenameStr;
    }

    /**
     * function to decrypt the content (called in decrypt())
     * @param outputFilename filename of the plaintext file to be written
     * @param fileData stream of the encrypted file
     * @param inputSize size of the encrypted file
     * @throws IOException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private void decryptContent(String outputFilename, InputStream fileData, int inputSize) throws IOException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        outputFilename = Path.of(outputFilename).normalize().getFileName().toString();
        final int HEADER_FULL_SIZE = IVLEN + KEY_SIZE + 1 + FILENAME_MAX_SIZE + 16;

        byte[] contentData = new byte[inputSize - HEADER_FULL_SIZE];
        int _fileBytesRead = fileData.read(contentData);

        byte[] iv = new byte[IVLEN];
        ByteArrayInputStream is = new ByteArrayInputStream(contentData); // input file stream to read chunks
        OutputStream outputFile = Files.newOutputStream(Path.of(this.path.toString(), outputFilename));

        // chunk: first part is IV, then the actual content plus the 16 bytes GCM authentication tag
        byte[] buffer = new byte[IVLEN + CHUNK_SIZE + 16];
        int bytesRead;
        int chunkIndex = 0;
        while ((bytesRead = is.read(buffer)) != -1) {
            // first part is the IV
            System.arraycopy(buffer, 0, iv, 0, IVLEN);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            this.c.init(Cipher.DECRYPT_MODE, this.fileKey, spec, this.gen);

            // Set AAD for chunk decryption
            this.c.updateAAD(String.format("%d", chunkIndex).getBytes()); // Chunk ID
            this.c.updateAAD(this.headerIV); // Header IV

            // second part is the ciphertext
            byte[] decryptedChunk = this.c.doFinal(buffer, IVLEN, bytesRead - IVLEN); // Decrypt the chunk

            outputFile.write(decryptedChunk);

            chunkIndex++;
        }

        is.close();
        outputFile.close();
    }

}
