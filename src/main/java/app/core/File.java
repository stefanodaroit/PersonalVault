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

    private static final int IVLEN = 12;
    private static final int CHUNK_SIZE = 64;
    private static final int ENCODED_KEY_LENGTH = 16;


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
        String encHeaderOutput = this.encryptHeader(encKey);
        this.encryptContent(encHeaderOutput);
        this.encrypted = true;

        return encHeaderOutput;
    }

    /**
     * function to encrypt the header (called in encrypt())
     */
    private String encryptHeader(SecretKey encKey) throws NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(128, this.gen);
        this.fileKey = keygen.generateKey();

        byte[] encodedKey = this.fileKey.getEncoded();
//        System.out.println(encodedKey.length);
        if (encodedKey.length != ENCODED_KEY_LENGTH) {
            throw new InvalidKeyException("encodedKey should be " + ENCODED_KEY_LENGTH + ", not " + encodedKey.length + "?");
        }
//        System.out.println(encodedKey);

        this.gen.nextBytes(this.headerIV);
        GCMParameterSpec spec = new GCMParameterSpec(128, headerIV);
        this.c.init(Cipher.ENCRYPT_MODE, encKey, spec, this.gen);

        byte[] filenameBytes = this.filename.getBytes();
        byte[] toEnc = new byte[ENCODED_KEY_LENGTH + filenameBytes.length];
        System.arraycopy(encodedKey, 0, toEnc, 0, ENCODED_KEY_LENGTH); // first part is the fileKey, needed to decrypt content
        System.arraycopy(filenameBytes, 0, toEnc, ENCODED_KEY_LENGTH, filenameBytes.length); // second part is the real filename

        byte[] encHeader = this.c.doFinal(toEnc);

        byte[] output = new byte[IVLEN + encHeader.length];
        System.arraycopy(headerIV, 0, output, 0, IVLEN);
        System.arraycopy(encHeader, 0, output, IVLEN, encHeader.length);

        String outputBase = Base64.getUrlEncoder().encodeToString(output);
        return outputBase;
    }

    /**
     * function to encrypt the content (called in encrypt())
     */
    private void encryptContent(String encryptedFilename) {
        try {
            byte[] iv = new byte[IVLEN];
            this.gen.nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            this.c.init(Cipher.ENCRYPT_MODE, this.fileKey, spec, this.gen);

            InputStream is = Files.newInputStream(Paths.get(this.path, this.filename)); // input file stream

            ByteArrayOutputStream temp = new ByteArrayOutputStream(); // temporary output
            OutputStream encryptedOutput = Files.newOutputStream(Paths.get(encryptedFilename)); // encrypted file output

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

            encryptedOutput.write(ivPlusEncryptedBytes);
//            System.out.println(ivPlusEncryptedBytes.length);
//            System.out.println(Arrays.toString(ivPlusEncryptedBytes));
            encryptedOutput.close();

        } catch (IOException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }




    /**
     * public method to decrypt the file
     */
    public String decrypt(SecretKey encKey) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String originalFilename = this.decryptHeader(encKey);
        this.decryptContent("decrypted_" + originalFilename);

        this.encrypted = false;
        return originalFilename;
    }

    /**
     * function to decrypt the header (called in decrypt())
     */
    private String decryptHeader(SecretKey encKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");

        byte[] encrypted = Base64.getUrlDecoder().decode(this.filename);
        byte[] ciphertext = new byte[encrypted.length - IVLEN];

        // first part is the IV
        System.arraycopy(encrypted, 0, this.headerIV, 0, IVLEN);
        GCMParameterSpec spec = new GCMParameterSpec(128, this.headerIV);
        c.init(Cipher.DECRYPT_MODE, encKey, spec, this.gen);

        // second part is the ciphertext
        System.arraycopy(encrypted, IVLEN, ciphertext, 0, ciphertext.length);

        byte[] headerBytes = c.doFinal(ciphertext);

        byte[] fKey = new byte[ENCODED_KEY_LENGTH];
        byte[] filename = new byte[headerBytes.length - ENCODED_KEY_LENGTH];
        System.arraycopy(headerBytes, 0, fKey, 0, ENCODED_KEY_LENGTH);
        this.fileKey = new SecretKeySpec(fKey, 0, fKey.length, "AES");

        System.arraycopy(headerBytes, ENCODED_KEY_LENGTH, filename, 0, headerBytes.length - ENCODED_KEY_LENGTH);

        String filenameStr = new String(filename, StandardCharsets.UTF_8);
//        System.out.println(filenameStr);
        return filenameStr;
    }

    /**
     * function to decrypt the content (called in decrypt())
     */
    private void decryptContent(String outputFilename) {
        try {
            // Initialize the cipher for content decryption
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            // TODO: Large file (>512MB?) overflows heap space... use a stream and get file size with   long size = Files.size(path);
            byte[] inputData = Files.readAllBytes(Paths.get(this.path, this.filename)); // read encrypted file

            byte[] iv = new byte[IVLEN];
            byte[] ciphertext = new byte[inputData.length - IVLEN];
            // first part is the IV
            System.arraycopy(inputData, 0, iv, 0, IVLEN);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, this.fileKey, spec, this.gen);

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

                cipher.update(buffer, 0, bytesRead); // Decrypt the chunk

                chunkIndex++;
            }
            byte[] decryptedBytes = cipher.doFinal();

            Path decryptedFilePath = Paths.get(path, outputFilename); // output file
            Files.write(decryptedFilePath, decryptedBytes, StandardOpenOption.CREATE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
