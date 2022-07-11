package org.meveo.api.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


/**
 * The Class CurrenciesDto.
 *
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @version 6.7.0
 */
@XmlRootElement(name = "Currencies")
@XmlAccessorType(XmlAccessType.FIELD)
@ApiModel
public class CurrenciesDto extends BaseEntityDto {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -7446426816621551014L;

    /** The currency. */
    @ApiModelProperty("List of currency information")
    private List<CurrencyDto> currency;

    /**
     * Gets the currency.
     *
     * @return the currency
     */
    public List<CurrencyDto> getCurrency() {
        if (currency == null)
            currency = new ArrayList<CurrencyDto>();
        return currency;
    }

    /**
     * Sets the currency.
     *
     * @param currency the new currency
     */
    public void setCurrency(List<CurrencyDto> currency) {
        this.currency = currency;
    }

}
