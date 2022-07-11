package org.meveo.api.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.meveo.api.dto.response.SearchResponse;

/**
 * The Class ProvidersDto.
 * 
 * @author anasseh
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @version 6.7.0
 */
@XmlRootElement(name = "Providers")
@XmlAccessorType(XmlAccessType.FIELD)
@ApiModel
public class ProvidersDto extends SearchResponse {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1893591052731642142L;

    /** The providers. */
    @XmlElementWrapper(name = "providers")
    @XmlElement(name = "provider")
    @ApiModelProperty("List of providers information")
    private List<ProviderDto> providers = new ArrayList<>();

    /**
     * Gets the providers.
     *
     * @return the providers
     */
    public List<ProviderDto> getProviders() {
        return providers;
    }

    /**
     * Sets the providers.
     *
     * @param providers the new providers
     */
    public void setProviders(List<ProviderDto> providers) {
        this.providers = providers;
    }
}