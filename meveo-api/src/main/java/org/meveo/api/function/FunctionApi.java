/*
 * (C) Copyright 2018-2019 Webdrone SAS (https://www.webdrone.fr/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. This program is
 * not suitable for any direct or indirect application in MILITARY industry See the GNU Affero
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.api.function;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.ws.rs.PathParam;

import org.apache.commons.io.FileUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.ElementNotFoundException;
import org.meveo.api.dto.function.FunctionDto;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.jobs.TimerEntity;
import org.meveo.model.scripts.Function;
import org.meveo.model.scripts.Sample;
import org.meveo.service.job.JobExecutionService;
import org.meveo.service.job.JobInstanceService;
import org.meveo.service.job.TimerEntityService;
import org.meveo.service.script.ConcreteFunctionService;
import org.meveo.service.script.DefaultFunctionService;
import org.meveo.service.script.FunctionService;

/**
 * @author clement.bareth
 * @author Edward P. Legaspi | <czetsuya@gmail.com>
 * @version 6.9.0
 * @since 6.5.0
 */
public class FunctionApi {

    public static final String TEST_MODE = "test-mode";

    @Inject
    private JobInstanceService jobService;

    @Inject
    private JobExecutionService jobExecutionService;

    @Inject
    private ConcreteFunctionService concreteFunctionService;
    
    @EJB
    private DefaultFunctionService defaultFunctionService;

    @Inject
    private TimerEntityService timerEntityService;
    
    /**
     * @param code Function code
     * @return the dto represenation of the function, or null if not found.
     * @throws BusinessException 
     */
    public FunctionDto find(String code) throws BusinessException {
    	Function function = concreteFunctionService.findByCode(code);
    	if(function == null) {
    		return null;
    	}
    	
        final FunctionDto functionDto = new FunctionDto();
        functionDto.setCode(function.getCode());
        functionDto.setTestSuite(function.getTestSuite());
        functionDto.setInputs(concreteFunctionService.getInputs(function));
        functionDto.setOutputs(concreteFunctionService.getOutputs(function));
        if(function.getCategory() != null) {
        	function.setCategory(function.getCategory());
        }
        return functionDto;
    }

    /**
     * @return the list of dto representation of the functions in databases
     */
    public List<FunctionDto> list() throws BusinessException {
        final List<Function> functions = concreteFunctionService.list();
        List<FunctionDto> result = new LinkedList<FunctionDto>();
        for (Function e : functions) {
            final FunctionDto functionDto = new FunctionDto();
            functionDto.setCode(e.getCode());
            functionDto.setTestSuite(e.getTestSuite());
			functionDto.setInputs(concreteFunctionService.getInputs(e));
            functionDto.setOutputs(concreteFunctionService.getOutputs(e));
            if(e.getCategory() != null) {
            	e.setCategory(e.getCategory());
            }            
            result.add(functionDto);
        }
        return result;
    }

    public Map<String, Object> execute(String code, Map<String, Object> inputs) throws BusinessException {
        return concreteFunctionService.getFunctionService(code).execute(code, inputs); 
    }

    public String getTest(String code) {
        final Function function = concreteFunctionService.findByCode(code);
        return function.getTestSuite();
    }

    /**
	 * Update test suite and schedule or re-schedule execution
	 *
	 * @param functionCode Code of the function to update
	 * @param file         Test Suite content
	 * @throws BusinessException if an error occurs
	 * @throws IOException       if the file cannot be read
	 */
    public void updateTest(String functionCode, File file) throws BusinessException, IOException {
        final String testSuite = FileUtils.readFileToString(file, "UTF-8");
        defaultFunctionService.updateTestSuite(functionCode, testSuite);

        final String testJobCode = getTestJobCode(functionCode);
        JobInstance jobInstance = jobService.findByCode(testJobCode);
        TimerEntity timerEntity;

        final Matcher matcher = Pattern.compile("<stringProp name=\"periodicity\">(.*)</stringProp>").matcher(testSuite);
        if(matcher.find()){
            timerEntity = timerEntityService.findByCode(matcher.group(1));
        } else {
            timerEntity = timerEntityService.findByCode("Daily-midnight");
        }

        // If job does not exists, create it, otherwise re-schedule it
        if (jobInstance == null) {
            jobInstance = new JobInstance();
            jobInstance.setJobCategoryEnum(JobCategoryEnum.TEST);
            jobInstance.setJobTemplate(FunctionService.FUNCTION_TEST_JOB);
            jobInstance.setCode(testJobCode);
            jobInstance.setParametres(functionCode);

            jobInstance.setTimerEntity(timerEntity);
            jobService.create(jobInstance);
        }else{
            jobInstance.setTimerEntity(timerEntity);
            jobService.update(jobInstance);
            jobService.scheduleUnscheduleJob(jobInstance.getId());
        }

    }

    @Asynchronous
    public void startJob(@PathParam("code") String fnCode) throws BusinessException {
        JobInstance jobInstance = jobService.findByCode(getTestJobCode(fnCode));
        jobExecutionService.executeJob(jobInstance, null);
    }

    public static String getTestJobCode(String functionCode) {
        return "FunctionTestJob_" + functionCode;
    }

    public List<Sample> getSamples(String functionCode) throws ElementNotFoundException {
        return concreteFunctionService.getFunctionService(functionCode).getSamples(functionCode);
    }

}