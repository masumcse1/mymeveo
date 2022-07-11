package org.meveo.api.rest.importExport;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.meveo.api.dto.response.utilities.ImportExportResponseDto;
import org.meveo.api.rest.IBaseRs;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * Web service for importing and exporting data to another instance of
 * application.
 * 
 * @author Andrius Karpavicius
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @version 6.7.0
 **/
@Path("/importExport")
@Consumes({ MediaType.MULTIPART_FORM_DATA })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Api("ImportExportRs")
public interface ImportExportRs extends IBaseRs {

	/**
	 * Send a file to be imported. ImportExportResponseDto.executionId contains
	 * 
	 * @param input file containing a list of object for import
	 * @return As import is async process, ImportExportResponseDto.executionId
	 *         contains and ID to be used to query for execution results via a call
	 *         to /importExport/checkImportDataResult?id=..
	 */
	@POST
	@Path("/importData")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@ApiOperation(value = "Import data")
	ImportExportResponseDto importData(@ApiParam("File containing a list of object for import") MultipartFormDataInput input);

	/**
	 * Check for execution results for a given execution identifier
	 * 
	 * @param executionId Returned in /importExport/importData call
	 * @return the execution result
	 */
	@GET
	@Path("/checkImportDataResult")
	@ApiOperation(value = "Check export data result")
	ImportExportResponseDto checkImportDataResult(@QueryParam("executionId") @ApiParam("The execution id") String executionId);
}