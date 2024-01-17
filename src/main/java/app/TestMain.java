package app;

import app.core.Vault;

public class TestMain {
  public static void main(String[] args) {
    String psw = "Password1234!";
    Vault v;
    try {
      // Create vault
      v = new Vault(".", psw, null);
      //v.changePsw(psw, "eee");
      //v.unlock(psw);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
