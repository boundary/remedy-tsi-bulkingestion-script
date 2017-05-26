package com.bmc.truesight.remedy.util;

import java.util.Map;

import org.apache.commons.lang.NotImplementedException;

import com.bmc.arsys.api.Entry;
import com.bmc.arsys.api.Value;
import com.bmc.truesight.remedy.beans.FieldItem;
import com.bmc.truesight.remedy.beans.Payload;
import com.bmc.truesight.remedy.beans.PayloadSource;

public class RemedyEntryEventAdapter {

    public Payload convertIncidentEntryToPayload(ConfigParser configParser, Entry entry) {

        Payload payload = new Payload(configParser.getPayload());

        payload.setTitle(getValueFromEntry(configParser, entry, payload.getTitle()));
        Map<String, String> properties = payload.getProperties();
        for (String key : properties.keySet()) {
            properties.put(key, getValueFromEntry(configParser, entry, properties.get(key)));
        }
        payload.setSeverity(getValueFromEntry(configParser, entry, payload.getSeverity()));
        payload.setStatus(getValueFromEntry(configParser, entry, payload.getStatus()));
        payload.setCreatedAt(getValueFromEntry(configParser, entry, payload.getCreatedAt()));
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
        throw new NotImplementedException();
    }

    private String getValueFromEntry(ConfigParser configParser, Entry entry, String placeholder) {
        if (placeholder.startsWith("@")) {
            FieldItem fieldItem = configParser.getFieldItemMap().get(placeholder);
            Value value = entry.get(fieldItem.getFieldId());
            String val = value.getValue().toString();
            if (fieldItem.getValueMap() != null && fieldItem.getValueMap().get(val) != null) {
                return fieldItem.getValueMap().get(val);
            } else {
                return val;
            }
        } else {
            return placeholder;
        }
    }
}
