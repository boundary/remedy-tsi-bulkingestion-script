package com.bmc.truesight.remedy.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.arsys.api.Entry;
import com.bmc.arsys.api.Value;
import com.bmc.truesight.remedy.beans.FieldItem;
import com.bmc.truesight.remedy.beans.Payload;
import com.bmc.truesight.remedy.beans.PayloadSource;

/**
 * This is an adapter which converts the remedy Entry items into Payload (TSI
 * Events)
 *
 * @author vitiwari
 */
public class RemedyEntryEventAdapter {

    private static final Logger log = LoggerFactory.getLogger(RemedyEntryEventAdapter.class);

    public Payload convertIncidentEntryToPayload(ConfigParser configParser, Entry entry) {

        Payload payload = new Payload(configParser.getPayload());

        payload.setTitle(getValueFromEntry(configParser, entry, payload.getTitle()));
        List<String> fPrintFields = new ArrayList<>();
        payload.getFingerprintFields().forEach(fingerPrint -> {
            fPrintFields.add(getValueFromEntry(configParser, entry, fingerPrint));
        });
        payload.setFingerprintFields(fPrintFields);
        Map<String, String> properties = payload.getProperties();
        for (String key : properties.keySet()) {
            properties.put(key, getValueFromEntry(configParser, entry, properties.get(key)));
        }
        payload.setSeverity(getValueFromEntry(configParser, entry, payload.getSeverity()));
        payload.setStatus(getValueFromEntry(configParser, entry, payload.getStatus()));
        payload.setCreatedAt(resolveDate(getValueFromEntry(configParser, entry, payload.getCreatedAt())));
        payload.setEventClass(getValueFromEntry(configParser, entry, payload.getEventClass()));

        // valiadting source
        PayloadSource source = payload.getSource();
        source.setName(getValueFromEntry(configParser, entry, source.getName()));
        source.setType(getValueFromEntry(configParser, entry, source.getType()));
        source.setRef(getValueFromEntry(configParser, entry, source.getRef()));

        PayloadSource sender = payload.getSender();
        sender.setName(getValueFromEntry(configParser, entry, sender.getName()));
        sender.setType(getValueFromEntry(configParser, entry, sender.getType()));
        sender.setRef(getValueFromEntry(configParser, entry, sender.getRef()));
        return payload;

    }

    public Payload convertChangeEntryToPayload(ConfigParser configParser, Entry entry) {
        return convertIncidentEntryToPayload(configParser, entry);
    }

    private String getValueFromEntry(ConfigParser configParser, Entry entry, String placeholder) {
        if (placeholder.startsWith("@")) {
            FieldItem fieldItem = configParser.getFieldItemMap().get(placeholder);
            Value value = entry.get(fieldItem.getFieldId());
            String val = "";
            if (value != null && value.getValue() != null) {
                val = value.getValue().toString();
            }
            if (fieldItem.getValueMap() != null && fieldItem.getValueMap().get(val) != null) {
                return fieldItem.getValueMap().get(val);
            } else {
                return val;
            }
        } else {
            return placeholder;
        }
    }

    private String resolveDate(String dateString) {
        Long longDate = null;
        try {
            if (dateString == null || dateString.trim().length() == 0) {
                return "";
            } else {
                longDate = Long.parseLong(dateString.substring(11, 21));
            }
        } catch (NumberFormatException nfe) {
            log.error("Remedy created date parsing failed, returning as blank value");
            return "";
        }
        return longDate.toString();
    }
}
