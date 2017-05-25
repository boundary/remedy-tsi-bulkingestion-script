package com.bmc.truesight.remedy.beans;

/**
 * AR Incident Form configuration.
 *
 * @author gokumar
 *
 */
public class ARIncidentFormConfig {

    private static final String DEFAULT_FORM_INCIDENT = "TS Incident";
    private static final int DEFAULT_FIELD_TASK_ID = 536870917;
    private static final int DEFAULT_FIELD_CLUSTER_ID = 536870913;
    private static final int DEFAULT_FIELD_INCIDENT_ID = 536870914;
    private static final int DEFAULT_FIELD_SUMMARY = 536870915;
    private static final int DEFAULT_FIELD_DETAILS = 536870918;

    private String formName;

    private ARServer arServer;

    private int fieldTaskID;
    private int fieldClusterID;
    private int fieldIncidentId;
    private int fieldSummary;
    private int fieldDetails;

    public ARIncidentFormConfig() {
        this.arServer = new ARServer();
        this.formName = DEFAULT_FORM_INCIDENT;

        this.fieldTaskID = DEFAULT_FIELD_TASK_ID;
        this.fieldClusterID = DEFAULT_FIELD_CLUSTER_ID;
        this.fieldIncidentId = DEFAULT_FIELD_INCIDENT_ID;
        this.fieldSummary = DEFAULT_FIELD_SUMMARY;
        this.fieldDetails = DEFAULT_FIELD_DETAILS;
    }

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public int getFieldTaskID() {
        return fieldTaskID;
    }

    public void setFieldTaskID(int fieldTaskID) {
        this.fieldTaskID = fieldTaskID;
    }

    public int getFieldClusterID() {
        return fieldClusterID;
    }

    public void setFieldClusterID(int fieldClusterID) {
        this.fieldClusterID = fieldClusterID;
    }

    public int getFieldIncidentId() {
        return fieldIncidentId;
    }

    public void setFieldIncidentId(int fieldIncidentId) {
        this.fieldIncidentId = fieldIncidentId;
    }

    public int getFieldSummary() {
        return fieldSummary;
    }

    public void setFieldSummary(int fieldSummary) {
        this.fieldSummary = fieldSummary;
    }

    public int getFieldDetails() {
        return fieldDetails;
    }

    public void setFieldDetails(int fieldDetails) {
        this.fieldDetails = fieldDetails;
    }

    public ARServer getArServer() {
        return arServer;
    }

    public void setArServer(ARServer arServer) {
        this.arServer = arServer;
    }
}
