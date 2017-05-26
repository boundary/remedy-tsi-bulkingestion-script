package com.bmc.truesight.remedy.util;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.truesight.remedy.beans.Configuration;
import com.bmc.truesight.remedy.beans.FieldItem;
import com.bmc.truesight.remedy.beans.Payload;
import com.bmc.truesight.remedy.beans.PayloadSource;
import com.bmc.truesight.remedy.exception.ValidationException;

/**
 * This class validates the configuration file
 *
 * @author vitiwari
 */
public class ConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(ConfigValidator.class);

    public void validate(ConfigParser parser) throws ValidationException {
        Configuration config = parser.getConfiguration();
        Payload payload = parser.getPayload();
        Map<String, FieldItem> fieldItemMap = parser.getFieldItemMap();

        // validate configuration
        //TODO REMOVE FOR PLUGIN IMPLEMENTATION
        if (config.getRemedyHostName().isEmpty() || config.getRemedyUserName().isEmpty()
                || config.getRemedyPassword().isEmpty()) {
            throw new ValidationException(StringUtils.format(Constants.CONFIG_VALIDATION_FAILED, new Object[]{}));
        }

        // validate payload configuration
        if (payload.getTitle().startsWith("@") && !fieldItemMap.containsKey(payload.getTitle())) {
            throw new ValidationException(StringUtils.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING,
                    new Object[]{payload.getTitle()}));
        }

        // validate payload configuration
        for (String fpField : payload.getFingerprintFields()) {
            if (fpField.startsWith("@") && !fieldItemMap.containsKey(fpField)) {
                throw new ValidationException(
                        StringUtils.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{fpField}));
            }
        }

        // validate payload configuration
        Map<String, String> properties = payload.getProperties();
        for (String key : properties.keySet()) {
            if (properties.get(key).startsWith("@") && !fieldItemMap.containsKey(properties.get(key))) {
                throw new ValidationException(
                        StringUtils.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{properties.get(key)}));
            }
        }

        if (payload.getSeverity().startsWith("@") && !fieldItemMap.containsKey(payload.getSeverity())) {
            throw new ValidationException(
                    StringUtils.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getSeverity()}));
        }

        if (payload.getStatus().startsWith("@") && !fieldItemMap.containsKey(payload.getStatus())) {
            throw new ValidationException(
                    StringUtils.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getStatus()}));
        }

        if (payload.getCreatedAt().startsWith("@") && !fieldItemMap.containsKey(payload.getCreatedAt())) {
            throw new ValidationException(
                    StringUtils.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getCreatedAt()}));
        }

        if (payload.getEventClass().startsWith("@") && !fieldItemMap.containsKey(payload.getEventClass())) {
            throw new ValidationException(
                    StringUtils.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getEventClass()}));
        }

        //valiadting source
        PayloadSource source = payload.getSource();
        if (source.getName().startsWith("@") && !fieldItemMap.containsKey(source.getName())) {
            throw new ValidationException(
                    StringUtils.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{source.getName()}));
        }
        if (source.getType().startsWith("@") && !fieldItemMap.containsKey(source.getType())) {
            throw new ValidationException(
                    StringUtils.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{source.getType()}));
        }
        if (source.getRef().startsWith("@") && !fieldItemMap.containsKey(source.getRef())) {
            throw new ValidationException(
                    StringUtils.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{source.getRef()}));
        }

        PayloadSource sender = payload.getSender();
        if (sender.getName().startsWith("@") && !fieldItemMap.containsKey(sender.getName())) {
            throw new ValidationException(
                    StringUtils.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{sender.getName()}));
        }
        if (sender.getType().startsWith("@") && !fieldItemMap.containsKey(sender.getType())) {
            throw new ValidationException(
                    StringUtils.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{sender.getType()}));
        }
        if (sender.getRef().startsWith("@") && !fieldItemMap.containsKey(sender.getRef())) {
            throw new ValidationException(
                    StringUtils.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{sender.getRef()}));
        }
    }
}
