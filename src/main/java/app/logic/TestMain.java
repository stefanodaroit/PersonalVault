package app.logic;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.codec.binary.Hex;

public class TestMain {
  public static void main(String[] args) throws FileNotFoundException, IOException {

    String password = "dhfmn284BBB'''13.";
    
    KeyDerivator k = new KeyDerivator();
    k.setPsw(password);
    byte[] hashedBytes = k.getMasterKey();
    System.out.println(hashedBytes.length);

    /*String hashedString = Hex.encodeHexString(hashedBytes);

    System.out.println(hashedBytes);
    System.out.println(hashedString);*/
  }
}
