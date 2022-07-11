package org.meveo.service.notification;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.ftp.event.*;
import org.meveo.audit.logging.annotations.MeveoAudit;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.commons.utils.StringUtils;
import org.meveo.elresolver.ELException;
import org.meveo.event.CFEndPeriodEvent;
import org.meveo.event.CounterPeriodEvent;
import org.meveo.event.IEvent;
import org.meveo.event.communication.InboundCommunicationEvent;
import org.meveo.event.logging.LoggedEvent;
import org.meveo.event.monitoring.BusinessExceptionEvent;
import org.meveo.event.qualifier.Created;
import org.meveo.event.qualifier.CreatedAfterTx;
import org.meveo.event.qualifier.Disabled;
import org.meveo.event.qualifier.Enabled;
import org.meveo.event.qualifier.InboundRequestReceived;
import org.meveo.event.qualifier.Processed;
import org.meveo.event.qualifier.Rejected;
import org.meveo.event.qualifier.Removed;
import org.meveo.event.qualifier.PostRemoved;
import org.meveo.event.qualifier.Terminated;
import org.meveo.event.qualifier.Updated;
import org.meveo.event.qualifier.UpdatedAfterTx;
import org.meveo.event.qualifier.git.CommitEvent;
import org.meveo.exceptions.EntityDoesNotExistsException;
import org.meveo.model.BaseEntity;
import org.meveo.model.IEntity;
import org.meveo.model.ModuleInstall;
import org.meveo.model.ModulePostInstall;
import org.meveo.model.NotifiableEntity;
import org.meveo.model.ObservableEntity;
import org.meveo.model.admin.User;
import org.meveo.model.mediation.MeveoFtpFile;
import org.meveo.model.module.MeveoModule;
import org.meveo.model.notification.EmailNotification;
import org.meveo.model.notification.InboundRequest;
import org.meveo.model.notification.InstantMessagingNotification;
import org.meveo.model.notification.JobTrigger;
import org.meveo.model.notification.Notification;
import org.meveo.model.notification.NotificationEventTypeEnum;
import org.meveo.model.notification.NotificationHistory;
import org.meveo.model.notification.NotificationHistoryStatusEnum;
import org.meveo.model.notification.ScriptNotification;
import org.meveo.model.notification.WebHook;
import org.meveo.model.notification.WebNotification;
import org.meveo.model.scripts.Function;
import org.meveo.persistence.neo4j.graph.Neo4jEntity;
import org.meveo.persistence.neo4j.graph.Neo4jRelationship;
import org.meveo.security.CurrentUser;
import org.meveo.security.MeveoUser;
import org.meveo.service.base.MeveoValueExpressionWrapper;
import org.meveo.service.billing.impl.CounterInstanceService;
import org.meveo.service.billing.impl.CounterValueInsufficientException;
import org.meveo.service.script.ConcreteFunctionService;
import org.meveo.service.script.Script;
import org.slf4j.Logger;

/**
 * Default observer for all the events.
 * 
 * @author Edward P. Legaspi | edward.legaspi@manaty.net
 * @version 6.11
 */
@Singleton
@Startup
@LoggedEvent
@Lock(LockType.READ)
public class DefaultObserver {

	@Inject
	private Logger log;
	
	@Inject
	private BeanManager manager;

	@Inject
	private NotificationHistoryService notificationHistoryService;

	@Inject
	private EmailNotifier emailNotifier;

	@Inject
	private WebHookNotifier webHookNotifier;

	@Inject
	private WebNotifier webNotifier;

	@Inject
	private InstantMessagingNotifier imNotifier;

	@Inject
	private CounterInstanceService counterInstanceService;

	@Inject
	private GenericNotificationService genericNotificationService;

	@Inject
	private ConcreteFunctionService concreteFunctionService;

	@Inject
	private JobTriggerLauncher jobTriggerLauncher;

	@Inject
	@CurrentUser
	protected MeveoUser currentUser;
	
	@Inject
	private ParamBeanFactory paramBeanFactory;

	private boolean matchExpression(String expression, Object entityOrEvent) throws BusinessException, ELException {
		Boolean result = true;
		if (StringUtils.isBlank(expression)) {
			return result;
		}
		Map<Object, Object> userMap = new HashMap<Object, Object>();
		userMap.put("event", entityOrEvent);

		Object res = MeveoValueExpressionWrapper.evaluateExpression(expression, userMap, Boolean.class);
		try {
			result = (Boolean) res;
		} catch (Exception e) {
			throw new BusinessException("Expression " + expression + " do not evaluate to boolean but " + res);
		}
		return result == null ? false : result;
	}

	private void executeFunction(Function function, Object entityOrEvent, Map<String, String> params, Map<String, Object> context, boolean afterTx) {
		log.debug("execute notification script: {}", function.getCode());

		try {
			Map<Object, Object> userMap = new HashMap<>();
			userMap.put("event", entityOrEvent);
			userMap.put("manager", manager);
			
			for (Map.Entry<String, String> entry : params.entrySet()) {
				context.put(entry.getKey(), MeveoValueExpressionWrapper.evaluateExpression(entry.getValue(), userMap, Object.class));
			}
			
			context.put(Script.APP_BASE_URL, paramBeanFactory.getInstance().getProperty("meveo.admin.baseUrl", "http://localhost:8080/meveo"));

			if (afterTx) {
				concreteFunctionService.postCommit(function.getCode(), context);

			} else {
				context.putAll(concreteFunctionService.execute(function.getCode(), context));
			}
			
			
		} catch (Exception e) {
			log.error("Failed script execution", e);

			try {
				concreteFunctionService.postRollback(function.getCode(), context);

			} catch (BusinessException e1) {
				log.error("Failed rolling back script", e1);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private boolean fireNotification(Notification notif, Object entityOrEvent, boolean afterTx) throws BusinessException {
		if (notif == null) {
			return false;
		}

		IEntity entity;
		if (entityOrEvent instanceof IEntity) {
			entity = (IEntity) entityOrEvent;
			log.debug("Fire Notification for notif with {} and entity with id={}", notif, entity.getId());
			
		} else if (entityOrEvent instanceof IEvent) {
			entity = ((IEvent) entityOrEvent).getEntity();
			log.debug("Fire Notification for notif with {} and entity with id={}", notif, entity.getId());
		}

		try {
			try {
				if (!matchExpression(notif.getElFilter(), entityOrEvent)) {
					log.debug("Expression {} does not match", notif.getElFilter());
					return false;
				}
			} catch (ELException e) {
				log.warn("Cannot evaluate expression {}. Reason : {}. Notification {} won't be executed", notif.getElFilter(),
						e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), notif);
				return false;
			}

			boolean sendNotify = true;
			// Check if the counter associated to notification was not exhausted yet
			if (notif.getCounterInstance() != null) {
				try {
					counterInstanceService.deduceCounterValue(notif.getCounterInstance(), new Date(), notif.getAuditable().getCreated(), new BigDecimal(1));
				} catch (CounterValueInsufficientException ex) {
					sendNotify = false;
				}
			}

			if (!sendNotify) {
				return false;
			}

			Map<String, Object> context = new HashMap<String, Object>();
			// TODO: Rethink notif and script - maybe create pre and post script
			if (!(notif instanceof WebHook)) {
				if (notif.getFunction() != null) {
					executeFunction(notif.getFunction(), entityOrEvent, notif.getParams(), context, afterTx);
				}
			}

			// Execute notification

			// ONLY ScriptNotifications will produce notification history in synchronous
			// mode. Other type notifications will produce notification history in
			// asynchronous mode and
			// thus
			// will not be related to inbound request.
			if (notif instanceof ScriptNotification) {
				NotificationHistory histo = notificationHistoryService.create(notif, entityOrEvent, (String) context.get(Script.RESULT_VALUE), NotificationHistoryStatusEnum.SENT);

				if (notif.getEventTypeFilter() == NotificationEventTypeEnum.INBOUND_REQ && histo != null) {
					((InboundRequest) entityOrEvent).add(histo);
				}

			} else if (notif instanceof EmailNotification) {
				MeveoUser lastCurrentUser = currentUser.unProxy();
				emailNotifier.sendEmail((EmailNotification) notif, entityOrEvent, context, lastCurrentUser);

			} else if (notif instanceof WebHook) {
				MeveoUser lastCurrentUser = currentUser.unProxy();
				webHookNotifier.sendRequest((WebHook) notif, entityOrEvent, context, lastCurrentUser);

			} else if (notif instanceof InstantMessagingNotification) {
				MeveoUser lastCurrentUser = currentUser.unProxy();
				imNotifier.sendInstantMessage((InstantMessagingNotification) notif, entityOrEvent, lastCurrentUser);

			} else if (notif instanceof JobTrigger) {
				MeveoUser lastCurrentUser = currentUser.unProxy();
				jobTriggerLauncher.launch((JobTrigger) notif, entityOrEvent, lastCurrentUser);

			} else if (notif instanceof WebNotification) {
				MeveoUser lastCurrentUser = currentUser.unProxy();
				webNotifier.sendMessage((WebNotification) notif, entityOrEvent, context, lastCurrentUser);
			}

		} catch(EntityDoesNotExistsException entityDoesNotExistsException) {
			if(entityDoesNotExistsException.concerns(notif)) {
				log.error("Notification {} present in cache but not in database. Refreshing cache", notif);
				genericNotificationService.refreshCache();
			}
			
		} catch (Exception e1) {
			log.error("Error while firing notification {} ", notif.getCode(), e1);
			try {
				NotificationHistory notificationHistory = notificationHistoryService.create(notif, entityOrEvent, e1.getMessage(), NotificationHistoryStatusEnum.FAILED);
				if (entityOrEvent instanceof InboundRequest) {
					((InboundRequest) entityOrEvent).add(notificationHistory);
				}
			} catch (Exception e2) {
				log.error("Failed to create notification history", e2);
			}
			
			if (!(e1 instanceof BusinessException)) {
				throw new BusinessException(e1);
			} else {
				throw (BusinessException) e1;
			}
			
		}

		return true;
	}
	
	private boolean checkEvent(NotificationEventTypeEnum type, Object entityOrEvent) throws BusinessException {
		return checkEvent(type, entityOrEvent, false);
	}

	/**
	 * Check and fire all matched notifications
	 *
	 * @param type          Notification type
	 * @param entityOrEvent Entity or event triggered
	 * @return True if at least one notification has been triggered
	 * @throws BusinessException Business exception
	 */
	private boolean checkEvent(NotificationEventTypeEnum type, Object entityOrEvent, boolean afterTx) throws BusinessException {
		boolean result = false;
		for (Notification notif : genericNotificationService.getApplicableNotifications(type, entityOrEvent)) {
			if (notif.isActive()) {
				result = fireNotification(notif, entityOrEvent, afterTx) || result;
			}
		}
		return result;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void onEntityRemoved(@Observes(during = TransactionPhase.AFTER_SUCCESS) @PostRemoved BaseEntity e) throws BusinessException {
		log.debug("Defaut observer : Entity {} with id {} after tx removed", e.getClass().getName(), e.getId());
		if (!e.getClass().isAnnotationPresent(ObservableEntity.class)) {
			return;
		}
		
		checkEvent(NotificationEventTypeEnum.CREATED, e, true);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void onEntityCreated(@Observes(during = TransactionPhase.AFTER_SUCCESS) @CreatedAfterTx BaseEntity e) throws BusinessException {
		log.debug("Defaut observer : Entity {} with id {} after tx created", e.getClass().getName(), e.getId());
		if (!e.getClass().isAnnotationPresent(ObservableEntity.class)) {
			return;
		}
		
		checkEvent(NotificationEventTypeEnum.CREATED, e, true);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void onEntityUpdated(@Observes(during = TransactionPhase.AFTER_SUCCESS) @UpdatedAfterTx BaseEntity e) throws BusinessException {
		log.debug("Defaut observer : Entity {} with id {} after tx updated", e.getClass().getName(), e.getId());
		if (!e.getClass().isAnnotationPresent(ObservableEntity.class)) {
			return;
		}
		checkEvent(NotificationEventTypeEnum.UPDATED, e, true);
	}
	
	public void onEntityCreate(@Observes @Created BaseEntity e) throws BusinessException {
		log.debug("Defaut observer : Entity {} with id {} created", e.getClass().getName(), e.getId());
		if (!e.getClass().isAnnotationPresent(ObservableEntity.class)) {
			return;
		}
		checkEvent(NotificationEventTypeEnum.CREATED, e);
	}

	public void onEntityUpdate(@Observes @Updated BaseEntity e) throws BusinessException {
		log.debug("Defaut observer : Entity {} with id {} updated", e.getClass().getName(), e.getId());
		if (!e.getClass().isAnnotationPresent(ObservableEntity.class)) {
			return;
		}
		checkEvent(NotificationEventTypeEnum.UPDATED, e);
	}

	public void onEntityRemove(@Observes(during = TransactionPhase.BEFORE_COMPLETION) @Removed BaseEntity e) throws BusinessException {
		log.debug("Defaut observer : Entity {} with id {} removed", e.getClass().getName(), e.getId());
		if (!e.getClass().isAnnotationPresent(ObservableEntity.class)) {
			return;
		}
		checkEvent(NotificationEventTypeEnum.REMOVED, e);
	}

	public void onEntityDisable(@Observes @Disabled BaseEntity e) throws BusinessException {
		log.debug("Defaut observer : Entity {} with id {} disabled", e.getClass().getName(), e.getId());
		if (!e.getClass().isAnnotationPresent(ObservableEntity.class)) {
			return;
		}
		checkEvent(NotificationEventTypeEnum.DISABLED, e);
	}

	public void onEntityEnable(@Observes @Enabled BaseEntity e) throws BusinessException {
		log.debug("Defaut observer : Entity {} with id {} enabled", e.getClass().getName(), e.getId());
		if (!e.getClass().isAnnotationPresent(ObservableEntity.class)) {
			return;
		}
		checkEvent(NotificationEventTypeEnum.ENABLED, e);
	}

	public void onEntityTerminate(@Observes @Terminated BaseEntity e) throws BusinessException {
		log.debug("Defaut observer : Entity {} with id {} terminated", e.getClass().getName(), e.getId());
		if (!e.getClass().isAnnotationPresent(ObservableEntity.class)) {
			return;
		}
		checkEvent(NotificationEventTypeEnum.TERMINATED, e);
	}

	public void onEntityProcess(@Observes @Processed Object e) throws BusinessException {
		if (e instanceof BaseEntity) {
			log.debug("Defaut observer : Entity {} with id {} processed", e.getClass().getName(), ((BaseEntity) e).getId());
		}
		
		if (!e.getClass().isAnnotationPresent(ObservableEntity.class) || !e.getClass().isAnnotationPresent(NotifiableEntity.class)) {
			return;
		}

		checkEvent(NotificationEventTypeEnum.PROCESSED, e);
	}

	public void onEntityRejecte(@Observes @Rejected BaseEntity e) throws BusinessException {
		log.debug("Defaut observer : Entity {} with id {} rejected", e.getClass().getName(), e.getId());
		if (!e.getClass().isAnnotationPresent(ObservableEntity.class)) {
			return;
		}
		checkEvent(NotificationEventTypeEnum.REJECTED, e);
	}

	public void onLoggedIn(@Observes User e) throws BusinessException {
		log.debug("Defaut observer : logged in class={} ", e.getClass().getName());
		checkEvent(NotificationEventTypeEnum.LOGGED_IN, e);
	}

	@MeveoAudit
	public void onInboundRequest(@Observes @InboundRequestReceived InboundRequest e) throws BusinessException {
		log.debug("Defaut observer : inbound request {} ", e.getCode());
		boolean fired = checkEvent(NotificationEventTypeEnum.INBOUND_REQ, e);
		e.getHeaders().put("fired", fired ? "true" : "false");
	}

	public void businesException(@Observes BusinessExceptionEvent bee) {
		log.debug("BusinessExceptionEvent handler inactivated {}", bee);

//		log.debug("Defaut observer : BusinessExceptionEvent {} ", bee);
//		StringWriter errors = new StringWriter();
//		bee.getException().printStackTrace(new PrintWriter(errors));
//		String meveoInstanceCode = ParamBean.getInstance().getCet("monitoring.instanceCode", "");
//		int bodyMaxLegthByte = Integer.parseInt(ParamBean.getInstance().getCet("meveo.notifier.stackTrace.lengthInBytes", "9999"));
//		String stackTrace = errors.toString();
//		String input = "{" + "      #meveoInstanceCode#: #" + meveoInstanceCode + "#," + "      #subject#: #" + bee.getException().getMessage() + "#," + "      #body#: #"
//				+ StringUtils.truncate(stackTrace, bodyMaxLegthByte, true) + "#," + "      #additionnalInfo1#: #" + LogExtractionService
//						.getLogs(new Date(System.currentTimeMillis() - Integer.parseInt(ParamBean.getInstance().getCet("meveo.notifier.log.timeBefore_ms", "5000"))), new Date())
//				+ "#," + "      #additionnalInfo2#: ##," + "      #additionnalInfo3#: ##," + "      #additionnalInfo4#: ##" + "}";
//		log.trace("Defaut observer : input {} ", input.replaceAll("#", "\""));
//		remoteInstanceNotifier.invoke(input.replaceAll("\"", "'").replaceAll("#", "\"").replaceAll("\\[", "(").replaceAll("\\]", ")"),
//				ParamBean.getInstance().getCet("inboundCommunication.url", "http://version.meveo.info/meveo-moni/api/rest/inboundCommunication"));

	}

	public void customFieldEndPeriodEvent(@Observes CFEndPeriodEvent event) {
		log.debug("DefaultObserver.customFieldEndPeriodEvent : {}", event);
	}

	public void knownMeveoInstance(@Observes InboundCommunicationEvent event) {
		log.debug("DefaultObserver.knownMeveoInstance" + event);
	}

	public void ftpFileUpload(@Observes @FileUpload MeveoFtpFile importedFile) throws BusinessException {
		log.debug("observe a file upload event ");
		checkEvent(NotificationEventTypeEnum.FILE_UPLOAD, importedFile);
	}

	public void ftpFileDownload(@Observes @FileDownload MeveoFtpFile importedFile) throws BusinessException {
		log.debug("observe a file download event ");
		checkEvent(NotificationEventTypeEnum.FILE_DOWNLOAD, importedFile);
	}

	public void ftpFileDelete(@Observes @FileDelete MeveoFtpFile importedFile) throws BusinessException {
		log.debug("observe a file delete event ");
		checkEvent(NotificationEventTypeEnum.FILE_DELETE, importedFile);
	}

	public void ftpFileRename(@Observes @FileRename MeveoFtpFile importedFile) throws BusinessException {
		log.debug("observe a file rename event ");
		checkEvent(NotificationEventTypeEnum.FILE_RENAME, importedFile);
	}

	public void counterUpdated(@Observes CounterPeriodEvent event) throws BusinessException {
		log.debug("DefaultObserver.counterUpdated " + event);
		checkEvent(NotificationEventTypeEnum.COUNTER_DEDUCED, event);
	}

	public void nodeCreated(@Observes @Created Neo4jEntity e) throws BusinessException {
		log.debug("Defaut observer : Node {} with id {} created", e.labels(), e.id());
		checkEvent(NotificationEventTypeEnum.CREATED, e);
	}

	public void nodeUpdated(@Observes @Updated Neo4jEntity e) throws BusinessException {
		log.debug("Defaut observer : Node {} with id {} updated", e.labels(), e.id());
		checkEvent(NotificationEventTypeEnum.UPDATED, e);
	}

	public void relationshipCreated(@Observes @Created Neo4jRelationship e) throws BusinessException {
		log.debug("Defaut observer : Relationship with startNodeId {}, endNodeId {}, type {} and id {} created", e.startNodeId(), e.endNodeId(), e.type(), e.id());
		checkEvent(NotificationEventTypeEnum.CREATED, e);
	}

	public void relationshipUpdated(@Observes @Updated Neo4jRelationship e) throws BusinessException {
		log.debug("Defaut observer : Relationship with startNodeId {}, endNodeId {}, type {} and id {} updated", e.startNodeId(), e.endNodeId(), e.type(), e.id());
		checkEvent(NotificationEventTypeEnum.UPDATED, e);
	}
	
	public void relationshipRemoved(@Observes @Removed Neo4jRelationship e) throws BusinessException {
		log.debug("Defaut observer : Relationship with startNodeId {}, endNodeId {}, type {} and id {} removed", e.startNodeId(), e.endNodeId(), e.type(), e.id());
		checkEvent(NotificationEventTypeEnum.REMOVED, e);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void commit(@Observes(during = TransactionPhase.AFTER_SUCCESS) CommitEvent commitEvent) throws BusinessException {
		checkEvent(NotificationEventTypeEnum.CREATED, commitEvent);
	}

	public void moduleInstall(@Observes @ModuleInstall MeveoModule meveoModule) throws BusinessException {
		checkEvent(NotificationEventTypeEnum.INSTALL, meveoModule);
	}

	public void modulePostInstall(@Observes @ModulePostInstall MeveoModule meveoModule) throws BusinessException {
		checkEvent(NotificationEventTypeEnum.POST_INSTALL, meveoModule);
	}
}