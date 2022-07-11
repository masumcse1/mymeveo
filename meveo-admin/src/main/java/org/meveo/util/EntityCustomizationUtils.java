package org.meveo.util;

import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ReflectionUtils;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.CustomFieldEntity;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.model.customEntities.CustomRelationshipTemplate;
import org.meveo.service.job.Job;

/**
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @version 6.8.0
 */
public class EntityCustomizationUtils {

    /**
     * Determine appliesTo value for custom field templates, actions, etc..
     * 
     * @param clazz Class customization applies to
     * @param code Entity code (applies to CustomEntityTemplate only)
     * @return An "appliesTo" value for a given class
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static String getAppliesTo(Class clazz, String code) {

		String appliesToPrefix = null;
		if (Job.class.isAssignableFrom(clazz)) {
			appliesToPrefix = Job.CFT_PREFIX + "_" + ReflectionUtils.getCleanClassName(clazz.getSimpleName());

		} else if (CustomEntityTemplate.class.isAssignableFrom(clazz)) {
			appliesToPrefix = CustomEntityTemplate.getAppliesTo(code);

		} else if (CustomRelationshipTemplate.class.isAssignableFrom(clazz)) {
			appliesToPrefix = CustomRelationshipTemplate.getAppliesTo(code);

		} else {
			appliesToPrefix = ((CustomFieldEntity) clazz.getAnnotation(CustomFieldEntity.class)).cftCodePrefix();
		}

		return appliesToPrefix;
	}

    /**
     * Get entity code from applies to value. Applicable to CustomEntityTempalate/Instance only
     * 
     * @param appliesTo An "appliesTo" value
     * @return Entity code part of "appliesTo" value
     */
    public static String getEntityCode(String appliesTo) {
    	if(appliesTo == null) {
    		return null;
    	}
    	
        int pos = appliesTo.indexOf("_");
        if (pos > 0) {
            return appliesTo.substring(pos + 1);
        } else {
            return null;
        }
    }

    public static boolean validateOntologyCode(String code) {
        if (code != null && !StringUtils.isMatch(code, ParamBean.getInstance().getProperty("ontology.code.pattern", StringUtils.CODE_ONTOLOGY_REGEX))) {
            return false;
        }
        return true;
    }
    
}