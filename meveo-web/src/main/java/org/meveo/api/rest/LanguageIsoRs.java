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
import org.meveo.api.dto.LanguageIsoDto;
import org.meveo.api.dto.response.GetLanguageIsoResponse;
import org.meveo.api.dto.response.GetLanguagesIsoResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * Web service for managing {@link org.meveo.model.billing.Language}.
 * 
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @version 6.7.0
 **/
@Path("/languageIso")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Api("LanguageIsoRs")
public interface LanguageIsoRs extends IBaseRs {

	/**
	 * Creates tradingLanguage base on language code. If the language code does not
	 * exists, a language record is created.
	 * 
	 * @param languageIsoDto language iso.
	 * @return action status
	 */
	@POST
	@Path("/")
	@ApiOperation(value = "Create language iso")
	ActionStatus create(@ApiParam("Language iso information") LanguageIsoDto languageIsoDto);

	/**
	 * Search language given a code.
	 * 
	 * @param languageCode code of language
	 * @return language iso for given code
	 */
	@GET
	@Path("/")
	@ApiOperation(value = "Find language iso by code")
	GetLanguageIsoResponse find(@QueryParam("languageCode") @ApiParam("Code of the language") String languageCode);

	/**
	 * Does not delete a language but the tradingLanguage associated to it.
	 * 
	 * @param languageCode code of language.
	 * @return action status
	 */
	@DELETE
	@Path("/{languageCode}")
	@ApiOperation(value = "Remove language iso by code")
	ActionStatus remove(@PathParam("languageCode") @ApiParam("Code of the language") String languageCode);

	/**
	 * modify a language. Same input parameter as create. The language and trading
	 * Language are created if they don't exists. The operation fails if the
	 * tradingLanguage is null.
	 * 
	 * @param languageIsoDto language iso
	 * @return action status
	 */
	@PUT
	@Path("/")
	@ApiOperation(value = "Update language iso")
	ActionStatus update(@ApiParam("Language iso information") LanguageIsoDto languageIsoDto);

	/**
	 * Create or update a language if it doesn't exists.
	 * 
	 * @param languageIsoDto langauge iso
	 * @return action status
	 */
	@POST
	@Path("/createOrUpdate")
	@ApiOperation(value = "Create or update language iso")
	ActionStatus createOrUpdate(@ApiParam("Language iso information") LanguageIsoDto languageIsoDto);

	/**
	 * List all languages.
	 * 
	 * @return all languages
	 */
	@GET
	@Path("/")
	@ApiOperation(value = "List all languages")
	GetLanguagesIsoResponse list();
}
