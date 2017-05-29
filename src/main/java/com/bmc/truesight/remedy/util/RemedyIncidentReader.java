package com.bmc.truesight.remedy.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import com.bmc.truesight.remedy.beans.Configuration;
import com.bmc.truesight.remedy.beans.Payload;
import com.bmc.truesight.remedy.exception.RemedyLoginFailedException;

public class RemedyIncidentReader {

    private static final Logger log = LoggerFactory.getLogger(RemedyIncidentReader.class);

    private ARServerUser arServerContext;
    private ConfigParser configParser;
    private RemedyEntryEventAdapter remedyEntryEventAdapter;

    public RemedyIncidentReader(ConfigParser configParser) {
        this.configParser = configParser;
        this.arServerContext = createARServerContext(this.configParser.getConfiguration());
    }

    private ARServerUser createARServerContext(Configuration config) {
        this.arServerContext = new ARServerUser();
        arServerContext.setServer(config.getRemedyHostName());
        arServerContext.setUser(config.getRemedyUserName());
        arServerContext.setPassword(config.getRemedyPassword());
        return arServerContext;
    }

    public void login() throws RemedyLoginFailedException {
        try {
            arServerContext.login();
            log.info("Login successful to remedy server");
        } catch (ARException e1) {
            throw new RemedyLoginFailedException(StringUtils.format(Constants.REMEDY_LOGIN_FAILED, new Object[]{e1.getMessage()}));
        }

    }

    public List<Payload> readIncidents(int startFrom, int chunkSize, OutputInteger nMatches) {

        //keeping as set to avoid duplicates
        Set<Integer> fieldsList = new HashSet<>();
        configParser.getFieldItemMap().values().forEach(fieldItem -> {
            fieldsList.add(fieldItem.getFieldId());
        });

        int[] queryFieldsList = new int[fieldsList.size()];
        int index = 0;
        for (Integer i : fieldsList) {
            queryFieldsList[index++] = i;
        }

        Date newDate = new Date();

        QualifierInfo qualInfo1 = buildFieldValueQualification(Constants.CLOSE_DATE_FIELD,
                new Value(new Timestamp(configParser.getConfiguration().getStartDateTime()), DataType.TIME), RelationalOperationInfo.AR_REL_OP_GREATER_EQUAL);

        QualifierInfo qualInfo2 = buildFieldValueQualification(Constants.CLOSE_DATE_FIELD,
                new Value(new Timestamp(configParser.getConfiguration().getEndDateTime()), DataType.TIME), RelationalOperationInfo.AR_REL_OP_LESS_EQUAL);

        QualifierInfo qualInfo = new QualifierInfo(QualifierInfo.AR_COND_OP_AND, qualInfo1, qualInfo2);

        List<SortInfo> sortOrder = new ArrayList<SortInfo>();
        List<Entry> entryList = new ArrayList<>();
        boolean isSuccessful = false;
        int retryCount = 0;
        while (!isSuccessful && retryCount <= configParser.getConfiguration().getRetryConfig()) {
            try {
                entryList = arServerContext.getListEntryObjects(Constants.HELP_DESK_FORM, qualInfo,
                        startFrom, chunkSize, sortOrder, queryFieldsList, false, nMatches);
                isSuccessful = true;
            } catch (ARException e) {
                if (retryCount < configParser.getConfiguration().getRetryConfig()) {
                    retryCount++;
                    log.error("Reading Incidents for starting : {}, chunk size {} resulted into exception, Re-trying for {} time", new Object[]{startFrom, chunkSize, retryCount});
                    continue;
                } else {
                    log.error("Skipping the read process, Reading Incidents Failed for starting : {}, chunk size {} even after retrying for {} times", new Object[]{startFrom, chunkSize, retryCount});
                    break;
                }
            }
            log.info("Recieved {} incidents in {} retry  for starting : {}, chunk size {}  ", new Object[]{entryList.size(), retryCount, startFrom, chunkSize});
        }
        List<Payload> payloadList = new ArrayList<>();
        entryList.forEach(entry -> {
            payloadList.add(remedyEntryEventAdapter.convertIncidentEntryToPayload(configParser, entry));
        });
        return payloadList;
    }

    /**
     * Prepare qualification "<fieldId>=<Value>"
     *
     * @return QualifierInfo
     */
    private QualifierInfo buildFieldValueQualification(int fieldId, Value value, int relationalOperation) {
        ArithmeticOrRelationalOperand leftOperand = new ArithmeticOrRelationalOperand(fieldId);
        ArithmeticOrRelationalOperand rightOperand = new ArithmeticOrRelationalOperand(value);
        RelationalOperationInfo relationalOperationInfo = new RelationalOperationInfo(relationalOperation, leftOperand,
                rightOperand);
        QualifierInfo qualification = new QualifierInfo(relationalOperationInfo);
        return qualification;
    }

    public void logout() {
        arServerContext.logout();
        log.info("Logout successful from remedy server");

    }
}
