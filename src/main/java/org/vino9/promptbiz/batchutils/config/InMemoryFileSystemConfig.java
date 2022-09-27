package org.vino9.promptbiz.batchutils.config;

import com.google.common.jimfs.Jimfs;
import java.nio.file.FileSystem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class InMemoryFileSystemConfig {

    @Value("${app.batch-utils.max-fs-memory-megabytes:512}")
    int maxFSMemory;

    public void setMaxFSMemory(int maxFSMemory) {
        this.maxFSMemory = maxFSMemory;
    }

    @Bean("inMemoryFileSystem")
    public FileSystem inMemoryFileSystem() {
        if (maxFSMemory <= 0) {
            maxFSMemory = 512;
        }

        log.info("In memory FS initialized to use max {}M of memory", maxFSMemory);
        return Jimfs.newFileSystem(com.google.common.jimfs.Configuration
            .unix().toBuilder()
            .setMaxSize(1024 * 1024L * maxFSMemory)
            .build());
    }
}
