/*
 *
 * Copyright 2018 Observational Health Data Sciences and Informatics
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
 * Created: May 17, 2017
 *
 */

package com.odysseusinc.arachne.executionengine.util;

public interface CdmSourceFields {

    String CDM_SOURCE_NAME = "cdm_source_name";
    String CDM_SOURCE_ABBREVIATION = "cdm_source_abbreviation";
    String CDM_HOLDER = "cdm_holder";
    String SOURCE_DESCRIPTION = "source_description";
    String SOURCE_DOCUMENTATION_REFERENCE = "source_documentation_reference";
    String CDM_ETL_REFERENCE = "cdm_etl_reference";
    String SOURCE_RELEASE_DATE = "source_release_date";
    String CDM_RELEASE_DATE = "cdm_release_date";
    String CDM_VERSION = "cdm_version";
    String VOCABULARY_VERSION = "vocabulary_version";
}
