package com.odysseusinc.arachne.executionengine.service.versiondetector;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.executionengine.util.SQLUtils;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class MetadataProvider {

    public Map<String, List<String>> extractMetadata(DataSourceUnsecuredDTO dataSource) throws SQLException {

        Map<String, List<String>> metadataMap = new TreeMap<>();
        final String schema = dataSource.getCdmSchema();
        try (Connection c = SQLUtils.getConnection(dataSource)) {
            DatabaseMetaData metaData = c.getMetaData();

            try (ResultSet columns = metaData.getColumns(null, schema, "%", "%")) {
                while (columns.next()) {
                    String tableName = columns.getString("TABLE_NAME").toLowerCase();
                    String columnName = columns.getString("COLUMN_NAME").toLowerCase();
                    List<String> tableColumns = metadataMap.getOrDefault(tableName, new ArrayList<>());
                    tableColumns.add(columnName);
                    metadataMap.putIfAbsent(tableName, tableColumns);
                }
            }
        }
        return metadataMap;
    }
}
