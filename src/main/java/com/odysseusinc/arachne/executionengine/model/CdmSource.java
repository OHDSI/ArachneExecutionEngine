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
 * Created: May 17, 2017
 *
 */

package com.odysseusinc.arachne.executionengine.model;

import java.util.Date;

public class CdmSource {
    private String name;
    private String abbreviation;
    private String holder;
    private String description;
    private String documentationReference;
    private String etlReference;
    private Date sourceReleaseDate;
    private Date cdmReleaseDate;
    private String cdmVersion;
    private String vocabularyVersion;

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getAbbreviation() {

        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {

        this.abbreviation = abbreviation;
    }

    public String getHolder() {

        return holder;
    }

    public void setHolder(String holder) {

        this.holder = holder;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public String getDocumentationReference() {

        return documentationReference;
    }

    public void setDocumentationReference(String documentationReference) {

        this.documentationReference = documentationReference;
    }

    public String getEtlReference() {

        return etlReference;
    }

    public void setEtlReference(String etlReference) {

        this.etlReference = etlReference;
    }

    public Date getSourceReleaseDate() {

        return sourceReleaseDate;
    }

    public void setSourceReleaseDate(Date sourceReleaseDate) {

        this.sourceReleaseDate = sourceReleaseDate;
    }

    public Date getCdmReleaseDate() {

        return cdmReleaseDate;
    }

    public void setCdmReleaseDate(Date cdmReleaseDate) {

        this.cdmReleaseDate = cdmReleaseDate;
    }

    public String getCdmVersion() {

        return cdmVersion;
    }

    public void setCdmVersion(String cdmVersion) {

        this.cdmVersion = cdmVersion;
    }

    public String getVocabularyVersion() {

        return vocabularyVersion;
    }

    public void setVocabularyVersion(String vocabularyVersion) {

        this.vocabularyVersion = vocabularyVersion;
    }
}
