package org.vino9.promptbiz.batchutils;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

public class TestUtils {

    // generate of file of kbytes and fill it with random content
    public static Path createRandomFile(Path parentPath, int kbytes) throws IOException {
        var bytes = new byte[1024 * kbytes];
        (new Random()).nextBytes(bytes);

        var path = parentPath.resolve(UUID.randomUUID().toString());
        var fout = Files.newOutputStream(path);
        fout.write(bytes);
        fout.close();

        return path;
    }

    public static List<String> zipContent(Path zipfilePath) throws IOException {
        var content = new ArrayList<String>();
        try (var stream = Files.newInputStream(zipfilePath)) {
            var zstream = new ZipInputStream(stream);
            var entry = zstream.getNextEntry();
            while (entry != null) {
                content.add(entry.getName());
                entry = zstream.getNextEntry();
            }
            zstream.close();

            return content;
        }
    }

    public static boolean isFileSystemClean(FileSystem fs) {
        // JimFs contains a "/work" directory upon creation, exclude that
        try {
            var list = Files.list(fs.getPath("/"))
                .filter(p -> !p.toString().equalsIgnoreCase("/work"))
                .collect(Collectors.toList());
            return list.size() == 0;
        } catch (IOException e) {
            return false;
        }
    }
}
