package com.odysseusinc.arachne.executionengine.api.v1;

import lombok.Getter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A partial copy from org.springframework.mock.web.MockMultipartFile.
 * Introduced to avoid spring-test dependency in production code.
 */
@Getter
public class MockMultipartFile implements MultipartFile {
    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final byte[] bytes;

    public MockMultipartFile(String name, @Nullable String originalFilename, @Nullable String contentType, InputStream contentStream) throws IOException {
        this(name, originalFilename, contentType, FileCopyUtils.copyToByteArray(contentStream));
    }

    public MockMultipartFile(String name, @Nullable String originalFilename, @Nullable String contentType, @Nullable byte[] bytes) {
        Assert.hasLength(name, "Name must not be empty");
        this.name = name;
        this.originalFilename = originalFilename != null ? originalFilename : "";
        this.contentType = contentType;
        this.bytes = bytes != null ? bytes : new byte[0];
    }


    public boolean isEmpty() {
        return this.bytes.length == 0;
    }

    public long getSize() {
        return this.bytes.length;
    }

    @NonNull
    public InputStream getInputStream() {
        return new ByteArrayInputStream(this.bytes);
    }

    public void transferTo(@NonNull File dest) throws IOException, IllegalStateException {
        FileCopyUtils.copy(this.bytes, dest);
    }
}
