package org.meveo.admin.action.notification;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.RejectedImportException;
import org.meveo.commons.utils.CsvBuilder;
import org.meveo.commons.utils.CsvReader;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.catalog.CounterTemplate;
import org.meveo.model.notification.EmailNotification;
import org.meveo.model.notification.NotificationEventTypeEnum;
import org.meveo.model.notification.StrategyImportTypeEnum;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.catalog.impl.CounterTemplateService;
import org.meveo.service.notification.EmailNotificationService;
import org.meveo.service.script.ScriptInstanceService;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 * Standard backing bean for {@link EmailNotification} (extends {@link BaseBean} that provides almost all common methods to handle entities filtering/sorting in datatable, their
 * create, edit, view, delete operations). It works with Manaty custom JSF components.
 * 
 * @author Wassim Drira
 * @lastModifiedVersion 5.0
 * 
 */
@Named
@ViewScoped
public class EmailNotificationBean extends BaseNotificationBean<EmailNotification> {

    private static final long serialVersionUID = 6473465285480945644L;

    @Inject
    private EmailNotificationService emailNotificationService;

    /** paramBean Factory allows to get application scope paramBean or provider specific paramBean */
    @Inject
    private ParamBeanFactory paramBeanFactory;

    @Inject
    CounterTemplateService counterTemplateService;

    @Inject
    ScriptInstanceService scriptInstanceService;

    CsvBuilder csv = null;
    private String existingEntitiesCsvFile = null;

    CsvReader csvReader = null;
    private UploadedFile file;

    private StrategyImportTypeEnum strategyImportType;

    private static final int CODE = 0;
    private static final int CLASS_NAME_FILTER = 1;
    private static final int EVENT_TYPE_FILTER = 2;
    private static final int SENT_FROM = 3;
    private static final int SEND_TO_EL = 4;
    private static final int SUBJECT = 5;
    private static final int ACTIVE = 6;
    private static final int SCRIPT_INSTANCE_CODE = 7;
    private static final int SEND_TO_MAILING_LIST = 8;
    private static final int EL_FILTER = 9;
    private static final int TEXT_BODY = 10;
    private static final int HTML_BODY = 11;
    private static final int COUNTER_TEMPLATE = 12;

    public EmailNotificationBean() {
        super(EmailNotification.class);
    }

    @Override
    protected IPersistenceService<EmailNotification> getPersistenceService() {
        return emailNotificationService;
    }

    public void handleFileUpload(FileUploadEvent event) throws Exception {
        try {
            file = event.getFile();
            log.debug("File uploaded " + file.getFileName());
            upload();
            messages.info(new BundleKey("messages", "import.csv.successful"));
        } catch (BusinessException e) {
            log.error("Failed to handle uploaded file {}", event.getFile().getFileName(), e);
            messages.error(new BundleKey("messages", "import.csv.failed"), e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }

    private void upload() throws IOException, BusinessException {
        if (file == null) {
            return;
        }
        csvReader = new CsvReader(file.getInputstream(), ';', Charset.forName("ISO-8859-1"));
        csvReader.readHeaders();
        String existingEntitiesCSV = paramBeanFactory.getInstance().getProperty("existingEntities.csv.dir", "existingEntitiesCSV");
        File dir = new File(paramBeanFactory.getChrootDir() + File.separator + existingEntitiesCSV);
        dir.mkdirs();
        existingEntitiesCsvFile = dir.getAbsolutePath() + File.separator + "EmailNotifications_" + new SimpleDateFormat("ddMMyyyyHHmmSS").format(new Date()) + ".csv";
        csv = new CsvBuilder();
        boolean isEntityAlreadyExist = false;
        while (csvReader.readRecord()) {
            String[] values = csvReader.getValues();
            EmailNotification existingEntity = emailNotificationService.findByCode(values[CODE]);
            if (existingEntity != null) {
                checkSelectedStrategy(values, existingEntity, isEntityAlreadyExist);
                isEntityAlreadyExist = true;
            } else {
                EmailNotification emailNotif = new EmailNotification();
                emailNotif.setCode(values[CODE]);
                emailNotif.setClassNameFilter(values[CLASS_NAME_FILTER]);
                emailNotif.setEventTypeFilter(NotificationEventTypeEnum.valueOf(values[EVENT_TYPE_FILTER]));
                // emailNotif.setElFilter(values[EL_FILTER]);
                emailNotif.setDisabled(Boolean.parseBoolean(values[ACTIVE]));
//                if (!StringUtils.isBlank(values[SCRIPT_INSTANCE_CODE])) {
//                    ScriptInstance scriptInstance = scriptInstanceService.findByCode(values[SCRIPT_INSTANCE_CODE]);
//                    emailNotif.setFunction(scriptInstance);
//                }
                emailNotif.setEmailFrom(values[SENT_FROM]);
                emailNotif.setEmailToEl(values[SEND_TO_EL]);
//                String emails = values[SEND_TO_MAILING_LIST].replace("[", "").replace("]", "");
//                if (!StringUtils.isBlank(emails)) {
//                    String[] emailList = emails.split(",");
//                    List<String> listMail = Arrays.asList(emailList);
//                    for (String email : listMail) {
//                        email = email.trim();
//                        if (emailNotif.getEmails() == null) {
//                            emailNotif.setEmails(new HashSet<>());
//                        }
//                        emailNotif.getEmails().add(email);
//                    }
//                }
                emailNotif.setSubject(values[SUBJECT]);
//                emailNotif.setBody(values[TEXT_BODY]);
//                emailNotif.setHtmlBody(values[HTML_BODY]);
//                if (!StringUtils.isBlank(values[COUNTER_TEMPLATE])) {
//                    CounterTemplate counterTemplate = counterTemplateService.findByCode(values[COUNTER_TEMPLATE]);
//                    emailNotif.setCounterTemplate(counterTemplate != null ? counterTemplate : null);
//                }

                emailNotificationService.create(emailNotif);
            }
        }
        if (isEntityAlreadyExist && StrategyImportTypeEnum.REJECT_EXISTING_RECORDS.equals(strategyImportType)) {
            csv.writeFile(csv.toString().getBytes(), existingEntitiesCsvFile);
        }
    }

    public void checkSelectedStrategy(String[] values, EmailNotification existingEntity, boolean isEntityAlreadyExist) throws BusinessException {
        if (StrategyImportTypeEnum.UPDATED.equals(strategyImportType)) {
            existingEntity.setClassNameFilter(values[CLASS_NAME_FILTER]);
            existingEntity.setEventTypeFilter(NotificationEventTypeEnum.valueOf(values[EVENT_TYPE_FILTER]));
//            existingEntity.setElFilter(values[EL_FILTER]);
            existingEntity.setDisabled(Boolean.parseBoolean(values[ACTIVE]));
//            if (!StringUtils.isBlank(values[SCRIPT_INSTANCE_CODE])) {
//                ScriptInstance scriptInstance = scriptInstanceService.findByCode(values[SCRIPT_INSTANCE_CODE]);
//                existingEntity.setFunction(scriptInstance);
//            }
            existingEntity.setEmailFrom(values[SENT_FROM]);
            existingEntity.setEmailToEl(values[SEND_TO_EL]);
//            String emails = values[SEND_TO_MAILING_LIST].replace("[", "").replace("]", "");
//            if (!StringUtils.isBlank(emails)) {
//                String[] emailList = emails.split(",");
//                List<String> listMail = Arrays.asList(emailList);
//                for (String email : listMail) {
//                    email = email.trim();
//                    if (existingEntity.getEmails() == null) {
//                        Set<String> setEmail = new HashSet<>();
//                        existingEntity.setEmails(setEmail);
//                    }
//                    existingEntity.getEmails().add(email);
//                }
//            }
            existingEntity.setSubject(values[SUBJECT]);
//            existingEntity.setBody(values[TEXT_BODY]);
//            existingEntity.setHtmlBody(values[HTML_BODY]);
//            if (!StringUtils.isBlank(values[COUNTER_TEMPLATE])) {
//                CounterTemplate counterTemplate = counterTemplateService.findByCode(values[COUNTER_TEMPLATE]);
//                existingEntity.setCounterTemplate(counterTemplate != null ? counterTemplate : null);
//            }
            emailNotificationService.update(existingEntity);
        } else if (StrategyImportTypeEnum.REJECTE_IMPORT.equals(strategyImportType)) {
            throw new RejectedImportException("notification.rejectImport");
        } else if (StrategyImportTypeEnum.REJECT_EXISTING_RECORDS.equals(strategyImportType)) {
            if (!isEntityAlreadyExist) {
                csv.appendValue("Code");
                csv.appendValue("Classename filter");
                csv.appendValue("Event type filter");
                csv.appendValue("El filter");
                csv.appendValue("Active");
                csv.appendValue("Script instance code");
                csv.appendValue("Sent from");
                csv.appendValue("Send to EL");
                csv.appendValue("Send to mailing list");
                csv.appendValue("Subject");
                csv.appendValue("Text body");
                csv.appendValue("HTML body");
                csv.appendValue("Counter template");
            }
            csv.startNewLine();
            csv.appendValue(values[CODE]);
            csv.appendValue(values[CLASS_NAME_FILTER]);
            csv.appendValue(values[EVENT_TYPE_FILTER]);
//            csv.appendValue(values[EL_FILTER]);
            csv.appendValue(values[ACTIVE]);
            // csv.appendValue(values[SCRIPT_INSTANCE_CODE]);
            csv.appendValue(values[SENT_FROM]);
            csv.appendValue(values[SEND_TO_EL]);
//            csv.appendValue(values[SEND_TO_MAILING_LIST]);
            csv.appendValue(values[SUBJECT]);
//            csv.appendValue(values[TEXT_BODY]);
//            csv.appendValue(values[HTML_BODY]);
//            csv.appendValue(values[COUNTER_TEMPLATE]);
        }
    }

    public StrategyImportTypeEnum getStrategyImportType() {
        return strategyImportType;
    }

    public void setStrategyImportType(StrategyImportTypeEnum strategyImportType) {
        this.strategyImportType = strategyImportType;
    }
}