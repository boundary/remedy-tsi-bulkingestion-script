package com.bmc.truesight.remedy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.arsys.api.ARServerUser;
import com.bmc.arsys.api.OutputInteger;
import com.bmc.truesight.remedy.exception.BulkEventsIngestionFailedException;
import com.bmc.truesight.remedy.util.Constants;
import com.bmc.truesight.remedy.util.ScriptUtil;
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
import com.bmc.truesight.saas.remedy.integration.exception.RemedyLoginFailedException;
import com.bmc.truesight.saas.remedy.integration.exception.RemedyReadFailedException;
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
            readAndIngest(ARServerForm.INCIDENT_FORM);
        }
        if (readChange) {
            readAndIngest(ARServerForm.CHANGE_FORM);
        }
    }

    private static void readAndIngest(ARServerForm form) {

        String path = null;
        boolean hasLoggedIntoRemedy = false;
        String name = "";
        if (form.equals(ARServerForm.INCIDENT_FORM)) {
            name = "Incidents";
        } else if (form.equals(ARServerForm.CHANGE_FORM)) {
            name = "Changes";
        }

        try {
            path = new java.io.File(".").getCanonicalPath();
            if (form.equals(ARServerForm.INCIDENT_FORM)) {
                path += Constants.INCIDENT_TEMPLATE_PATH;
            } else if (form.equals(ARServerForm.CHANGE_FORM)) {
                path += Constants.CHANGE_TEMPLATE_PATH;
            }

        } catch (IOException e2) {
            log.error("The file path couldnot be found ");
        }
        // PARSING THE CONFIGURATION FILE
        Template template = null;
        TemplatePreParser preParser = new GenericTemplatePreParser();
        TemplateParser parser = new GenericTemplateParser();
        try {
            Template defaultTemplate = preParser.loadDefaults(form);
            template = parser.readParseConfigFile(defaultTemplate, path);
        } catch (ParsingException ex) {
            log.error(ex.getMessage());
            System.exit(0);
        }
        log.info("{} file reading and parsing succesfull", path);
        // VALIDATION OF THE CONFIGURATION
        TemplateValidator validator = new GenericTemplateValidator();
        try {
            validator.validate(template);
        } catch (ValidationException ex) {
            log.error(ex.getMessage());
            System.exit(0);
        }
        log.info("{} configuration file validation succesfull", path);

        Configuration config = template.getConfig();
        RemedyReader reader = new GenericRemedyReader();
        TsiHttpClient client = new TsiHttpClient(config);
        ARServerUser user = reader.createARServerContext(config.getRemedyHostName(), config.getRemedyPort(), config.getRemedyUserName(), config.getRemedyPassword());
        List<TSIEvent> eventList = new ArrayList<>();
        List<TSIEvent> lastEventList = new ArrayList<>();
        try {
            // Start Login
            hasLoggedIntoRemedy = reader.login(user);
            RemedyEntryEventAdapter adapter = new RemedyEntryEventAdapter();
            int chunkSize = config.getChunkSize();
            int startFrom = 0;
            int iteration = 1;
            int totalRecordsRead = 0;
            OutputInteger nMatches = new OutputInteger();
            boolean readNext = true;
            int successfulEntries = 0;
            boolean exceededMaxServerEntries = false;
            log.info("Started reading {} remedy {} starting from index {} , [Start Date: {}, End Date: {}]", new Object[]{chunkSize, name, startFrom, ScriptUtil.dateToString(config.getStartDateTime()), ScriptUtil.dateToString(config.getEndDateTime())});
            while (readNext) {
                log.info("Iteration : " + iteration);
                eventList = reader.readRemedyTickets(user, form, template, startFrom, chunkSize, nMatches, adapter);
                exceededMaxServerEntries = reader.exceededMaxServerEntries(user);
                totalRecordsRead += eventList.size();
                if (eventList.size() < chunkSize && totalRecordsRead < nMatches.intValue() && exceededMaxServerEntries) {
                    log.info(" Request Sent to remedy (startFrom:" + startFrom + ",chunkSize:" + chunkSize + "), Response Got(RecordsRead:" + eventList.size() + ", totalRecordsRead:" + totalRecordsRead + ", recordsAvailable:" + nMatches.intValue() + ")");
                    log.info(" Based on exceededMaxServerEntries response as(" + exceededMaxServerEntries + "), adjusting the chunk Size as " + eventList.size());
                    chunkSize = eventList.size();
                } else if (eventList.size() <= chunkSize) {
                    log.info(" Request Sent to remedy (startFrom:" + startFrom + ", chunkSize:" + chunkSize + "), Response Got (RecordsRead:" + eventList.size() + ", totalRecordsRead:" + totalRecordsRead + ", recordsAvailable:" + nMatches.intValue() + ")");
                }
                if (totalRecordsRead < nMatches.longValue() && (totalRecordsRead + chunkSize) > nMatches.longValue()) {
                    //assuming the long value would be in int range always
                    chunkSize = (int) (nMatches.longValue() - totalRecordsRead);
                } else if (totalRecordsRead >= nMatches.longValue()) {
                    readNext = false;
                }
                iteration++;
                startFrom = totalRecordsRead;
                int successCount = client.pushBulkEventsToTSI(eventList);
                successfulEntries += successCount;
                lastEventList = new ArrayList<>(eventList);
            }
            log.info("________________________ Total {} Entries from Remedy = {}, Successful Ingestion Count = {} ______", new Object[]{name, nMatches.longValue(), successfulEntries});
        } catch (RemedyLoginFailedException e) {
            log.error("Login Failed : {}", e.getMessage());
        } catch (RemedyReadFailedException e) {
            if (lastEventList.isEmpty()) {
                log.error("Reading tickets from Remedy server failed for StartDateTime, EndDateTime ({},{}). Please try running the script after some time with the same timestamps.", new Object[]{e.getMessage(), ScriptUtil.dateToString(config.getStartDateTime()), ScriptUtil.dateToString(config.getEndDateTime())});
            } else {
                log.error("Due to some issue Reading tickets from Remedy server failed for StartDateTime, EndDateTime ({},{}). Please try running the script after some time from the last successful timestamp as below.", new Object[]{ScriptUtil.dateToString(config.getStartDateTime()), ScriptUtil.dateToString(config.getEndDateTime())});
                Date createdDate = convertToDate(lastEventList.get((lastEventList.size() - 1)).getCreatedAt());
                Date modDate = convertToDate(lastEventList.get((lastEventList.size() - 1)).getProperties().get("LastModDate"));
                Date closedDate = convertToDate(lastEventList.get((lastEventList.size() - 1)).getProperties().get("ClosedDate"));
                log.info("Created Date : {}", new Object[]{createdDate});
                if (lastEventList.get((lastEventList.size() - 1)).getProperties().get("LastModDate") != null) {
                    log.info("Last Modified Date : {}", new Object[]{modDate});
                }
                if (lastEventList.get((lastEventList.size() - 1)).getProperties().get("ClosedDate") != null) {
                    log.info("Closed Date : {}", new Object[]{closedDate});
                }
            }
        } catch (BulkEventsIngestionFailedException e) {
            if (lastEventList.isEmpty()) {
                log.error("Ingestion Failed (Reason : {}) for StartDateTime, EndDateTime ({},{}). Please try running the script after some time with the same timestamps.", new Object[]{e.getMessage(), ScriptUtil.dateToString(config.getStartDateTime()), ScriptUtil.dateToString(config.getEndDateTime())});
            } else {
                log.error("Ingestion Failed (Reason : {}) for StartDateTime, EndDateTime ({},{}). Please try running the script after some time from the last successful timestamp as below.", new Object[]{e.getMessage(), ScriptUtil.dateToString(config.getStartDateTime()), ScriptUtil.dateToString(config.getEndDateTime())});
                Date createdDate = convertToDate(lastEventList.get((lastEventList.size() - 1)).getCreatedAt());
                Date modDate = convertToDate(lastEventList.get((lastEventList.size() - 1)).getProperties().get("LastModDate"));
                Date closedDate = convertToDate(lastEventList.get((lastEventList.size() - 1)).getProperties().get("ClosedDate"));
                log.info("Created Date : {}", new Object[]{ScriptUtil.dateToString(createdDate)});
                if (lastEventList.get((lastEventList.size() - 1)).getProperties().get("LastModDate") != null) {
                    log.info("Last Modified Date : {}", new Object[]{ScriptUtil.dateToString(modDate)});
                }
                if (lastEventList.get((lastEventList.size() - 1)).getProperties().get("ClosedDate") != null) {
                    log.info("Closed Date : {}", new Object[]{ScriptUtil.dateToString(closedDate)});
                }
            }

        } catch (Exception ex) {
            log.error("Error {}", ex.getMessage());
        } finally {
            if (hasLoggedIntoRemedy) {
                reader.logout(user);
            }
        }

    }

    private static Date convertToDate(String date) {
        Date dt = null;
        try {
            dt = new Date(Long.parseLong(date) * 1000L);
        } catch (Exception ex) {
            log.debug("Date conversion failed for date [{}]", date);
            //do nothing 
        }
        return dt;
    }

}
