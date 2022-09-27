package org.vino9.promptbiz.batchutils.fileformat;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/*
 <name>.zip
   +--  content (file name differs based on PackageType)
   |
   +--  Attachments.zip
         +- file_1
         |
         +- ...
         |
         +- files_n
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true)
public class DocumentWithAttachments {

    @ToString.Include
    private final String name;

    @ToString.Include
    private final int numOfAttachments = 0;

    @ToString.Include
    private final Direction direction;

    @ToString.Include
    private final Path path; // Path object of this file

    private Path attachmentsPath;

    // the file system for file read/write
    private final FileSystem fs;

    // stream objects

    // main file output
    private OutputStream fout;
    private ZipOutputStream zout;

    // attachment file output
    private OutputStream fattout; // attachment zip file
    private ZipOutputStream zattout; // attachment zipstream

    @ToString.Include
    private String workDir;

    // ctor
    private DocumentWithAttachments(String name, Direction direction, Path path, FileSystem fs) {
        this.name = name;
        this.direction = direction;
        this.path = path;
        this.fs = fs;
    }

    public static DocumentWithAttachments create(String name, FileSystem fs) {
        if (name == null || name.isEmpty() || fs == null) {
            return null;
        }

        var filePath = name.endsWith(".zip") ? name : name + ".zip";
        return new DocumentWithAttachments(name, Direction.OUTPUT, fs.getPath(filePath), fs);
    }

    public static DocumentWithAttachments open(String filePath, FileSystem fs)
        throws IOException {
        if (filePath == null || filePath.isEmpty() || fs == null) {
            return null;
        }

        var name = filePath.split("\\.")[0];
        var doc = new DocumentWithAttachments(name, Direction.INPUT, fs.getPath(filePath), fs);
        doc.ensureWorkDir();

        // extract input zip to workdir

        return doc;
    }

    // add a document to the main zip file
    public void addDocument(String name, Path docPath) throws IOException {
        if (this.direction != Direction.OUTPUT) {
            log.debug("attempt to add document to INPUT file {}", this);
            return;
        }

        ensureOutput();
        ensureWorkDir();

        zout.putNextEntry(new ZipEntry(name));
        Files.copy(docPath, this.zout);
    }

    // add an attachment to an interim zip file
    // call finalizeAttachment to add the interim zip file to the main zip file
    public void stageAttachment(String attachmentName, Path attachmentPath) throws IOException {
        if (this.direction != Direction.OUTPUT) {
            log.debug("attempt to stage attachment to INPUT file {}", this);
            return;
        }

        ensureOutput();
        ensureWorkDir();

        if (this.numOfAttachments == 0) {
            var randFile = UUID.randomUUID().toString();
            this.attachmentsPath = fs.getPath(this.workDir + "/" + randFile);
            this.fattout = Files.newOutputStream(this.attachmentsPath, CREATE_NEW);
            this.zattout = new ZipOutputStream(this.fattout);
        }

        this.zattout.putNextEntry(new ZipEntry(attachmentName));
        Files.copy(attachmentPath, this.zattout);
    }

    public void finalizeAttachments(String name) throws IOException {
        if (this.direction != Direction.OUTPUT) {
            log.debug("attempt to finalize attachment to INPUT file {}", this);
            return;
        }

        if (this.numOfAttachments == 0) {
            return;
        }

        this.zattout.close();
        this.fattout.close();

        Files.copy(this.attachmentsPath, this.zout);
        Files.deleteIfExists(this.attachmentsPath);
    }

    // private methods

    @SneakyThrows
    public void destroy(boolean deleteSelf) {
        if (this.direction == Direction.OUTPUT) {
            if (zout != null) {
                zout.close();
            }
            if (fout != null) {
                fout.close();
            }
            if (zattout != null) {
                zattout.close();
            }
            if (fattout != null) {
                fattout.close();
            }
        }

        if (this.workDir != null) {
            FsUtil.deleteDirectory(this.fs, this.workDir);
        }

        if (deleteSelf) {
            Files.deleteIfExists(this.path);
        }
    }

    // ensure workdir is created and clear
    private void ensureWorkDir() throws IOException {
        if (this.workDir != null && !this.workDir.isEmpty()) {
            return;
        }
        var dir = UUID.randomUUID().toString();
        FsUtil.deleteDirectory(fs, dir);
        var path = fs.getPath(dir);
        Files.createDirectory(path);

        log.debug("DocumentWithAttachments[{}] workdir={}", this.name, dir);

        this.workDir = dir;
    }

    // ensure output streams are open
    private boolean ensureOutput() throws IOException {
        if (zout == null) {
            if (fout == null) {
                this.fout = Files.newOutputStream(this.path, CREATE_NEW);
            }
            this.zout = new ZipOutputStream(this.fout);
        }
        return true;
    }

    public enum Direction {INPUT, OUTPUT}
}
