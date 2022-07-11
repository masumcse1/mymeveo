package org.meveo.api.rest.importExport.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.meveo.api.MeveoApiErrorCodeEnum;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.dto.response.utilities.FieldsNotImportedStringCollectionDto;
import org.meveo.api.dto.response.utilities.ImportExportResponseDto;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.api.rest.impl.BaseRs;
import org.meveo.api.rest.importExport.ImportExportRs;
import org.meveo.export.EntityExportImportService;
import org.meveo.export.ExportImportStatistics;
import org.meveo.export.RemoteAuthenticationException;

/**
 * @author Andrius Karpavicius
 * 
 */
@ApplicationScoped
@Interceptors({ WsRestApiInterceptor.class })
public class ImportExportRsImpl extends BaseRs implements ImportExportRs {

    @Inject
    private EntityExportImportService entityExportImportService;

    private LinkedHashMap<String, Future<ExportImportStatistics>> executionResults = new LinkedHashMap<String, Future<ExportImportStatistics>>();

    @Override
    public ImportExportResponseDto importData(MultipartFormDataInput input) {

        try {
            // Check user has utilities/remoteImport permission
            if (!currentUser.hasRole("remoteImport")) {
                throw new RemoteAuthenticationException("User does not have utilities/remoteImport permission");
            }

            cleanupImportResults();

            Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
            List<InputPart> inputParts = uploadForm.get("file");
            if (inputParts == null) {
                return new ImportExportResponseDto(ActionStatusEnum.FAIL, MeveoApiErrorCodeEnum.MISSING_PARAMETER, "Missing a file. File is expected as part name 'file'");
            }
            InputPart inputPart = inputParts.get(0);
            String fileName = getFileName(inputPart.getHeaders());
            if (fileName == null) {
                return new ImportExportResponseDto(ActionStatusEnum.FAIL, MeveoApiErrorCodeEnum.MISSING_PARAMETER, "Missing a file name");
            }

            // Convert the uploaded file from inputstream to a file

            File tempFile = null;
            try {
                InputStream inputStream = inputPart.getBody(InputStream.class, null);
                tempFile = File.createTempFile(FilenameUtils.getBaseName(fileName).replaceAll(" ", "_"), "." + FilenameUtils.getExtension(fileName));
                FileUtils.copyInputStreamToFile(inputStream, tempFile);

            } catch (IOException e) {
                log.error("Failed to save uploaded {} file to temp file {}", fileName, tempFile, e);
                return new ImportExportResponseDto(ActionStatusEnum.FAIL, MeveoApiErrorCodeEnum.GENERIC_API_EXCEPTION, e.getClass().getName() + " " + e.getMessage());
            }
            String executionId = (new Date()).getTime() + "_" + fileName;

            log.info("Received file {} from remote meveo instance. Saved to {} for importing. Execution id {}", fileName, tempFile.getAbsolutePath(), executionId);
            Future<ExportImportStatistics> exportImportFuture = entityExportImportService.importEntities(tempFile, fileName.replaceAll(" ", "_"), false, false);

            executionResults.put(executionId, exportImportFuture);
            return new ImportExportResponseDto(executionId);

        } catch (RemoteAuthenticationException e) {
            log.error("Failed to authenticate for a rest call {}", e.getMessage());
            return new ImportExportResponseDto(ActionStatusEnum.FAIL, MeveoApiErrorCodeEnum.AUTHENTICATION_AUTHORIZATION_EXCEPTION, e.getMessage());

        } catch (Exception e) {
            log.error("Failed to import data from rest call", e);
            return new ImportExportResponseDto(ActionStatusEnum.FAIL, MeveoApiErrorCodeEnum.GENERIC_API_EXCEPTION, e.getClass().getName() + " " + e.getMessage());
        }

    }

    /**
     * Obtain a filename from a header. Header sample: { Content-Type=[image/png], Content-Disposition=[form-data; name="file"; filename="filename.extension"] }
     **/
    private String getFileName(MultivaluedMap<String, String> header) {

        String[] contentDisposition = header.getFirst("Content-Disposition").split(";");

        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {

                String[] name = filename.split("=");

                String finalFileName = name[1].trim().replaceAll("\"", "");
                return finalFileName;
            }
        }
        return null;
    }

    @Override
    public ImportExportResponseDto checkImportDataResult(String executionId) {
        log.debug("Checking remote import execution status {}", executionId);

        Future<ExportImportStatistics> future = executionResults.get(executionId);
        if (future == null) {
            return new ImportExportResponseDto(ActionStatusEnum.FAIL, MeveoApiErrorCodeEnum.INVALID_PARAMETER, "Execution with id " + executionId + " has expired");
        }

        if (future.isDone()) {
            try {
                log.info("Remote import execution {} status is {}", executionId, future.get());
                return exportImportStatisticsToDto(executionId, future.get());

            } catch (InterruptedException | ExecutionException e) {
                return new ImportExportResponseDto(ActionStatusEnum.FAIL, MeveoApiErrorCodeEnum.GENERIC_API_EXCEPTION, "Failed while executing import " + e.getClass().getName()
                        + " " + e.getMessage());
            }
        } else {
            log.info("Remote import execution {} status is still in progress", executionId);
            return new ImportExportResponseDto(executionId);
        }
    }

    /**
     * Remove expired import results - keep for 1 hour only
     */
    private void cleanupImportResults() {

        long hourAgo = (new Date()).getTime() - 3600000;

        List<String> keysToRemove = new ArrayList<>();

        for (String key : executionResults.keySet()) {
            long exportTime = Long.parseLong(key.substring(0, key.indexOf('_')));
            if (exportTime < hourAgo) {
                keysToRemove.add(key);
            }
        }

        for (String key : keysToRemove) {
            log.debug("Removing remote import execution result {}", key);
            executionResults.remove(key);
        }
    }

    @SuppressWarnings("rawtypes")
    public ImportExportResponseDto exportImportStatisticsToDto(String executionId, ExportImportStatistics statistics) {
        ImportExportResponseDto dto = new ImportExportResponseDto(executionId);

        if (statistics.getException() != null) {
            dto.setExceptionMessage(statistics.getException().getClass().getSimpleName() + ": " + statistics.getException().getMessage());
        }
        dto.setFailureMessageKey(statistics.getErrorMessageKey());

        if (!statistics.getFieldsNotImported().isEmpty()) {
            dto.setFieldsNotImported(new HashMap<String, FieldsNotImportedStringCollectionDto>());
            for (Map.Entry<String, Collection<String>> entry : statistics.getFieldsNotImported().entrySet()) {
                dto.getFieldsNotImported().put(entry.getKey(), new FieldsNotImportedStringCollectionDto(entry.getValue()));
            }
        }
        dto.setSummary(new HashMap<String, Integer>());
        for (Entry<Class, Integer> summaryInfo : statistics.getSummary().entrySet()) {
            dto.getSummary().put(summaryInfo.getKey().getName(), summaryInfo.getValue());
        }

        return dto;
    }
}