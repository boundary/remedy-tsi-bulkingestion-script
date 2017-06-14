package com.bmc.truesight.remedy;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.arsys.api.ARServerUser;
import com.bmc.arsys.api.OutputInteger;
import com.bmc.truesight.remedy.util.Constants;
import com.bmc.truesight.remedy.util.TsiHttpClient;
import com.bmc.truesight.saas.remedy.integration.ARServerForm;
import com.bmc.truesight.saas.remedy.integration.RemedyReader;
import com.bmc.truesight.saas.remedy.integration.TemplateParser;
import com.bmc.truesight.saas.remedy.integration.TemplatePreParser;
import com.bmc.truesight.saas.remedy.integration.TemplateValidator;
import com.bmc.truesight.saas.remedy.integration.adapter.RemedyEntryEventAdapter;
import com.bmc.truesight.saas.remedy.integration.beans.Configuration;
import com.bmc.truesight.saas.remedy.integration.beans.TSIEvent;
import com.bmc.truesight.saas.remedy.integration.beans.Template;
import com.bmc.truesight.saas.remedy.integration.exception.ParsingException;
import com.bmc.truesight.saas.remedy.integration.exception.ValidationException;
import com.bmc.truesight.saas.remedy.integration.impl.GenericRemedyReader;
import com.bmc.truesight.saas.remedy.integration.impl.GenericTemplateParser;
import com.bmc.truesight.saas.remedy.integration.impl.GenericTemplatePreParser;
import com.bmc.truesight.saas.remedy.integration.impl.GenericTemplateValidator;

/**
 * Main Application Entry This application reads the files incidentTemplate.json
 * & changeTemplate.json It parses the files and validates for the syntactical
 * and semantical correctness It Reads the incident/Change tickets from the
 * Remedy Server based on the configuration provided and then sends as an events
 * to the Truesight Intelligence server
 *
 * @author vitiwari
 */
public class App {

    private final static Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        boolean readIncidents = false;
        boolean readChange = false;
        if (args.length == 0) {
            System.out.println("Do you want to ingest Remedy Incidents tickets as events? (y/n)");
            Scanner scanner = new Scanner(System.in);
            String input1 = scanner.next();
            if (input1.equalsIgnoreCase("y")) {
                readIncidents = true;
            }
            System.out.println("Do you want to ingest Remedy Change tickets as events also? (y/n)");
            String input2 = scanner.next();
            if (input2.equalsIgnoreCase("y")) {
                readChange = true;
            }
        } else {
            for (String module : args) {
                if (module.equalsIgnoreCase("incident") || module.equalsIgnoreCase("incidents")) {
                    readIncidents = true;
                }
                if (module.equalsIgnoreCase("change") || module.equalsIgnoreCase("changes")) {
                    readChange = true;
                }
            }
        }
        if (readIncidents) {
            readAndIngestIncidents();
        }
        if (readChange) {
            readAndIngestChanges();
        }
    }

    private static void readAndIngestIncidents() {

        String path = null;
        boolean hasLoggedIntoRemedy = false;
        try {
            path = new java.io.File(".").getCanonicalPath();
            path += Constants.INCIDENT_TEMPLATE_PATH;
        } catch (IOException e2) {
            log.error("The file path couldnot be found ");
        }
        // PARSING THE CONFIGURATION FILE
        Template template = null;
        TemplatePreParser incidentPreParser = new GenericTemplatePreParser();
        TemplateParser incidentParser = new GenericTemplateParser();
        try {
            Template defaultTemplate = incidentPreParser.loadDefaults(ARServerForm.INCIDENT_FORM);
            template = incidentParser.readParseConfigFile(defaultTemplate, path);
        } catch (ParsingException ex) {
            log.error(ex.getMessage());
            System.exit(0);
        }
        log.info("{} file reading and parsing succesfull", path);
        // VALIDATION OF THE CONFIGURATION
        TemplateValidator incidentValidator = new GenericTemplateValidator();
        try {
            incidentValidator.validate(template);
        } catch (ValidationException ex) {
            log.error(ex.getMessage());
            System.exit(0);
        }
        log.info("{} configuration file validation succesfull", path);

        Configuration config = template.getConfig();
        RemedyReader incidentReader = new GenericRemedyReader();
        TsiHttpClient client = new TsiHttpClient(config);
        ARServerUser user = incidentReader.createARServerContext(config.getRemedyHostName(), null, config.getRemedyUserName(), config.getRemedyPassword());
        try {
            // Start Login
            hasLoggedIntoRemedy = incidentReader.login(user);
            RemedyEntryEventAdapter adapter = new RemedyEntryEventAdapter();
            int chunkSize = config.getChunkSize();
            int startFrom = 0;
            int iteration = 1;
            OutputInteger nMatches = new OutputInteger();
            boolean readNext = true;
            int successfulEntries = 0;
            log.info("Started reading {} remedy incidents starting from index {} , [Start Date: {}, End Date: {}]", new Object[]{chunkSize, startFrom, config.getStartDateTime(), config.getEndDateTime()});
            while (readNext) {
                List<TSIEvent> eventList = incidentReader.readRemedyTickets(user, ARServerForm.INCIDENT_FORM, template, startFrom, chunkSize, nMatches, adapter);
                log.info("[iteration : {}]  Recieved {} remedy incidents", new Object[]{iteration, eventList.size()});

                if (nMatches.longValue() <= (startFrom + chunkSize)) {
                    readNext = false;
                }
                iteration++;
                startFrom = startFrom + chunkSize;
                int successCount = client.pushBulkEventsToTSI(eventList);
                successfulEntries += successCount;
            }
            log.info("________________________ Total Entries from Remedy = {}, Successful Ingestion Count = {} ______", new Object[]{nMatches.longValue(), successfulEntries});  
        } catch (Exception ex) {
            log.error("Error {}", ex.getMessage());
        } finally {
            if (hasLoggedIntoRemedy) {
                incidentReader.logout(user);
            }
        }

    }

    private static void readAndIngestChanges() {
        String path = null;
        boolean hasLoggedIntoRemedy = false;
        try {
            path = new java.io.File(".").getCanonicalPath();
            path += Constants.CHANGE_TEMPLATE_PATH;
        } catch (IOException e2) {
            log.error("The file path couldnot be found ");
        }
        Template template = null;
        // PARSING THE CONFIGURATION FILE
        TemplateParser changeParser = new GenericTemplateParser();
        TemplatePreParser changePreParser = new GenericTemplatePreParser();
        try {

            Template defaultTemplate = changePreParser.loadDefaults(ARServerForm.CHANGE_FORM);
            template = changeParser.readParseConfigFile(defaultTemplate, path);
        } catch (ParsingException ex) {
            log.error(ex.getMessage());
            System.exit(0);
        }
        log.info("{} file reading and parsing succesfull", path);
        // VALIDATION OF THE CONFIGURATION
        TemplateValidator changeValidator = new GenericTemplateValidator();
        try {
            changeValidator.validate(template);
        } catch (ValidationException ex) {
            log.error(ex.getMessage());
            System.exit(0);
        }
        log.info("{} configuration file validation succesfull", path);

        Configuration config = template.getConfig();
        RemedyReader changeReader = new GenericRemedyReader();
        TsiHttpClient client = new TsiHttpClient(config);
        ARServerUser user = changeReader.createARServerContext(config.getRemedyHostName(), null, config.getRemedyUserName(), config.getRemedyPassword());
        try {

            // Start Login
            hasLoggedIntoRemedy = changeReader.login(user);
            RemedyEntryEventAdapter adapter = new RemedyEntryEventAdapter();
            int chunkSize = config.getChunkSize();
            int startFrom = 1;
            int iteration = 1;
            OutputInteger nMatches = new OutputInteger();
            boolean readNext = true;
            log.info("Started reading {} remedy Change tickets starting from index {} , [Start Date: {}, End Date: {}]", new Object[]{chunkSize, startFrom, config.getStartDateTime(), config.getEndDateTime()});
            while (readNext) {
                List<TSIEvent> eventList = changeReader.readRemedyTickets(user, ARServerForm.CHANGE_FORM, template, startFrom, chunkSize, nMatches, adapter);
                log.info("[iteration : {}]  Recieved {} remedy Changes", new Object[]{iteration, eventList.size()});

                if (nMatches.longValue() <= (startFrom + chunkSize)) {
                    readNext = false;
                }
                iteration++;
                startFrom = startFrom + chunkSize;

                // Send the events to TSI
                client.pushBulkEventsToTSI(eventList);
            }
        } catch (Exception ex) {
            log.error("Error {}", ex.getMessage());
            ex.printStackTrace();
        } finally {
            if (hasLoggedIntoRemedy) {
                changeReader.logout(user);
            }
        }
    }
}
