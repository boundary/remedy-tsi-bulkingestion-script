package com.bmc.truesight.remedy.util;

import java.util.ArrayList;
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

/**
 * This Class is responsible for reading form entries from the Remedy AR server
 * Using Java Api. It takes Configuration & a formName (ex HPD:Help Desk) as
 * constructor Arg
 *
 * @author vitiwari
 */
public class RemedyReader {

    private static final Logger log = LoggerFactory.getLogger(RemedyReader.class);

    private ARServerUser arServerContext;
    private ConfigParser configParser;
    private RemedyEntryEventAdapter remedyEntryEventAdapter;
    private String formName;

    public RemedyReader(ConfigParser configParser, String formName) {
        this.configParser = configParser;
        this.arServerContext = createARServerContext(this.configParser.getConfiguration());
        this.remedyEntryEventAdapter = new RemedyEntryEventAdapter();
        this.formName = formName;
    }

    private ARServerUser createARServerContext(Configuration config) {
        this.arServerContext = new ARServerUser();
        arServerContext.setServer(config.getRemedyHostName());
        arServerContext.setUser(config.getRemedyUserName());
        arServerContext.setPassword(config.getRemedyPassword());
        return arServerContext;
    }

    public boolean login() throws RemedyLoginFailedException {
        try {
            arServerContext.login();
            log.info("Login successful to remedy server");
        } catch (ARException e1) {
            throw new RemedyLoginFailedException(StringUtil.format(Constants.REMEDY_LOGIN_FAILED, new Object[]{e1.getMessage()}));
        }
        return true;
    }

    public List<Payload> readRemedyTickets(int startFrom, int chunkSize, OutputInteger nMatches) {

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

        QualifierInfo qualInfoF = null;
        for (int fieldId : configParser.getConfiguration().getConditionFields()) {
            QualifierInfo qualInfo1 = buildFieldValueQualification(fieldId,
                    new Value(new Timestamp(configParser.getConfiguration().getStartDateTime()), DataType.TIME), RelationalOperationInfo.AR_REL_OP_GREATER_EQUAL);

            QualifierInfo qualInfo2 = buildFieldValueQualification(fieldId,
                    new Value(new Timestamp(configParser.getConfiguration().getEndDateTime()), DataType.TIME), RelationalOperationInfo.AR_REL_OP_LESS_EQUAL);

            QualifierInfo qualInfo = new QualifierInfo(QualifierInfo.AR_COND_OP_AND, qualInfo1, qualInfo2);

            if (qualInfoF != null) {
                qualInfoF = new QualifierInfo(QualifierInfo.AR_COND_OP_OR, qualInfoF, qualInfo);
            } else {
                qualInfoF = qualInfo;
            }

        }

        List<SortInfo> sortOrder = new ArrayList<SortInfo>();
        List<Entry> entryList = new ArrayList<>();
        boolean isSuccessful = false;
        int retryCount = 0;
        while (!isSuccessful && retryCount <= configParser.getConfiguration().getRetryConfig()) {
            try {
                entryList = arServerContext.getListEntryObjects(this.formName, qualInfoF,
                        startFrom, chunkSize, sortOrder, queryFieldsList, false, nMatches);
                isSuccessful = true;
                log.info("Recieved {} tickets  for starting index : {}, chunk size {}  ", new Object[]{entryList.size(), startFrom, chunkSize});
            } catch (ARException e) {
                if (retryCount < configParser.getConfiguration().getRetryConfig()) {
                    retryCount++;
                    log.error("Reading  {} tickets from {} resulted into exception[{}], Re-trying for {} time", new Object[]{chunkSize, startFrom, e.getMessage(), retryCount});
                    try {
                        log.error("Waiting for {} sec before trying again ......", (configParser.getConfiguration().getWaitMsBeforeRetry() / 1000));
                        Thread.sleep(configParser.getConfiguration().getWaitMsBeforeRetry());
                    } catch (InterruptedException e1) {
                    }

                    continue;
                } else {
                    log.error("Skipping the read process, Reading tickets Failed for starting : {}, chunk size {} even after retrying for {} times", new Object[]{startFrom, chunkSize, retryCount});
                    break;
                }
            }
        }
        List<Payload> payloadList = new ArrayList<Payload>();
        if (this.formName.equalsIgnoreCase(Constants.HELP_DESK_FORM)) {
            entryList.forEach(entry -> {
                payloadList.add(remedyEntryEventAdapter.convertIncidentEntryToPayload(configParser, entry));
            });
        } else if (this.formName.equalsIgnoreCase(Constants.CHANGE_FORM)) {
            entryList.forEach(entry -> {
                payloadList.add(remedyEntryEventAdapter.convertChangeEntryToPayload(configParser, entry));
            });
        }
        return payloadList;
    }

    /**
     * Prepare qualification
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
