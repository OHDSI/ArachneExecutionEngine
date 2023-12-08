package com.odysseusinc.arachne.executionengine.model.descriptor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExecutionRuntimeHelperTest {

    @Test
    public void test() {
        List<File> files = new ArrayList<File>();
        
        File file = new File("src/test/resources/strategus-renv.lock");
        
        Assertions.assertTrue(file.exists());
        files.add(file);
        List<ExecutionRuntime> executionRuntimes = ExecutionRuntimeHelper.getRuntimes(files);
        Assertions.assertEquals(1, executionRuntimes.size());
    }

}
