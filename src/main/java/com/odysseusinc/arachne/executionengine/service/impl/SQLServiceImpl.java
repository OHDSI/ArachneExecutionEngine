/*
 *
 * Copyright 2017 Observational Health Data Sciences and Informatics
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
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: March 24, 2017
 *
 */

package com.odysseusinc.arachne.executionengine.service.impl;

import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.executionengine.aspect.FileDescriptorCount;
import com.odysseusinc.arachne.executionengine.service.CallbackService;
import com.odysseusinc.arachne.executionengine.service.SQLService;
import com.odysseusinc.arachne.executionengine.util.AnalisysUtils;
import com.odysseusinc.arachne.executionengine.util.FailedCallback;
import com.odysseusinc.arachne.executionengine.aspect.FileDescriptorCount;
import com.odysseusinc.arachne.executionengine.util.ResultCallback;
import com.odysseusinc.arachne.executionengine.util.SQLUtils;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.ohdsi.sql.SqlSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;


@Service
public class SQLServiceImpl implements SQLService {
    private static final PathMatcher SQL_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.sql");
    private final Logger log = LoggerFactory.getLogger(SQLServiceImpl.class);
    private final TaskExecutor taskExecutor;
    private final CallbackService callbackService;

    @Value("${csv.separator}")
    private char csvSeparator;

    @Autowired
    public SQLServiceImpl(TaskExecutor taskExecutor, CallbackService callbackService) {

        this.taskExecutor = taskExecutor;
        this.callbackService = callbackService;
    }

    @Override
    @FileDescriptorCount
    public void analyze(AnalysisRequestDTO analysis, File file, ResultCallback resultCallback, FailedCallback failedCallback) {

        taskExecutor.execute(() -> {
            try {
                AnalysisResultStatusDTO status = AnalysisResultStatusDTO.EXECUTED;
                StringBuilder stdout = new StringBuilder();
                DataSourceUnsecuredDTO dataSource = analysis.getDataSource();
                Long id = analysis.getId();
                String callbackPassword = analysis.getCallbackPassword();

                try (Connection conn = SQLUtils.getConnectionWithAutoCommit(dataSource)) {

                    List<File> files = AnalisysUtils.getDirectoryItemsFiltered(file, SQL_MATCHER);
                    for (File sqlFile : files) {
                        final String sqlFileName = sqlFile.getName();
                        try {
                            SqlExecutor sqlExecutor;

                            if (analysis.getDataSource().getType().equals(DBMSType.ORACLE)) {
                                sqlExecutor = new OracleSqlExecutor();
                            } else {
                                sqlExecutor = new DefaultSqlExecutor();
                            }
                            List<Path> resultFileList = sqlExecutor.runSql(conn, sqlFile);
                            //
                            stdout.append(sqlFileName).append("\r\n\r\n").append("has been executed correctly").append("\r\n");
                            if (resultFileList.size() > 0) {
                                stdout.append("has result file: ").append(resultFileList.stream().map(rf -> rf.getFileName().toString()).collect(Collectors.joining(", ")));
                            } else {
                                stdout.append("does not have a result file");
                            }
                        } catch (IOException ex) {
                            String errorMessage = sqlFileName + "\r\n\r\nError reading file: " + ex.getMessage();
                            log.error(errorMessage);
                            if (log.isDebugEnabled()) {
                                log.debug("Stacktrace: ", ex);
                            }
                            status = AnalysisResultStatusDTO.FAILED;
                            stdout.append(errorMessage);
                        } catch (SQLException ex) {
                            String errorMessage = sqlFileName + "\r\n\r\nError executing query: " + ex.getMessage();
                            log.error(errorMessage);
                            if (log.isDebugEnabled()) {
                                log.debug("Stacktrace: ", ex);
                            }
                            status = AnalysisResultStatusDTO.FAILED;
                            stdout.append(errorMessage);
                        }
                        stdout.append("\r\n---\r\n\r\n");
                        String updateURL = analysis.getUpdateStatusCallback();
                        callbackService.updateAnalysisStatus(updateURL, id, stdout.toString(), callbackPassword);
                    }
                } catch (SQLException ex) {
                    String errorMessage = "Error getting connection to CDM: " + ex.getMessage();
                    log.error(errorMessage);
                    if (log.isDebugEnabled()) {
                        log.debug("Stacktrace: ", ex);
                    }
                    status = AnalysisResultStatusDTO.FAILED;
                    stdout.append(errorMessage).append("\r\n");
                }
                resultCallback.execute(analysis, status, stdout.toString(), file);
            } catch (Throwable t) {
                failedCallback.execute(analysis, t, file);
            }
        });
    }

    public abstract class SqlExecutor {
        public abstract List<Path> runSql(Connection conn, File sqlFile) throws SQLException, IOException;

        Path processResultSet(Statement statement, String resultFileName) throws IOException, SQLException {
            Path resultFile = null;
            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet != null) {
                    resultFile = Paths.get(resultFileName);
                    try (PrintWriter out = new PrintWriter(new BufferedWriter(
                            new FileWriter(resultFile.toFile(), true))
                    )) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        for (int column = 1; column <= columnCount; column++) {
                            String columnLabel = metaData.getColumnLabel(column);
                            out.append(columnLabel);
                            if (column < columnCount) {
                                out.append(csvSeparator);
                            }
                        }
                        out.append("\r\n");
                        while (resultSet.next()) {
                            for (int ii = 1; ii <= columnCount; ii++) {
                                Object object = resultSet.getObject(ii);
                                out.print(object);
                                if (ii < columnCount) {
                                    out.print(csvSeparator);
                                }
                            }
                            out.print("\r\n");
                        }
                    }
                }
            }
            return resultFile;
        }
    }

    public class DefaultSqlExecutor extends SqlExecutor {

        public List<Path> runSql(Connection conn, File sqlFile) throws SQLException, IOException {

            List<Path> resultFileList = new ArrayList<>();
            try (OutputStream outputStream = new ByteArrayOutputStream()) {
                Files.copy(sqlFile.toPath(), outputStream);
                try (Statement statement = conn.createStatement();) {
                    boolean hasMoreResultSets = statement.execute(outputStream.toString());
                    int resultIdx = 0;
                    while (hasMoreResultSets || statement.getUpdateCount() != -1) {
                        if (hasMoreResultSets) {
                            Path resultFile = processResultSet(statement, sqlFile.getAbsolutePath() + ".result_" + resultIdx + ".csv");
                            if (resultFile != null) {
                                resultFileList.add(resultFile);
                            }
                        }
                        hasMoreResultSets = statement.getMoreResults();
                        resultIdx++;
                    }
                }
            }
            return resultFileList;
        }
    }

    public class OracleSqlExecutor extends SqlExecutor {

        public List<Path> runSql(Connection conn, File sqlFile) throws SQLException, IOException {

            List<Path> resultFileList = new ArrayList<>();
            try (OutputStream outputStream = new ByteArrayOutputStream()) {
                Files.copy(sqlFile.toPath(), outputStream);
                try (Statement statement = conn.createStatement()) {
                    String[] sqlParts = SqlSplit.splitSql(outputStream.toString());
                    for (int i = 0; i < sqlParts.length; i++) {
                        statement.execute(sqlParts[i]);
                        Path resultFile = processResultSet(statement, sqlFile.getAbsolutePath() + ".result_" + i + ".csv");
                        if (resultFile != null) {
                            resultFileList.add(resultFile);
                        }
                    }
                }
            }
            return resultFileList;
        }
    }
}
