package com.bmc.truesight.remedy.beans;

import java.sql.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

public class Configuration {

    private String remedyHostName;
    private int remedyPort;
    private String remedyUserName;
    private String remedyPassword;
    private String tsiApiKey;
    private int chunkSize;
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

    public String getTsiApiKey() {
        return tsiApiKey;
    }

    public void setTsiApiKey(String tsiApiKey) {
        this.tsiApiKey = tsiApiKey;
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

}
