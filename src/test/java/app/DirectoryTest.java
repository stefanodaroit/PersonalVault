package app;

import app.core.Directory;
import app.core.File;
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
        Directory d = new Directory(null);
    }

    @Test()
    public void testDirFound() throws Exception {
        Directory d = new Directory("./tmp");
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
        Directory de = new Directory("./tmp");
        de.encrypt(null, this.dstTestPath);
    }

    @Test(expected = InvalidKeyException.class)
    public void testDecryptNullKey() throws Exception {
        Directory de = new Directory("./tmp");
        de.decrypt(null, this.dstTestPath);
    }

    @Test(expected = IOException.class)
    public void testEncryptNullDstPath() throws Exception {
        Directory de = new Directory("./tmp");
        de.encrypt(this.encKey, null);
    }

    @Test(expected = IOException.class)
    public void testDecryptNullDstPath() throws Exception {
        Directory de = new Directory("./tmp");
        de.decrypt(this.encKey, null);
    }

    @Test(expected = AEADBadTagException.class)
    public void testDirDifferentKey() throws Exception {
        String encName = "not-existing-enc";
        try {
            Directory de = new Directory("./tmp");
            encName = de.encrypt(encKey, dstTestPath);

            SecretKey encKey2 = keygen.generateKey();
            Directory dd = new Directory(Path.of(dstTestPath.toString(), encName).toString());
            String decName = dd.decrypt(encKey2, dstTestPath); // should not start creating the file
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
            Directory de = new Directory("./tmp"); // simplified to one subfolder
            encName = de.encrypt(encKey, dstTestPath);

            Directory dd = new Directory(Path.of(dstTestPath.toString(), encName).toString());
            decName = dd.decrypt(encKey, dstTestPath);
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
            Directory de = new Directory("./tmp");
            encName = de.encrypt(encKey, dstTestPath);

            // overwrite encrypted file header byte
            byte[] fb = Files.readAllBytes(Path.of(dstTestPath.toString(), encName, encName + ".dir"));

            // generate one different byte at an arbitrary position
            byte x;
            do {
                x = (byte) this.r.nextInt();
            } while (x == fb[0]);
            fb[0] = x;
            Files.write(Path.of(dstTestPath.toString(), encName, encName + ".dir"), fb);

            Directory dd = new Directory(Path.of(dstTestPath.toString(), encName).toString());
            decName = dd.decrypt(encKey, dstTestPath); // should not start creating the file
        } finally {
            Files.delete(Path.of(dstTestPath.toString(), encName, encName + ".dir"));
            Files.delete(Path.of(dstTestPath.toString(), encName));
        }
    }


}
