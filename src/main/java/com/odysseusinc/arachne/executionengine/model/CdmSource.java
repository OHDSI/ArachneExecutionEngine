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
