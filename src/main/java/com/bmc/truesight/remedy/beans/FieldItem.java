package com.bmc.truesight.remedy.beans;

import java.util.Map;

public class FieldItem {

	private long fieldId;
	Map<String, String> valueMap;

	public Map<String, String> getValueMap() {
		return valueMap;
	}

	public void setValueMap(Map<String, String> valueMap) {
		this.valueMap = valueMap;
	}

	public long getFieldId() {
		return fieldId;
	}

	public void setFieldId(long fieldId) {
		this.fieldId = fieldId;
	}

	@Override
	public String toString() {
		return "FieldItem [ fieldId=" + fieldId + ", valueMap=" + valueMap
				+ "]";
	}

}
