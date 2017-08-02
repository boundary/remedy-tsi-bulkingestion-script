package com.bmc.truesight.remedy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.arsys.api.ARServerUser;
import com.bmc.arsys.api.OutputInteger;
import com.bmc.truesight.remedy.util.Constants;
import com.bmc.truesight.remedy.util.ScriptUtil;
import com.bmc.truesight.saas.remedy.integration.ARServerForm;
import com.bmc.truesight.saas.remedy.integration.BulkEventHttpClient;
import com.bmc.truesight.saas.remedy.integration.RemedyReader;
import com.bmc.truesight.saas.remedy.integration.TemplateParser;
import com.bmc.truesight.saas.remedy.integration.TemplatePreParser;
import com.bmc.truesight.saas.remedy.integration.TemplateValidator;
import com.bmc.truesight.saas.remedy.integration.adapter.RemedyEntryEventAdapter;
import com.bmc.truesight.saas.remedy.integration.beans.Configuration;
import com.bmc.truesight.saas.remedy.integration.beans.RemedyEventResponse;
import com.bmc.truesight.saas.remedy.integration.beans.Result;
import com.bmc.truesight.saas.remedy.integration.beans.Success;
import com.bmc.truesight.saas.remedy.integration.beans.TSIEvent;
import com.bmc.truesight.saas.remedy.integration.beans.Template;
import com.bmc.truesight.saas.remedy.integration.exception.BulkEventsIngestionFailedException;
import com.bmc.truesight.saas.remedy.integration.exception.ParsingException;
import com.bmc.truesight.saas.remedy.integration.exception.RemedyLoginFailedException;
import com.bmc.truesight.saas.remedy.integration.exception.RemedyReadFailedException;
import com.bmc.truesight.saas.remedy.integration.exception.ValidationException;
import com.bmc.truesight.saas.remedy.integration.impl.GenericBulkEventHttpClient;
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
                } else if (module.equalsIgnoreCase("change") || module.equalsIgnoreCase("changes")) {
                    readChange = true;
                } else {
                    setLoglevel(module);
                    log.debug("Log level changed as per command line arguments, new log level is {}", module);
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

    private static void setLoglevel(String module) {
        switch (module) {
            case "ALL":
                LogManager.getLogger("com.bmc.truesight").setLevel(Level.ALL);
            case "DEBUG":
                LogManager.getLogger("com.bmc.truesight").setLevel(Level.DEBUG);
                break;
            case "ERROR":
                LogManager.getLogger("com.bmc.truesight").setLevel(Level.ERROR);
                break;
            case "FATAL":
                LogManager.getLogger("com.bmc.truesight").setLevel(Level.FATAL);
                break;
            case "INFO":
                LogManager.getLogger("com.bmc.truesight").setLevel(Level.INFO);
                break;
            case "OFF":
                LogManager.getLogger("com.bmc.truesight").setLevel(Level.OFF);
                break;
            case "TRACE":
                LogManager.getLogger("com.bmc.truesight").setLevel(Level.TRACE);
                break;
            case "WARN":
                LogManager.getLogger("com.bmc.truesight").setLevel(Level.WARN);
                break;
            default:
                log.error("Arguments not recognised, available arguments are <incident> <change> <loglevel>.");
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
            log.debug("{} default template loading sucessfuly finished , default status configured to query is {}", name, defaultTemplate.getConfig().getQueryStatusList());
            template = parser.readParseConfigFile(defaultTemplate, path);
            log.debug("{} user template configuration parsing successful , status configured to be queried is {}", name, template.getConfig().getQueryStatusList());
        } catch (ParsingException ex) {
            log.error(ex.getMessage());
            System.exit(0);
        }
        log.info("{} file reading and parsing successful", path);
        // VALIDATION OF THE CONFIGURATION
        TemplateValidator validator = new GenericTemplateValidator();
        try {
            validator.validate(template);
        } catch (ValidationException ex) {
            log.error(ex.getMessage());
            System.exit(0);
        }
        log.info("{} configuration file validation successful", path);

        Configuration config = template.getConfig();
        RemedyReader reader = new GenericRemedyReader();
        BulkEventHttpClient client = new GenericBulkEventHttpClient(config);
        ARServerUser user = reader.createARServerContext(config.getRemedyHostName(), config.getRemedyPort(), config.getRemedyUserName(), config.getRemedyPassword());
        RemedyEventResponse remedyResponse = new RemedyEventResponse();
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
            int totalFailure = 0;
            int totalSuccessful = 0;
            boolean exceededMaxServerEntries = false;
            log.info("Started reading {} remedy {} starting from index {} , [Start Date: {}, End Date: {}]", new Object[]{chunkSize, name, startFrom, ScriptUtil.dateToString(config.getStartDateTime()), ScriptUtil.dateToString(config.getEndDateTime())});
            Map<String, Integer> errorsMap = new HashMap<String, Integer>();
            while (readNext) {
                log.info("Iteration : " + iteration);
                remedyResponse = reader.readRemedyTickets(user, form, template, startFrom, chunkSize, nMatches, adapter);
                int recordsCount = remedyResponse.getValidEventList().size() + remedyResponse.getLargeInvalidEventCount();
                exceededMaxServerEntries = reader.exceededMaxServerEntries(user);
                totalRecordsRead += recordsCount;
                if (recordsCount < chunkSize && totalRecordsRead < nMatches.intValue() && exceededMaxServerEntries) {
                    log.info(" Request Sent to remedy (startFrom:" + startFrom + ",chunkSize:" + chunkSize + "), Response Got(Valid Events:" + remedyResponse.getValidEventList().size() + ", Invalid Events:" + remedyResponse.getLargeInvalidEventCount() + ", totalRecordsRead: (" + totalRecordsRead + "/" + nMatches.intValue() + ")");
                    log.info(" Based on exceededMaxServerEntries response as(" + exceededMaxServerEntries + "), adjusting the chunk Size as " + recordsCount);
                    chunkSize = recordsCount;
                } else if (recordsCount <= chunkSize) {
                    log.info(" Request Sent to remedy (startFrom:" + startFrom + ",chunkSize:" + chunkSize + "), Response Got(Valid Events:" + remedyResponse.getValidEventList().size() + ", Invalid Events:" + remedyResponse.getLargeInvalidEventCount() + ", totalRecordsRead: (" + totalRecordsRead + "/" + nMatches.intValue() + ")");
                }
                if (totalRecordsRead < nMatches.longValue() && (totalRecordsRead + chunkSize) > nMatches.longValue()) {
                    //assuming the long value would be in int range always
                    chunkSize = ((int) (nMatches.longValue()) - totalRecordsRead);
                } else if (totalRecordsRead >= nMatches.longValue()) {
                    readNext = false;
                }
                iteration++;
                startFrom = totalRecordsRead;
                Result result = client.pushBulkEventsToTSI(remedyResponse.getValidEventList());
                if (result.getAccepted() != null) {
                    totalSuccessful += result.getAccepted().size();
                }
                if (result.getErrors() != null) {
                    totalFailure += result.getErrors().size();
                }
                lastEventList = new ArrayList<>(remedyResponse.getValidEventList());
                if (result.getSuccess() == Success.PARTIAL) {
                    log.debug("events rejected from tsi for following reasons");
                    result.getErrors().forEach(error -> {
                        String msg = error.getMessage().trim();
                        if (errorsMap.containsKey(msg)) {
                            errorsMap.put(msg, (errorsMap.get(msg) + 1));
                        } else {
                            errorsMap.put(msg, 1);
                        }
                        log.debug("Index :" + error.getIndex() + ": reason -" + error.getMessage());
                    });
                }
            }
            log.info("________________________ {} ingestion to truesight intelligence final status: Remedy Records = {}, Valid Records Sent = {}, Successful = {} , Failure = {} ______", new Object[]{name, nMatches.longValue(), remedyResponse.getValidEventList().size(), totalSuccessful, totalFailure});
            if (totalFailure > 0) {
                log.info("________________________  Errors (No of times seen)______");
                errorsMap.keySet().forEach(msg -> {
                    log.info("{}   ({})", msg, errorsMap.get(msg));
                });

            }

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
