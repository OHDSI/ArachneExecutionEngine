package com.odysseusinc.arachne.executionengine.service.versiondetector;

import com.google.common.collect.ImmutableMap;
import com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO.V5_0_1;
import static com.odysseusinc.arachne.executionengine.service.versiondetector.CDMResources.V4_SCHEMA;
import static com.odysseusinc.arachne.executionengine.service.versiondetector.CDMResources.V5_0_1_SCHEMA_DIFF;
import static com.odysseusinc.arachne.executionengine.service.versiondetector.CDMResources.V5_0_SCHEMA_DIFF;
import static com.odysseusinc.arachne.executionengine.service.versiondetector.CDMResources.V5_1_SCHEMA_DIFF;
import static com.odysseusinc.arachne.executionengine.service.versiondetector.CDMResources.V5_2_SCHEMA_DIFF;
import static com.odysseusinc.arachne.executionengine.service.versiondetector.CDMResources.V5_3_1_SCHEMA_DIFF;
import static com.odysseusinc.arachne.executionengine.service.versiondetector.CDMResources.V5_3_SCHEMA_DIFF;
import static com.odysseusinc.arachne.executionengine.service.versiondetector.CDMResources.V5_COMMONS_SCHEMA;
import static com.odysseusinc.arachne.executionengine.service.versiondetector.CDMResources.V6_SCHEMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class DefaultVersionDetectionServiceTest {

    private final ImmutableMap<String, List<String>> common_schema = ImmutableMap.of("COMMON_TABLE", Arrays.asList("one", "two", "three"));
    private final ImmutableMap<String, List<String>> v5_0_diff_schema = ImmutableMap.of("VERSION_ZERO_TABLE", Arrays.asList("one", "two", "three"));
    private final ImmutableMap<String, List<String>> v5_0_1_diff_schema = ImmutableMap.of("VERSION_ONE_TABLE", Arrays.asList("one", "two", "three"));
    private final ImmutableMap<String, List<String>> stub_diff_schema = ImmutableMap.of("STUB_TABLE", Arrays.asList("aaa", "bbb", "ccc"));

    private final ImmutableMap<String, List<String>> test_one_schema = ImmutableMap.of("COMMON_TABLE", Arrays.asList("one", "two", "three"), "EXTRA_TABLE", Arrays.asList("one", "two"));
    private final ImmutableMap<String, List<String>> test_wrong_schema = ImmutableMap.of("WRONG_TABLE", Arrays.asList("one", "two", "three"), "EXTRA_TABLE", Arrays.asList("one", "two"));

    @Mock
    private DataSourceUnsecuredDTO dataSource;
    @Mock
    private CDMSchemaProvider cdmSchemaProvider;
    @Mock
    private MetadataProvider metadataProvider;
    @InjectMocks
    private DefaultVersionDetectionService defaultVersionDetectionService;

    @Before
    public void setUp() {
        when(cdmSchemaProvider.loadMandatorySchemaJson(V5_COMMONS_SCHEMA.getPath())).thenReturn(common_schema);
        when(cdmSchemaProvider.loadMandatorySchemaJson(V5_0_SCHEMA_DIFF.getPath())).thenReturn(v5_0_diff_schema);
        when(cdmSchemaProvider.loadMandatorySchemaJson(V5_0_1_SCHEMA_DIFF.getPath())).thenReturn(v5_0_1_diff_schema);
        when(cdmSchemaProvider.loadMandatorySchemaJson(V4_SCHEMA.getPath())).thenReturn(stub_diff_schema);
        when(cdmSchemaProvider.loadMandatorySchemaJson(V5_1_SCHEMA_DIFF.getPath())).thenReturn(stub_diff_schema);
        when(cdmSchemaProvider.loadMandatorySchemaJson(V5_2_SCHEMA_DIFF.getPath())).thenReturn(stub_diff_schema);
        when(cdmSchemaProvider.loadMandatorySchemaJson(V5_3_SCHEMA_DIFF.getPath())).thenReturn(stub_diff_schema);
        when(cdmSchemaProvider.loadMandatorySchemaJson(V5_3_1_SCHEMA_DIFF.getPath())).thenReturn(stub_diff_schema);
        when(cdmSchemaProvider.loadMandatorySchemaJson(V6_SCHEMA.getPath())).thenReturn(stub_diff_schema);
    }

    @Test
    public void shouldFindV5CommonPartAndReportOnlyV5DiffErrors() throws SQLException {

        when(metadataProvider.extractMetadata(dataSource)).thenReturn(test_one_schema);

        final Pair<CommonCDMVersionDTO, String> result = defaultVersionDetectionService.detectCDMVersion(dataSource);

        assertThat(result.getKey()).isNull();
        assertThat(result.getValue()).contains("[V5_3_1]", "[V5_3]", "[V5_2]", "[V5_1]", "[V5_0_1]", "[V5_0]");
        assertThat(result.getValue()).doesNotContain("[V6]", "[V4]");
    }

    @Test
    public void shouldNotFindV5CommonPartAndReportOnlyV4V5V6DiffErrors() throws SQLException {

        when(metadataProvider.extractMetadata(dataSource)).thenReturn(test_wrong_schema);

        final Pair<CommonCDMVersionDTO, String> result = defaultVersionDetectionService.detectCDMVersion(dataSource);

        assertThat(result.getKey()).isNull();
        assertThat(result.getValue()).contains("[V4_0]", "[V5_COMMONS]", "[V6_0]");
        assertThat(result.getValue()).doesNotContain("[V5_3_1]", "[V5_3]", "[V5_2]", "[V5_1]", "[V5_0_1]", "[V5_0]");
    }

    @Test
    public void shouldReportCDMDetectionDiffsSortedByVersion() throws SQLException {

        when(metadataProvider.extractMetadata(dataSource)).thenReturn(test_wrong_schema);

        final Pair<CommonCDMVersionDTO, String> result = defaultVersionDetectionService.detectCDMVersion(dataSource);

        final String[] ordereredVersions = result.getValue().replaceAll("]\\s.*", "").split(System.lineSeparator());

        assertThat(ordereredVersions)
                .containsExactly("[V4_0", "[V5_COMMONS", "[V6_0");
    }

    @Test
    public void shouldDetectV51WithNoOptionalWarnings() throws SQLException {
        final HashMap test_v5_0_1 = new HashMap(common_schema);
        test_v5_0_1.putAll(v5_0_1_diff_schema);
        when(metadataProvider.extractMetadata(dataSource)).thenReturn(test_v5_0_1);

        final Pair<CommonCDMVersionDTO, String> result = defaultVersionDetectionService.detectCDMVersion(dataSource);

        assertThat(result.getKey()).isEqualTo(V5_0_1);
        assertThat(result.getValue()).isNull();
    }

    @Test
    public void shouldDetectV51WithOptionalWarning() throws SQLException {
        when(cdmSchemaProvider.loadOptionalSchemaJson(V5_0_1_SCHEMA_DIFF.getPath())).thenReturn(ImmutableMap.of("VERSION_ONE_TABLE", Arrays.asList("optional_column")));
        final HashMap test_v5_0_1 = new HashMap(common_schema);
        test_v5_0_1.putAll(v5_0_1_diff_schema);
        when(metadataProvider.extractMetadata(dataSource)).thenReturn(test_v5_0_1);

        final Pair<CommonCDMVersionDTO, String> result = defaultVersionDetectionService.detectCDMVersion(dataSource);

        assertThat(result.getKey()).isEqualTo(V5_0_1);
        assertThat(result.getValue()).isEqualToIgnoringWhitespace("[V5_0_1] Database table VERSION_ONE_TABLE  missed optional fields: optional_column");
    }
}