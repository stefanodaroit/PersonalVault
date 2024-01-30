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
        StringBuilder encFilename = new StringBuilder ();

        byte[] encHeader = this.encryptHeader(encKey, encFilename);

        String encFilenameStr = encFilename.toString();
        OutputStream encryptedOutput = Files.newOutputStream(Paths.get(encFilenameStr)); // encrypted file output
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

        byte[] filenameBytes = this.filename.getBytes();
        if (filenameBytes.length > FILENAME_MAX_SIZE) {
            throw new IllegalBlockSizeException("filename should be <= " + FILENAME_MAX_SIZE + " bytes, instead it is" + filenameBytes.length + " bytes long");
        }
        byte[] toEnc = new byte[KEY_SIZE_ENCODED + 1 + FILENAME_MAX_SIZE];
        System.arraycopy(encodedKey, 0, toEnc, 0, KEY_SIZE_ENCODED); // first part is the fileKey, needed to decrypt content
        toEnc[KEY_SIZE_ENCODED] = (byte) filenameBytes.length; // second part is the plain filename length expressed in a single byte
        System.arraycopy(filenameBytes, 0, toEnc, KEY_SIZE_ENCODED+1, filenameBytes.length); // third part is the filename value

        byte[] encHeader = this.c.doFinal(toEnc);

        byte[] output = new byte[IVLEN + encHeader.length];
        System.arraycopy(headerIV, 0, output, 0, IVLEN);
        System.arraycopy(encHeader, 0, output, IVLEN, encHeader.length);

        String temp = Base64.getUrlEncoder().encodeToString(encHeader);
        temp = temp.substring(0, Math.min(temp.length(), 15));
        encFilename.append(temp); // param as reference

        return output;
    }

    /**
     * function to encrypt the content (called in encrypt())
     */
    private byte[] encryptContent() {
        try {
            byte[] iv = new byte[IVLEN];
            this.gen.nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            this.c.init(Cipher.ENCRYPT_MODE, this.fileKey, spec, this.gen);

            InputStream is = Files.newInputStream(Paths.get(this.path, this.filename)); // input file stream

            ByteArrayOutputStream temp = new ByteArrayOutputStream(); // temporary output


            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            int chunkIndex = 0;
            while ((bytesRead = is.read(buffer)) != -1) {
//                cipher.updateAAD(String.format("%d", chunkIndex).getBytes());
//                cipher.updateAAD(this.headerIV);

                byte[] encryptedChunk = this.c.update(buffer, 0, bytesRead);
                temp.write(encryptedChunk);

                chunkIndex++;
            }
            byte[] finalChunk = this.c.doFinal();
            temp.write(finalChunk);

            byte[] ivPlusEncryptedBytes = new byte[IVLEN + temp.size()];
            // Copy IV bytes into the new array
            System.arraycopy(iv, 0, ivPlusEncryptedBytes, 0, IVLEN);
            // Copy encryptedBytes into the new array
            System.arraycopy(temp.toByteArray(), 0, ivPlusEncryptedBytes, IVLEN, temp.size());

            return ivPlusEncryptedBytes;

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
        this.decryptContent("decrypted_" + originalFilename, inputData);

        this.encrypted = false;
        return originalFilename;
    }

    /**
     * function to decrypt the header (called in decrypt())
     */
    private String decryptHeader(SecretKey encKey, byte[] inputData) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        int headerFullSize = IVLEN + KEY_SIZE_ENCODED + 1 + FILENAME_MAX_SIZE + 16; // last 16 bytes are GCM authentication tag
        byte[] encrypted = new byte[headerFullSize];
        System.arraycopy(inputData, 0, encrypted, 0, headerFullSize);

        // first part is the IV
        System.arraycopy(encrypted, 0, this.headerIV, 0, IVLEN);
        GCMParameterSpec spec = new GCMParameterSpec(128, this.headerIV);
        this.c.init(Cipher.DECRYPT_MODE, encKey, spec, this.gen);

        byte[] ciphertext = new byte[headerFullSize - IVLEN];
        // second part is the full ciphertext
        System.arraycopy(encrypted, IVLEN, ciphertext, 0, ciphertext.length);

        byte[] headerBytes = this.c.doFinal(ciphertext);

        byte[] fKey = new byte[KEY_SIZE_ENCODED];
        // first part of ciphertext is the fileKey
        System.arraycopy(headerBytes, 0, fKey, 0, KEY_SIZE_ENCODED);
        this.fileKey = new SecretKeySpec(fKey, 0, fKey.length, "AES");

        // second part is the plain filename length expressed in a byte
        int filenameSize = headerBytes[KEY_SIZE_ENCODED];

        // third part is the filename value
        byte[] filename = new byte[filenameSize];
        System.arraycopy(headerBytes, KEY_SIZE_ENCODED+1, filename, 0, filenameSize);

        String filenameStr = new String(filename, StandardCharsets.UTF_8);
        return filenameStr;
    }

    /**
     * function to decrypt the content (called in decrypt())
     */
    private void decryptContent(String outputFilename, byte[] file) {
        try {
            final int HEADER_FULL_SIZE = IVLEN + KEY_SIZE_ENCODED + 1 + FILENAME_MAX_SIZE + 16;

            byte[] inputData = new byte[file.length - HEADER_FULL_SIZE];
            System.arraycopy(file, HEADER_FULL_SIZE, inputData, 0, file.length - HEADER_FULL_SIZE);

            byte[] iv = new byte[IVLEN];
            byte[] ciphertext = new byte[inputData.length - IVLEN];
            // first part is the IV
            System.arraycopy(inputData, 0, iv, 0, IVLEN);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            this.c.init(Cipher.DECRYPT_MODE, this.fileKey, spec, this.gen);

            // second part is the ciphertext
            System.arraycopy(inputData, IVLEN, ciphertext, 0, ciphertext.length);

            ByteArrayInputStream is = new ByteArrayInputStream(ciphertext); // input file stream to read chunks

            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            int chunkIndex = 0;
            while ((bytesRead = is.read(buffer)) != -1) {
                // Set AAD for chunk decryption
//                cipher.updateAAD(String.format("%d", chunkIndex).getBytes()); // Chunk ID
//                cipher.updateAAD(this.headerIV); // Header IV

                this.c.update(buffer, 0, bytesRead); // Decrypt the chunk

                chunkIndex++;
            }
            byte[] decryptedBytes = this.c.doFinal();

            Path decryptedFilePath = Paths.get(path, outputFilename); // output file
            Files.write(decryptedFilePath, decryptedBytes, StandardOpenOption.CREATE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
