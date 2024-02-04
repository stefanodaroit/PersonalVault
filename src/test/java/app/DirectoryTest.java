package app;

import app.core.VaultDirectory;
import org.junit.*;

import javax.crypto.AEADBadTagException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class DirectoryTest {

    private KeyGenerator keygen;
    private SecretKey encKey;
    private final Random r = new Random();
    private final Path dstTestPath = Path.of("./tmp");

    // https://junit.org/junit4/javadoc/latest/org/junit/BeforeClass.html
    @Before
    public void createTestFolder() throws IOException, NoSuchAlgorithmException {
        keygen = KeyGenerator.getInstance("AES");
        keygen.init(256); // bits
        this.encKey = keygen.generateKey();

        Files.createDirectory(dstTestPath);
    }

    // https://junit.org/junit4/javadoc/latest/org/junit/AfterClass.html
    @After
    public void deleteTestFolder() throws IOException {
        Files.delete(dstTestPath);
    }


    @Test(expected = IOException.class)
    public void testDirNullParam() throws Exception {
        new VaultDirectory(null, false);
    }

    @Test()
    public void testDirFound() throws Exception {
        new VaultDirectory(Path.of("./tmp"), false);
    }

    /*@Test(expected = IOException.class)
    public void testDirNotFound() throws Exception {
        Directory d = new Directory("./not-existing-dir");
    }

    @Test(expected = IOException.class)
    public void testDirIsFile() throws Exception {
        Directory d = new Directory("./README.md");
    }*/

    @Test(expected = InvalidKeyException.class)
    public void testEncryptNullKey() throws Exception {
        VaultDirectory de = new VaultDirectory(Path.of("./tmp"), false);
        de.encrypt(this.dstTestPath, null);
    }

    @Test(expected = InvalidKeyException.class)
    public void testDecryptNullKey() throws Exception {
        VaultDirectory de = new VaultDirectory(Path.of("./tmp"), true);
        de.decrypt(this.dstTestPath, null);
    }

    @Test(expected = IOException.class)
    public void testDecryptNullDstPath() throws Exception {
        VaultDirectory de = new VaultDirectory(Path.of("./tmp"), true);
        de.decrypt(null, this.encKey);
    }

    @Test(expected = AEADBadTagException.class)
    public void testDirDifferentKey() throws Exception {
        String encName = "not-existing-enc";
        try {
            VaultDirectory de = new VaultDirectory(dstTestPath.resolve("tmp"), false);
            encName = de.encrypt(dstTestPath, encKey);

            SecretKey encKey2 = keygen.generateKey();
            VaultDirectory dd = new VaultDirectory(dstTestPath.resolve(encName), true);
            dd.decrypt(dstTestPath, encKey2); // should not start creating the file
        } finally {
            Files.delete(Path.of(dstTestPath.toString(), encName, encName + ".dir"));
            Files.delete(Path.of(dstTestPath.toString(), encName));
        }
    }

    @Test()
    public void testDirEncryptDecrypt() throws Exception {
        String encName = "not-existing-enc";
        String decName = "not-existing-dec";
        try {
            VaultDirectory de = new VaultDirectory(dstTestPath.resolve("tmp"), false); // simplified to one subfolder
            encName = de.encrypt(dstTestPath, encKey);

            VaultDirectory dd = new VaultDirectory(dstTestPath.resolve(encName), true);
            decName = dd.decrypt(dstTestPath, encKey);
        } finally {
            Files.delete(Path.of(dstTestPath.toString(), encName, encName + ".dir"));
            Files.delete(Path.of(dstTestPath.toString(), encName));
            Files.delete(Path.of(dstTestPath.toString(), decName));
        }

        Assert.assertEquals("tmp", decName);
    }

    @Test(expected = AEADBadTagException.class)
    public void testDirEncryptDecryptTampered() throws Exception {
        String encName = "not-existing-enc";
        String decName = "not-existing-dec";
        try {
            VaultDirectory de = new VaultDirectory(dstTestPath.resolve("tmp"), false);
            encName = de.encrypt(dstTestPath, encKey);

            // overwrite encrypted file header byte
            byte[] fb = Files.readAllBytes(Path.of(dstTestPath.toString(), encName, encName + ".dir"));

            // generate one different byte at an arbitrary position
            byte x;
            do {
                x = (byte) this.r.nextInt();
            } while (x == fb[0]);
            fb[0] = x;
            Files.write(Path.of(dstTestPath.toString(), encName, encName + ".dir"), fb);

            VaultDirectory dd = new VaultDirectory(dstTestPath.resolve(encName), true);
            decName = dd.decrypt(dstTestPath, encKey); // should not start creating the file
        } finally {
            Files.delete(Path.of(dstTestPath.toString(), encName, encName + ".dir"));
            Files.delete(Path.of(dstTestPath.toString(), encName));
        }
    }


}
