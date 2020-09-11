/*
 *
 * Copyright 2020 Odysseus Data Services, inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Alex Cumarav, Vitaly Koulakov, Yaroslav Molodkov
 * Created: July 27, 2020
 *
 */

package com.odysseusinc.arachne.executionengine.util;

import com.odysseusinc.arachne.executionengine.exceptions.ExecutionEngineRuntimeException;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FileResourceUtils {

    private FileResourceUtils() {
    }

    public static File extractResourceToTempFile(ResourceLoader loader, String resourceName, String prefix, String suffix) throws IOException {

        File runFile = Files.createTempFile(prefix, suffix).toFile();
        try (final InputStream in = loader.getResource(resourceName).getInputStream();
             final FileOutputStream fos = new FileOutputStream(runFile)) {
            IOUtils.copy(in, fos);
        }
        return runFile;
    }

    public static String loadStringResource(String resourcePath) {

        try {
            return IOUtils.toString(FileResourceUtils.class.getResourceAsStream(resourcePath), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ExecutionEngineRuntimeException("Cannot load resource: " + resourcePath, e);
        }
    }
}
