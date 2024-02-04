package app;

import app.core.VaultFile;
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
    private static final Path localPath = Path.of(".");
    private static final Path dstTestPath = Path.of("./test");
    private static final Path dstOutputPath = Path.of("./output");
    private final Path notExistingPath = Path.of("not-existing");
    private final Path notExistingEncPath = Path.of("not-existing-enc");

    // https://junit.org/junit4/javadoc/latest/org/junit/BeforeClass.html
    @BeforeClass
    public static void createTestFolder() throws IOException {
        Files.createDirectory(dstTestPath);
        Files.createDirectory(dstOutputPath);
    }

    // https://junit.org/junit4/javadoc/latest/org/junit/AfterClass.html
    @AfterClass
    public static void deleteTestFolder() throws IOException {
        Files.delete(dstTestPath);
        Files.delete(dstOutputPath);
    }

    private Path createRandomFile() throws IOException {
        byte[] fileOutput = new byte[this.r.nextInt(1000000)];
        this.r.nextBytes(fileOutput);

        String filename = Base64.getUrlEncoder().encodeToString(fileOutput);
        filename = filename.substring(0, Math.min(this.r.nextInt(10, 50), filename.length()));

        Path dstFilePath = dstTestPath.resolve(filename);
        Files.write(dstFilePath, fileOutput);
        return dstFilePath;
    }

    @Test(expected = IOException.class)
    public void testFileNullParam() throws Exception {
        new VaultFile(null, false);
    }

    @Test()
    public void testFileFound() throws Exception {
        new VaultFile(Path.of("./README.md"), false);
    }

    @Test(expected = IOException.class)
    public void testFileNotFound() throws Exception {
        new VaultFile(Path.of("./not-existing-file"), false);
    }

    @Test(expected = InvalidKeyException.class)
    public void testEncryptNullKey() throws Exception {
        Path filePath = notExistingPath;
        try {
            filePath = createRandomFile();

            VaultFile fe = new VaultFile(filePath, false);
            fe.encrypt(dstTestPath, null);
        } finally {
            Files.delete(filePath);
        }
    }

    @Test(expected = InvalidKeyException.class)
    public void testDecryptNullKey() throws Exception {
        Path filePath = notExistingPath;
        try {
            filePath = createRandomFile();

            VaultFile fe = new VaultFile(filePath, true);
            fe.decrypt(dstTestPath, null);
        } finally {
            Files.delete(filePath);
        }
    }

    @Test(expected = AEADBadTagException.class)
    public void testFileDifferentKey() throws Exception {
        Path filePath = notExistingPath;
        Path encFilename = notExistingEncPath;
        try {
            filePath = createRandomFile();

            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            keygen.init(256); // bits
            SecretKey encKey1 = keygen.generateKey();

            VaultFile fe = new VaultFile(filePath, false);
            encFilename = dstTestPath.resolve(fe.encrypt(filePath, encKey1));

            SecretKey encKey2 = keygen.generateKey();
            VaultFile fd = new VaultFile(encFilename, true);
            fd.decrypt(dstOutputPath, encKey2); // should not start creating the file
        } finally {
            Files.delete(filePath);
            Files.delete(encFilename);
        }
    }

    @Test()
    public void testFileEncryptDecrypt() throws Exception {
        Path filePath = createRandomFile();

        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(256); // bits
        SecretKey encKey = keygen.generateKey();

        byte[] f1 = Files.readAllBytes(filePath);
        VaultFile fe = new VaultFile(filePath, false);
        Path encFilename = dstTestPath.resolve(fe.encrypt(filePath, encKey));

        VaultFile fd = new VaultFile(encFilename, true);
        Path decFilename = dstOutputPath.resolve(fd.decrypt(dstOutputPath, encKey));
        byte[] f2 = Files.readAllBytes(decFilename);

        Files.delete(filePath);
        Files.delete(encFilename);
        Files.delete(decFilename);

        Assert.assertArrayEquals(f1, f2);
    }

    @Test(expected = AEADBadTagException.class)
    public void testFileTamperedHeader() throws Exception {
        Path filePath = notExistingPath;
        Path encFilePath = notExistingEncPath;
        try {
            filePath = createRandomFile();

            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            keygen.init(256); // bits
            SecretKey encKey = keygen.generateKey();

            VaultFile fe = new VaultFile(filePath, false);
            encFilePath = dstTestPath.resolve(fe.encrypt(filePath, encKey));

            // overwrite encrypted file header byte
            byte[] fb = Files.readAllBytes(encFilePath);

            // generate one different byte at an arbitrary position
            byte x;
            do {
                x = (byte) this.r.nextInt();
            } while (x == fb[10]);
            fb[10] = x;
            Files.write(encFilePath, fb);

            VaultFile fd = new VaultFile(encFilePath, true);
            // the header does not match, the file is not created
            fd.decrypt(dstOutputPath, encKey);
        } finally {
            Files.delete(filePath);
            Files.delete(encFilePath);
        }
    }

    @Test(expected = AEADBadTagException.class)
    public void testFileTamperedContent() throws Exception {
        Path filePath = notExistingPath;
        Path encFilePath = notExistingEncPath;
        try {
            filePath = createRandomFile();

            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            keygen.init(256); // bits
            SecretKey encKey = keygen.generateKey();

            VaultFile fe = new VaultFile(filePath, false);
            encFilePath = dstTestPath.resolve(fe.encrypt(filePath, encKey));

            Files.write(encFilePath, new byte[]{(byte) this.r.nextInt()}, StandardOpenOption.APPEND);

            VaultFile fd = new VaultFile(encFilePath, true);
            // file is created because header bytes match,
            // but the content bytes does not match, so throw an exception. The under-the-hood file is deleted
            fd.decrypt(dstOutputPath, encKey);
        } finally {
            Files.delete(filePath);
            Files.delete(encFilePath);
        }
    }

    @Test()
    public void testEqualFilenames() throws Exception {
        Path filePath = createRandomFile();

        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(256); // bits
        SecretKey encKey = keygen.generateKey();

        VaultFile fe = new VaultFile(filePath, false);
        Path encFilename = dstTestPath.resolve(fe.encrypt(filePath, encKey));

        VaultFile fd = new VaultFile(encFilename, true);
        Path decFilename = dstOutputPath.resolve(fd.decrypt(dstOutputPath, encKey));

        Files.delete(filePath);
        Files.delete(encFilename);
        Files.delete(decFilename);

        Assert.assertEquals(filePath.getFileName().toString(), decFilename.getFileName().toString());
    }

    @Test()
    public void testIndexFilenames() throws Exception {
        Path filePath = createRandomFile();

        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(256); // bits
        SecretKey encKey = keygen.generateKey();

        VaultFile fe = new VaultFile(filePath, false);
        Path encFilename = dstTestPath.resolve(fe.encrypt(filePath, encKey));

        VaultFile fd = new VaultFile(encFilename, true);
        Path decFilename = dstTestPath.resolve(fd.decrypt(dstTestPath, encKey));

        Files.delete(filePath);
        Files.delete(encFilename);
        Files.delete(decFilename);

        // same source and destination folder, the original filename already exists,
        // so check if the code correctly prepends an index
        Assert.assertEquals("0-" + filePath.getFileName().toString(), decFilename.getFileName().toString());
    }
}