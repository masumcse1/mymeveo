package org.meveo.service.notification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.mail.Session;

import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.MailerSessionFactory;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.notification.InstantMessagingNotification;
import org.meveo.model.notification.NotificationHistoryStatusEnum;
import org.meveo.security.MeveoUser;
import org.meveo.security.keycloak.CurrentUserProvider;
import org.meveo.service.base.MeveoValueExpressionWrapper;
import org.slf4j.Logger;

import com.skype.Skype;

@Stateless
public class InstantMessagingNotifier {

    @Inject
    Logger log;

    private Session mailSession;

    @Inject
    private MailerSessionFactory mailerSessionFactory;

    @Inject
    NotificationHistoryService notificationHistoryService;
    

    @Inject
    private CurrentUserProvider currentUserProvider;

    @PostConstruct
    public void init() {
        mailSession = mailerSessionFactory.getSession();
    }

    // Jabber jabber = new Jabber();

    /**
     * Send instant message as fired notification result
     * 
     * @param notification Instant message type notification that was fired
     * @param entityOrEvent Entity or event that triggered notification
     * @param lastCurrentUser Current user. In case of multitenancy, when user authentication is forced as result of a fired trigger (scheduled jobs, other timed event
     *        expirations), current user might be lost, thus there is a need to reestablish.
     */
    @Asynchronous
    public void sendInstantMessage(InstantMessagingNotification notification, Object entityOrEvent, MeveoUser lastCurrentUser) {
        

        currentUserProvider.reestablishAuthentication(lastCurrentUser);
        
        try {
            HashMap<Object, Object> userMap = new HashMap<Object, Object>();
            userMap.put("event", entityOrEvent);
            Set<String> imIdSet = notification.getIds();
            if (imIdSet == null) {
                imIdSet = new HashSet<String>();
            }
            if (!StringUtils.isBlank(notification.getIdEl())) {
                imIdSet.add((String) MeveoValueExpressionWrapper.evaluateExpression(notification.getIdEl(), userMap, String.class));
            }
            String message = (String) MeveoValueExpressionWrapper.evaluateExpression(notification.getMessage(), userMap, String.class);

            switch (notification.getImProvider()) {
            case SKYPE:
                for (String imId : imIdSet) {
                    log.debug("send skype message to {}, mess={}", imId, message);
                    Skype.chat(imId).send(message);
                }
                break;
            case FACEBOOK:
                break;
            case GTALK:

                break;
            case TWITTER:
                break;
            case YAHOO_MESSENGER:
                break;
            }
            notificationHistoryService.create(notification, entityOrEvent, "", NotificationHistoryStatusEnum.SENT);

        } catch (Exception e) {
            try {
                notificationHistoryService.create(notification, entityOrEvent, e.getMessage(), NotificationHistoryStatusEnum.FAILED);
            } catch (BusinessException e2) {
                log.error("Failed to create notification history", e2);
            }
        }
    }
}
