package com.bmc.truesight.remedy.beans;

import java.sql.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public class Configuration {

    private String remedyHostName;
    private int remedyPort;
    private String remedyUserName;
    private String remedyPassword;
    private String tsiEventEndpoint;
    private String tsiApiToken;
    private int chunkSize;
    private List<Integer> conditionFields;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm a z")
    private Date startDateTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm a z")
    private Date endDateTime;
    private int retryConfig;

    public Date getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(Date endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getRemedyHostName() {
        return remedyHostName;
    }

    public void setRemedyHostName(String remedyHostName) {
        this.remedyHostName = remedyHostName;
    }

    public int getRemedyPort() {
        return remedyPort;
    }

    public void setRemedyPort(int remedyPort) {
        this.remedyPort = remedyPort;
    }

    public String getRemedyUserName() {
        return remedyUserName;
    }

    public void setRemedyUserName(String remedyUserName) {
        this.remedyUserName = remedyUserName;
    }

    public String getRemedyPassword() {
        return remedyPassword;
    }

    public void setRemedyPassword(String remedyPassword) {
        this.remedyPassword = remedyPassword;
    }

    public String getTsiApiToken() {
        return tsiApiToken;
    }

    public void setTsiApiToken(String tsiApiToken) {
        this.tsiApiToken = tsiApiToken;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Date getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(Date startDateTime) {
        this.startDateTime = startDateTime;
    }

    public int getRetryConfig() {
        return retryConfig;
    }

    public void setRetryConfig(int retryConfig) {
        this.retryConfig = retryConfig;
    }

    public List<Integer> getConditionFields() {
        return conditionFields;
    }

    public void setConditionFields(List<Integer> conditionFields) {
        this.conditionFields = conditionFields;
    }

    public String getTsiEventEndpoint() {
        return tsiEventEndpoint;
    }

    public void setTsiEventEndpoint(String tsiEventEndpoint) {
        this.tsiEventEndpoint = tsiEventEndpoint;
    }

}
