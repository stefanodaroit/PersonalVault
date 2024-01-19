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
    }


    /**
     * public method to encrypt the file
     */
    public void encrypt(SecretKey encKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
//        GCMParameterSpec spec = new GCMParameterSpec(128, cipher.getIV());
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, encKey);

        try (InputStream is = new FileInputStream(Paths.get(this.path, this.filename).toString()); CipherOutputStream cos = new CipherOutputStream(new FileOutputStream("a" + "_chunk_0.bin"), cipher)) {

            // Encrypt the filename and write it as the first chunk
//            byte[] encryptedFilename = encryptHeader(encKey);
//            cos.write(encryptedFilename);

            this.encryptContent(is, cos, cipher);


        } catch (Exception e) {
            System.out.println(e);
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
        byte[] buffer = new byte[8];
        int bytesRead;
        int chunkIndex = 0;

        while ((bytesRead = in.read(buffer)) != -1) {
            cipher.updateAAD(ByteBuffer.allocateDirect(chunkIndex).put(this.headerIV));
//            cipher.updateAAD(this.headerIV);

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
        }
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
