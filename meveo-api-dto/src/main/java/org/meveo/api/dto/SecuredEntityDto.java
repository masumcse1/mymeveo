package org.meveo.api.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.meveo.model.admin.SecuredEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * The Class SecuredEntityDto.
 * 
 * @author anasseh
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @version 6.7.0
 */
@XmlRootElement(name = "SecuredEntity")
@XmlAccessorType(XmlAccessType.FIELD)
@ApiModel
public class SecuredEntityDto extends BaseEntityDto {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 8941891021770440273L;

	/** The code. */
	@XmlAttribute(required = true)
	@ApiModelProperty(required = true, value = "Code of this entity")
	private String code;

	/** The entity class. */
	@XmlAttribute(required = true)
	@ApiModelProperty(required = true, value = "The entity class")
	private String entityClass;

	/**
	 * Instantiates a new secured entity dto.
	 */
	public SecuredEntityDto() {
	}

	/**
	 * Instantiates a new secured entity dto.
	 *
	 * @param entity the entity
	 */
	public SecuredEntityDto(SecuredEntity entity) {
		this.code = entity.getCode();
		this.entityClass = entity.getEntityClass();
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
	 * Gets the entity class.
	 *
	 * @return the entity class
	 */
	public String getEntityClass() {
		return entityClass;
	}

	/**
	 * Sets the entity class.
	 *
	 * @param entityClass the new entity class
	 */
	public void setEntityClass(String entityClass) {
		this.entityClass = entityClass;
	}

}
