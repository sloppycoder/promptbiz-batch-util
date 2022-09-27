package org.vino9.promptbiz.batchutils.fileformat;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

public class FsUtil {

    // delete file or directory specified by path
    // delete all files in a directory, then the directory itself
    // delete directory operation is not recursive (due to JimFS does not support walk method)
    public static void deleteDirectory(FileSystem fs, String dirName) throws IOException {
        var path = fs.getPath(dirName);
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

    public static String hashDocument(Path path) {
        // TODO: add hash implementation
        return "=ABCDE=";
    }
}
