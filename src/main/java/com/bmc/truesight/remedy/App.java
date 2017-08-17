package com.bmc.truesight.remedy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
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
import com.bmc.truesight.saas.remedy.integration.RemedyReader;
import com.bmc.truesight.saas.remedy.integration.adapter.RemedyEntryEventAdapter;
import com.bmc.truesight.saas.remedy.integration.beans.Configuration;
import com.bmc.truesight.saas.remedy.integration.beans.Error;
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
import com.bmc.truesight.saas.remedy.integration.impl.EventIngestionExecuterService;
import com.bmc.truesight.saas.remedy.integration.impl.GenericRemedyReader;
import com.opencsv.CSVWriter;

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

    private static boolean incidentExportToCsvFlag = false;
    private static boolean changeExportToCsvFlag = false;

    public static void main(String[] args) {
        if (args.length > 0) {
            String logLevel = args[0];
            setLoglevel(logLevel);
        }
        Template incidentTemplate = inputIncidentChoice();
        Template changeTemplate = inputChangeChoice();
        if (incidentTemplate != null) {
            readAndIngest(ARServerForm.INCIDENT_FORM, incidentTemplate);
        }
        if (changeTemplate != null) {
            readAndIngest(ARServerForm.CHANGE_FORM, changeTemplate);
        }

    }

    private static Template inputIncidentChoice() {
        boolean isIncidentFileValid = false;
        boolean incidentIngestionFlag = false;

        Template incidentTemplate = null;
        try {
            incidentTemplate = ScriptUtil.prepareTemplate(ARServerForm.INCIDENT_FORM);
            isIncidentFileValid = true;
        } catch (ParsingException e) {
            log.error(e.getMessage());
        } catch (ValidationException e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            log.error("The Incident template file could not be found, please check the file name and location");
        } catch (Exception e) {
            log.error(e.getMessage());
            incidentTemplate = null;
        }
        log.debug("Incident template file reading and parsing successful");
        if (isIncidentFileValid) {
            Configuration config = incidentTemplate.getConfig();
            RemedyReader incidentReader = new GenericRemedyReader();
            ARServerUser user = incidentReader.createARServerContext(config.getRemedyHostName(), config.getRemedyPort(), config.getRemedyUserName(), config.getRemedyPassword());
            boolean hasLoggedIntoRemedy = false;
            try {
                hasLoggedIntoRemedy = incidentReader.login(user);
            } catch (RemedyLoginFailedException e) {
                log.error(e.getMessage());
                incidentTemplate = null;
            }
            if (hasLoggedIntoRemedy) {
                try {
                    int incidentsCount = ScriptUtil.getAvailableRecordsCount(user, ARServerForm.INCIDENT_FORM, incidentTemplate);
                    System.out.println(incidentsCount + " Incidents available for the input time range, do you want to ingest these to TSIntelligence?(y/n)");
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.next();
                    if (input.equalsIgnoreCase("y")) {
                        incidentIngestionFlag = true;
                    }
                } catch (RemedyReadFailedException e) {
                    log.error(e.getMessage());
                    incidentTemplate = null;
                } finally {
                    incidentReader.logout(user);
                }
                if (incidentIngestionFlag) {
                    System.out.println("Do you also want to export these events as CSV?(y/n)");
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.next();
                    if (input.equalsIgnoreCase("y")) {
                        incidentExportToCsvFlag = true;
                    }
                } else {
                    incidentTemplate = null;
                }
            }
        }
        return incidentTemplate;
    }

    private static Template inputChangeChoice() {
        boolean isChangeFileValid = false;
        boolean changeIngestionFlag = false;
        Template changeTemplate = null;
        try {
            changeTemplate = ScriptUtil.prepareTemplate(ARServerForm.CHANGE_FORM);
            isChangeFileValid = true;
        } catch (ParsingException e) {
            log.error(e.getMessage());
        } catch (ValidationException e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            log.error("The Change Template file couldnot be found, please check the file name and location");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        log.debug("Change template file reading and parsing successful");
        if (isChangeFileValid) {
            Configuration config = changeTemplate.getConfig();
            RemedyReader changeReader = new GenericRemedyReader();
            ARServerUser user = changeReader.createARServerContext(config.getRemedyHostName(), config.getRemedyPort(), config.getRemedyUserName(), config.getRemedyPassword());
            boolean hasLoggedIntoRemedy = false;
            try {
                hasLoggedIntoRemedy = changeReader.login(user);
            } catch (RemedyLoginFailedException e) {
                log.error(e.getMessage());
                changeTemplate = null;
            }
            if (hasLoggedIntoRemedy) {
                try {
                    int changesCount = ScriptUtil.getAvailableRecordsCount(user, ARServerForm.CHANGE_FORM, changeTemplate);
                    System.out.println(changesCount + " Change tickets available, do you want to ingest these to TSIntelligence?(y/n)");
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.next();
                    if (input.equalsIgnoreCase("y")) {
                        changeIngestionFlag = true;
                    }
                } catch (RemedyReadFailedException e) {
                    log.error(e.getMessage());
                    changeTemplate = null;
                } finally {
                    changeReader.logout(user);
                }
                if (changeIngestionFlag) {
                    System.out.println("Do you also want to export these events as CSV?(y/n)");
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.next();
                    if (input.equalsIgnoreCase("y")) {
                        changeExportToCsvFlag = true;
                    }
                } else {
                    changeTemplate = null;
                }
            }

        }
        return changeTemplate;
    }

    private static void setLoglevel(String module) {
        switch (module) {
            case "ALL":
            case "all":
                LogManager.getLogger("com.bmc.truesight").setLevel(Level.ALL);
            case "DEBUG":
            case "debug":
                LogManager.getLogger("com.bmc.truesight").setLevel(Level.DEBUG);
                break;
            case "ERROR":
            case "error":
                LogManager.getLogger("com.bmc.truesight").setLevel(Level.ERROR);
                break;
            case "FATAL":
            case "fatal":
                LogManager.getLogger("com.bmc.truesight").setLevel(Level.FATAL);
                break;
            case "INFO":
            case "info":
                LogManager.getLogger("com.bmc.truesight").setLevel(Level.INFO);
                break;
            case "OFF":
            case "off":
                LogManager.getLogger("com.bmc.truesight").setLevel(Level.OFF);
                break;
            case "TRACE":
            case "trace":
                LogManager.getLogger("com.bmc.truesight").setLevel(Level.TRACE);
                break;
            case "WARN":
            case "warn":
                LogManager.getLogger("com.bmc.truesight").setLevel(Level.WARN);
                break;
            default:
                log.error("Argument not recognised, available argument is <loglevel>(ex DEBUG).");
                System.exit(0);
        }
    }

    private static void readAndIngest(ARServerForm form, Template template) {

        boolean hasLoggedIntoRemedy = false;
        String name = ScriptUtil.getEventTypeByFormNameCaps(form);
        CSVWriter writer = null;
        Configuration config = template.getConfig();
        RemedyReader reader = new GenericRemedyReader();
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
            int validRecords = 0;
            boolean exceededMaxServerEntries = false;
            log.info("Started reading {} remedy {} starting from index {} , [Start Date: {}, End Date: {}]", new Object[]{chunkSize, name, startFrom, ScriptUtil.dateToString(config.getStartDateTime()), ScriptUtil.dateToString(config.getEndDateTime())});
            Map<String, List<String>> errorsMap = new HashMap<>();
            //Reading first Iteration to get the Idea of total available count
            String csv = name + "_records_" + config.getStartDateTime().getTime() + "_TO_" + config.getEndDateTime().getTime() + ".csv";
            String[] headers = getFieldHeaders(template);
            if ((form == ARServerForm.INCIDENT_FORM && incidentExportToCsvFlag) || (form == ARServerForm.CHANGE_FORM && changeExportToCsvFlag)) {
                try {
                    writer = new CSVWriter(new FileWriter(csv));
                } catch (IOException e) {
                    log.error("CSV file creation failed[{}], Do you want to proceed without csv export ?(y/n)", e.getMessage());
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.next();
                    if (input.equalsIgnoreCase("n")) {
                        System.exit(0);
                    } else {
                        resetExportToCSVFlag();
                    }
                }
                writer.writeNext(headers);
            }

            while (readNext) {
                log.info("_________________Iteration : " + iteration);
                remedyResponse = reader.readRemedyTickets(user, form, template, startFrom, chunkSize, nMatches, adapter);
                int recordsCount = remedyResponse.getValidEventList().size() + remedyResponse.getLargeInvalidEventCount();
                exceededMaxServerEntries = reader.exceededMaxServerEntries(user);
                totalRecordsRead += recordsCount;
                validRecords += remedyResponse.getValidEventList().size();
                if (recordsCount < chunkSize && totalRecordsRead < nMatches.intValue() && exceededMaxServerEntries) {
                    log.info(" Request Sent to remedy (startFrom:" + startFrom + ",chunkSize:" + chunkSize + "), Response(Valid Event(s):" + remedyResponse.getValidEventList().size() + ", Invalid Event(s):" + remedyResponse.getLargeInvalidEventCount() + ", totalRecordsRead: (" + totalRecordsRead + "/" + nMatches.intValue() + ")");
                    log.info(" Based on exceededMaxServerEntries response as(" + exceededMaxServerEntries + "), adjusting the chunk Size as " + recordsCount);
                    chunkSize = recordsCount;
                } else if (recordsCount <= chunkSize) {
                    log.info(" Request Sent to remedy (startFrom:" + startFrom + ",chunkSize:" + chunkSize + "), Response(Valid Event(s):" + remedyResponse.getValidEventList().size() + ", Invalid Event(s):" + remedyResponse.getLargeInvalidEventCount() + ", totalRecordsRead: (" + totalRecordsRead + "/" + nMatches.intValue() + ")");
                }
                if (totalRecordsRead < nMatches.longValue() && (totalRecordsRead + chunkSize) > nMatches.longValue()) {
                    //assuming the long value would be in int range always
                    chunkSize = ((int) (nMatches.longValue()) - totalRecordsRead);
                } else if (totalRecordsRead >= nMatches.longValue()) {
                    readNext = false;
                }
                iteration++;
                startFrom = totalRecordsRead;
                if (remedyResponse.getLargeInvalidEventCount() > 0) {
                    List<String> eventIds = new ArrayList<>();
                    if (form == ARServerForm.INCIDENT_FORM) {
                        for (TSIEvent event : remedyResponse.getInvalidEventList()) {
                            eventIds.add(event.getProperties().get(Constants.PROPERTY_INCIDENTNO));
                        }
                    } else {
                        for (TSIEvent event : remedyResponse.getInvalidEventList()) {
                            eventIds.add(event.getProperties().get(Constants.PROPERTY_INCIDENTNO));
                        }
                    }
                    log.error("following {} ids are larger than allowed limits [{}]", name, String.join(",", eventIds));
                }
                if ((form == ARServerForm.INCIDENT_FORM && incidentExportToCsvFlag) || (form == ARServerForm.CHANGE_FORM && changeExportToCsvFlag)) {
                    for (TSIEvent event : remedyResponse.getValidEventList()) {
                        writer.writeNext(getFieldValues(event, headers));
                    }
                    log.debug("{} events written to the CSV file", remedyResponse.getValidEventList().size());
                }
                Result result = new EventIngestionExecuterService().ingestEvents(remedyResponse.getValidEventList(), config);
                if (result != null && result.getAccepted() != null) {
                    totalSuccessful += result.getAccepted().size();
                }
                if (result != null && result.getErrors() != null) {
                    totalFailure += result.getErrors().size();
                }
                lastEventList = new ArrayList<>(remedyResponse.getValidEventList());
                if (result != null && result.getSuccess() == Success.PARTIAL) {
                    for (Error error : result.getErrors()) {
                        String id = "";
                        String msg = error.getMessage().trim();
                        if (form == ARServerForm.INCIDENT_FORM) {
                            id = remedyResponse.getValidEventList().get(error.getIndex()).getProperties().get(Constants.PROPERTY_INCIDENTNO);
                        } else {
                            id = remedyResponse.getValidEventList().get(error.getIndex()).getProperties().get(Constants.PROPERTY_CHANGEID);
                        }
                        if (errorsMap.containsKey(msg)) {
                            List<String> errorsId = errorsMap.get(msg);
                            errorsId.add(id);
                            errorsMap.put(msg, errorsId);
                        } else {
                            List<String> errorsId = new ArrayList<String>();
                            errorsId.add(id);
                            errorsMap.put(msg, errorsId);
                        }
                    }
                }

            }
            log.info("__________________ {} ingestion to truesight intelligence final status: Remedy Record(s) = {}, Valid Record(s) Sent = {}, Successful = {} , Failure = {} ______", new Object[]{name, nMatches.longValue(), validRecords, totalSuccessful, totalFailure});
            if ((form == ARServerForm.INCIDENT_FORM && incidentExportToCsvFlag) || (form == ARServerForm.CHANGE_FORM && changeExportToCsvFlag)) {
                log.info("__________________{} event(s) written to the CSV file {}", validRecords, csv);
            }
            if (totalFailure > 0) {
                log.error("______ Event Count, Failure reason , [Reference Id(s)] ______");
                errorsMap.keySet().forEach(msg -> {
                    log.error("______ {}    , {},  {}", errorsMap.get(msg).size(), msg, errorsMap.get(msg));
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
            if ((form == ARServerForm.INCIDENT_FORM && incidentExportToCsvFlag) || (form == ARServerForm.CHANGE_FORM && changeExportToCsvFlag)) {
                try {
                    writer.close();
                } catch (IOException e) {
                    log.error("Closing CSV Writer failed {}", e.getMessage());
                }
            }
            File file = new File(Constants.REGKEY_FILE_NAME);
            if (file.exists()) {
                boolean isDeleted = file.delete();
                log.debug("{} file deleted = {}", Constants.REGKEY_FILE_NAME, isDeleted);
            }
        }

    }

    private static String[] getFieldHeaders(Template template) {
        TSIEvent event = template.getEventDefinition();
        List<String> fields = new ArrayList<>();
        for (Field field : event.getClass().getDeclaredFields()) {
            if (field.getName().equalsIgnoreCase("properties")) {
                for (String key : event.getProperties().keySet()) {
                    fields.add(key);
                }
            } else if (field.getName().equals("source") || field.getName().equals("sender")) {
                for (Field field1 : event.getSource().getClass().getDeclaredFields()) {
                    fields.add(field.getName() + "_" + field1.getName());
                }
            } else {
                fields.add(field.getName());
            }
        }
        return fields.toArray(new String[0]);
    }

    private static String[] getFieldValues(TSIEvent event, String[] header) {
        List<String> values = new ArrayList<>();
        for (String fieldName : header) {
            switch (fieldName) {
                case "title":
                    values.add(event.getTitle());
                    break;
                case "status":
                    values.add(event.getStatus());
                    break;
                case "severity":
                    values.add(event.getSeverity());
                    break;
                case "message":
                    values.add(event.getMessage());
                    break;
                case "createdAt":
                    values.add(event.getCreatedAt());
                    break;
                case "eventClass":
                    values.add(event.getEventClass());
                    break;
                case "sender_name":
                    values.add(event.getSender().getName());
                    break;
                case "sender_ref":
                    values.add(event.getSender().getRef());
                    break;
                case "sender_type":
                    values.add(event.getSender().getType());
                    break;
                case "source_name":
                    values.add(event.getSource().getName());
                    break;
                case "source_ref":
                    values.add(event.getSource().getRef());
                    break;
                case "source_type":
                    values.add(event.getSource().getType());
                    break;
                case "fingerprintFields":
                    values.add(String.join(",", event.getFingerprintFields()));
                    break;
                default:
                    values.add(event.getProperties().get(fieldName));
            }
        }
        return values.toArray(new String[0]);
    }

    private static Date convertToDate(String date) {
        Date dt = null;
        try {
            dt = new Date(Long.parseLong(date) * 1000L);
        } catch (Exception ex) {
            log.debug("Date conversion failed for date [{}]", date);
        }
        return dt;
    }

    private static void resetExportToCSVFlag() {
        incidentExportToCsvFlag = false;
        changeExportToCsvFlag = false;
    }

}
