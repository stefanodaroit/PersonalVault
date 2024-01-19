package app.core;

import javax.crypto.SecretKey;
import java.security.SecureRandom;

public class File {

    private String path; // "./dir/dir2/"
    private String filename; // "file.txt"
    private SecureRandom gen; // random bytes generator
    private boolean encrypted; // flag
    private SecretKey fileKey; // used to encrypt the content
    private byte[] headerIV; // Initialization Vector of the header


    public File(String path, String filename) {

    }


    /**
    public method to encrypt the file
     */
    public void encrypt(SecretKey encKey) {

    }


    /**
    function to encrypt the header (called in encrypt())
     */
    public void encryptHeader(SecretKey encKey) {

    }


    /**
    function to encrypt the content (called in encrypt())
     */
    public void encryptContent() {

    }


    /**
    public method to decrypt the file
     */
    public void decrypt(SecretKey encKey) {

    }


    /**
    function to decrypt the header (called in decrypt())
     */
    public void decryptHeader(SecretKey encKey) {

    }


    /**
    function to decrypt the content (called in decrypt())
     */
    public void decryptContent() {

    }

}
