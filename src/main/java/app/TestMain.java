package app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;

import app.core.Vault;
import app.core.VaultConfiguration;

import app.core.Vault;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.file.Path;

public class TestMain {
  private static final String PSW = "Password1234!";
  private static final String PATH = "/mnt/c/Users/stefa/Desktop";
  private static final String NAME = "SuperVault";
  public static void main(String[] args) {

    try {
      Vault v = new Vault(UUID.fromString("0700114b-58c7-4a99-95a7-6b714a2a4dfd"), NAME, PATH);
      v.unlock(PSW);
      v.addDirectory(Path.of(PATH, "dir"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
