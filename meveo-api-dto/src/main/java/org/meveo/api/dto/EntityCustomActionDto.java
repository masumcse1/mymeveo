package org.meveo.api.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.meveo.model.crm.custom.EntityCustomAction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Custom action.
 *
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @version 6.12
 */
@XmlRootElement(name = "EntityCustomAction")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel
public class EntityCustomActionDto extends BaseEntityDto {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -2916923287316823939L;

    /** Code. */
    @XmlAttribute(required = true)
    @ApiModelProperty(required = true, value = "Code of entity custom action")
    private String code;

    /** Description. */
    @XmlAttribute()
    @ApiModelProperty("Description of entity custom action")
    private String description;

    /** Entity action applies to. */
    @XmlAttribute(required = false)
    @ApiModelProperty(required = false, value = "Entity action applies to")
    protected String appliesTo;

    /** EL expression when action button should be visible. */
    @XmlElement(required = false)
    @ApiModelProperty(required = false, value = "EL expression when action button should be visible.")
    private String applicableOnEl;

    /** Button label. */
    @XmlElement(required = false)
    @ApiModelProperty(required = false, value = "Button label")
    private String label;

    /** Button label translations. */
    @ApiModelProperty("Button label translations")
    protected List<LanguageDescriptionDto> labelsTranslated;

    /** Script to execute. */
    @ApiModelProperty("Script instance information")
    private String script;

    /**
     * Where action should be displayed. Format: tab:&lt;tab name&gt;:&lt;tab relative position&gt;;action:&lt;action relative position in tab&gt;
     * 
     * 
     * Tab and field group names support translation in the following format: &lt;default value&gt;|&lt;language3 letter key=translated value&gt;
     * 
     * e.g. tab:Tab default title|FRA=Title in french|ENG=Title in english:0;fieldGroup:Field group default label|FRA=Field group label in french|ENG=Field group label in
     * english:0;action:0 OR tab:Second tab:1;action:1
     */
    @ApiModelProperty("Gui position")
    private String guiPosition;

    /**
     * GUI only. Whether to show this action in CEI list.
     */
	private Boolean applicableToEntityList = false;

	/**
	 * GUI only, Whether to show this action in CEI detail.
	 */
	private Boolean applicableToEntityInstance = true;
	
	private Map<String, String> scriptParameters = new HashMap<>();

    /**
     * Instantiates a new entity custom action dto.
     */
    public EntityCustomActionDto() {
        super();
    }

    /**
     * Instantiates a new entity custom action dto.
     *
     * @param action the action
     */
    public EntityCustomActionDto(EntityCustomAction action) {
        this.code = action.getCode();
        this.description = action.getDescription();

        this.appliesTo = action.getAppliesTo();
        this.applicableOnEl = action.getApplicableOnEl();
        this.label = action.getLabel();
        this.labelsTranslated = LanguageDescriptionDto.convertMultiLanguageFromMapOfValues(action.getLabelI18n());
        this.guiPosition = action.getGuiPosition();
        this.applicableToEntityList = action.getApplicableToEntityList();
        this.applicableToEntityInstance = action.getApplicableToEntityInstance();
        this.setScriptParameters(scriptParameters);
        
        this.setScript(action.getScript().getCode());
    }
    

    /**
	 * @return the {@link #scriptParameters}
	 */
	public Map<String, String> getScriptParameters() {
		return scriptParameters;
	}

	/**
	 * @param scriptParameters the scriptParameters to set
	 */
	public void setScriptParameters(Map<String, String> scriptParameters) {
		this.scriptParameters = scriptParameters;
	}

	/**
     * Gets the code.
     *
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the code.
     *
     * @param code the new code
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Gets the description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description the new description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the applies to.
     *
     * @return the applies to
     */
    public String getAppliesTo() {
        return appliesTo;
    }

    /**
     * Sets the applies to.
     *
     * @param appliesTo the new applies to
     */
    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }

    /**
     * Gets the applicable on el.
     *
     * @return the applicable on el
     */
    public String getApplicableOnEl() {
        return applicableOnEl;
    }

    /**
     * Sets the applicable on el.
     *
     * @param applicableOnEl the new applicable on el
     */
    public void setApplicableOnEl(String applicableOnEl) {
        this.applicableOnEl = applicableOnEl;
    }

    /**
     * Gets the label.
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the label.
     *
     * @param label the new label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets the script.
     *
     * @return the script
     */
    public String getScript() {
        return script;
    }

    /**
     * Sets the script.
     *
     * @param script the new script
     */
    public void setScript(String script) {
        this.script = script;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("EntityCustomActionDto [code=%s, description=%s, appliesTo=%s, applicableOnEl=%s, label=%s, script=%s]", code, description, appliesTo, applicableOnEl,
            label, script);
    }

    /**
     * Gets the gui position.
     *
     * @return the gui position
     */
    public String getGuiPosition() {
        return guiPosition;
    }

    /**
     * Sets the gui position.
     *
     * @param guiPosition the new gui position
     */
    public void setGuiPosition(String guiPosition) {
        this.guiPosition = guiPosition;
    }

    /**
     * Gets the labels translated.
     *
     * @return the labels translated
     */
    public List<LanguageDescriptionDto> getLabelsTranslated() {
        return labelsTranslated;
    }

    /**
     * Sets the labels translated.
     *
     * @param labelsTranslated the new labels translated
     */
    public void setLabelsTranslated(List<LanguageDescriptionDto> labelsTranslated) {
        this.labelsTranslated = labelsTranslated;
    }

	public Boolean getApplicableToEntityList() {
		return applicableToEntityList;
	}

	public void setApplicableToEntityList(Boolean applicableToEntityList) {
		this.applicableToEntityList = applicableToEntityList;
	}

	public Boolean getApplicableToEntityInstance() {
		return applicableToEntityInstance;
	}

	public void setApplicableToEntityInstance(Boolean applicableToEntityInstance) {
		this.applicableToEntityInstance = applicableToEntityInstance;
	}
}