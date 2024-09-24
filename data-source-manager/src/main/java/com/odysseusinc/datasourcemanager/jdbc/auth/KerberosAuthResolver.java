package com.odysseusinc.datasourcemanager.jdbc.auth;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.datasourcemanager.krblogin.KerberosService;
import com.odysseusinc.datasourcemanager.krblogin.KrbConfig;
import com.odysseusinc.datasourcemanager.krblogin.RuntimeServiceMode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class KerberosAuthResolver implements DataSourceAuthResolver<KrbConfig> {

	private final KerberosService kerberosService;

	public KerberosAuthResolver(KerberosService kerberosService) {

		this.kerberosService = kerberosService;
	}

	@Override
	public boolean supports(DataSourceUnsecuredDTO dataSourceData) {

		return Objects.nonNull(dataSourceData) && dataSourceData.getUseKerberos();
	}

	@Override
	public Optional<KrbConfig> resolveAuth(DataSourceUnsecuredDTO dataSourceData, File workDir) {

		try {
			KrbConfig krbConfig = kerberosService.runKinit(dataSourceData, RuntimeServiceMode.SINGLE, workDir);
			return Optional.of(krbConfig);
		} catch (IOException e) {
			log.error("Failed to resolve Kerberos authentication for Source: {}", dataSourceData.getName(), e);
			throw new RuntimeException(e);
		}
	}
}
