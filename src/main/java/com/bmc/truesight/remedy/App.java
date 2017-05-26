package com.bmc.truesight.remedy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.arsys.api.ARException;
import com.bmc.arsys.api.ARServerUser;
import com.bmc.arsys.api.ArithmeticOrRelationalOperand;
import com.bmc.arsys.api.DataType;
import com.bmc.arsys.api.Entry;
import com.bmc.arsys.api.OutputInteger;
import com.bmc.arsys.api.QualifierInfo;
import com.bmc.arsys.api.RelationalOperationInfo;
import com.bmc.arsys.api.SortInfo;
import com.bmc.arsys.api.Timestamp;
import com.bmc.arsys.api.Value;
import com.bmc.truesight.remedy.beans.ARConstants;
import com.bmc.truesight.remedy.beans.Configuration;
import com.bmc.truesight.remedy.beans.Payload;
import com.bmc.truesight.remedy.exception.ParsingException;
import com.bmc.truesight.remedy.exception.ValidationException;
import com.bmc.truesight.remedy.util.ConfigParser;
import com.bmc.truesight.remedy.util.ConfigValidator;
import com.bmc.truesight.remedy.util.RemedyIncidentReader;

/**
 * Main Application Entry 
 *
 */
public class App {

    private final static Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        run();
    }

    public static void run() {

        String path = null;
        try {
            path = new java.io.File(".").getCanonicalPath();
            path += "\\incidentTemplate.json";
        } catch (IOException e2) {
            log.error("The file path couldnot be found ");
        }
        // PARSING THE CONFIGURATION FILE
        ConfigParser incidentParser = new ConfigParser(path);
        try {
            incidentParser.readParseConfigFile();
        } catch (ParsingException ex) {
            log.error(ex.getMessage());
            System.exit(0);
        }
        log.info("{} file reading and parsing succesfull", path);
        // VALIDATION OF THE CONFIGURATION
        ConfigValidator incidentValidator = new ConfigValidator();
        try {
            incidentValidator.validate(incidentParser);
        } catch (ValidationException ex) {
            log.error(ex.getMessage());
            System.exit(0);
        }
        log.info("{} configuration file validation succesfull", path);

        Configuration config = incidentParser.getConfiguration();
        RemedyIncidentReader incidentReader = new RemedyIncidentReader(incidentParser);

        try {

            // Start Login
            incidentReader.login();

            int chunkSize = config.getChunkSize();
            int startFrom = 1;
            int iteration = 1;
            OutputInteger nMatches = new OutputInteger();
            boolean readNext = true;
            while (readNext) {
                log.info("Started reading remedy incidents with start & chunkSize as {},{},{},{}", new Object[]{startFrom, chunkSize, config.getStartDateTime(), config.getEndDateTime()});
                List<Payload> eventList = incidentReader.readIncidents(startFrom, chunkSize, nMatches);
                log.info("recieved remedy incidents chunk with start & chunkSize as {},{}", startFrom, chunkSize);
                log.info("Recieved {} remedy incidents as part of iteration {}", eventList.size(), iteration);

                eventList.forEach(event -> {
                    log.info("Event --> [title :{},severity:{}", event.getTitle(), event.getSeverity());
                });

                if (nMatches.longValue() <= (startFrom + chunkSize)) {
                    readNext = false;
                }
                iteration++;
                startFrom = startFrom + chunkSize;

                // TODO start Sending the events to TSI
            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
        } finally {
            incidentReader.logout();
        }

    }

    /*	private static Collection<Event> fetchData(ARServerUser arServerContext, Date date, long maxRecords,
			Configuration config) throws ARException {

		String strShortSummary = null;
		String strRequestId = null;
		String strSummary = null;
		String strSubmitDate = null;
		String strCloseDate = null;
		String strOwningGroup = null;
		String strService = null;
		String strPriority = null;
		String strReportedSource = null;
		String strStauts = null;
		String strAssignee = null;

		List<Event> events = new ArrayList<>();

		int[] queryFieldsList = { ARConstants.INCIDENT_ID_FIELD, ARConstants.SUMMARY_FIELD,
				ARConstants.SHORT_SUMMARY_FIELD, ARConstants.SUBMIT_DATE_FIELD, ARConstants.CLOSE_DATE_FIELD,
				ARConstants.OWNING_GROUP_FIELD, ARConstants.SERVICE_FIELD, ARConstants.PRIORITY_FIELD,
				ARConstants.REPORTED_SOURCE_FIELD, ARConstants.STATUS_FIELD, ARConstants.ASSIGNEE_FIELD };

		Date newDate = new Date();

		QualifierInfo qualInfo1 = buildFieldValueQualification(ARConstants.SUBMIT_DATE_FIELD,
				new Value(new Timestamp(date), DataType.TIME), RelationalOperationInfo.AR_REL_OP_GREATER_EQUAL);

		QualifierInfo qualInfo2 = buildFieldValueQualification(ARConstants.SUBMIT_DATE_FIELD,
				new Value(new Timestamp(newDate), DataType.TIME), RelationalOperationInfo.AR_REL_OP_LESS_EQUAL);

		QualifierInfo qualInfo = new QualifierInfo(QualifierInfo.AR_COND_OP_AND, qualInfo1, qualInfo2);

		OutputInteger nMatches = new OutputInteger();
		List<SortInfo> sortOrder = new ArrayList<SortInfo>();

		long totalIterations = 1;
		if (maxRecords > (long) config.getChunkSize()) {
			totalIterations = maxRecords / (long) config.getChunkSize();
		}

		boolean stopProcessing = false;
		int totalDocuments = 0;
		for (int count = 0; count < totalIterations + 1; count++) {
			List<Entry> entryList = arServerContext.getListEntryObjects(ARConstants.HELP_DESK_FORM, qualInfo,
					config.getChunkSize() * (count), config.getChunkSize(), sortOrder, queryFieldsList, false,
					nMatches);
			log.info("nMatches : " + nMatches + " entry List Size " + entryList.size());
			if (entryList.size() < 1) {
				break;
			}

			for (Entry queryEntry : entryList) {

				strShortSummary = null;
				strRequestId = null;
				strSummary = null;
				
				for (Map.Entry<Integer, Value> fieldIdVal : queryEntry.entrySet()) {
					if ((fieldIdVal.getKey()).toString().equals(Integer.toString(ARConstants.INCIDENT_ID_FIELD))) {
						if (fieldIdVal.getValue().getValue() != null) {
							strRequestId = fieldIdVal.getValue().getValue().toString();
						}
					}
					if ((fieldIdVal.getKey()).toString().equals(Integer.toString(ARConstants.SHORT_SUMMARY_FIELD))) {
						if (fieldIdVal.getValue().getValue() != null) {
							strShortSummary = fieldIdVal.getValue().getValue().toString();
						}
					}

					if ((fieldIdVal.getKey()).toString().equals(Integer.toString(ARConstants.SUMMARY_FIELD))) {
						if (fieldIdVal.getValue().getValue() != null) {
							strSummary = fieldIdVal.getValue().getValue().toString();
						}
					}
					if ((fieldIdVal.getKey()).toString().equals(Integer.toString(ARConstants.SUBMIT_DATE_FIELD))) {
						if (fieldIdVal.getValue().getValue() != null) {
							strSubmitDate = fieldIdVal.getValue().getValue().toString();
						}
					}
					if ((fieldIdVal.getKey()).toString().equals(Integer.toString(ARConstants.CLOSE_DATE_FIELD))) {
						if (fieldIdVal.getValue().getValue() != null) {
							strCloseDate = fieldIdVal.getValue().getValue().toString();
						}
					}
					if ((fieldIdVal.getKey()).toString().equals(Integer.toString(ARConstants.OWNING_GROUP_FIELD))) {
						if (fieldIdVal.getValue().getValue() != null) {
							strOwningGroup = fieldIdVal.getValue().getValue().toString();
						}
					}
					if ((fieldIdVal.getKey()).toString().equals(Integer.toString(ARConstants.SERVICE_FIELD))) {
						if (fieldIdVal.getValue().getValue() != null) {
							strService = fieldIdVal.getValue().getValue().toString();
						}
					}
					if ((fieldIdVal.getKey()).toString().equals(Integer.toString(ARConstants.PRIORITY_FIELD))) {
						if (fieldIdVal.getValue().getValue() != null) {
							strPriority = fieldIdVal.getValue().getValue().toString();
						}
					}
					if ((fieldIdVal.getKey()).toString().equals(Integer.toString(ARConstants.REPORTED_SOURCE_FIELD))) {
						if (fieldIdVal.getValue().getValue() != null) {
							strReportedSource = fieldIdVal.getValue().getValue().toString();
						}
					}
					if ((fieldIdVal.getKey()).toString().equals(Integer.toString(ARConstants.STATUS_FIELD))) {
						if (fieldIdVal.getValue().getValue() != null) {
							strStauts = fieldIdVal.getValue().getValue().toString();
						}
					}
					if ((fieldIdVal.getKey()).toString().equals(Integer.toString(ARConstants.ASSIGNEE_FIELD))) {
						if (fieldIdVal.getValue().getValue() != null) {
							strAssignee = fieldIdVal.getValue().getValue().toString();
						}
					}
				}

				// EventSeverity severity, String title, String message, String
				// host, String source, List<String> tags
				Event event = new Event(Event.EventSeverity.INFO, strShortSummary, strSummary,
						config.getRemedyHostName(), config.getRemedyHostName(), null);

				events.add(event);
				totalDocuments++;

				if (totalDocuments == 20) {
					stopProcessing = true;
					break;
				}
			}

			if (stopProcessing == true) {
				break;
			}
		}
		return events;
	}

     */
    /**
     * Prepare qualification "<fieldId>=<Value>"
     *
     * @return QualifierInfo
     *//*
	public static QualifierInfo buildFieldValueQualification(int fieldId, Value value, int relationalOperation) {
		ArithmeticOrRelationalOperand leftOperand = new ArithmeticOrRelationalOperand(fieldId);
		ArithmeticOrRelationalOperand rightOperand = new ArithmeticOrRelationalOperand(value);
		RelationalOperationInfo relationalOperationInfo = new RelationalOperationInfo(relationalOperation, leftOperand,
				rightOperand);
		QualifierInfo qualification = new QualifierInfo(relationalOperationInfo);
		return qualification;
	}*/

}
