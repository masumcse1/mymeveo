/*
 * (C) Copyright 2018-2020 Webdrone SAS (https://www.webdrone.fr/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.model.crm.custom;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.meveo.model.BusinessEntity;
import org.meveo.model.ExportIdentifier;
import org.meveo.model.ModuleItem;
import org.meveo.model.ModuleItemOrder;
import org.meveo.model.crm.CustomFieldTemplate.GroupedCustomFieldTreeItemType;
import org.meveo.model.persistence.JsonTypes;
import org.meveo.model.scripts.ScriptInstance;

/**
 * @author Clément Bareth
 * @author Edward P. Legaspi | edward.legaspi@manaty.net
 * @version 6.12
 */
@Entity
@ModuleItem(value = "EntityCustomAction", path = "entityCustomActions")
@ModuleItemOrder(70)
@Cacheable
@ExportIdentifier({ "code", "appliesTo" })
@Table(name = "crm_custom_action", uniqueConstraints = @UniqueConstraint(columnNames = { "code", "applies_to" }))
@GenericGenerator(name = "ID_GENERATOR", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
        @Parameter(name = "sequence_name", value = "crm_custom_action_seq"), })
public class EntityCustomAction extends BusinessEntity {

    private static final long serialVersionUID = -1640429569087958881L;

    @Column(name = "applies_to", nullable = false, length = 100)
    @Size(max = 100)
    @NotNull
    private String appliesTo;

    @Column(name = "applicable_on_el", length = 2000)
    @Size(max = 2000)
    private String applicableOnEl;

    @Column(name = "label", length = 50)
    @Size(max = 50)
    private String label;

    @Type(type = JsonTypes.JSON)
    @Column(name = "label_i18n", columnDefinition = "text")
    private Map<String, String> labelI18n;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "script_instance_id")
    private ScriptInstance script;
    
	@Type(type = "numeric_boolean")
	@Column(name = "applicable_to_entity_list")
	private Boolean applicableToEntityList = false;

	@Type(type = "numeric_boolean")
	@Column(name = "applicable_to_entity_instance")
	private Boolean applicableToEntityInstance = true;
	
    /**
     * Map representing the script parameter where the key is the paramer name
     * and the value the el to evaluate
     */
    @Column(name = "script_parameters", columnDefinition = "text")
    @Type(type = JsonTypes.JSON)
    private Map<String, String> scriptParameters = new HashMap<>();

    /**
     * Where action should be displayed. Format: tab:&lt;tab name&gt;:&lt;tab relative position&gt;;action:&lt;action relative position in tab&gt;
     * 
     * 
     * Tab and field group names support translation in the following format: &lt;default value&gt;|&lt;language3 letter key=translated value&gt;
     * 
     * e.g. tab:Tab default title|FRA=Title in french|ENG=Title in english:0;fieldGroup:Field group default label|FRA=Field group label in french|ENG=Field group label in
     * english:0;action:0 OR tab:Second tab:1;action:1
     */
    @Column(name = "gui_position", length = 2000)
    @Size(max = 2000)
    private String guiPosition;
    
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

	public String getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }

    public String getApplicableOnEl() {
        return applicableOnEl;
    }

    public void setApplicableOnEl(String applicableOnEl) {
        this.applicableOnEl = applicableOnEl;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setScript(ScriptInstance script) {
        this.script = script;
    }

    public ScriptInstance getScript() {
        return script;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof EntityCustomAction)) {
            return false;
        }

        EntityCustomAction other = (EntityCustomAction) obj;

        if (getId() != null && other.getId() != null && getId().equals(other.getId())) {
            return true;
        }

        if (code == null && other.getCode() != null) {
            return false;
        } else if (!code.equals(other.getCode())) {
            return false;
        } else if (appliesTo == null && other.getAppliesTo() != null) {
            return false;
        } else if (!appliesTo.equals(other.getAppliesTo())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("EntityActionScript [id=%s, appliesTo=%s, code=%s]", id, appliesTo, code);
    }

    public String getGuiPosition() {
        return guiPosition;
    }

    public void setGuiPosition(String guiPosition) {
        this.guiPosition = guiPosition;
    }

    public Map<String, String> getGuiPositionParsed() {

        if (guiPosition == null) {
            return null;
        }

        Map<String, String> parsedInfo = new HashMap<String, String>();

        String[] positions = guiPosition.split(";");

        for (String position : positions) {
            String[] positionDetails = position.split(":");
            if (!positionDetails[0].equals(GroupedCustomFieldTreeItemType.action.positionTag)) {
                parsedInfo.put(positionDetails[0] + "_name", positionDetails[1]);
                if (positionDetails.length == 3) {
                    parsedInfo.put(positionDetails[0] + "_pos", positionDetails[2]);
                }
            } else if (positionDetails[0].equals(GroupedCustomFieldTreeItemType.action.positionTag) && positionDetails.length == 2) {
                parsedInfo.put(positionDetails[0] + "_pos", positionDetails[1]);
            }
        }

        return parsedInfo;
    }

    public Map<String, String> getLabelI18n() {
        return labelI18n;
    }

    public void setLabelI18n(Map<String, String> labelI18n) {
        this.labelI18n = labelI18n;
    }

    /**
     * Instantiate labelI18n field if it is null. NOTE: do not use this method unless you have an intention to modify it's value, as entity will be marked dirty and record will be
     * updated in DB
     * 
     * @return labelI18n value or instantiated labelI18n field value
     */
    public Map<String, String> getLabelI18nNullSafe() {
        if (labelI18n == null) {
            labelI18n = new HashMap<>();
        }
        return labelI18n;
    }

    public String getLabel(String language) {

        if (language == null || labelI18n == null || labelI18n.isEmpty()) {
            return label;
        }

        language = language.toUpperCase();
        if (!labelI18n.containsKey(language)) {
            return label;
        } else {
            return labelI18n.get(language);
        }
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