package org.vino9.promptbiz.batchutils.fileformat;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/*
 <name>.zip
   +--  content (Presentment.xml/Invoice.xml/etc)
   |
   +--  attachments.zip (Attached_Presentments.zip/Attached_Invoices.zip)
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
    private int numOfAttachments = 0;

    @ToString.Include
    private final Direction direction;

    @ToString.Include
    @Getter
    private final Path path; // Path object of this file
    private Path attachmentsPath; //

    // the file system for file read/write
    private final FileSystem fs;

    // main file output
    private OutputStream fout;
    private ZipOutputStream zout;

    // attachment file output
    private OutputStream fattout; // attachment zip file
    private ZipOutputStream zattout; // attachment zipstream

    @Getter
    private Path workPath;

    // ctor
    private DocumentWithAttachments(String name, Direction direction, Path path, FileSystem fs) {
        this.name = name;
        this.direction = direction;
        this.path = path;
        this.fs = fs;
    }

    // public APIs

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

        doc.ensureWorkPath();

        // extract input zip to workdir

        return doc;
    }

    // add a document to the main zip file
    public void addDocument(String name, Path docPath) throws IOException {
        if (direction != Direction.OUTPUT) {
            log.debug("attempt to add document to INPUT file {}", this);
            return;
        }

        ensureOutput();
        ensureWorkPath();

        zout.putNextEntry(new ZipEntry(name));
        Files.copy(docPath, zout);
    }

    // add an attachment to an interim zip file
    // call finalizeAttachment to add the interim zip file to the main zip file
    public void stageAttachment(String attachmentName, Path attachmentPath) throws IOException {
        if (direction != Direction.OUTPUT) {
            log.debug("attempt to stage attachment to INPUT file {}", this);
            throw new IOException("can only add attachment to an OUTPUT file");
        }

        ensureOutput();
        ensureWorkPath();

        if (numOfAttachments == 0) {
            attachmentsPath = workPath.resolve(FsUtils.randFileName());
            fattout = Files.newOutputStream(attachmentsPath, CREATE_NEW);
            zattout = new ZipOutputStream(fattout);
        }

        zattout.putNextEntry(new ZipEntry(attachmentName));
        Files.copy(attachmentPath, zattout);
        numOfAttachments += 1;
    }

    public void finalizeAttachments(String name) throws IOException {
        if (direction != Direction.OUTPUT) {
            log.debug("attempt to finalize attachment to INPUT file {}", this);
            throw new IOException("cannot write to an INPUT file");
        }

        if (numOfAttachments == 0) {
            return;
        }

        zattout.close();
        fattout.close();

        zout.putNextEntry(new ZipEntry(name));
        Files.copy(attachmentsPath, zout);
        Files.deleteIfExists(attachmentsPath);
        attachmentsPath = null;
    }

    @SneakyThrows
    public void close() {
        if (direction == Direction.OUTPUT) {
            if (zout != null) {
                zout.close();
                zout = null;
            }
            if (fout != null) {
                fout.close();
                fout = null;
            }
        }
    }

    @SneakyThrows
    public void destroy(boolean deleteSelf) {
        if (direction == Direction.OUTPUT) {
            close();

            if (zattout != null) {
                zattout.close();
                zattout = null;
            }
            if (fattout != null) {
                fattout.close();
                fattout = null;
            }
        }

        if (attachmentsPath != null) {
            Files.deleteIfExists(attachmentsPath);
            attachmentsPath = null;
        }

        if (workPath != null) {
            FsUtils.delete(workPath);
            workPath = null;
        }

        if (deleteSelf) {
            Files.deleteIfExists(path);
        }
    }

    // private methods

    // ensure workdir is created and clear
    private void ensureWorkPath() throws IOException {
        if (workPath != null) {
            return;
        }

        var tmpPath = fs.getPath(FsUtils.randFileName());
        FsUtils.delete(tmpPath);
        Files.createDirectory(tmpPath);

        log.debug("DocumentWithAttachments[{}] ensureWorkDir {}", name, tmpPath);
        this.workPath = tmpPath;
    }

    // ensure output streams are open
    private boolean ensureOutput() throws IOException {
        if (zout == null) {
            if (fout == null) {
                fout = Files.newOutputStream(path, CREATE_NEW);
            }
            zout = new ZipOutputStream(fout);
        }
        return true;
    }

    public enum Direction {INPUT, OUTPUT}
}
