/*
 *
 * Copyright 2019 Odysseus Data Services, inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Vitaly Koulakov, Anastasiia Klochkova, Yaroslav Molodkov, Alexander Cumarav
 * Created: October 21, 2019
 *
 */

package com.odysseusinc.arachne.executionengine.service.impl;

import com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO;
import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.executionengine.service.VersionDetectionService;
import com.odysseusinc.arachne.executionengine.util.SQLUtils;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.ohdsi.sql.SqlRender;
import org.ohdsi.sql.SqlTranslate;
import org.springframework.stereotype.Service;

@Service
public class ImpalaVersionDetectionService extends BaseVersionDetectionService implements VersionDetectionService {

    private static String[] CTE_PARAMS = new String[]{ "table", "cdmDatabaseSchema", "fields" };

    @Override
    public CommonCDMVersionDTO detectCDMVersion(DataSourceUnsecuredDTO dataSource) throws SQLException, IOException {

        return doDetectVersion(schema -> {
            try {
                return checkSchema(dataSource, schema);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private boolean checkSchema(DataSourceUnsecuredDTO dataSource, Map<String, List<String>> schema) throws SQLException {

        String cteSql = schema.keySet().stream()
                .map(tbl -> {
                    String fields = schema.get(tbl).stream().map(f -> String.format("`%s`", f)).collect(Collectors.joining(","));
                    String[] values = new String[]{ tbl, dataSource.getCdmSchema(), fields };
                    String sql = "cte_@table as (select cast('@table' as varchar(50)) as tablename from (select top 1 @fields from @cdmDatabaseSchema.`@table`) as `@table`)";
                    return SqlRender.renderSql(sql, CTE_PARAMS, values);
                }).collect(Collectors.joining(","));
        String cteAll = schema.keySet().stream()
                .map(tbl -> {
                    String[] values = new String[] { tbl };
                    return SqlRender.renderSql("select tablename from cte_@table", new String[]{ "table" }, values);
                }).collect(Collectors.joining(" union all "));
        String[] values = new String[]{ cteSql, cteAll };
        String sql = SqlRender.renderSql("with @cteTables, cte_all as (@cteAll) select tablename from cte_all;",
                new String[]{ "cteTables", "cteAll" }, values);
        sql = SqlTranslate.translateSql(sql, DBMSType.IMPALA.getOhdsiDB());
        try(Connection c  = SQLUtils.getConnection(dataSource)) {
            try {
                PreparedStatement query = c.prepareStatement(sql);
                query.executeQuery();
            }catch (SQLException e) {
                LOGGER.debug("DBMS: {} detection error: {}", dataSource.getType(), e.getMessage());
                return false;
            }
        }
        return true;
    }

}
