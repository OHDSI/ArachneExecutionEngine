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
 * Created: August 08, 2017
 *
 */

package com.odysseusinc.arachne.execution_engine_common.util;

import static org.apache.commons.lang3.StringUtils.split;

import com.odysseusinc.arachne.execution_engine_common.exception.IORuntimeException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.AntPathMatcher;

public class CommonFileUtils {

    private static final Logger log = LoggerFactory.getLogger(CommonFileUtils.class);
    private static final AntPathMatcher matcher = new AntPathMatcher();
    private static final String DELETE_IN_ZIP_ERROR = "Error deleting file in zip archive. Skipped";
    private static final PathMatcher ZIP_FILES_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.zip");
    private static final PathMatcher RENV_FILES_MATCHER = FileSystems.getDefault()
            .getPathMatcher("glob:{,**/}renv{,/**}");

    private CommonFileUtils() {

    }

    public static List<FileSystemResource> getFSResources(List<File> files) {

        return files.stream()
                .map(FileSystemResource::new)
                .collect(Collectors.toList());
    }

    public static void unzipFiles(File zipArchive, File destination) throws FileNotFoundException {

        if (destination == null || !destination.exists()) {
            throw new FileNotFoundException("Destination directory must be exist");
        }
        String destPath = destination.getAbsolutePath();
        try {
            ZipFile zipFile = new ZipFile(zipArchive);
            zipFile.extractAll(destPath);
        } catch (ZipException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public static boolean isValidZipFile(File file) {
        ZipFile zipFile = new ZipFile(file);
        return zipFile.isValidZipFile();
    }

    public static File compressAndSplit(File folder, File zipArchive, Long maximumSize) throws ZipException {

        return compressAndSplit(folder, zipArchive, maximumSize, "");
    }

    public static File compressAndSplit(File folder, File zipArchive, Long maximumSize, String exclusions)
            throws ZipException {

        File zipDir = new File(zipArchive.getParent());
        try {
            Files.createDirectories(zipDir.toPath());
            ZipFile zipFile = new ZipFile(zipArchive);

            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(CompressionMethod.DEFLATE);
            // High compression level was set selected as possible fix for a bug:
            // http://www.lingala.net/zip4j/forum/index.php?topic=225.0
            parameters.setCompressionLevel(CompressionLevel.MAXIMUM);
            parameters.setIncludeRootFolder(false);
            parameters.setReadHiddenFiles(false);

            parameters.setDefaultFolderPath(folder.getAbsolutePath());
            ArrayList<File> filesToAdd = filterFiles(folder.toPath(), exclusions);

            if (maximumSize != null) {
                zipFile.createSplitZipFile(filesToAdd, parameters, true, maximumSize);
            } else {
                zipFile.addFiles(filesToAdd, parameters);
            }
        } catch (ZipException zipException) {
            throw new ZipException(String.format("Zip exception [folder: %s, zipArchive: %s]: %s",
                    folder.getAbsolutePath(), zipArchive.getAbsolutePath(), zipException.getMessage()), zipException);
        } catch (IOException ioException) {
            log.error(ioException.getMessage(), ioException);
            throw new IORuntimeException(ioException.getMessage());
        }
        return zipDir;
    }

    private static ArrayList<File> filterFiles(Path folderPath, String exclusions) throws IOException {

        List<String> patterns = Arrays.asList(split(exclusions, ","));

        ArrayList<File> files = Files.walk(folderPath)
                .filter(path -> !RENV_FILES_MATCHER.matches(folderPath.relativize(path))
                        && noneMatch(patterns, folderPath.relativize(path).toString()))
                .filter(path -> !Files.isDirectory(path))
                .map(Path::toFile).collect(Collectors.toCollection(ArrayList::new));

        files.stream()
                .filter(file -> ZIP_FILES_MATCHER.matches(file.toPath()))
                .forEach(file -> filterFilesInZip(patterns, file));

        return files;
    }

    private static boolean noneMatch(List<String> patterns, String fileName) {

        return patterns.stream()
                .filter(matcher::isPattern)
                .noneMatch(e -> matcher.match(e.trim(), fileName));
    }

    public static void filterFilesInZip(List<String> patterns, File file) {
        try {
            ZipFile zipFile = new ZipFile(file);
            List<FileHeader> headersList = zipFile.getFileHeaders();
            List<String> headersToDelete = new ArrayList<>();
            headersList.forEach(header -> {
                String fileName = header.getFileName();
                if (patterns.stream()
                        .filter(matcher::isPattern)
                        .anyMatch(e -> matcher.match(e.trim(), fileName))) {
                    headersToDelete.add(fileName);
                }
            });
            headersToDelete.forEach(header -> {
                try {
                    zipFile.removeFile(header);
                } catch (final ZipException e) {
                    log.warn(DELETE_IN_ZIP_ERROR, e);
                }
            });
        } catch (final ZipException e) {
            log.warn(DELETE_IN_ZIP_ERROR, e);
        }
    }
}
