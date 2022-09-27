package org.vino9.promptbiz.batchutils;


import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.vino9.promptbiz.batchutils.config.InMemoryFileSystemConfig;
import org.vino9.promptbiz.batchutils.fileformat.DocumentWithAttachments;

public class DocumentWithAttachmentsTests {

    @TempDir
    static Path workDir;

    private FileSystem imfs;


    @Test
    void createNewDoc() throws IOException {
        var doc = DocumentWithAttachments.create("mystuff.zip", this.imfs);
        Assertions.assertNotNull(doc);
        var file1 = createRandomFile(1024);
        doc.addDocument("Presentment.xml", file1);
    }

    @Test
    void openDocument() throws IOException {
        var doc = DocumentWithAttachments.open("stuff.zip", this.imfs);
        Assertions.assertNotNull(doc);
    }

    @BeforeEach
    void setupJimFS() {
        var config = new InMemoryFileSystemConfig();
        config.setMaxFSMemory(128);
        this.imfs = config.inMemoryFileSystem();
    }

    private Path createRandomFile(int kbytes) throws IOException {
        var path = workDir.resolve(UUID.randomUUID().toString());

        var bytes = new byte[1024 * kbytes];
        (new Random()).nextBytes(bytes);

        var fout = Files.newOutputStream(path);
        fout.write(bytes);
        fout.close();

        return path;
    }
}
