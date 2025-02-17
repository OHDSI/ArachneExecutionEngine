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

import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.ExecutionOutcome;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.Stage;
import com.odysseusinc.arachne.executionengine.auth.AuthEffects;
import com.odysseusinc.arachne.executionengine.execution.AbstractOverseer;
import com.odysseusinc.arachne.executionengine.execution.ExecutionService;
import com.odysseusinc.arachne.executionengine.execution.Overseer;
import com.odysseusinc.arachne.executionengine.util.AnalisysUtils;
import com.odysseusinc.arachne.executionengine.util.SQLUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.ohdsi.sql.SqlSplit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO.SQL;


@Slf4j
@Service
public class SQLService implements ExecutionService {
    private static final List<DBMSType> SINGLE_STATEMENT_TYPES = Arrays.asList(DBMSType.ORACLE, DBMSType.BIGQUERY, DBMSType.SPARK);
    private static final PathMatcher SQL_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.sql");
    @Autowired
    @Qualifier("analysisTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Value("${csv.separator}")
    private char csvSeparator;

    @Override
    public String getExtension() {
        return "sql";
    }

    @Override
    public Overseer analyze(AnalysisSyncRequestDTO analysis, File dir, BiConsumer<String, String> callback, Integer updateInterval, AuthEffects auth) {
        Instant started = Instant.now();
        Long id = analysis.getId();
        DataSourceUnsecuredDTO dataSource = analysis.getDataSource();
        StringBuffer stdout = new StringBuffer();
        CompletableFuture<ExecutionOutcome> future = CompletableFuture.supplyAsync(
                () -> execute(dir, callback, dataSource, id, stdout), taskExecutor
        );
        log.info("Execution [{}] started in SQL Service", id);
        return new SqlOverseer(id, started, stdout, future);
    }

    private ExecutionOutcome execute(File dir, BiConsumer<String, String> callback, DataSourceUnsecuredDTO dataSource, Long id, StringBuffer stdout) {
        try (Connection conn = SQLUtils.getConnectionWithAutoCommit(dataSource)) {
            String name = conn.getMetaData().getDatabaseProductName();
            boolean singleStatement = SINGLE_STATEMENT_TYPES.contains(dataSource.getType());
            log.info("Execution [{}] connected to [{}], single statement: {}", id, name, singleStatement);

            List<File> files = AnalisysUtils.getDirectoryItemsFiltered(dir, SQL_MATCHER);
            log.info("Execution [{}] has {} files", id, files.size());
            SqlExecutor sqlExecutor = singleStatement ? new SingleStatementSqlExecutor() : new DefaultSqlExecutor();
            for (File sqlFile : files) {
                final String sqlFileName = sqlFile.getName();
                log.info("Execution [{}] processing file [{}]", id, sqlFileName);
                stdout.append("Executing [").append(sqlFileName).append("]\r\n");
                Function<Integer, String> naming = index -> sqlFile.getAbsolutePath() + ".result_" + index + ".csv";
                String file = FileUtils.readFileToString(sqlFile, StandardCharsets.UTF_8);
                try {
                    List<Path> resultFileList = sqlExecutor.runSql(conn, file, naming);
                    String names = resultFileList.stream().map(rf -> rf.getFileName().toString()).collect(Collectors.joining(", ", "[", "]"));
                    stdout.append("has completed correctly, result files: ").append(names);
                } catch (IOException ex) {
                    return error(id, callback, ex, stdout, "has failed reading file: " + ex.getMessage());
                } catch (SQLException ex) {
                    return error(id, callback, ex, stdout, "has failed executing query: " + ex.getMessage());
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
        } catch (Throwable t) {
            log.error("Execution [{}] error: {}", id, t.getMessage(), t);
            return new ExecutionOutcome(Stage.EXECUTE, "Error: " + t.getClass() + ":" + t.getMessage(), null);
        }
    }

    private ExecutionOutcome error(Long id, BiConsumer<String, String> callback, Exception e, StringBuffer stdout, String pretext) {
        String error = e.getMessage();
        log.error("Execution [{}] FAILED: {}", id, error);
        if (log.isDebugEnabled()) {
            log.debug("Stacktrace: ", e);
        }
        String fullMsg = pretext + error;
        stdout.append(fullMsg);
        callback.accept(Stage.EXECUTE, stdout.toString());
        return new ExecutionOutcome(Stage.EXECUTE, fullMsg, stdout.toString());
    }

    public abstract class SqlExecutor {
        public abstract List<Path> runSql(Connection conn, String sql, Function<Integer, String> fnResultName) throws SQLException, IOException;

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

        public List<Path> runSql(Connection conn, String sql, Function<Integer, String> fnResultName) throws SQLException, IOException {
            List<Path> resultFileList = new ArrayList<>();
            try (Statement statement = conn.createStatement()) {
                boolean hasMoreResultSets = statement.execute(sql);
                int resultIdx = 0;
                while (hasMoreResultSets || statement.getUpdateCount() != -1) {
                    if (hasMoreResultSets) {
                        Path resultFile = processResultSet(statement, fnResultName.apply(resultIdx));
                        if (resultFile != null) {
                            resultFileList.add(resultFile);
                        }
                    }
                    hasMoreResultSets = statement.getMoreResults();
                    resultIdx++;
                }
            }
            return resultFileList;
        }
    }

    public class SingleStatementSqlExecutor extends SqlExecutor {

        public List<Path> runSql(Connection conn, String sql, Function<Integer, String> fnResultName) throws SQLException, IOException {
            List<Path> resultFileList = new ArrayList<>();
            try (Statement statement = conn.createStatement()) {
                String[] sqlParts = SqlSplit.splitSql(sql);
                for (int i = 0; i < sqlParts.length; i++) {
                    String sqlPart = sqlParts[i];
                    statement.execute(sqlPart);
                    Path resultFile = processResultSet(statement, fnResultName.apply(i));
                    if (resultFile != null) {
                        resultFileList.add(resultFile);
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
