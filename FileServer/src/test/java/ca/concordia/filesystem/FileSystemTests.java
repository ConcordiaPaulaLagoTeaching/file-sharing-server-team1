package ca.concordia.filesystem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class FileSystemTests {

    private FileSystemManager fsManager;
    private final String testFilePath = "test-filesystem.dat";

    @BeforeEach
    public void setUp() {
        // Clean up any existing test file
        File file = new File(testFilePath);
        if (file.exists()) {
            file.delete();
        }
        // Create a new file system manager
        fsManager = new FileSystemManager(testFilePath, 10 * 128);
    }

    @AfterEach
    public void tearDown() {
        // Clean up test file
        File file = new File(testFilePath);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void testCreateFile() throws Exception {
        fsManager.createFile("test.txt");
        String[] files = fsManager.listFiles();
        assertEquals(1, files.length);
        assertEquals("test.txt", files[0]);
    }

    @Test
    public void testTooLongFilename() {
        assertThrows(IllegalArgumentException.class, () -> {
            fsManager.createFile("verylongfilename.txt");
        });
    }

    @Test
    public void testWriteAndReadFile() throws Exception {
        String fileName = "test.txt";
        String content = "Hello World!";

        fsManager.createFile(fileName);
        fsManager.writeFile(fileName, content);

        byte[] readContent = fsManager.readFile(fileName);
        assertEquals(content, new String(readContent));
    }

    @Test
    public void testWriteAndReadLongFile() throws Exception {
        String fileName = "long.txt";
        // Create content larger than one block (128 bytes)
        String content = "A".repeat(300);

        fsManager.createFile(fileName);
        fsManager.writeFile(fileName, content);

        byte[] readContent = fsManager.readFile(fileName);
        assertEquals(content, new String(readContent));
    }

    @Test
    public void testDeleteFile() throws Exception {
        String fileName = "delete.txt";

        fsManager.createFile(fileName);
        fsManager.writeFile(fileName, "Test content");

        String[] filesBeforeDelete = fsManager.listFiles();
        assertTrue(filesBeforeDelete.length > 0);

        fsManager.deleteFile(fileName);

        String[] filesAfterDelete = fsManager.listFiles();
        assertEquals(0, filesAfterDelete.length);
    }
}
