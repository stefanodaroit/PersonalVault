package app.logic;

import org.apache.commons.codec.binary.Hex;

public class TestMain {
  public static void main(String[] args) {

    String password = "PSsmcjdk1345,?24n";
    
    KeyDerivator k = new KeyDerivator();
    k.setPsw(password);
    byte[] hashedBytes = k.getMasterKey();

    String hashedString = Hex.encodeHexString(hashedBytes);

    System.out.println(hashedBytes);
    System.out.println(hashedString);
  }
}
