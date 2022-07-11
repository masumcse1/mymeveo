package org.meveo.api.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.meveo.model.admin.Currency;

/**
 * The Class CurrencyDto.
 *
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @version 6.7.0
 */
@XmlRootElement(name = "Currency")
@XmlAccessorType(XmlAccessType.FIELD)
@ApiModel
public class CurrencyDto extends BaseEntityDto {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 9143645109603442839L;

    /** The code. */
    @XmlAttribute(required = true)
    @ApiModelProperty(required = true, value = "Code of the currency")
    private String code;

    /** The description. */
    @ApiModelProperty("Description of the currency")
    private String description;

    /**
     * Instantiates a new currency dto.
     */
    public CurrencyDto() {

    }

    /**
     * Instantiates a new currency dto.
     *
     * @param currency the currency
     */
    public CurrencyDto(Currency currency) {
        code = currency.getCurrencyCode();
        description = currency.getDescriptionEn();
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

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "CurrencyDto [code=" + code + ", description=" + description + "]";
    }

}
