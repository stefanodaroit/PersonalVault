package app;

import java.nio.file.Path;
import java.util.UUID;

import app.core.Vault;

public class TestMain {
  private static final String PSW = "Password1234!";
  private static final String PATH = "/mnt/c/Users/stefa/Desktop";
  private static final String NAME = "SuperVault";
  public static void main(String[] args) {

    try {
      Vault v = new Vault(UUID.fromString("0700114b-58c7-4a99-95a7-6b714a2a4dfd"), NAME, Path.of(PATH));
      v.unlock(PSW, Path.of("/mnt/c/Users/stefa/Desktop/unlk"));
      //v.addFile(Path.of(PATH, "Security Testing.docx"));
      //v.addDirectory(Path.of(PATH, "dir"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
