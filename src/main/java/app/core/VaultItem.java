package app.core;

import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

public interface VaultItem {
  public String encrypt(Path srcPath, SecretKey encKey) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException;
  public String decrypt(Path dstPath, SecretKey encKey) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, IOException;
  public Path getRelPath(Path vaultPath);
  public Path getAbsPath();
  public String getName();
  public String getEncName();

  public static class VaultItemComparator implements Comparator<VaultItem> {
    @Override
    public int compare(VaultItem vaultItem1, VaultItem vaultItem2) {
      return - vaultItem1.getAbsPath().compareTo(vaultItem2.getAbsPath());
    }
  }
}
