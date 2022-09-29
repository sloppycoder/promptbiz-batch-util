package org.vino9.promptbiz.batchutils.fileformat;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class FsUtils {

    public static String randFileName() {
        return UUID.randomUUID().toString().replace("-", "").substring(1, 16);
    }

    // delete file or directory specified by path
    // delete all files in a directory, then the directory itself
    // delete directory operation is not recursive (due to JimFS does not support walk method)
    public static void delete(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.list(path).forEach(f -> {
                try {
                    Files.deleteIfExists(f);
                } catch (IOException e) {
                }
            });
        } else if (Files.isRegularFile(path)) {
            Files.deleteIfExists(path);
        }
    }

    // copy a Path (e.g. file in JimFS) to a real file
    public static void pathToFile(Path inputPath, String outputFilename)
        throws IOException {
        var is = new FileOutputStream(outputFilename);
        Files.copy(inputPath, is);
        is.close();
    }

    public static String getDocumentHash(Path path) {
        // TODO: add hash implementation
        return path != null ? "=ABCDE=" : "=NULL=";
    }
}
