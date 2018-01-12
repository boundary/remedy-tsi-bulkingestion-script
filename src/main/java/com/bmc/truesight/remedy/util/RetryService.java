package com.bmc.truesight.remedy.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.arsys.api.ARServerUser;
import com.bmc.arsys.api.OutputInteger;
import com.bmc.truesight.remedy.App;
import com.bmc.truesight.saas.remedy.integration.ARServerForm;
import com.bmc.truesight.saas.remedy.integration.RemedyReader;
import com.bmc.truesight.saas.remedy.integration.adapter.RemedyEntryEventAdapter;
import com.bmc.truesight.saas.remedy.integration.beans.Configuration;
import com.bmc.truesight.saas.remedy.integration.beans.Error;
import com.bmc.truesight.saas.remedy.integration.beans.InvalidEvent;
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
import com.bmc.truesight.saas.remedy.integration.util.Constants;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class RetryService {

    private static final Logger LOG = LoggerFactory.getLogger(RetryService.class);

    public static void reingestFailedTickets(ARServerForm incidentForm) {
        String fileNamePrefix = ScriptUtil.getEventTypeByFormNameCaps(incidentForm) + "failure";
        String fileNameToRead = null;
        boolean isFileReadSuccess = false;

        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        File dir = new File(s);
        Collection<File> foundFiles = FileUtils.listFiles(dir, new WildcardFileFilter(fileNamePrefix + "*.csv"), null);
        long maxTimeStamp = 0l;
        for (File file : foundFiles) {
            String name = file.getName();
            if (name.contains("_")) {
                String[] nameBlocks = name.split("_");
                if (nameBlocks.length == 2) {
                    String tsstring = nameBlocks[1];
                    String[] splitted = tsstring.split("\\.");
                    String timestamp = splitted[0];
                    long tsno = Long.parseLong(timestamp);
                    if (tsno > maxTimeStamp) {
                        maxTimeStamp = tsno;
                    }
                }
            }
        }
        if (maxTimeStamp == 0) {
            fileNameToRead = fileNamePrefix + ".csv";
        } else {
            fileNameToRead = fileNamePrefix + "_" + maxTimeStamp + ".csv";
        }
        LOG.debug("reading file {} for recovery retry", fileNameToRead);

        //read csv file for incident/Change Ids
        List<String> ids = new ArrayList<>();
        try {
            CSVReader reader = new CSVReader(new FileReader(fileNameToRead), ',', '"', 1);
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine != null) {
                    //Verifying the read data here
                    ids.add(nextLine[0]);
                    LOG.debug(Arrays.toString(nextLine));
                }
            }
            isFileReadSuccess = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (isFileReadSuccess) {
            if (incidentForm == ARServerForm.INCIDENT_FORM) {
                Template incidentTemplate = getIncidentTemplateforReIngestion(ids);
                if (incidentTemplate != null) {
                    readAndIngest(incidentForm, incidentTemplate, ids);
                }
            } else {
                Template changeTemplate = getChangeTemplateforReIngestion(ids);
                if (changeTemplate != null) {
                    readAndIngest(incidentForm, changeTemplate, ids);
                }
            }
        }
    }

    public static void writeCSVFailure(String name, List<InvalidEvent> droppedEvents, String idPropertyName) {
        String failurefile = name + "failure";
        String[] failureheader = new String[]{"Entry ID", "Incident/Change ID", "Size", "Field with max size", "Field size"};
        String fileName = failurefile + ".csv";
        CSVWriter failWriter = null;
        File f = new File(fileName);
        if (f.exists() && !f.isDirectory()) {
            fileName = failurefile + "_" + new Date().getTime() + ".csv";
            LOG.debug("Creating a new file as {} to store the invalid errors", fileName);
        }
        try {
            failWriter = new CSVWriter(new FileWriter(fileName));
        } catch (IOException e) {
            LOG.error("CSV file creation failed[{}], Please check the LOGs for the details of failed ingestion.", e.getMessage());
        }
        if (failWriter != null) {
            failWriter.writeNext(failureheader);
            for (InvalidEvent invalidEvent : droppedEvents) {
                failWriter.writeNext(new String[]{invalidEvent.getEntryId(), invalidEvent.getInvalidEvent().getProperties().get(idPropertyName), String.valueOf(invalidEvent.getEventSize()), invalidEvent.getMaxSizePropertyName(), String.valueOf(invalidEvent.getPropertySize())});
            }
            try {
                if (failWriter != null) {
                    failWriter.close();
                }
            } catch (IOException e) {
                LOG.error("Closing CSV Writer failed {}", e.getMessage());
            }
        }

    }

    private static Template getIncidentTemplateforReIngestion(List<String> ids) {
        boolean isIncidentFileValid = false;
        boolean incidentIngestionFlag = false;
        RemedyReader incidentReader = new GenericRemedyReader();
        Template incidentTemplate = null;
        try {
            incidentTemplate = ScriptUtil.prepareTemplate(ARServerForm.INCIDENT_FORM);
            isIncidentFileValid = true;
        } catch (ParsingException e) {
            LOG.error("Incident template preparation failed: Parsing Exception, {}", e.getMessage());
        } catch (ValidationException e) {
            LOG.error("Incident template preparation failed: Validation Exception, {}", e.getMessage());
        } catch (IOException e) {
            LOG.error("The Incident template file could not be found, please check the file name and location");
        } catch (Exception e) {
            LOG.error("Incident template preparation failed: Exception, {}", e.getMessage());
            e.printStackTrace();
            incidentTemplate = null;
        } finally {
            App.deleteRegFile();
        }
        LOG.debug("Incident template file reading and parsing success state is {}", isIncidentFileValid);
        if (isIncidentFileValid) {
            try {
                if (!App.hasLoggedIntoRemedyIncident) {
                    App.hasLoggedIntoRemedyIncident = incidentReader.login(App.incidentUser);
                }
            } catch (RemedyLoginFailedException e) {
                LOG.error(e.getMessage());
                incidentTemplate = null;
            } finally {
                App.deleteRegFile();
            }
            if (App.hasLoggedIntoRemedyIncident) {
                System.out.println(ids.size() + " dropped incidents available for re-ingestion, do you want to ingest these to TSIntelligence?(y/n)");
                if (!App.silentFlag) {
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.next();
                    if (input.equalsIgnoreCase("y")) {
                        incidentIngestionFlag = true;
                    }
                } else {
                    LOG.info("Silent Mode: proceeding with yes");
                    incidentIngestionFlag = true;
                }
                if (incidentIngestionFlag) {
                    if (!App.silentFlag) {
                        if (!App.incidentExportToCsvFlag) {
                            System.out.println("Do you also want to export these events as CSV?(y/n)");
                            Scanner scanner = new Scanner(System.in);
                            String exportInput = scanner.next();
                            if (exportInput.equalsIgnoreCase("y")) {
                                App.incidentExportToCsvFlag = true;
                            }
                        }
                    }
                } else {
                    incidentTemplate = null;
                }
            }
        } else {
            if (App.hasLoggedIntoRemedyIncident) {
                incidentReader.logout(App.incidentUser);
            }
        }
        return incidentTemplate;
    }

    private static Template getChangeTemplateforReIngestion(List<String> ids) {
        boolean isChangeFileValid = false;
        boolean changeIngestionFlag = false;
        Template changeTemplate = null;
        RemedyReader changeReader = new GenericRemedyReader();
        try {
            changeTemplate = ScriptUtil.prepareTemplate(ARServerForm.CHANGE_FORM);
            isChangeFileValid = true;
        } catch (ParsingException e) {
            LOG.error(e.getMessage());
        } catch (ValidationException e) {
            LOG.error(e.getMessage());
        } catch (IOException e) {
            LOG.error("The Change Template file couldnot be found, please check the file name and location");
        } catch (Exception e) {
            LOG.error(e.getMessage());
        } finally {
            App.deleteRegFile();
        }
        LOG.debug("Change template file reading and parsing success state is {}", isChangeFileValid);
        if (isChangeFileValid) {
            try {
                if (!App.hasLoggedIntoRemedyChange) {
                    App.hasLoggedIntoRemedyChange = changeReader.login(App.changeUser);
                }
            } catch (RemedyLoginFailedException e) {
                LOG.error(e.getMessage());
                changeTemplate = null;
            } finally {
                App.deleteRegFile();
            }
            if (App.hasLoggedIntoRemedyChange) {
                System.out.println(ids.size() + " dropped change tickets available for re-ingestion, do you want to ingest these to TSIntelligence?(y/n)");
                if (!App.silentFlag) {
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.next();
                    if (input.equalsIgnoreCase("y")) {
                        changeIngestionFlag = true;
                    }
                } else {
                    LOG.info("Silent Mode: proceeding with yes");
                    changeIngestionFlag = true;
                }
                if (changeIngestionFlag) {
                    if (!App.silentFlag) {
                        if (!App.changeExportToCsvFlag) {
                            System.out.println("Do you also want to export these events as CSV?(y/n)");
                            Scanner scanner = new Scanner(System.in);
                            String exportInput = scanner.next();
                            if (exportInput.equalsIgnoreCase("y")) {
                                App.changeExportToCsvFlag = true;
                            }
                        }
                    }
                } else {
                    changeTemplate = null;
                }
            }

        } else {
            if (App.hasLoggedIntoRemedyChange) {
                changeReader.logout(App.changeUser);
            }
        }
        return changeTemplate;
    }

    private static void readAndIngest(ARServerForm form, Template template, List<String> ids) {

        String name = ScriptUtil.getEventTypeByFormNameCaps(form);
        CSVWriter writer = null;
        Configuration config = template.getConfig();
        RemedyReader reader = new GenericRemedyReader();
        ARServerUser user = null;
        final String idPropertyName;
        RemedyEventResponse remedyResponse = new RemedyEventResponse();
        RemedyEntryEventAdapter adapter = null;
        try {
            if (form.equals(ARServerForm.INCIDENT_FORM)) {
                if (!App.hasLoggedIntoRemedyIncident) {
                    App.hasLoggedIntoRemedyIncident = reader.login(App.incidentUser);
                }
                user = App.incidentUser;
                idPropertyName = Constants.PROPERTY_INCIDENT_NO;
                adapter = new RemedyEntryEventAdapter(App.incidentFieldIdMap);
            } else {
                if (!App.hasLoggedIntoRemedyChange) {
                    App.hasLoggedIntoRemedyChange = reader.login(App.changeUser);
                }
                user = App.changeUser;
                idPropertyName = Constants.PROPERTY_CHANGE_ID;
                adapter = new RemedyEntryEventAdapter(App.changeFieldIdMap);
            }
            int chunkSize = config.getRetryChunkSize(); 
            int startFrom = 0;
            int iteration = 1;
            int totalRecordsRead = 0;
            OutputInteger nMatches = new OutputInteger();
            boolean readNext = true;
            int totalFailure = 0;
            int totalSuccessful = 0;
            int validRecords = 0;
            List<InvalidEvent> droppedEvents = new ArrayList<>();
            LOG.info("Started reading {} remedy {} starting from index {}", new Object[]{chunkSize, name, startFrom});
            Map<String, List<String>> errorsMap = new HashMap<>();
            //Reading first Iteration to get the Idea of total available count
            String csv = name + "_reingestion_records_" + new Date().getTime() + ".csv";
            String[] headers = App.getFieldHeaders(template);
            if ((form == ARServerForm.INCIDENT_FORM && App.incidentExportToCsvFlag) || (form == ARServerForm.CHANGE_FORM && App.changeExportToCsvFlag)) {
                try {
                    writer = new CSVWriter(new FileWriter(csv));
                } catch (IOException e) {
                    LOG.error("CSV file creation failed[{}], Do you want to proceed without csv export ?(y/n)", e.getMessage());
                    if (!App.silentFlag) {
                        Scanner scanner = new Scanner(System.in);
                        String input = scanner.next();
                        if (input.equalsIgnoreCase("n")) {
                            System.exit(0);
                        } else {
                            App.resetExportToCSVFlag();
                        }
                    } else {
                        LOG.info("Silent Mode: proceeding with yes");
                        App.resetExportToCSVFlag();
                    }
                }
                if (writer != null) {
                    writer.writeNext(headers);
                }
            }

            while (readNext) {
                LOG.info("_________________Iteration : " + iteration);
                int endIndex = (startFrom + chunkSize - 1);
                if (endIndex >= ids.size()) {
                    endIndex = ids.size() - 1;
                    chunkSize = ids.size() - startFrom;
                    readNext = false;
                }
                remedyResponse = reader.readRemedyTicketsWithId(user, form, template, ids.subList(startFrom, endIndex + 1), adapter);
                int recordsCount = remedyResponse.getValidEventList().size() + remedyResponse.getInvalidEventList().size();

                totalRecordsRead += recordsCount;
                validRecords += remedyResponse.getValidEventList().size();
                LOG.info(" Request Sent to remedy (startFrom:" + startFrom + ",chunkSize:" + chunkSize + "), Response(Valid Event(s):" + remedyResponse.getValidEventList().size() + ", Invalid Event(s):" + remedyResponse.getInvalidEventList().size() + ", totalRecordsRead: (" + totalRecordsRead + "/" + nMatches.intValue() + ")");

                if (recordsCount == 0) {
                    break;
                }

                iteration++;
                startFrom = totalRecordsRead;
                if (remedyResponse.getInvalidEventList().size() > 0) {
                    List<String> eventIds = new ArrayList<>();
                    remedyResponse.getInvalidEventList().forEach(event -> {
                        eventIds.add(event.getInvalidEvent().getProperties().get(idPropertyName));
                    });
                    droppedEvents.addAll(remedyResponse.getInvalidEventList());
                    totalFailure += remedyResponse.getInvalidEventList().size();
                    LOG.debug("following {} ids are larger than allowed limits [{}]", name, String.join(",", eventIds));
                }
                if ((form == ARServerForm.INCIDENT_FORM && App.incidentExportToCsvFlag) || (form == ARServerForm.CHANGE_FORM && App.changeExportToCsvFlag)) {
                    for (TSIEvent event : remedyResponse.getValidEventList()) {
                        if (writer != null) {
                            writer.writeNext(App.getFieldValues(event, headers));
                        }
                    }
                    LOG.debug("{} events written to the CSV file", remedyResponse.getValidEventList().size());
                }
                Result result = new EventIngestionExecuterService().ingestEvents(remedyResponse.getValidEventList(), config);
                if (result != null && result.getAccepted() != null) {
                    totalSuccessful += result.getAccepted().size();
                }
                if (result != null && result.getErrors() != null) {
                    totalFailure += result.getErrors().size();
                }
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
            LOG.info("__________________ {} ingestion to truesight intelligence final status: Remedy Record(s) = {}, Valid Record(s) Sent = {}, Successful = {} , Failure = {} ______", new Object[]{name, totalRecordsRead, validRecords, totalSuccessful, totalFailure});
            if ((form == ARServerForm.INCIDENT_FORM && App.incidentExportToCsvFlag) || (form == ARServerForm.CHANGE_FORM && App.changeExportToCsvFlag)) {
                LOG.info("__________________{} event(s) written to the CSV file {}", validRecords, csv);
            }
            if (droppedEvents.size() > 0) {
                LOG.error("______Following {} events were invalid & dropped. Please remove the offending properties from the field mapping and run the script again with \"retry\" argument", droppedEvents.size());
                writeCSVFailure(name, droppedEvents, idPropertyName);
                droppedEvents.forEach(invalidEvent -> {
                    LOG.error("{} : {} , Event Size :{}, Field with max size  : {}, Field Size: {} ", idPropertyName, invalidEvent.getInvalidEvent().getProperties().get(idPropertyName), invalidEvent.getEventSize(), invalidEvent.getMaxSizePropertyName(), invalidEvent.getPropertySize());
                });
            }
            if (totalFailure > 0) {
                LOG.error("______ Event Count, Failure reason , [Reference Id(s)] ______");
                errorsMap.keySet().forEach(msg -> {
                    LOG.error("______ {} , {},  {}", errorsMap.get(msg).size(), msg, errorsMap.get(msg));
                });
            }
        } catch (RemedyLoginFailedException e) {
            LOG.error("Login Failed : {}", e.getMessage());
        } catch (RemedyReadFailedException e) {
            LOG.error("{}, failed ids are [{}].", e.getMessage(), ids);
        } catch (BulkEventsIngestionFailedException e) {
            LOG.error("Ingestion Failed (Reason : {}) for ids [{}].", e.getMessage(), ids);
        } catch (TsiAuthenticationFailedException e) {
            LOG.error("Exception in ingestion, {}", e.getMessage());
        } catch (Exception ex) {
            LOG.error("Exception {}", ex.getMessage());
        } finally {
            if (form == ARServerForm.INCIDENT_FORM && App.hasLoggedIntoRemedyIncident) {
                reader.logout(App.incidentUser);
            } else if (form == ARServerForm.CHANGE_FORM && App.hasLoggedIntoRemedyChange) {
                reader.logout(App.changeUser);
            }
            if ((form == ARServerForm.INCIDENT_FORM && App.incidentExportToCsvFlag) || (form == ARServerForm.CHANGE_FORM && App.changeExportToCsvFlag)) {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException e) {
                    LOG.error("Closing CSV Writer failed {}", e.getMessage());
                }
            }
            App.deleteRegFile();
        }

    }

}
