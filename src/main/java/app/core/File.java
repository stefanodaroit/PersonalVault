package app.core;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class File {

    private String path; // "./dir/dir2/"
    private String filename; // "file.txt"
    private SecureRandom gen; // random bytes generator
    private boolean encrypted; // flag
    private SecretKey fileKey; // used to encrypt the content
    private byte[] headerIV; // Initialization Vector of the header


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
            headerIV = new byte[]{1, 2, 3, 4, 5};
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e);
        }
    }


    /**
     * public method to encrypt the file
     */
    public void encrypt() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        try {
            byte[] iv = new byte[16];
            this.gen.nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, this.fileKey, spec); // this.fileKey

            InputStream is = new FileInputStream(Paths.get(this.path, this.filename).toString());
            CipherOutputStream cos = new CipherOutputStream(new FileOutputStream("a" + "_chunk_0.bin"), cipher);
            // Encrypt the filename and write it as the first chunk
//            byte[] encryptedFilename = encryptFilename(inputFile, secretKey);
//            cos.write(encryptedFilename);

            byte[] buffer = new byte[64];
            int bytesRead;
            int chunkIndex = 0;

            while ((bytesRead = is.read(buffer)) != -1) {
                cipher.updateAAD(String.format("%d", chunkIndex).getBytes());
                cipher.updateAAD(this.headerIV);

                cos.write(buffer, 0, bytesRead);
                if (bytesRead < buffer.length) {
                    // If the last chunk is less than 64 bits, pad with zeros
                    for (int i = bytesRead; i < buffer.length; i++) {
                        cos.write(0);
                    }
                }

                // Close the current output stream and open a new one for the next chunk
                cos.close();
                chunkIndex++;
                cos = new CipherOutputStream(new FileOutputStream("a" + "_chunk_" + chunkIndex + ".bin"), cipher);

                this.gen.nextBytes(iv);
                spec = new GCMParameterSpec(128, iv);
                cipher.init(Cipher.ENCRYPT_MODE, this.fileKey, spec);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * function to encrypt the header (called in encrypt())
     */
    public byte[] encryptHeader(SecretKey encKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(Constants.SALT_LENGTH, this.gen);
        this.fileKey = keygen.generateKey();

        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, this.fileKey);

        this.headerIV = c.getIV();

        c.update(this.filename.getBytes());
        c.update(this.fileKey.getEncoded());
        return c.doFinal();
    }


    /**
     * function to encrypt the content (called in encrypt())
     */
    public void encryptContent(InputStream in, CipherOutputStream cos, Cipher cipher) throws IOException {

    }


    /**
     * public method to decrypt the file
     */
    public void decrypt(SecretKey encKey) {

    }


    /**
     * function to decrypt the header (called in decrypt())
     */
    public void decryptHeader(SecretKey encKey) {

    }


    /**
     * function to decrypt the content (called in decrypt())
     */
    public void decryptContent() {

    }

}
