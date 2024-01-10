package app;

public class TestMain {
  public static void main(String[] args) {
    String psw = "Password1234!";
    Vault v;
    try {
      // Create vault
      v = new Vault(".", psw);
      // Import vault
      v = new Vault(0, "."); 
      v.unlock(psw);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
