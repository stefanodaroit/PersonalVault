package app;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

import app.KeyDerivator.InvalidSaltException;

public class TestMain {
  public static void main(String[] args) throws FileNotFoundException, IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException, InvalidSaltException {

    String password = "dhfmn284BBB'''13.";
    
    KeyDerivator k = new KeyDerivator();
  
    KeyManager km = new KeyManager();
    km.wrapSecretKeys(password);

    System.out.println("WRAP KEYS");

    String first = Hex.encodeHexString(km.getWrapAuthKey());
    String second = Hex.encodeHexString(km.getWrapEncKey());

    System.out.println(first);
    System.out.println(km.getWrapAuthKey().length);
    System.out.println(second);
    System.out.println(km.getWrapEncKey().length);

    System.out.println("UNWRAP KEYS");

    km.unwrapSecretKeys(password);

    System.out.println(km.getUnwrapAuthKey());
    System.out.println(km.getUnwrapEncKey());

  }
}
