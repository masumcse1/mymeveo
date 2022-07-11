package org.meveo.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.meveo.model.crm.Provider;

import javax.xml.bind.annotation.*;

/**
 * The Class ProviderDto.
 *
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @version 6.7.0
 */
@XmlRootElement(name = "Provider")
@XmlAccessorType(XmlAccessType.FIELD)
@ApiModel
public class ProviderDto extends BaseEntityDto {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 5599223889050605880L;

    /** The code. */
    @XmlAttribute(required = true)
    @ApiModelProperty(required = true, value = "Code of the provider")
    private String code;

    /** The description. */
    @ApiModelProperty("The description")
    private String description;
    
    /** The currency. */
    @ApiModelProperty("The currency")
    private String currency;
    
    /** The country. */
    @ApiModelProperty("The country")
    private String country;
    
    /** The language. */
    @ApiModelProperty("The country")
    private String language;
    
    /** The multi currency. */
    @ApiModelProperty("The multi currency")
    private Boolean multiCurrency;
    
    /** The multi country. */
    @ApiModelProperty("The multi country")
    private Boolean multiCountry;
    
    /** The multi language. */
    @ApiModelProperty("The multi language")
    private Boolean multiLanguage;
    
    /** The user account. */
    @ApiModelProperty("The user account")
    private String userAccount;

    /** The enterprise. */
    @ApiModelProperty("The enterprise")
    private Boolean enterprise;
    
    /** The level duplication. */
    @ApiModelProperty("The level duplication")
    private Boolean levelDuplication;
    
    /** The rounding. */
    @ApiModelProperty("The rounding")
    private Integer rounding;
    
    /** The prepaid reservation expiration delayin millisec. */
    @ApiModelProperty("The prepaid reservation expiration delayin millisec")
    private Long prepaidReservationExpirationDelayinMillisec;
    
    /**
     * The discount accounting code.
     *
     * @deprecated Not used.
     */
    @Deprecated
    @ApiModelProperty("Code of the discount accounting")
    private String discountAccountingCode;
    
    /** The email. */
    @ApiModelProperty("The email")
    private String email;
    
    /** The recognize revenue. */
    @ApiModelProperty("The recognize revenue")
    private Boolean recognizeRevenue;
    
    /** The custom fields. */
    @XmlElement(required = false)
    @ApiModelProperty("Custom fields information")
    private CustomFieldsDto customFields;

    /**
     * Instantiates a new provider dto.
     */
    public ProviderDto() {
    }

    /**
     * Instantiates a new provider dto.
     *
     * @param provider the provider entity
     * @param customFieldInstances the custom field instances
     */
    public ProviderDto(Provider provider, CustomFieldsDto customFieldInstances) {
        this(provider, customFieldInstances, true);
    }

    /**
     * Instantiates a new provider dto.
     *
     * @param provider the provider
     * @param customFieldInstances the custom field instances
     * @param loadProviderData the load provider data
     */
    public ProviderDto(Provider provider, CustomFieldsDto customFieldInstances, boolean loadProviderData) {
        code = provider.getCode();

        if (loadProviderData) {
            description = provider.getDescription();
            email = provider.getEmail();
        }

        customFields = customFieldInstances;
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
     * Gets the currency.
     *
     * @return the currency
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Sets the currency.
     *
     * @param currency the new currency
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Gets the country.
     *
     * @return the country
     */
    public String getCountry() {
        return country;
    }

    /**
     * Sets the country.
     *
     * @param country the new country
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * Gets the language.
     *
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the language.
     *
     * @param language the new language
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Checks if is multi currency.
     *
     * @return the boolean
     */
    public Boolean isMultiCurrency() {
        return multiCurrency;
    }

    /**
     * Sets the multi currency.
     *
     * @param multiCurrency the new multi currency
     */
    public void setMultiCurrency(Boolean multiCurrency) {
        this.multiCurrency = multiCurrency;
    }

    /**
     * Checks if is multi country.
     *
     * @return the boolean
     */
    public Boolean isMultiCountry() {
        return multiCountry;
    }

    /**
     * Sets the multi country.
     *
     * @param multiCountry the new multi country
     */
    public void setMultiCountry(Boolean multiCountry) {
        this.multiCountry = multiCountry;
    }

    /**
     * Checks if is multi language.
     *
     * @return the boolean
     */
    public Boolean isMultiLanguage() {
        return multiLanguage;
    }

    /**
     * Sets the multi language.
     *
     * @param multiLanguage the new multi language
     */
    public void setMultiLanguage(Boolean multiLanguage) {
        this.multiLanguage = multiLanguage;
    }

    /**
     * Gets the user account.
     *
     * @return the user account
     */
    public String getUserAccount() {
        return userAccount;
    }

    /**
     * Sets the user account.
     *
     * @param userAccount the new user account
     */
    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }

    /**
     * Gets the custom fields.
     *
     * @return the custom fields
     */
    public CustomFieldsDto getCustomFields() {
        return customFields;
    }

    /**
     * Sets the custom fields.
     *
     * @param customFields the new custom fields
     */
    public void setCustomFields(CustomFieldsDto customFields) {
        this.customFields = customFields;
    }

    /**
     * Checks if is enterprise.
     *
     * @return the boolean
     */
    public Boolean isEnterprise() {
        return enterprise;
    }

    /**
     * Sets the enterprise.
     *
     * @param enterprise the new enterprise
     */
    public void setEnterprise(Boolean enterprise) {
        this.enterprise = enterprise;
    }

    /**
     * Checks if is level duplication.
     *
     * @return the boolean
     */
    public Boolean isLevelDuplication() {
        return levelDuplication;
    }

    /**
     * Sets the level duplication.
     *
     * @param levelDuplication the new level duplication
     */
    public void setLevelDuplication(Boolean levelDuplication) {
        this.levelDuplication = levelDuplication;
    }

    /**
     * Gets the rounding.
     *
     * @return the rounding
     */
    public Integer getRounding() {
        return rounding;
    }

    /**
     * Sets the rounding.
     *
     * @param rounding the new rounding
     */
    public void setRounding(Integer rounding) {
        this.rounding = rounding;
    }

    /**
     * Gets the prepaid reservation expiration delayin millisec.
     *
     * @return the prepaid reservation expiration delayin millisec
     */
    public Long getPrepaidReservationExpirationDelayinMillisec() {
        return prepaidReservationExpirationDelayinMillisec;
    }

    /**
     * Sets the prepaid reservation expiration delayin millisec.
     *
     * @param prepaidReservationExpirationDelayinMillisec the new prepaid reservation expiration delayin millisec
     */
    public void setPrepaidReservationExpirationDelayinMillisec(Long prepaidReservationExpirationDelayinMillisec) {
        this.prepaidReservationExpirationDelayinMillisec = prepaidReservationExpirationDelayinMillisec;
    }

    /**
     * Gets the discount accounting code.
     *
     * @return the discount accounting code
     */
    public String getDiscountAccountingCode() {
        return discountAccountingCode;
    }

    /**
     * Sets the discount accounting code.
     *
     * @param discountAccountingCode the new discount accounting code
     */
    public void setDiscountAccountingCode(String discountAccountingCode) {
        this.discountAccountingCode = discountAccountingCode;
    }

    /**
     * Gets the email.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email.
     *
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Checks if is recognize revenue.
     *
     * @return the boolean
     */
    public Boolean isRecognizeRevenue() {
        return recognizeRevenue;
    }

    /**
     * Sets the recognize revenue.
     *
     * @param recognizeRevenue the new recognize revenue
     */
    public void setRecognizeRevenue(Boolean recognizeRevenue) {
        this.recognizeRevenue = recognizeRevenue;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ProviderDto [code=" + code + ", description=" + description + ", currency=" + currency + ", country=" + country + ", language=" + language + ", multiCurrency="
                + multiCurrency + ", multiCountry=" + multiCountry + ", multiLanguage=" + multiLanguage + ", userAccount=" + userAccount + ", enterprise=" + enterprise
                + ", levelDuplication=" + levelDuplication + ", rounding=" + rounding + ", prepaidReservationExpirationDelayinMillisec="
                + prepaidReservationExpirationDelayinMillisec + ", discountAccountingCode=" + discountAccountingCode + ", email=" + email
                + ", recognizeRevenue=" + recognizeRevenue + ", customFields=" + customFields + "]";
    }

}
