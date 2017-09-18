package com.bmc.truesight.remedy;

import java.io.File;
import java.io.FileWriter;
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
import com.bmc.arsys.api.Field;
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
import com.bmc.truesight.saas.remedy.integration.exception.TsiAuthenticationFailedException;
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
    private static boolean incidentChoice = false;
    private static boolean changeChoice = false;
    private static ARServerUser incidentUser;
    private static ARServerUser changeUser;
    private static boolean hasLoggedIntoRemedyIncident = false;
    private static boolean hasLoggedIntoRemedyChange = false;
    private static Map<Integer, Field> incidentFieldIdMap;
    private static Map<Integer, Field> changeFieldIdMap;

    public static void main(String[] args) {
        if (args.length > 0) {
            applyChoices(args);
        }
        if (!(incidentChoice || changeChoice)) {
            incidentChoice = getIncidentChoice();
            changeChoice = getChangeChoice();
        }
        Template incidentTemplate = null;
        if (incidentChoice) {
            incidentTemplate = inputIncidentChoice();
        }
        Template changeTemplate = null;
        if (changeChoice) {
            changeTemplate = inputChangeChoice();
        }
        if (incidentTemplate != null) {
            readAndIngest(ARServerForm.INCIDENT_FORM, incidentTemplate);
        }
        if (changeTemplate != null) {
            readAndIngest(ARServerForm.CHANGE_FORM, changeTemplate);
        }

    }

    private static boolean getChangeChoice() {
        System.out.println("Do you want to ingest Remedy change tickets to Truesight Intelligence?(y/n)");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();
        if (input.equalsIgnoreCase("y")) {
            return true;
        }
        return false;
    }

    private static boolean getIncidentChoice() {
        System.out.println("Do you want to ingest Remedy incident tickets to Truesight Intelligence?(y/n)");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();
        if (input.equalsIgnoreCase("y")) {
            return true;
        }
        return false;
    }

    private static void applyChoices(String[] args) {
        for (String input : args) {
            if (input.equalsIgnoreCase("incident")) {
                incidentChoice = true;
            } else if (input.equalsIgnoreCase("change")) {
                changeChoice = true;
            } else {
                setLoglevel(input);
            }
        }
    }

    private static Template inputIncidentChoice() {
        boolean isIncidentFileValid = false;
        boolean incidentIngestionFlag = false;
        RemedyReader incidentReader = new GenericRemedyReader();
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
        } finally {
            deleteRegFile();
        }
        log.debug("Incident template file reading and parsing success state is {}", isIncidentFileValid);
        if (isIncidentFileValid) {
            try {
                if (!hasLoggedIntoRemedyIncident) {
                    hasLoggedIntoRemedyIncident = incidentReader.login(incidentUser);
                }
            } catch (RemedyLoginFailedException e) {
                log.error(e.getMessage());
                incidentTemplate = null;
            } finally {
                deleteRegFile();
            }
            if (hasLoggedIntoRemedyIncident) {
                try {
                    RemedyEntryEventAdapter adapter = new RemedyEntryEventAdapter(incidentFieldIdMap);
                    int incidentsCount = ScriptUtil.getAvailableRecordsCount(incidentUser, ARServerForm.INCIDENT_FORM, incidentTemplate, adapter);
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
                    if (!incidentIngestionFlag && hasLoggedIntoRemedyIncident) {
                        incidentReader.logout(incidentUser);
                    }
                    deleteRegFile();
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
        } else {
            if (hasLoggedIntoRemedyIncident) {
                incidentReader.logout(incidentUser);
            }
        }
        return incidentTemplate;
    }

    private static Template inputChangeChoice() {
        boolean isChangeFileValid = false;
        boolean changeIngestionFlag = false;
        Template changeTemplate = null;
        RemedyReader changeReader = new GenericRemedyReader();

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
        } finally {
            deleteRegFile();
        }
        log.debug("Change template file reading and parsing success state is {}", isChangeFileValid);
        if (isChangeFileValid) {
            try {
                if (!hasLoggedIntoRemedyChange) {
                    hasLoggedIntoRemedyChange = changeReader.login(changeUser);
                }
            } catch (RemedyLoginFailedException e) {
                log.error(e.getMessage());
                changeTemplate = null;
            } finally {
                deleteRegFile();
            }
            if (hasLoggedIntoRemedyChange) {
                try {
                    RemedyEntryEventAdapter adapter = new RemedyEntryEventAdapter(changeFieldIdMap);
                    int changesCount = ScriptUtil.getAvailableRecordsCount(changeUser, ARServerForm.CHANGE_FORM, changeTemplate, adapter);
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
                    if (!changeIngestionFlag && hasLoggedIntoRemedyChange) {
                        changeReader.logout(changeUser);
                    }
                    deleteRegFile();
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

        } else {
            if (hasLoggedIntoRemedyChange) {
                changeReader.logout(changeUser);
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
                log.error("Argument \"{}\" is not a valid log level, please use a valid log level (ex debug).", module);
                System.exit(0);
        }
    }

    private static void readAndIngest(ARServerForm form, Template template) {

        String name = ScriptUtil.getEventTypeByFormNameCaps(form);
        CSVWriter writer = null;
        Configuration config = template.getConfig();
        RemedyReader reader = new GenericRemedyReader();
        ARServerUser user = null;
        String idPropertyName = null;
        RemedyEventResponse remedyResponse = new RemedyEventResponse();
        List<TSIEvent> lastEventList = new ArrayList<>();
        RemedyEntryEventAdapter adapter = null;
        try {
            if (form.equals(ARServerForm.INCIDENT_FORM)) {
                if (!hasLoggedIntoRemedyIncident) {
                    hasLoggedIntoRemedyIncident = reader.login(incidentUser);
                }
                user = incidentUser;
                idPropertyName = Constants.PROPERTY_INCIDENTNO;
                adapter = new RemedyEntryEventAdapter(incidentFieldIdMap);
            } else if (form.equals(ARServerForm.CHANGE_FORM)) {
                if (!hasLoggedIntoRemedyChange) {
                    hasLoggedIntoRemedyChange = reader.login(changeUser);
                }
                user = changeUser;
                idPropertyName = Constants.PROPERTY_CHANGEID;
                adapter = new RemedyEntryEventAdapter(changeFieldIdMap);
            }
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
            List<String> droppedEventIds = new ArrayList<>();
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
                if (writer != null) {
                    writer.writeNext(headers);
                }
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
                
                if (recordsCount == 0){
                	break;
                }
                
                iteration++;
                startFrom = totalRecordsRead;
                if (remedyResponse.getLargeInvalidEventCount() > 0) {
                    List<String> eventIds = new ArrayList<>();
                    for (TSIEvent event : remedyResponse.getInvalidEventList()) {
                        eventIds.add(event.getProperties().get(idPropertyName));
                    }
                    droppedEventIds.addAll(eventIds);
                    log.error("following {} ids are larger than allowed limits [{}]", name, String.join(",", eventIds));
                }
                if ((form == ARServerForm.INCIDENT_FORM && incidentExportToCsvFlag) || (form == ARServerForm.CHANGE_FORM && changeExportToCsvFlag)) {
                    for (TSIEvent event : remedyResponse.getValidEventList()) {
                        if (writer != null) {
                            writer.writeNext(getFieldValues(event, headers));
                        }
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
                        id = remedyResponse.getValidEventList().get(error.getIndex()).getProperties().get(idPropertyName);
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
            if (droppedEventIds.size() > 0) {
                log.error("______Following {} events were invalid & dropped. {}", droppedEventIds.size(), droppedEventIds);
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
                Date modDate = convertToDate(lastEventList.get((lastEventList.size() - 1)).getProperties().get("LastModDate"));
                Date closedDate = convertToDate(lastEventList.get((lastEventList.size() - 1)).getProperties().get("ClosedDate"));
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
                Date modDate = convertToDate(lastEventList.get((lastEventList.size() - 1)).getProperties().get("LastModDate"));
                Date closedDate = convertToDate(lastEventList.get((lastEventList.size() - 1)).getProperties().get("ClosedDate"));
                if (lastEventList.get((lastEventList.size() - 1)).getProperties().get("LastModDate") != null) {
                    log.info("Last Modified Date : {}", new Object[]{ScriptUtil.dateToString(modDate)});
                }
                if (lastEventList.get((lastEventList.size() - 1)).getProperties().get("ClosedDate") != null) {
                    log.info("Closed Date : {}", new Object[]{ScriptUtil.dateToString(closedDate)});
                }
            }
        } catch (TsiAuthenticationFailedException e) {
            log.error("Error {}", e.getMessage());
        } catch (Exception ex) {
            log.error("Error {}", ex.getMessage());
        } finally {
            if (form == ARServerForm.INCIDENT_FORM && hasLoggedIntoRemedyIncident) {
                reader.logout(incidentUser);
            } else if (form == ARServerForm.CHANGE_FORM && hasLoggedIntoRemedyChange) {
                reader.logout(changeUser);
            }
            if ((form == ARServerForm.INCIDENT_FORM && incidentExportToCsvFlag) || (form == ARServerForm.CHANGE_FORM && changeExportToCsvFlag)) {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException e) {
                    log.error("Closing CSV Writer failed {}", e.getMessage());
                }
            }
            deleteRegFile();
        }

    }

    private static String[] getFieldHeaders(Template template) {
        TSIEvent event = template.getEventDefinition();
        List<String> fields = new ArrayList<>();
        for (java.lang.reflect.Field field : event.getClass().getDeclaredFields()) {
            if (field.getName().equalsIgnoreCase("properties")) {
                for (String key : event.getProperties().keySet()) {
                    fields.add(key);
                }
            } else if (field.getName().equals("source") || field.getName().equals("sender")) {
                for (java.lang.reflect.Field field1 : event.getSource().getClass().getDeclaredFields()) {
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

    private static void deleteRegFile() {
        File file = new File(Constants.REGKEY_FILE_NAME);
        if (file.exists()) {
            boolean isDeleted = file.delete();
            log.debug("{} file deleted = {}", Constants.REGKEY_FILE_NAME, isDeleted);
        }
    }

    public static ARServerUser getIncidentUser() {
        return incidentUser;
    }

    public static void setIncidentUser(ARServerUser incidentUser) {
        App.incidentUser = incidentUser;
    }

    public static ARServerUser getChangeUser() {
        return changeUser;
    }

    public static void setChangeUser(ARServerUser changeUser) {
        App.changeUser = changeUser;
    }

    public static boolean isHasLoggedIntoRemedyIncident() {
        return hasLoggedIntoRemedyIncident;
    }

    public static void setHasLoggedIntoRemedyIncident(boolean hasLoggedIntoRemedyIncident) {
        App.hasLoggedIntoRemedyIncident = hasLoggedIntoRemedyIncident;
    }

    public static boolean isHasLoggedIntoRemedyChange() {
        return hasLoggedIntoRemedyChange;
    }

    public static void setHasLoggedIntoRemedyChange(boolean hasLoggedIntoRemedyChange) {
        App.hasLoggedIntoRemedyChange = hasLoggedIntoRemedyChange;
    }

    public static Map<Integer, Field> getIncidentFieldIdMap() {
        return incidentFieldIdMap;
    }

    public static void setIncidentFieldIdMap(Map<Integer, Field> incidentFieldIdMap) {
        App.incidentFieldIdMap = incidentFieldIdMap;
    }

    public static Map<Integer, Field> getChangeFieldIdMap() {
        return changeFieldIdMap;
    }

    public static void setChangeFieldIdMap(Map<Integer, Field> changeFieldIdMap) {
        App.changeFieldIdMap = changeFieldIdMap;
    }

}
