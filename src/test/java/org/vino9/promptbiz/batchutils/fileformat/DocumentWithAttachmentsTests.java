package org.vino9.promptbiz.batchutils.fileformat;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.vino9.promptbiz.batchutils.TestUtils;
import org.vino9.promptbiz.batchutils.config.InMemoryFileSystemConfig;

public class DocumentWithAttachmentsTests {

    public static final String TEST_FILE = "main.zip";

    @TempDir
    static Path tmpDir;

    private FileSystem imfs;
    private Path randfile;

    @Test
    @Order(1)
    void createNewDoc() throws IOException {
        var attached = "Attached.zip";

        var doc = DocumentWithAttachments.create(TEST_FILE, imfs);
        Assertions.assertNotNull(doc);
        Assertions.assertNull(doc.getWorkPath(), "work path should not exist initially");

        doc.addDocument("main.bin", randfile);
        Assertions.assertTrue(Files.isDirectory(doc.getWorkPath()),
            "work path does not existing after adding document");

        doc.stageAttachment("att1.bin", randfile);
        doc.stageAttachment("att2.bin", randfile);
        doc.finalizeAttachments(attached);
        doc.close();

        // copy the created file to local tmpdir so that it can be used for subsequent tests
        // not a good practice, i know, but it's easy
        FsUtils.pathToFile(doc.getPath(), tmpDir + "/test.zip");

        var content = TestUtils.zipContent(doc.getPath());
        Assertions.assertTrue(content.stream()
                .anyMatch(name -> name.equalsIgnoreCase("main.bin")),
            "main.bin does not exist");
        Assertions.assertTrue(content.stream()
                .anyMatch(name -> name.equalsIgnoreCase(attached)),
            attached + " does not exist");

        doc.destroy(true);
        Assertions.assertNull(doc.getWorkPath(), "work path should not exist after destroy");
        Assertions.assertFalse(Files.isRegularFile(imfs.getPath(TEST_FILE)),
            "original file should have been deleted after destroy(true)");

        Assertions.assertTrue(TestUtils.isFileSystemClean(imfs),
            "file system not clean destroy(true)");
    }

    @Test
    void openDocument() throws IOException {
        var doc = DocumentWithAttachments.open("stuff.zip", imfs);
        Assertions.assertNotNull(doc);
        Assertions.assertTrue(Files.isDirectory(doc.getWorkPath()));
    }

    @BeforeEach
    void setupJimFS() throws IOException {
        var config = new InMemoryFileSystemConfig();
        config.setMaxFSMemory(128);
        imfs = config.inMemoryFileSystem();
        randfile = TestUtils.createRandomFile(tmpDir, 1024);
    }
}
