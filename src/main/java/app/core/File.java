package app.core;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
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

public class File {

    private String path; // "./dir/dir2/"
    private String filename; // "file.txt"
    private SecureRandom gen; // random bytes generator
    private boolean encrypted; // flag
    private SecretKey fileKey; // used to encrypt the content
    private byte[] headerIV; // Initialization Vector of the header

    private static final int IVLEN = 12;
    private static final int CHUNK_SIZE = 64;


    public File(String path, String filename) {
        this.path = Paths.get(path).toString();
        this.filename = filename;
        this.gen = new SecureRandom();


        try {
            // TODO TO REMOVE
            // Create a KeyGenerator object for AES
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            // Initialize the KeyGenerator with a 128-bit key size
            keyGenerator.init(128);
            // Generate a SecretKey
            SecretKey secretKey = keyGenerator.generateKey();
            this.fileKey = secretKey;
            // Get the raw key bytes
            byte[] keyBytes = secretKey.getEncoded();
            System.out.println(keyBytes);

            this.headerIV = new byte[IVLEN];
            this.gen.nextBytes(this.headerIV);
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e);
        }
    }


    /**
     * public method to encrypt the file
     */
    public void encrypt() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        try {
            // Initialize the cipher for content decryption
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            byte[] iv = new byte[IVLEN];
            this.gen.nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, this.fileKey, spec, this.gen);

            InputStream is = Files.newInputStream(Paths.get(this.path, this.filename)); // input file stream

            ByteArrayOutputStream temp = new ByteArrayOutputStream(); // temporary output
            OutputStream encryptedOutput = Files.newOutputStream(Paths.get(this.filename + ".enc")); // encrypted file output

            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            int chunkIndex = 0;
            while ((bytesRead = is.read(buffer)) != -1) {
//                cipher.updateAAD(String.format("%d", chunkIndex).getBytes());
//                cipher.updateAAD(this.headerIV);

                byte[] encryptedChunk = cipher.update(buffer, 0, bytesRead);
                temp.write(encryptedChunk);

                chunkIndex++;
            }
            byte[] finalChunk = cipher.doFinal();
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

            this.encrypted = true;
        } catch (IOException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();;
        }
    }


    /**
     * function to encrypt the header (called in encrypt())
     */
    public byte[] encryptHeader(SecretKey encKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(128, this.gen);
        this.fileKey = keygen.generateKey();

        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        this.gen.nextBytes(this.headerIV);
        GCMParameterSpec spec = new GCMParameterSpec(128, headerIV);
        c.init(Cipher.ENCRYPT_MODE, encKey, spec, this.gen);

        byte[] encFilename = c.doFinal(this.filename.getBytes());

        byte[] output = new byte[IVLEN + encFilename.length];
        System.arraycopy(headerIV, 0, output, 0, IVLEN);
        // Copy encryptedBytes into the new array
        System.arraycopy(encFilename, 0, output, IVLEN, encFilename.length);

//        c.update(this.fileKey.getEncoded());
//        return c.doFinal();
        return output;
    }


    /**
     * function to encrypt the content (called in encrypt())
     */
    public void encryptContent(InputStream in, CipherOutputStream cos, Cipher cipher) throws IOException {

    }


    /**
     * public method to decrypt the file
     */
    public void decrypt() {
        System.out.println("DECRYPT");
        try {
            // Initialize the cipher for content decryption
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            byte[] inputData = Files.readAllBytes(Paths.get(this.path, this.filename + ".enc")); // read encrypted file

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

            Path decryptedFilePath = Paths.get(path, "decrypted_" + filename); // output file
            Files.write(decryptedFilePath, decryptedBytes, StandardOpenOption.CREATE);

            this.encrypted = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


        /**
     * function to decrypt the header (called in decrypt())
     */
    public String decryptHeader(SecretKey encKey, byte[] encrypted) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");

        byte[] iv = new byte[IVLEN];
        byte[] ciphertext = new byte[encrypted.length - IVLEN];
        // first part is the IV
        System.arraycopy(encrypted, 0, iv, 0, IVLEN);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        c.init(Cipher.DECRYPT_MODE, encKey, spec, this.gen);

        // second part is the ciphertext
        System.arraycopy(encrypted, IVLEN, ciphertext, 0, ciphertext.length);

        byte[] filenameBytes = c.doFinal(ciphertext);
        String filenameStr = new String(filenameBytes, StandardCharsets.UTF_8);

        System.out.println(filenameStr);
        return filenameStr;
    }


    /**
     * function to decrypt the content (called in decrypt())
     */
    public void decryptContent() {
//                byte[] plainText = new byte[CHUNK_SIZE];
//                System.arraycopy(buffer, 0, plainText, 0, bytesRead);
//                System.out.println(bytesRead);
//                System.out.println(Arrays.toString(plainText));

    }

}
