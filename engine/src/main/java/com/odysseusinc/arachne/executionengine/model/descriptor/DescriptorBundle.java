package com.odysseusinc.arachne.executionengine.model.descriptor;

public class DescriptorBundle {
    private String path;
    private Descriptor descriptor;

    public DescriptorBundle(String path, Descriptor descriptor) {
        this.path = path;
        this.descriptor = descriptor;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Descriptor getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(Descriptor descriptor) {
        this.descriptor = descriptor;
    }
}
