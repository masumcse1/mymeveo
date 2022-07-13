package org.meveo.api.rest.impl;

import io.swagger.annotations.ApiOperation;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.ValidationException;
import org.meveo.api.MeveoApiErrorCodeEnum;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.dto.response.BaseResponse;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.api.rest.IBaseRs;
import org.meveo.api.rest.exception.*;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.crm.Provider;
import org.meveo.security.CurrentUser;
import org.meveo.security.MeveoUser;
import org.meveo.util.ApplicationProvider;
import org.meveo.util.MeveoParamBean;
import org.meveo.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.sql.SQLException;
import java.util.Set;

/**
 * @author Edward P. Legaspi
 **/
public abstract class BaseRs implements IBaseRs {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    @MeveoParamBean
    protected ParamBean paramBean;

    @Context
    protected HttpServletRequest httpServletRequest;

    // one way to get HttpServletResponse
    @Context
    protected HttpServletResponse httpServletResponse;

    @Inject
    @CurrentUser
    protected MeveoUser currentUser;

    @Inject
    @ApplicationProvider
    protected Provider appProvider;

    protected final String RESPONSE_DELIMITER = " - ";

    @Override
    public ActionStatus index() {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "Meveo Rest API version " + Version.appVersion + " flush " + Version.buildNumber);
        return result;
    }

    /**
     * Returns the authenticated user.
     * 
     * @return action status.
     */
    @GET
    @Path("/user")
    @ApiOperation(value = "user", hidden = true)
    public ActionStatus user() {
        ActionStatus result = new ActionStatus();

        result = new ActionStatus(ActionStatusEnum.SUCCESS, "WS User is=" + currentUser.getUserName());

        return result;
    }

    protected Response.ResponseBuilder createResponseFromMeveoApiException(MeveoApiException e, ActionStatus result) {
        Response.ResponseBuilder responseBuilder = null;

        if (e instanceof EntityDoesNotExistsException) {
            responseBuilder = Response.status(Response.Status.NOT_FOUND).entity(result);
        } else if (e instanceof EntityAlreadyExistsException) {
            responseBuilder = Response.status(Response.Status.FOUND).entity(result);
        } else if (e instanceof MissingParameterException) {
            responseBuilder = Response.status(Response.Status.PRECONDITION_FAILED).entity(result);
        } else {
            responseBuilder = Response.status(Response.Status.BAD_REQUEST).entity(result);
        }

        return responseBuilder;
    }

    protected ResponseBuilder createResponseFromMeveoApiException(MeveoApiException e, BaseResponse result) {
        Response.ResponseBuilder responseBuilder = null;

        if (e instanceof EntityDoesNotExistsException) {
            responseBuilder = Response.status(Response.Status.NOT_FOUND).entity(result);
        } else if (e instanceof EntityAlreadyExistsException) {
            responseBuilder = Response.status(Response.Status.FOUND).entity(result);
        } else if (e instanceof MissingParameterException) {
            responseBuilder = Response.status(Response.Status.PRECONDITION_FAILED).entity(result);
        } else {
            responseBuilder = Response.status(Response.Status.BAD_REQUEST).entity(result);
        }

        return responseBuilder;
    }

    /**
     * Process exception and update status of response
     * 
     * @param e Exception
     * @param status Status dto to update
     */
    protected void processException(Exception e, ActionStatus status) {
    	boolean unkwownException = true;

        if (e instanceof MeveoApiException) {
            status.setErrorCode(((MeveoApiException) e).getErrorCode());
            status.setStatus(ActionStatusEnum.FAIL);
            status.setMessage(e.getMessage());
            unkwownException = false;

        } else {

            String message = e.getMessage();
            
            MeveoApiErrorCodeEnum errorCode = e instanceof BusinessException ? MeveoApiErrorCodeEnum.BUSINESS_API_EXCEPTION : MeveoApiErrorCodeEnum.GENERIC_API_EXCEPTION;
            Throwable cause = e;

            // See if can get to the root of the exception cause
            if (!(e instanceof BusinessException)) {
                cause = e.getCause();
                while (cause != null) {

                    message = cause.getMessage();

                    if (cause instanceof SQLException || cause instanceof BusinessException || cause instanceof ConstraintViolationException) {
                        unkwownException = false;

                        if (cause instanceof ConstraintViolationException) {
                            ConstraintViolationException cve = (ConstraintViolationException) (cause);
                            Set<ConstraintViolation<?>> violations = cve.getConstraintViolations();
                            message = "";
                            for (ConstraintViolation<?> cv : violations) {
                                message += cv.getPropertyPath() + " " + cv.getMessage() + ",";
                            }
                            errorCode = MeveoApiErrorCodeEnum.INVALID_PARAMETER;
                        } else {
                            errorCode = cause instanceof BusinessException ? MeveoApiErrorCodeEnum.BUSINESS_API_EXCEPTION : MeveoApiErrorCodeEnum.GENERIC_API_EXCEPTION;
                        }
                        
                        break;
                    }
                    
                    if(cause instanceof IllegalArgumentException) {
                    	unkwownException = false;
                    	errorCode = MeveoApiErrorCodeEnum.INVALID_PARAMETER;
                    	break;
                    }
                    
                    cause = cause.getCause();
                }
            }
            
            if(e instanceof ValidationException) {
            	unkwownException = false;
            	errorCode = MeveoApiErrorCodeEnum.INVALID_PARAMETER;
            }
            
            status.setStatus(ActionStatusEnum.FAIL);
            status.setErrorCode(errorCode);
            status.setMessage(message);
        }
        
        if(unkwownException) {
            log.warn("Failed to execute API", e);
        }

        handleErrorStatus(status);
    }

    private void handleErrorStatus(ActionStatus status) {
        if (StringUtils.isBlank(status.getErrorCode())) {
            throw new InternalServerErrorException(status);
        } else {
            String str = status.getErrorCode().toString();
            if ("MISSING_PARAMETER".equals(str)//
                    || "INVALID_PARAMETER".equals(str)//
                    || "INVALID_ENUM_VALUE".equals(str)//
                    || "INVALID_IMAGE_DATA".equals(str)) {
                throw new BadRequestException(status);
            } else if ("UNAUTHORIZED".equals(str) //
                    || "AUTHENTICATION_AUTHORIZATION_EXCEPTION".equals(str)) {
                throw new NotAuthorizedException(status);
            } else if ("ENTITY_ALREADY_EXISTS_EXCEPTION".equals(str) //
                    || "DELETE_REFERENCED_ENTITY_EXCEPTION".equals(str) //
                    || "DUPLICATE_ACCESS".equals(str) //
                    || "INSUFFICIENT_BALANCE".equals(str)//
                    || "ACTION_FORBIDDEN".equals(str)//
                    || "INSUFFICIENT_BALANCE".equals(str)//
                    || "DUPLICATE_ACCESS".equals(str)) {
                throw new ForbiddenException(status);
            } else if ("ENTITY_DOES_NOT_EXISTS_EXCEPTION".equals(str)) {
                throw new NotFoundException(status);
            } else {
                throw new InternalServerErrorException(status);
            }
        }
    }
}