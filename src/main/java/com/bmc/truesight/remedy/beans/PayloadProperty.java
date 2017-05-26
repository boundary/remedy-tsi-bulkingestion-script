package com.bmc.truesight.remedy.beans;

public class PayloadProperty {

    private String Assignee;
    private String IncidentNumber;
    private String CI;
    private String Priority;
    private String PriorityWeight;
    private String app_id;

    public String getAssignee() {
        return Assignee;
    }

    public void setAssignee(String assignee) {
        Assignee = assignee;
    }

    public String getIncidentNumber() {
        return IncidentNumber;
    }

    public void setIncidentNumber(String incidentNumber) {
        IncidentNumber = incidentNumber;
    }

    public String getCI() {
        return CI;
    }

    public void setCI(String cI) {
        CI = cI;
    }

    public String getPriority() {
        return Priority;
    }

    public void setPriority(String priority) {
        Priority = priority;
    }

    public String getPriorityWeight() {
        return PriorityWeight;
    }

    public void setPriorityWeight(String priorityWeight) {
        PriorityWeight = priorityWeight;
    }

    public String getApp_id() {
        return app_id;
    }

    public void setApp_id(String app_id) {
        this.app_id = app_id;
    }
}
