package com.odysseusinc.arachne.executionengine.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ResourceLoader;

public class FileResourceUtils {

    public static File extractResourceToTempFile(ResourceLoader loader, String resourceName, String prefix, String suffix) throws IOException {

        File runFile = Files.createTempFile(prefix, suffix).toFile();
        try (final InputStream in = loader.getResource(resourceName).getInputStream();
             final FileOutputStream fos = new FileOutputStream(runFile)) {
            IOUtils.copy(in, fos);
        }
        return runFile;
    }
}
