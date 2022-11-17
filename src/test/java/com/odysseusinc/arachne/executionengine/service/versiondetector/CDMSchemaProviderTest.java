package com.odysseusinc.arachne.executionengine.service.versiondetector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static com.odysseusinc.arachne.executionengine.service.versiondetector.BaseVersionDetectionService.COMMONS_SCHEMA;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class CDMSchemaProviderTest {

    @InjectMocks
    private CDMSchemaProvider cdmSchemaProvider;

    @Test
    public void shouldLoadCommonCDMPart() {

        final Map<String, List<String>> common = cdmSchemaProvider.loadMandatorySchemaJson(COMMONS_SCHEMA);

        assertThat(common).containsOnlyKeys("attribute_definition",
                "care_site",
                "cdm_source",
                "cohort",
                "cohort_attribute",
                "cohort_definition",
                "concept",
                "concept_ancestor",
                "concept_class",
                "concept_relationship",
                "concept_synonym",
                "condition_era",
                "condition_occurrence",
                "death",
                "device_exposure",
                "domain",
                "dose_era",
                "drug_era",
                "drug_exposure",
                "drug_strength",
                "fact_relationship",
                "location",
                "measurement",
                "note",
                "observation",
                "observation_period",
                "payer_plan_period",
                "person",
                "procedure_occurrence",
                "provider",
                "relationship",
                "source_to_concept_map",
                "specimen",
                "visit_occurrence",
                "vocabulary");
    }

    @Test
    public void shouldLoadEmptyMapIfOptionalPartIsNotDefined() {

        final Map<String, List<String>> commonOptional = cdmSchemaProvider.loadOptionalSchemaJson(COMMONS_SCHEMA);

        assertThat(commonOptional).isEmpty();
    }


    @Test
    public void shouldLoadCDM531OptionalColumns() {

        final Map<String, List<String>> cdm531Optionals = cdmSchemaProvider.loadOptionalSchemaJson("/cdm/v5/diff_V5_3_1.json");

        assertThat(cdm531Optionals).containsOnlyKeys("cost", "measurement");
    }

    @Test
    public void shouldLoadAllAndParseAllCDMResourcesFiles() {

        for (CDMResources schemaFile : CDMResources.values()) {
            final Map<String, List<String>> map = cdmSchemaProvider.loadMandatorySchemaJson(schemaFile.getPath());
            assertThat(map).isNotEmpty();
        }
    }
}