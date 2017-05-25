package com.bmc.truesight.remedy.beans;

/**
 * AR Field, list of such fields can be passed by AR to do the clustering on.
 * @author sbayani
 *
 */
public class ARField {

	private int field;
	private String name;

    public ARField(){
        
    }
    public ARField(int field, String name) {
        this.field = field;
        this.name = name;
    }

    public int getField() {
		return this.field;
	}
	public void setField(int field) {
		this.field = field;
	}
	public String getName() {
		return this.name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
