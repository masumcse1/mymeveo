package org.meveo.service.notification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.meveo.cache.NotificationCacheContainerProvider;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.commons.utils.ReflectionUtils;
import org.meveo.event.IEvent;
import org.meveo.model.AuditableEntity;
import org.meveo.model.BaseEntity;
import org.meveo.model.BusinessCFEntity;
import org.meveo.model.BusinessEntity;
import org.meveo.model.IEntity;
import org.meveo.model.notification.Notification;
import org.meveo.model.notification.NotificationEventTypeEnum;
import org.meveo.service.base.BusinessService;

/**
 * @author Tyshan Shi(tyshan@manaty.net)
 *
 **/
@Stateless
public class GenericNotificationService extends BusinessService<Notification> {

    @Inject
    private NotificationCacheContainerProvider notificationCacheContainerProvider;

    /** paramBeanFactory */
    @Inject
    private ParamBeanFactory paramBeanFactory;

    static boolean useNotificationCache = true;

    @PostConstruct
    private void init() {
        useNotificationCache = Boolean.parseBoolean(paramBeanFactory.getInstance().getProperty("cache.cacheNotification", "true"));
    }
    
    /**
     * Refresh the notification cache
     */
    public void refreshCache() {
    	if(useNotificationCache) {
    		notificationCacheContainerProvider.refreshCache(null);
    	}
    }

    /**
     * Get a list of notifications that match event type and entity class. Notifications are looked up from cache or retrieved from DB.
     * 
     * @param eventType Event type
     * @param entityOrEvent Entity involved or event containing the entity involved
     * @return A list of notifications
     */
    public List<Notification> getApplicableNotifications(NotificationEventTypeEnum eventType, Object entityOrEvent) {

        if (useNotificationCache) {

            List<Notification> notifications = notificationCacheContainerProvider.getApplicableNotifications(eventType, entityOrEvent);

            // Populate cache if no record was found in cache
            if (notifications == null || notifications.isEmpty()) {

                notifications = getApplicableNotificationsNoCache(eventType, entityOrEvent);
                if (notifications.isEmpty()) {
                    notificationCacheContainerProvider.markNoNotifications(eventType, entityOrEvent);
                } else {
                    notifications.forEach((notification) -> notificationCacheContainerProvider.addNotificationToCache(notification));
                }
            }
            List<Notification> notificationList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(notifications)) {
                for (Notification notification : notifications) {
                    if (notification.getEventTypeFilter().equals(eventType)) {
                        notificationList.add(notification);
                    }
                }
            }

            return notificationList;

        } else {
            return getApplicableNotificationsNoCache(eventType, entityOrEvent);
        }
    }

    /**
     * Get a list of notifications that match event type and entity class - always do a lookup in DB
     * 
     * @param eventType Event type
     * @param entityOrEvent Entity involved or event containing the entity involved
     * @return A list of notifications
     */
    @SuppressWarnings("unchecked")
    public List<Notification> getApplicableNotificationsNoCache(NotificationEventTypeEnum eventType, Object entityOrEvent) {

        Object entity = null;
        if (entityOrEvent instanceof IEntity) {
            entity = entityOrEvent;
        } else if (entityOrEvent instanceof IEvent) {
            entity = ((IEvent) entityOrEvent).getEntity();
        } else {
            entity = entityOrEvent;
        }

        @SuppressWarnings("rawtypes")
        Class entityClass = entity.getClass();

        List<String> classNames = new ArrayList<>();

        while (!entityClass.isAssignableFrom(BusinessCFEntity.class) && !entityClass.isAssignableFrom(BusinessEntity.class) && !entityClass.isAssignableFrom(BaseEntity.class)
                && !entityClass.isAssignableFrom(AuditableEntity.class) && !entityClass.isAssignableFrom(Object.class)) {

            classNames.add(ReflectionUtils.getCleanClassName(entityClass.getName()));
            List<String> interfaces = Arrays.stream(entityClass.getInterfaces())
                    .map(Class::getCanonicalName)
                    .collect(Collectors.toList());

            classNames.addAll(interfaces);
            entityClass = entityClass.getSuperclass();
        }

        return getEntityManager().createNamedQuery("Notification.getActiveNotificationsByEventAndClasses", Notification.class)
                .setParameter("eventTypeFilter", eventType)
                .setParameter("classNameFilter", classNames)
                .getResultList();
    }
}
