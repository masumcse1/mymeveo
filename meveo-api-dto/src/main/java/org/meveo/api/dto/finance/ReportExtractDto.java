package org.meveo.api.dto.finance;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.meveo.api.dto.BusinessEntityDto;
import org.meveo.model.finance.ReportExtractScriptTypeEnum;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Use to generate a report on different format using a query or script. Format
 * supported are CSV and HTML.
 *
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @version 6.7.0
 * @since 5.0
 */
@ApiModel("ReportExtractDto")
public class ReportExtractDto extends BusinessEntityDto {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 3600792942683148893L;

	/** The script type. */
	@NotNull
	@ApiModelProperty("The script type")
	private ReportExtractScriptTypeEnum scriptType;

	/** The filename format. */
	@NotNull
	@ApiModelProperty("The filename format")
	private String filenameFormat;

	/** The category. */
	@ApiModelProperty("The category")
	private String category;

	/** The script instance code. */
	@ApiModelProperty("Code of the script instance")
	private String scriptInstanceCode;

	/** The sql query. */
	@ApiModelProperty("The sql query")
	private String sqlQuery;

	/** The params. */
	@ApiModelProperty("Map of params")
	private Map<String, String> params = new HashMap<>();

	/** The start date. */
	@ApiModelProperty("The start date")
	private Date startDate;

	/** The end date. */
	@ApiModelProperty("The end date")
	private Date endDate;

	/**
	 * Gets the category.
	 *
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * Sets the category.
	 *
	 * @param category the new category
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * Gets the script type.
	 *
	 * @return the script type
	 */
	public ReportExtractScriptTypeEnum getScriptType() {
		return scriptType;
	}

	/**
	 * Sets the script type.
	 *
	 * @param scriptType the new script type
	 */
	public void setScriptType(ReportExtractScriptTypeEnum scriptType) {
		this.scriptType = scriptType;
	}

	/**
	 * Gets the filename format.
	 *
	 * @return the filename format
	 */
	public String getFilenameFormat() {
		return filenameFormat;
	}

	/**
	 * Sets the filename format.
	 *
	 * @param filenameFormat the new filename format
	 */
	public void setFilenameFormat(String filenameFormat) {
		this.filenameFormat = filenameFormat;
	}

	/**
	 * Gets the script instance code.
	 *
	 * @return the script instance code
	 */
	public String getScriptInstanceCode() {
		return scriptInstanceCode;
	}

	/**
	 * Sets the script instance code.
	 *
	 * @param scriptInstanceCode the new script instance code
	 */
	public void setScriptInstanceCode(String scriptInstanceCode) {
		this.scriptInstanceCode = scriptInstanceCode;
	}

	/**
	 * Gets the sql query.
	 *
	 * @return the sql query
	 */
	public String getSqlQuery() {
		return sqlQuery;
	}

	/**
	 * Sets the sql query.
	 *
	 * @param sqlQuery the new sql query
	 */
	public void setSqlQuery(String sqlQuery) {
		this.sqlQuery = sqlQuery;
	}

	/**
	 * Gets the params.
	 *
	 * @return the params
	 */
	public Map<String, String> getParams() {
		return params;
	}

	/**
	 * Sets the params.
	 *
	 * @param params the params
	 */
	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	/**
	 * Gets the start date.
	 *
	 * @return the start date
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * Sets the start date.
	 *
	 * @param startDate the new start date
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * Gets the end date.
	 *
	 * @return the end date
	 */
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * Sets the end date.
	 *
	 * @param endDate the new end date
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
}