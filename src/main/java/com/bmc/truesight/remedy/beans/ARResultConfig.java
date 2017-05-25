package com.bmc.truesight.remedy.beans;

/**
 * AR Result configuration.
 *
 * @author gokumar
 *
 */
public class ARResultConfig {

    private static final String DEFAULT_FORM_CLUSTER = "TS Cluster";
    private static final int DEFAULT_FIELD_TASK_ID = 536870917;
    private static final int DEFAULT_FIELD_CLUSTER_ID = 536870913;
    private static final int DEFAULT_FIELD_CLUSTER_NAME = 536870914;
    private static final int DEFAULT_FIELD_CLUSTER_SIZE = 536870915;

    private String formName;

    private ARServer arServer;

    private int fieldTaskID;
    private int fieldClusterID;
    private int fieldClusterName;
    private int fieldClusterSize;

    public ARResultConfig() {
        this.formName = DEFAULT_FORM_CLUSTER;

        this.fieldTaskID = DEFAULT_FIELD_TASK_ID;
        this.fieldClusterID = DEFAULT_FIELD_CLUSTER_ID;
        this.fieldClusterName = DEFAULT_FIELD_CLUSTER_NAME;
        this.fieldClusterSize = DEFAULT_FIELD_CLUSTER_SIZE;
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

    public int getFieldClusterName() {
        return fieldClusterName;
    }

    public void setFieldClusterName(int fieldClusterName) {
        this.fieldClusterName = fieldClusterName;
    }

    public int getFieldClusterSize() {
        return fieldClusterSize;
    }

    public void setFieldClusterSize(int fieldClusterSize) {
        this.fieldClusterSize = fieldClusterSize;
    }

    public ARServer getArServer() {
        return arServer;
    }

    public void setArServer(ARServer arServer) {
        this.arServer = arServer;
    }

    public void printConfig() {

        this.arServer.printConfig();
        System.out.println("FormName: " + formName);
        System.out.println("filedTaskID: " + fieldTaskID);
        System.out.println("fieldClusterID: " + fieldClusterID);
        System.out.println("fieldClusterName: " + fieldClusterName);
        System.out.println("fieldClusterSize: " + fieldClusterSize);
    }
}
