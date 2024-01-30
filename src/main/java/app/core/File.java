package app.core;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class File {

    private String path; // "./dir/dir2/"
    private String filename; // "file.txt"
    private SecureRandom gen; // random bytes generator
    private boolean encrypted; // flag
    private SecretKey fileKey; // used to encrypt the content
    private byte[] headerIV; // Initialization Vector of the header
    private final Cipher c;

    private static final int IVLEN = 12; // bytes
    private static final int CHUNK_SIZE = 64; // bytes
    private static final int KEY_SIZE = 256; // bits
    private static final int KEY_SIZE_ENCODED = KEY_SIZE / 8; // bytes
    private static final int FILENAME_MAX_SIZE = 256; // bytes


    public File(String path, String filename) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.path = Paths.get(path).toString();
        this.filename = filename;
        this.gen = new SecureRandom();
        this.headerIV = new byte[IVLEN];
        this.c = Cipher.getInstance("AES/GCM/NoPadding");
    }


    /**
     * public method to encrypt the file
     */
    public String encrypt(SecretKey encKey) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        StringBuilder encFilename = new StringBuilder();

        byte[] encHeader = this.encryptHeader(encKey, encFilename);

        String encFilenameStr = encFilename.toString();
        OutputStream encryptedOutput = Files.newOutputStream(Paths.get(this.path, encFilenameStr)); // encrypted file output
        encryptedOutput.write(encHeader);

        byte[] encContent = this.encryptContent();
        encryptedOutput.write(encContent);

        encryptedOutput.close();
        this.encrypted = true;

        return encFilenameStr;
    }

    /**
     * function to encrypt the header (called in encrypt())
     */
    private byte[] encryptHeader(SecretKey encKey, StringBuilder encFilename) throws NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(KEY_SIZE, this.gen); // bits
        this.fileKey = keygen.generateKey(); // used to encrypt content later
        byte[] encodedKey = this.fileKey.getEncoded();
        if (encodedKey.length != KEY_SIZE_ENCODED) {
            throw new InvalidKeyException("encodedKey should be " + KEY_SIZE_ENCODED + ", not " + encodedKey.length + "?");
        }

        this.gen.nextBytes(this.headerIV);
        GCMParameterSpec spec = new GCMParameterSpec(128, headerIV);
        this.c.init(Cipher.ENCRYPT_MODE, encKey, spec, this.gen);

        byte[] toEnc = new byte[KEY_SIZE_ENCODED + 1 + FILENAME_MAX_SIZE];
        // first part is the fileKey, needed to decrypt content
        System.arraycopy(encodedKey, 0, toEnc, 0, KEY_SIZE_ENCODED);

        byte[] filenameBytes = this.filename.getBytes();
        if (filenameBytes.length > FILENAME_MAX_SIZE) {
            throw new IllegalBlockSizeException("filename should be <= " + FILENAME_MAX_SIZE + " bytes, instead it is" + filenameBytes.length + " bytes long");
        }
        // second part is the plain filename length expressed in a single byte
        toEnc[KEY_SIZE_ENCODED] = (byte) filenameBytes.length;

        // third part is the filename value
        System.arraycopy(filenameBytes, 0, toEnc, KEY_SIZE_ENCODED + 1, filenameBytes.length);

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
     */
    private byte[] encryptContent() {
        try {
            byte[] iv = new byte[IVLEN];

            InputStream is = Files.newInputStream(Paths.get(this.path, this.filename)); // input file stream
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

        } catch (IOException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        return new byte[0];
    }


    /**
     * public method to decrypt the file
     */
    public String decrypt(SecretKey encKey) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException {

        // TODO: Large file (>512MB?) overflows heap space... use a stream and get file size with   long size = Files.size(path);
        byte[] inputData = Files.readAllBytes(Paths.get(this.path, this.filename)); // read encrypted file

        String originalFilename = this.decryptHeader(encKey, inputData);
        // TODO: filename should be only the originalFilename
        this.decryptContent("decrypted_" + originalFilename, inputData);

        this.encrypted = false;

        return originalFilename;
    }

    /**
     * function to decrypt the header (called in decrypt())
     */
    private String decryptHeader(SecretKey encKey, byte[] inputData) throws InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        int headerFullSize = IVLEN + KEY_SIZE_ENCODED + 1 + FILENAME_MAX_SIZE + 16; // last 16 bytes are GCM authentication tag
        byte[] encrypted = new byte[headerFullSize];
        System.arraycopy(inputData, 0, encrypted, 0, headerFullSize);

        // first part of the full header data is the IV
        System.arraycopy(encrypted, 0, this.headerIV, 0, IVLEN);
        GCMParameterSpec spec = new GCMParameterSpec(128, this.headerIV);
        this.c.init(Cipher.DECRYPT_MODE, encKey, spec, this.gen);

        byte[] ciphertext = new byte[headerFullSize - IVLEN];
        // second part of the full header data is the full ciphertext
        System.arraycopy(encrypted, IVLEN, ciphertext, 0, ciphertext.length);

        byte[] headerContent = this.c.doFinal(ciphertext);

        byte[] fKey = new byte[KEY_SIZE_ENCODED];
        // first part of ciphertext is the fileKey
        System.arraycopy(headerContent, 0, fKey, 0, KEY_SIZE_ENCODED);
        this.fileKey = new SecretKeySpec(fKey, 0, fKey.length, "AES");

        // second part is the plain filename length expressed in a byte
        int filenameSize = headerContent[KEY_SIZE_ENCODED];

        // third part is the filename value
        byte[] filename = new byte[filenameSize];
        System.arraycopy(headerContent, KEY_SIZE_ENCODED + 1, filename, 0, filenameSize);

        String filenameStr = new String(filename, StandardCharsets.UTF_8);
        return filenameStr;
    }

    /**
     * function to decrypt the content (called in decrypt())
     */
    private void decryptContent(String outputFilename, byte[] fileData) {
        try {
            final int HEADER_FULL_SIZE = IVLEN + KEY_SIZE_ENCODED + 1 + FILENAME_MAX_SIZE + 16;

            byte[] contentData = new byte[fileData.length - HEADER_FULL_SIZE];
            System.arraycopy(fileData, HEADER_FULL_SIZE, contentData, 0, fileData.length - HEADER_FULL_SIZE);

            byte[] iv = new byte[IVLEN];
            ByteArrayInputStream is = new ByteArrayInputStream(contentData); // input file stream to read chunks
            ByteArrayOutputStream temp = new ByteArrayOutputStream(); // temporary output

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

                temp.write(decryptedChunk);

                chunkIndex++;
            }

            Path decryptedFilePath = Paths.get(this.path, outputFilename); // output file
            Files.write(decryptedFilePath, temp.toByteArray(), StandardOpenOption.CREATE);

            is.close();
            temp.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
