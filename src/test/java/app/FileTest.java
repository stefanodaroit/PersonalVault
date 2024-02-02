package app;

import app.core.File;
import app.core.KeyManager;
import org.junit.*;

import javax.crypto.AEADBadTagException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.InvalidKeyException;
import java.util.Base64;
import java.util.Random;

public class FileTest {

    private final Random r = new Random();
    private final Path localPath = Path.of(".");
    private final Path dstTestPath = Path.of("./test");

    // https://junit.org/junit4/javadoc/latest/org/junit/BeforeClass.html
    @Before
    public void createTestFolder() throws IOException {
        Files.createDirectory(dstTestPath);
    }
    // https://junit.org/junit4/javadoc/latest/org/junit/AfterClass.html
    @After
    public void deleteTestFolder() throws IOException {
        Files.delete(dstTestPath);
    }

    private String createRandomFile() throws IOException {
        byte[] fileOutput = new byte[this.r.nextInt(1000000)];
        this.r.nextBytes(fileOutput);

        String filename = Base64.getUrlEncoder().encodeToString(fileOutput);
        filename = filename.substring(0, Math.min(this.r.nextInt(10, 50), filename.length()));

        Files.write(Path.of(filename), fileOutput);
        return filename;
    }

    @Test(expected = IOException.class)
    public void testFileNullParams() throws Exception {
        File f = new File(null, null);
    }

    @Test(expected = IOException.class)
    public void testFileNullPath() throws Exception {
        File f = new File(null, "filename");
    }

    @Test(expected = IOException.class)
    public void testFileNullFilename() throws Exception {
        File f = new File(".", null);
    }

    @Test()
    public void testFileFound() throws Exception {
        File f = new File(".", "README.md");
    }

    @Test(expected = IOException.class)
    public void testFileNotFound() throws Exception {
        File f = new File(".", "not-existing-file");
    }

    @Test(expected = InvalidKeyException.class)
    public void testEncryptNullKey() throws Exception {
        String filename = "not-existing";
        try {
            filename = createRandomFile();

            File fe = new File(".", filename);
            fe.encrypt(null, localPath);
        } finally {
            Files.delete(Path.of(".", filename));
        }
    }

    @Test(expected = InvalidKeyException.class)
    public void testDecryptNullKey() throws Exception {
        String filename = "not-existing";
        try {
            filename = createRandomFile();

            File fe = new File(".", filename);
            fe.decrypt(null, dstTestPath);
        } finally {
            Files.delete(Path.of(".", filename));
        }
    }

    @Test(expected = AEADBadTagException.class)
    public void testFileDifferentKey() throws Exception {
        String filename = "not-existing";
        String encFilename = "not-existing-enc";
        try {
            filename = createRandomFile();

            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            keygen.init(256); // bits
            SecretKey encKey1 = keygen.generateKey();

            File fe = new File(".", filename);
            encFilename = fe.encrypt(encKey1, localPath);

            SecretKey encKey2 = keygen.generateKey();
            File fd = new File(localPath.toString(), encFilename);
            String decFilename = fd.decrypt(encKey2, dstTestPath); // should not start creating the file
        } finally {
            Files.delete(Path.of(".", filename));
            Files.delete(Path.of(localPath.toString(), encFilename));
        }
    }

    @Test()
    public void testFileEncryptDecrypt() throws Exception {
        String filename = createRandomFile();

        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(256); // bits
        SecretKey encKey = keygen.generateKey();

        File fe = new File(".", filename);
        String encFilename = fe.encrypt(encKey, localPath);
        byte[] f1 = Files.readAllBytes(Path.of(filename));

        File fd = new File(localPath.toString(), encFilename);
        String decFilename = fd.decrypt(encKey, dstTestPath);
        byte[] f2 = Files.readAllBytes(Path.of(dstTestPath.toString(), decFilename));

        Files.delete(Path.of(".", filename));
        Files.delete(Path.of(localPath.toString(), encFilename));
        Files.delete(Path.of(dstTestPath.toString(), decFilename));

        Assert.assertArrayEquals(f1, f2);
    }

    @Test(expected = AEADBadTagException.class)
    public void testFileTamperedHeader() throws Exception {
        String filename = "not-existing";
        String encFilename = "not-existing-enc";
        try {
            filename = createRandomFile();

            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            keygen.init(256); // bits
            SecretKey encKey = keygen.generateKey();

            File fe = new File(".", filename);
            encFilename = fe.encrypt(encKey, localPath);

            // overwrite encrypted file header byte
            byte[] fb = Files.readAllBytes(Path.of(localPath.toString(), encFilename));

            // generate one different byte at an arbitrary position
            byte x;
            do {
                x = (byte) this.r.nextInt();
            } while (x == fb[10]);
            fb[10] = x;
            Files.write(Path.of(localPath.toString(), encFilename), fb);

            File fd = new File(localPath.toString(), encFilename);
            // the header does not match, the file is not created
            String decFilename = fd.decrypt(encKey, dstTestPath);
        } finally {
            Files.delete(Path.of(".", filename));
            Files.delete(Path.of(localPath.toString(), encFilename));
        }
    }

    @Test(expected = AEADBadTagException.class)
    public void testFileTamperedContent() throws Exception {
        String filename = "not-existing";
        String encFilename = "not-existing-enc";
        try {
            filename = createRandomFile();

            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            keygen.init(256); // bits
            SecretKey encKey = keygen.generateKey();

            File fe = new File(".", filename);
            encFilename = fe.encrypt(encKey, localPath);

            Files.write(Path.of(encFilename), new byte[]{(byte) this.r.nextInt()}, StandardOpenOption.APPEND);

            File fd = new File(localPath.toString(), encFilename);
            // file is created because header bytes match,
            // but the content bytes does not match, so throw an exception. The under-the-hood file is deleted
            String decFilename = fd.decrypt(encKey, dstTestPath);
        } finally {
            Files.delete(Path.of(".", filename));
            Files.delete(Path.of(localPath.toString(), encFilename));
        }
    }

    @Test()
    public void testEqualFilenames() throws Exception {
        String filename = "not-existing";
        String encFilename = "not-existing-enc";
        String decFilename = "not-existing-dec";
        try {
            filename = createRandomFile();

            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            keygen.init(256); // bits
            SecretKey encKey = keygen.generateKey();

            File fe = new File(".", filename);
            encFilename = fe.encrypt(encKey, localPath);

            File fd = new File(localPath.toString(), encFilename);
            decFilename = fd.decrypt(encKey, dstTestPath);
        } finally {
            Files.delete(Path.of(".", filename));
            Files.delete(Path.of(localPath.toString(), encFilename));
            Files.delete(Path.of(dstTestPath.toString(), decFilename));
        }

        Assert.assertEquals(filename, decFilename);
    }


}
