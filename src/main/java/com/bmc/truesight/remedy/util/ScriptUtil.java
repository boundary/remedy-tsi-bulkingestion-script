package com.bmc.truesight.remedy.util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.arsys.api.ARServerUser;
import com.bmc.arsys.api.Field;
import com.bmc.arsys.api.OutputInteger;
import com.bmc.truesight.remedy.App;
import com.bmc.truesight.saas.remedy.integration.ARServerForm;
import com.bmc.truesight.saas.remedy.integration.RemedyReader;
import com.bmc.truesight.saas.remedy.integration.TemplateParser;
import com.bmc.truesight.saas.remedy.integration.TemplatePreParser;
import com.bmc.truesight.saas.remedy.integration.TemplateValidator;
import com.bmc.truesight.saas.remedy.integration.adapter.RemedyEntryEventAdapter;
import com.bmc.truesight.saas.remedy.integration.beans.Configuration;
import com.bmc.truesight.saas.remedy.integration.beans.Template;
import com.bmc.truesight.saas.remedy.integration.exception.ParsingException;
import com.bmc.truesight.saas.remedy.integration.exception.RemedyLoginFailedException;
import com.bmc.truesight.saas.remedy.integration.exception.RemedyReadFailedException;
import com.bmc.truesight.saas.remedy.integration.exception.ValidationException;
import com.bmc.truesight.saas.remedy.integration.impl.GenericRemedyReader;
import com.bmc.truesight.saas.remedy.integration.impl.GenericTemplateParser;
import com.bmc.truesight.saas.remedy.integration.impl.GenericTemplatePreParser;
import com.bmc.truesight.saas.remedy.integration.impl.GenericTemplateValidator;

public class ScriptUtil {

    private final static Logger log = LoggerFactory.getLogger(ScriptUtil.class);

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    public static String dateToString(Date date) {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        return DATE_FORMAT.format(date);
    }

    public static int getAvailableRecordsCount(ARServerUser context, ARServerForm form, Template template, RemedyEntryEventAdapter adapter) throws RemedyReadFailedException {
        OutputInteger totalCount = new OutputInteger();
        log.debug("Read call made to get available count");
        new GenericRemedyReader().readRemedyTickets(context, form, template, 1, 1, totalCount, adapter);
        return totalCount.intValue();
    }

    public static Template prepareTemplate(ARServerForm form) throws ParsingException, ValidationException, IOException, RemedyLoginFailedException, RemedyReadFailedException {
        String path = null;
        String name = "";

        path = new java.io.File(".").getCanonicalPath();
        if (form.equals(ARServerForm.INCIDENT_FORM)) {
            path += Constants.INCIDENT_TEMPLATE_PATH;
            name = "Incidents";
        } else if (form.equals(ARServerForm.CHANGE_FORM)) {
            path += Constants.CHANGE_TEMPLATE_PATH;
            name = "Changes";
        }

        // PARSING THE CONFIGURATION FILE
        Template template = null;
        TemplatePreParser preParser = new GenericTemplatePreParser();
        TemplateParser parser = new GenericTemplateParser();
        Template defaultTemplate = preParser.loadDefaults(form);
        log.debug("{} defaults loading sucessfuly finished , default status configured to query is {}", name, defaultTemplate.getConfig().getQueryStatusList());
        template = parser.readParseConfigFile(defaultTemplate, path);
        log.debug("{} user template configuration parsing successful , status configured to be queried is {}", name, template.getConfig().getQueryStatusList());
        Configuration config = template.getConfig();
        RemedyReader reader = new GenericRemedyReader();
        ARServerUser user = reader.createARServerContext(config.getRemedyHostName(), config.getRemedyPort(), config.getRemedyUserName(), config.getRemedyPassword());
        if (form.equals(ARServerForm.INCIDENT_FORM)) {
            App.setHasLoggedIntoRemedyIncident(reader.login(user));
            App.setIncidentUser(user);
        } else if (form.equals(ARServerForm.CHANGE_FORM)) {
            App.setHasLoggedIntoRemedyChange(reader.login(user));
            App.setChangeUser(user);
        }
        Map<Integer, Field> fieldmap = reader.getFieldsMap(user, form);
        // VALIDATION OF THE CONFIGURATION
        TemplateValidator validator = new GenericTemplateValidator(fieldmap);
        validator.validate(template);
        if (form.equals(ARServerForm.INCIDENT_FORM)) {
            App.setIncidentFieldIdMap(fieldmap);
        } else if (form.equals(ARServerForm.CHANGE_FORM)) {
            App.setChangeFieldIdMap(fieldmap);
        }

        return template;
    }

    public static String getEventTypeByFormNameCaps(ARServerForm form) {
        if (form.equals(ARServerForm.INCIDENT_FORM)) {
            return "Incident";
        } else if (form.equals(ARServerForm.CHANGE_FORM)) {
            return "Change";
        } else {
            return "";
        }
    }

    public static String getEventTypeByFormNameSmall(ARServerForm form) {
        if (form.equals(ARServerForm.INCIDENT_FORM)) {
            return "incident";
        } else if (form.equals(ARServerForm.CHANGE_FORM)) {
            return "change";
        } else {
            return "";
        }
    }
}
