package com.bmc.truesight.remedy.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.truesight.remedy.beans.Configuration;
import com.bmc.truesight.remedy.beans.FieldItem;
import com.bmc.truesight.remedy.beans.Payload;
import com.bmc.truesight.remedy.exception.ParsingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class parses the configuration file and
 *
 * @author vitiwari
 */
public class ConfigParser {

    private static final Logger log = LoggerFactory.getLogger(ConfigParser.class);

    private String configPath;
    private Configuration configuration;
    private Payload payload;
    private Map<String, FieldItem> fieldItemMap;

    public ConfigParser(String configPath) {
        this.configPath = configPath;
        this.fieldItemMap = new HashMap<String, FieldItem>();
    }

    /**
     * Used to parse the configuration in case of configuration available as
     * json String
     *
     * @param configJson
     * @throws ParsingException
     */
    public void readParseConfigJson(String configJson) throws ParsingException {
        parse(configJson);
    }

    /**
     * Used to parse the configuration in case of configuration available as
     * json file
     *
     * @param configJson
     * @throws ParsingException
     */
    public void readParseConfigFile() throws ParsingException {

        // Read the file in String
        String configJson = null;
        try {
            configJson = FileUtils.readFileToString(new File(this.configPath), "UTF8");
        } catch (IOException e) {
            throw new ParsingException(StringUtils.format(Constants.CONFIG_FILE_NOT_FOUND, new Object[]{this.configPath}));
        }
        parse(configJson);
    }

    private void parse(String configJson) throws ParsingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = null;
        try {
            rootNode = mapper.readTree(configJson);
        } catch (IOException e) {
            throw new ParsingException(StringUtils.format(Constants.CONFIG_FILE_NOT_VALID, new Object[]{this.configPath}));
        }

        // Read the config details and map to pojo
        String configString;
        try {
            JsonNode configuration = rootNode.get("config");
            configString = mapper.writeValueAsString(configuration);
            this.configuration = mapper.readValue(configString, Configuration.class);
        } catch (IOException e) {
            throw new ParsingException(StringUtils.format(Constants.CONFIG_PROPERTY_NOT_FOUND, new Object[]{}));
        }

        // Read the payload details and map to pojo
        try {
            JsonNode payloadNode = rootNode.get("payload");
            String payloadString = mapper.writeValueAsString(payloadNode);
            this.setPayload(mapper.readValue(payloadString, Payload.class));
        } catch (IOException e) {
            throw new ParsingException(StringUtils.format(Constants.PAYLOAD_PROPERTY_NOT_FOUND, new Object[]{}));
        }

        // Iterate over the properties and if it starts with '@', put it to
        // itemValueMap
        Iterator<Entry<String, JsonNode>> nodes = rootNode.fields();
        while (nodes.hasNext()) {
            Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) nodes.next();
            if (entry.getKey().startsWith("@")) {
                try {
                    String placeholderNode = mapper.writeValueAsString(entry.getValue());
                    FieldItem placeholderDefinition = mapper.readValue(placeholderNode, FieldItem.class);
                    fieldItemMap.put(entry.getKey(), placeholderDefinition);
                } catch (IOException e) {
                    throw new ParsingException(StringUtils.format(Constants.PAYLOAD_PROPERTY_NOT_FOUND, new Object[]{entry.getKey()}));
                }
            }

        }

    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Payload getPayload() {
        return payload;
    }

    public Map<String, FieldItem> getFieldItemMap() {
        return fieldItemMap;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

}
