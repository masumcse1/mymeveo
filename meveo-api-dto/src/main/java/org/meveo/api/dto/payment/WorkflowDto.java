/*
 * (C) Copyright 2015-2016 Opencell SAS (http://opencellsoft.com/) and contributors.
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
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
package org.meveo.api.dto.payment;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.meveo.api.dto.BaseEntityDto;
import org.meveo.model.wf.WFTransition;
import org.meveo.model.wf.Workflow;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * The Class WorkflowDto.
 * 
 * @author anasseh
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @version 6.7.0
 */
@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
@ApiModel("WorkflowDto")
public class WorkflowDto extends BaseEntityDto {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 8309866046667741458L;

	/** The code. */
	@XmlAttribute(required = true)
	@ApiModelProperty("Code of workflow")
	private String code;

	/** The description. */
	@XmlAttribute
	@ApiModelProperty("Description of workflow")
	private String description;

	/** The cetCode. */
	@XmlElement(required = true)
	@ApiModelProperty("Code of custom entity template")
	private String cetCode;

	/** The wf type. */
	@XmlElement(required = true)
	@ApiModelProperty("The type")
	private String wfType;

	/** The enable history. */
	@ApiModelProperty("If history is save when executing this workflow")
	private Boolean enableHistory = false;

	/** The list WF transition dto. */
	@XmlElementWrapper(name = "transitions")
	@XmlElement(name = "transition")
	@ApiModelProperty("List of transitions associated with this workflow")
	private List<WFTransitionDto> listWFTransitionDto = new ArrayList<WFTransitionDto>();

	/**
	 * Instantiates a new workflow dto.
	 */
	public WorkflowDto() {
	}

	/**
	 * Instantiates a new workflow dto.
	 *
	 * @param workflow the workflow entity
	 */
	public WorkflowDto(Workflow workflow) {
		this.code = workflow.getCode();
		this.description = workflow.getDescription();
		this.cetCode = workflow.getCetCode();
		this.wfType = workflow.getWfType();
		this.enableHistory = workflow.isEnableHistory();
		for (WFTransition wfTransition : workflow.getTransitions()) {
			WFTransitionDto wftdto = new WFTransitionDto(wfTransition);
			listWFTransitionDto.add(wftdto);
		}
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
	 * @param code the code to set
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
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gets the cetCode.
	 *
	 * @return the cetCode
	 */
	public String getCetCode() {
		return cetCode;
	}

	/**
	 * Sets the cetCode.
	 *
	 * @param cetCode the cetCode to set
	 */
	public void setCetCode(String cetCode) {
		this.cetCode = cetCode;
	}

	/**
	 * Gets the wf type.
	 *
	 * @return the wfType
	 */
	public String getWfType() {
		return wfType;
	}

	/**
	 * Sets the wf type.
	 *
	 * @param wfType the wfType to set
	 */
	public void setWfType(String wfType) {
		this.wfType = wfType;
	}

	/**
	 * Gets the enable history.
	 *
	 * @return the enableHistory
	 */
	public Boolean getEnableHistory() {
		return enableHistory;
	}

	/**
	 * Sets the enable history.
	 *
	 * @param enableHistory the enableHistory to set
	 */
	public void setEnableHistory(Boolean enableHistory) {
		this.enableHistory = enableHistory;
	}

	/**
	 * Gets the list WF transition dto.
	 *
	 * @return the listWFTransitionDto
	 */
	public List<WFTransitionDto> getListWFTransitionDto() {
		return listWFTransitionDto;
	}

	/**
	 * Sets the list WF transition dto.
	 *
	 * @param listWFTransitionDto the listWFTransitionDto to set
	 */
	public void setListWFTransitionDto(List<WFTransitionDto> listWFTransitionDto) {
		this.listWFTransitionDto = listWFTransitionDto;
	}

	@Override
	public String toString() {
		return "WorkflowDto [code=" + code + ", description=" + description + ", cetCode=" + cetCode + ", wfType=" + wfType + " enableHistory=" + enableHistory + ", listWFTransitionDto="
				+ listWFTransitionDto + "]";
	}

}