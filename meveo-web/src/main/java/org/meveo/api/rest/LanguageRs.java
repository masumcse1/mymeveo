package org.meveo.api.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.LanguageDto;
import org.meveo.api.dto.response.GetLanguageResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * Web service for managing {@link org.meveo.model.billing.Language} and
 * {@link org.meveo.model.billing.TradingLanguage}.
 * 
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @version 6.7.0
 *
 **/
@Path("/language")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Api("LanguageRs")
public interface LanguageRs extends IBaseRs {

	/**
	 * Creates tradingLanguage base on language code. If the language code does not
	 * exists, a language record is created.
	 * 
	 * @param postData language to be created
	 * @return action status
	 */
	@POST
	@Path("/")
	@ApiOperation(value = "Create language")
	ActionStatus create(@ApiParam("Language information") LanguageDto postData);

	/**
	 * Search language given a code.
	 * 
	 * @param languageCode language's code
	 * @return language
	 */
	@GET
	@Path("/")
	@ApiOperation(value = "Find language by code")
	GetLanguageResponse find(@QueryParam("languageCode") @ApiParam("Code of the language") String languageCode);

	/**
	 * Does not delete a language but the tradingLanguage associated to it.
	 * 
	 * @param languageCode language's code
	 * @return action satus
	 */
	@DELETE
	@Path("/{languageCode}")
	@ApiOperation(value = "Remove language by code")
	ActionStatus remove(@PathParam("languageCode") @ApiParam("Code of the language") String languageCode);

	/**
	 * modify a language. Same input parameter as create. The language and trading
	 * Language are created if they don't exists. The operation fails if the
	 * tradingLanguage is null.
	 * 
	 * @param postData language to be updated
	 * @return action status
	 */
	@PUT
	@Path("/")
	@ApiOperation(value = "Update language")
	ActionStatus update(@ApiParam("Language information") LanguageDto postData);

	/**
	 * Create or update a language if it doesn't exists.
	 * 
	 * @param postData language to be created or updated
	 * @return action status.
	 */
	@POST
	@Path("/createOrUpdate")
	@ApiOperation(value = "Create or update language")
	ActionStatus createOrUpdate(@ApiParam("Language information") LanguageDto postData);
}
