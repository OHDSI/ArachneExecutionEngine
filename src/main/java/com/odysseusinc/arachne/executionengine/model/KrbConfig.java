package com.odysseusinc.arachne.executionengine.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class KrbConfig {

    private static final String RUNTIME_ENV_KRB_KEYTAB = "KRB_KEYTAB";
    private static final String RUNTIME_ENV_KRB_CONF = "KRB_CONF";
    private static final String RUNTIME_ENV_KINIT_PARAMS = "KINIT_PARAMS";
    private static final String KRB_KEYTAB_PATH = "/etc/krb.keytab";

    private Path keytabPath = Paths.get("");
    private Path confPath = Paths.get("");
    private String[] kinitCommand;

    public Map<String, String> getIsolatedRuntimeEnvs() {

        Map<String, String> krbEnvProps = new HashMap<>();
        krbEnvProps.put(RUNTIME_ENV_KRB_KEYTAB, getKeytabPath().toString());
        krbEnvProps.put(RUNTIME_ENV_KRB_CONF, getConfPath().toString());

        String[] kinitParams = Arrays.copyOfRange(getKinitCommand(), 1, getKinitCommand().length);
        String kinitParamsLine = StringUtils.join(kinitParams, " ").replace(getKeytabPath().toString(), KRB_KEYTAB_PATH);

        krbEnvProps.put(RUNTIME_ENV_KINIT_PARAMS, kinitParamsLine);

        return krbEnvProps;
    }

    public Path getKeytabPath() {

        return keytabPath;
    }

    public void setKeytabPath(Path keytabPath) {

        this.keytabPath = keytabPath;
    }

    public Path getConfPath() {

        return confPath;
    }

    public void setConfPath(Path confPath) {

        this.confPath = confPath;
    }

    public String[] getKinitCommand() {

        return kinitCommand;
    }

    public void setKinitCommand(String[] kinitCommand) {

        this.kinitCommand = kinitCommand;
    }
}
