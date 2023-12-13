package com.odysseusinc.arachne.executionengine.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

public class ZipInputSubStream extends FilterInputStream {
    private final ZipInputStream zipInputStream;

    public ZipInputSubStream(ZipInputStream zipInputStream) {
        super(zipInputStream);
        this.zipInputStream = zipInputStream;
    }

    @Override
    public void close() throws IOException {
        zipInputStream.closeEntry();
    }
}
