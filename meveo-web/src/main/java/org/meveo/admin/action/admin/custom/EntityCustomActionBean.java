package org.meveo.admin.action.admin.custom;

import java.util.Arrays;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.action.admin.ViewBean;
import org.meveo.admin.action.catalog.ScriptInstanceBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.web.interceptor.ActionMethod;
import org.meveo.elresolver.ELException;
import org.meveo.model.crm.custom.EntityCustomAction;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.custom.EntityCustomActionService;

/**
 * @author Edward P. Legaspi | edward.legaspi@manaty.net
 * @version 6.12
 **/
@Named
@ViewScoped
public class EntityCustomActionBean extends BaseBean<EntityCustomAction> {

    private static final long serialVersionUID = 5401687428382698718L;

    @Inject
    private EntityCustomActionService entityActionScriptService;

    @Inject
    @ViewBean
    protected ScriptInstanceBean scriptInstanceBean;

    public EntityCustomActionBean() {
        super(EntityCustomAction.class);
    }

    @Override
    @ActionMethod
    public String saveOrUpdate(boolean killConversation) throws BusinessException, ELException {

        EntityCustomAction actionDuplicate = entityActionScriptService.findByCodeAndAppliesTo(entity.getCode(), entity.getAppliesTo());
        if (actionDuplicate != null && !actionDuplicate.getId().equals(entity.getId())) {
            messages.error(new BundleKey("messages", "customizedEntities.actionAlreadyExists"));
            return null;
        }

        String result = super.saveOrUpdate(killConversation);
        return result;

    }

    @Override
    protected IPersistenceService<EntityCustomAction> getPersistenceService() {
        return entityActionScriptService;
    }

    @Override
    protected String getDefaultSort() {
        return "code";
    }
    
    @Override
    protected List<String> getFormFieldsToFetch() {
    	return Arrays.asList("script");
    }

    public void refreshScript() {
        entity.setScript(scriptInstanceBean.getEntity());
    }

    /**
     * Prepare to show a popup to view or edit script
     */
    public void viewEditScript() {
        if (entity.getScript() != null) {
            scriptInstanceBean.initEntity(entity.getScript().getId());
        } else {
            scriptInstanceBean.newEntity();
        }
        scriptInstanceBean.setBackViewSave(this.getEditViewName());
    }

    /**
     * Prepare to show a popup to enter new script
     */
    public void newScript() {
        scriptInstanceBean.newEntity();
        scriptInstanceBean.setBackViewSave(this.getEditViewName());
    }
}