package com.bmc.truesight.remedy.beans;

import java.util.HashMap;
import java.util.Map;


/**
 * AR Form Configuration
 * 
 * @author sbayani
 *
 */
public class ARFormConfig {

	private String formName;
	private Map<String, Integer> fields;
	
	public ARFormConfig() {
		fields = new HashMap<String, Integer>();
		setDefaults();
	}
	
	private void setDefaults() {
		this.formName = ARConstants.HELP_DESK_FORM; // Default is the incidences form
		
		setField(ARConstants.INCIDENT_ID_NAME, ARConstants.INCIDENT_ID_FIELD);
		setField(ARConstants.SUMMARY_NAME, ARConstants.SUMMARY_FIELD);
		setField(ARConstants.SHORT_SUMMARY_NAME, ARConstants.SHORT_SUMMARY_FIELD);
		setField(ARConstants.SUBMIT_DATE_NAME, ARConstants.SUBMIT_DATE_FIELD);
		setField(ARConstants.ASSIGNEE_NAME, ARConstants.ASSIGNEE_FIELD);
	}
	public Map<String, Integer> getConfig() {
		return this.fields;
	}
	public int get(String name) {
		return this.fields.get(name);
	}
	public String getFormName() {
		return formName;
	}
	public void setFormName(String formName) {
		this.formName = formName;
	}
	public void setField(String name, int id) {
		this.fields.put(name, id);
	}
	public int getFieldId(String name) {
		return this.fields.get(name);
	}
}
