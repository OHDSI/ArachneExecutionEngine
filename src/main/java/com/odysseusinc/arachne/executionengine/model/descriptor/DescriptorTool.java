package com.odysseusinc.arachne.executionengine.model.descriptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odysseusinc.arachne.execution_engine_common.descriptor.RuntimeType;
import com.odysseusinc.arachne.executionengine.config.runtimeservice.RIsolatedRuntimeProperties;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RDependency;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RDependencySourceType;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RExecutionRuntime;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.REnvLock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

// Used for creating descriptors from REnv lock files
// Only for developers
public class DescriptorTool {
    private static final String ENV_LOCK_FILES_FOLDER = ""; // "c:\\projects\\r_base\\modules\\"
    private static final String BUNDLE_NAME = ""; // "r_base_focal_amd64.tar.gz"
    private static final String DESCRIPTOR_LABEL = ""; // "Runtime for Strategus 0.0.1"
    private static final String DESCRIPTOR_ID = ""; // "descriptor_strategus_experimental_0.0.1"
    private static final String DESCRIPTOR_FILENAME = ""; // "c:\\projects\\r_base\\descriptor_strategus_experimental_0.0.1.json"

    public static void main(String[] args) throws IOException {
        RIsolatedRuntimeProperties properties = new RIsolatedRuntimeProperties();
        properties.setArchiveFolder(ENV_LOCK_FILES_FOLDER);
        File dir = new File(ENV_LOCK_FILES_FOLDER);
        RExecutionRuntime executionRuntime = null;
        for (File file: dir.listFiles()) {
            REnvLock lock = getREnvLock(file);
            if (executionRuntime == null) {
                executionRuntime = RExecutionRuntime.fromREnvLock(lock);
            } else {
                RExecutionRuntime tempExecutionRuntime = RExecutionRuntime.fromREnvLock(lock);
                executionRuntime.setDependencies(concat(executionRuntime.getDependencies(), tempExecutionRuntime.getDependencies()));
            }
        }
        Descriptor descriptor = new Descriptor();
        descriptor.setExecutionRuntimes(new ExecutionRuntime[]{executionRuntime});
        descriptor.setBundleName(BUNDLE_NAME);
        descriptor.setLabel(DESCRIPTOR_LABEL);
        descriptor.setId(DESCRIPTOR_ID);
        saveDescriptor(DESCRIPTOR_FILENAME, descriptor);
        String desc = prepareScript(descriptor);
        System.out.println(desc);
    }

    private static void saveDescriptor(String filename, Descriptor descriptor) throws IOException {
        File f = new File(filename);
        OutputStream is = new FileOutputStream(f);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(is, descriptor);
    }

    private static String prepareScript(Descriptor descriptor) {
        StringBuffer sb = new StringBuffer("install.packages(\"devtools\")");
        sb.append(System.lineSeparator());
        sb.append("library(\"devtools\")");
        sb.append(System.lineSeparator());
        sb.append("install.packages(\"drat\")");
        sb.append(System.lineSeparator());
        sb.append("drat::addRepo(c(\"OHDSI\", \"cloudyr\"))");
        sb.append(System.lineSeparator());
        String cranString = "install_version(\"%s\", version = \"%s\", type = \"source\")";
        String githubString = "install_github(\"%s/%s\", ref = \"%s\")";
        for (ExecutionRuntime executionRuntime: descriptor.getExecutionRuntimes()) {
            if (executionRuntime.getType().equals(RuntimeType.R)) {
                RExecutionRuntime rExecutionRuntime = (RExecutionRuntime) executionRuntime;
                for (RDependency rDependency: rExecutionRuntime.getDependencies()) {
                    if (rDependency.getDependencySourceType().equals(RDependencySourceType.CRAN)) {
                        sb.append(String.format(cranString, rDependency.getName(), rDependency.getVersion()));
                    } else {
                        sb.append(String.format(githubString, rDependency.getOwner(), rDependency.getName(), rDependency.getVersion()));
                    }
                    sb.append(System.lineSeparator());
                }
            }
        }
        return sb.toString();
    }

    public static REnvLock getREnvLock(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(is, REnvLock.class);
    }
    
    static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
