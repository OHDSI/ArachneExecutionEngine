/*
 *
 * Copyright 2018 Odysseus Data Services, inc.
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

package com.odysseusinc.arachne.executionengine.execution.sql;

import static com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO.SQL;

import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.ExecutionOutcome;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.Stage;
import com.odysseusinc.arachne.executionengine.execution.AbstractOverseer;
import com.odysseusinc.arachne.executionengine.execution.ExecutionService;
import com.odysseusinc.arachne.executionengine.execution.Overseer;
import com.odysseusinc.arachne.executionengine.service.ConnectionPoolService;
import com.odysseusinc.arachne.executionengine.util.AnalisysUtils;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Getter;
import org.ohdsi.sql.SqlSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;


@Service
public class SQLService implements ExecutionService {
    private static final PathMatcher SQL_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.sql");
    private final Logger log = LoggerFactory.getLogger(SQLService.class);
    @Autowired
    @Qualifier("analysisTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;
    @Autowired
    private ConnectionPoolService poolService;

    @Value("${csv.separator}")
    private char csvSeparator;

    @Override
    public String getExtension() {
        return "sql";
    }

    @Override
    public Overseer analyze(AnalysisSyncRequestDTO analysis, File dir, BiConsumer<String, String> callback, Integer updateInterval) {
        Instant started = Instant.now();
        StringBuffer stdout = new StringBuffer();
        Supplier<ExecutionOutcome> task = () -> {
            try {
                DataSourceUnsecuredDTO dataSource = analysis.getDataSource();

                try (Connection conn = poolService.getDataSource(dataSource).getConnection()) {

                    List<File> files = AnalisysUtils.getDirectoryItemsFiltered(dir, SQL_MATCHER);
                    for (File sqlFile : files) {
                        final String sqlFileName = sqlFile.getName();
                        try {
                            SqlExecutor sqlExecutor;

                            if (analysis.getDataSource().getType().equals(DBMSType.ORACLE) ||
                                    analysis.getDataSource().getType().equals(DBMSType.BIGQUERY)) {
                                sqlExecutor = new SingleStatementSqlExecutor();
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
                            stdout.append(errorMessage);
                            callback.accept(Stage.EXECUTE, stdout.toString());
                            return new ExecutionOutcome(Stage.EXECUTE, errorMessage, stdout.toString());
                        } catch (SQLException ex) {
                            String errorMessage = sqlFileName + "\r\n\r\nError executing query: " + ex.getMessage();
                            log.error(errorMessage);
                            if (log.isDebugEnabled()) {
                                log.debug("Stacktrace: ", ex);
                            }
                            stdout.append(errorMessage);
                            callback.accept(Stage.EXECUTE, stdout.toString());
                            return new ExecutionOutcome(Stage.EXECUTE, errorMessage, stdout.toString());
                        }
                    }
                    return new ExecutionOutcome(Stage.COMPLETED, null, stdout.toString());
                } catch (SQLException ex) {
                    String errorMessage = "Error getting connection to CDM: " + ex.getMessage();
                    log.error(errorMessage);
                    if (log.isDebugEnabled()) {
                        log.debug("Stacktrace: ", ex);
                    }
                    stdout.append(errorMessage).append("\r\n");
                    return new ExecutionOutcome(Stage.EXECUTE, "SQLException: " + ex.getMessage(), stdout.toString());
                }
            } catch (Throwable t) {
                return new ExecutionOutcome(Stage.EXECUTE, "Error: " + t.getMessage(), null);
            }
        };

        CompletableFuture<ExecutionOutcome> future = CompletableFuture.supplyAsync(task, taskExecutor);
        log.info("Execution [{}] started in SQL Service", analysis.getId());
        return new SqlOverseer(analysis.getId(), started, stdout, future);
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
                try (Statement statement = conn.createStatement()) {
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

    public class SingleStatementSqlExecutor extends SqlExecutor {

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

    @Getter
    public static class SqlOverseer extends AbstractOverseer {
        private final StringBuffer stdout;

        public SqlOverseer(long id, Instant started, StringBuffer stdout, CompletableFuture<ExecutionOutcome> result) {
            super(id, (stage, out) -> {}, started, null, 0, new StringBuffer(), result);
            this.stdout = stdout;
        }

        @Override
        public String getStdout() {
            return stdout.toString();
        }

        @Override
        public CompletableFuture<ExecutionOutcome> abort() {
            return result.isDone() ? result : CompletableFuture.completedFuture(
                    new ExecutionOutcome(Stage.ABORT, "Abort is not supported for SQL analysis", null)
            );
        }

        @Override
        public AnalysisRequestTypeDTO getType() {
            return SQL;
        }

    }
}
