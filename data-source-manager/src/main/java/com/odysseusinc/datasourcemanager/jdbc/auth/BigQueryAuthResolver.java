package com.odysseusinc.datasourcemanager.jdbc.auth;

import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.execution_engine_common.util.BigQueryUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class BigQueryAuthResolver implements DataSourceAuthResolver<File> {

	@Override
	public boolean supports(DataSourceUnsecuredDTO dataSourceData) {

		return DBMSType.BIGQUERY.equals(dataSourceData.getType())
						&& Objects.nonNull(dataSourceData.getKeyfile());
	}

	@Override
	public Optional<File> resolveAuth(DataSourceUnsecuredDTO dataSourceData, File workDir) {

		try {
			File keyFile = Files.createTempFile(workDir.toPath(), "", ".json").toFile();
			try (OutputStream out = new FileOutputStream(keyFile)) {
				IOUtils.write(dataSourceData.getKeyfile(), out);
			}
			String connStr = BigQueryUtils.replaceBigQueryKeyPath(dataSourceData.getConnectionString(), keyFile.getAbsolutePath());
			dataSourceData.setConnectionString(connStr);
			return Optional.of(keyFile);
		} catch (IOException e) {
			log.error("Failed to resolve BigQuery authentication for Source: [{}]", dataSourceData.getName(), e);
			throw new RuntimeException(e);
		}
	}
}
