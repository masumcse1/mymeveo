package org.meveo.admin.action.notification;

import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.action.UpdateMapTypeFieldBean;
import org.meveo.commons.utils.ReflectionUtils;
import org.meveo.model.NotifiableEntity;
import org.meveo.model.ObservableEntity;
import org.meveo.model.billing.CounterPeriod;
import org.meveo.model.mediation.MeveoFtpFile;
import org.meveo.model.module.MeveoModule;
import org.meveo.model.notification.InboundRequest;
import org.meveo.model.notification.Notification;
import org.meveo.model.notification.NotificationEventTypeEnum;
import org.meveo.service.notification.DefaultObserver;

import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Tyshan　Shi(tyshan@manaty.net)
 * @since Aug 11, 2016 11:02:44 AM
 **/
public abstract class BaseNotificationBean<T extends Notification>  extends UpdateMapTypeFieldBean<T>{

	private static final long serialVersionUID = 1L;
	
	public BaseNotificationBean(){
	}
	
	public BaseNotificationBean(Class<T> clazz){
		super(clazz);
	}

	/**
     * Autocomplete method for class filter field - search entity type classes with {@link ObservableEntity} or {@link NotifiableEntity} annotation
     * 
     * @param query A partial class name (including a package)
     * @return A list of classnames
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<String> autocompleteClassNames(String query) {

        List<Class> classes = null;
        try {
            classes = ReflectionUtils.getClasses("org.meveo.model");
        } catch (Exception e) {
            log.error("Failed to get a list of classes for a model package", e);
            return null;
        }

        String queryLc = query.toLowerCase();
        List<String> classNames = new ArrayList<String>();
        for (Class clazz : classes) {
            if (((clazz.isAnnotationPresent(Entity.class) && clazz.isAnnotationPresent(ObservableEntity.class))||clazz.isAnnotationPresent(NotifiableEntity.class)) && clazz.getName().toLowerCase().contains(queryLc)) {
                classNames.add(clazz.getName());
            }
        }

        Collections.sort(classNames);
        return classNames;
    }
    /**
     * filter the event type of the notification by class
     */
    public List<NotificationEventTypeEnum> getNotificationEventTypeFilters(){
        String clazzStr = getEntity().getClassNameFilter();
        if (StringUtils.isBlank(clazzStr)) {
            return null;
        }
        return getEventTypesByClazz(clazzStr);
    }

    /**
     * get notification eventType by class name filter, refer for {@link NotificationEventTypeEnum} and {@link DefaultObserver}
     *
     * @param clazzStr
     * @return
     */
    private List<NotificationEventTypeEnum> getEventTypesByClazz(String clazzStr) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(clazzStr);
        } catch (Exception e) {
            return null;
        }

        List<NotificationEventTypeEnum> events = new ArrayList<NotificationEventTypeEnum>();
        if (hasObservableEntity(clazz)) {
            events.addAll(Arrays.asList(NotificationEventTypeEnum.CREATED, NotificationEventTypeEnum.UPDATED, NotificationEventTypeEnum.REMOVED, NotificationEventTypeEnum.DISABLED,
                NotificationEventTypeEnum.ENABLED));
            if (clazzStr.equals(org.meveo.model.admin.User.class.getName())) {
                events.add(NotificationEventTypeEnum.LOGGED_IN);
            } else if (clazzStr.equals(InboundRequest.class.getName())) {
                events.add(NotificationEventTypeEnum.INBOUND_REQ);
            } else if (clazzStr.equals(CounterPeriod.class.getName())) {
                events.add(NotificationEventTypeEnum.COUNTER_DEDUCED);
            } else if (clazzStr.equals(MeveoModule.class.getName())) {
                events.addAll(Arrays.asList(NotificationEventTypeEnum.INSTALL, NotificationEventTypeEnum.POST_INSTALL));
            }
        } else if (hasNotificableEntity(clazz)) {
            if (clazzStr.equals(MeveoFtpFile.class.getName())) {
                events = Arrays.asList(NotificationEventTypeEnum.FILE_UPLOAD, NotificationEventTypeEnum.FILE_DOWNLOAD, NotificationEventTypeEnum.FILE_DELETE,
                    NotificationEventTypeEnum.FILE_RENAME);
            }
        } else if (clazzStr.equals("org.meveo.service.neo4j.graph.Neo4jEntity") || clazzStr.equals("org.meveo.service.neo4j.graph.Neo4jRelationship")) {
            events.addAll(Arrays.asList(NotificationEventTypeEnum.CREATED, NotificationEventTypeEnum.UPDATED));
        }
		return events;
	}
	
	private static boolean hasObservableEntity(Class<?> clazz){
    	return clazz.isAnnotationPresent(Entity.class)&&clazz.isAnnotationPresent(ObservableEntity.class);
    }
	private static boolean hasNotificableEntity(Class<?> clazz){
    	return clazz.isAnnotationPresent(NotifiableEntity.class);
    }
}

