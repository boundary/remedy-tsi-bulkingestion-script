package com.bmc.truesight.remedy.beans;

import java.util.ArrayList;
import java.util.List;

/**
 * AR Configuration to be sent by AR Client.
 *
 * @author gokumar
 *
 */
public class ARInputConfig {

    private String formName;
    private ARServer arServer;
    private String qualificationString;
    private List<ARField> arFields;

    public ARInputConfig() {
        this.arServer = new ARServer();
    }

    public String getFormName() {
        return this.formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public String getQualificationString() {
        return this.qualificationString;
    }

    public void setQualificationString(String qualificationString) {
        this.qualificationString = qualificationString;
    }

    public List<ARField> getArFields() {
        if (null == this.arFields) {
            return new ArrayList<ARField>();
        }
        return this.arFields;
    }

    public void setArFields(List<ARField> arFields) {
        this.arFields = arFields;
    }

    public ARServer getArServer() {
        return this.arServer;
    }

    public void setArServer(ARServer arServer) {
        this.arServer = arServer;
    }
}
