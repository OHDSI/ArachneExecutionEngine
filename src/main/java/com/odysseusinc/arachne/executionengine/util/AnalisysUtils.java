/*
 *
 * Copyright 2018 Odysseus Data Services, inc.
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
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: March 24, 2017
 *
 */

package com.odysseusinc.arachne.executionengine.util;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.util.CommonFileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.lingala.zip4j.exception.ZipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.multipart.MultipartFile;

public class AnalisysUtils {
    private static final Logger log = LoggerFactory.getLogger(AnalisysUtils.class);
    private static final String VISITOR_ACCESS_ERROR = "Access error when access to file '{}'. Skipped";
    private static final PathMatcher EXCLUDE_JARS_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.jar");

    public static List<File> getDirectoryItems(File parentDir, Function<Path, Optional<File>> func) {

        if (!parentDir.isDirectory()) {
            throw new IllegalArgumentException();
        }
        List<File> result = new ArrayList<>();
        try {
            Files.walkFileTree(parentDir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {

                    func.apply(path).ifPresent(result::add);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {

                    log.info(VISITOR_ACCESS_ERROR, file.getFileName().toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        result.sort(Comparator.comparing(File::getName));
        return result;
    }

    public static List<File> getDirectoryItems(File parentDir) {

        return getDirectoryItems(parentDir, p -> Optional.of(p.toFile()));
    }

    public static List<File> getDirectoryItemsFiltered(File parentDir, PathMatcher matcher) {

        return getDirectoryItems(parentDir, p -> matcher.matches(p) ? Optional.of(p.toFile()) : Optional.empty());
    }

    public static List<File> getDirectoryItemsExclude(File parentDir, PathMatcher exclude) {

        return getDirectoryItems(parentDir, p -> exclude.matches(p) ? Optional.empty() : Optional.of(p.toFile()));
    }

    public static File extractFiles(List<MultipartFile> files, boolean compressed)
            throws IOException, ZipException {

        File temporaryDir = com.google.common.io.Files.createTempDir();
        if (compressed) {
            decompressToDir(temporaryDir, files);
        } else {
            writeContentToDir(temporaryDir, files);
        }
        return temporaryDir;
    }

    private static void writeContentToDir(File parent, List<MultipartFile> files) {

        files.forEach(file -> {
            try {
                com.google.common.io.Files.write(file.getBytes(), new File(parent, file.getOriginalFilename()));
            } catch (IOException ex) {
                log.error("File writing error", ex);
            }
        });
    }

    private static void decompressToDir(File parent, List<MultipartFile> files) throws IOException, ZipException {

        File temporaryDir = com.google.common.io.Files.createTempDir();
        writeContentToDir(temporaryDir, files);
        try {
            // TODO. Temp solution - this will not work correct with splitted archives
            List<File> fileList = getDirectoryItems(temporaryDir);
            for (File zippedFile: fileList) {
                CommonFileUtils.unzipFiles(zippedFile, parent);
            }
        } finally {
            org.apache.commons.io.FileUtils.deleteQuietly(temporaryDir);
        }
    }

    public static List<FileSystemResource> getFileSystemResources(AnalysisRequestDTO analysis,
                                                                  File file,
                                                                  Boolean compressedResult,
                                                                  Long chunkSize,
                                                                  File dir) throws ZipException {

        List<File> resultFiles;
        if (compressedResult) {
            final File zipArchive = new File(dir, String.valueOf(analysis.getId()) + "_result.zip");
            log.info("Adding folder \"{}\" to zip \"{}\" with chunk size = {}", file.getAbsolutePath(),
                    zipArchive.getAbsolutePath(), chunkSize);
            final File zipDir = CommonFileUtils.compressAndSplit(file, zipArchive, chunkSize, analysis.getResultExclusions());
            resultFiles = AnalisysUtils.getDirectoryItemsExclude(zipDir, EXCLUDE_JARS_MATCHER);
        } else {
            resultFiles = AnalisysUtils.getDirectoryItemsExclude(file, EXCLUDE_JARS_MATCHER);
        }
        return CommonFileUtils.getFSResources(resultFiles);
    }
}
