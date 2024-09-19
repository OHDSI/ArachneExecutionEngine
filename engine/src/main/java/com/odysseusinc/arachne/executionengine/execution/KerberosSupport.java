package com.odysseusinc.arachne.executionengine.execution;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.datasourcemanager.krblogin.KerberosService;
import com.odysseusinc.datasourcemanager.krblogin.KrbConfig;
import com.odysseusinc.datasourcemanager.krblogin.RuntimeServiceMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
public class KerberosSupport {
    @Autowired
    private KerberosService kerberosService;

    public KrbConfig getConfig(AnalysisSyncRequestDTO analysis, File keystoreDir) {
        DataSourceUnsecuredDTO ds = analysis.getDataSource();
        if (ds.getUseKerberos()) {
            keystoreDir.mkdirs();
            try {
                return kerberosService.runKinit(ds, RuntimeServiceMode.SINGLE, keystoreDir);
            } catch (IOException e) {
                log.error("Analysis [{}] failed to resolve Kerberos auth for Datasource: {}", analysis.getId(), ds.getName(), e);
                throw new RuntimeException(e);
            }
        } else {
            return new KrbConfig();
        }
    }
}
