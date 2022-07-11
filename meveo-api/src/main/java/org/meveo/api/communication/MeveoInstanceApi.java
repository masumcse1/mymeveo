package org.meveo.api.communication;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.BaseApi;
import org.meveo.api.dto.communication.MeveoInstanceDto;
import org.meveo.api.dto.config.MavenConfigurationResponseDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.model.admin.User;
import org.meveo.model.communication.MeveoInstance;
import org.meveo.service.admin.impl.UserService;
import org.meveo.service.communication.impl.MeveoInstanceService;
import org.meveo.service.config.impl.MavenConfigurationService;

/**
 * 
 * @author Tyshan Shi(tyshan@manaty.net)
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @version 6.6.0
 * @since Jun 3, 2016 7:11:03 AM
 */
@Stateless
public class MeveoInstanceApi extends BaseApi {

	@Inject
	private MeveoInstanceService meveoInstanceService;

	@Inject
	private UserService userService;
	
	@Inject
	private MavenConfigurationService mavenConfigurationService;

	public void create(MeveoInstanceDto postData) throws MeveoApiException, BusinessException {
		log.debug("meveo instance api create by code {}", postData.getCode());
		if (StringUtils.isBlank(postData.getCode())) {
			missingParameters.add("code");
		}
		if (StringUtils.isBlank(postData.getUrl())) {
			missingParameters.add("url");
		}
		handleMissingParametersAndValidate(postData);

		MeveoInstance meveoInstance = meveoInstanceService.findByCode(postData.getCode());
		if (meveoInstance != null) {
			throw new EntityAlreadyExistsException(MeveoInstance.class, postData.getCode());
		}

		meveoInstance = new MeveoInstance();
		meveoInstance.setCode(postData.getCode());
		meveoInstance.setDescription(postData.getDescription());
		meveoInstance.setProductName(postData.getProductName());
		meveoInstance.setProductVersion(postData.getProductVersion());
		meveoInstance.setOwner(postData.getOwner());
		meveoInstance.setMd5(postData.getMd5());
		meveoInstance.setStatus(postData.getStatus());
		meveoInstance.setCreationDate(postData.getCreationDate());
		meveoInstance.setUpdateDate(postData.getUpdateDate());
		meveoInstance.setKeyEntreprise(postData.getKeyEntreprise());
		meveoInstance.setMacAddress(postData.getMacAddress());
		meveoInstance.setMachineVendor(postData.getMachineVendor());
		meveoInstance.setInstallationMode(postData.getInstallationMode());
		meveoInstance.setNbCores(postData.getNbCores());
		meveoInstance.setMemory(postData.getMemory());
		meveoInstance.setHdSize(postData.getHdSize());
		meveoInstance.setOsName(postData.getOsName());
		meveoInstance.setOsVersion(postData.getOsVersion());
		meveoInstance.setOsArch(postData.getOsArch());
		meveoInstance.setJavaVmName(postData.getJavaVmName());
		meveoInstance.setJavaVmVersion(postData.getJavaVmVersion());
		meveoInstance.setAsVendor(postData.getAsVendor());
		meveoInstance.setAsVersion(postData.getAsVersion());
		meveoInstance.setUrl(postData.getUrl());
		meveoInstance.setAuthUsername(postData.getAuthUsername());
		meveoInstance.setClearPassword(postData.getAuthPassword());
		if (!StringUtils.isBlank(postData.getUser())) {
			User user = userService.findByUsername(postData.getUser());
			if (user == null) {
				throw new EntityDoesNotExistsException(User.class, postData.getUser());
			}
			meveoInstance.setUser(user);
		}
		meveoInstanceService.create(meveoInstance);
	}

	public void update(MeveoInstanceDto postData) throws MeveoApiException, BusinessException {
		if (StringUtils.isBlank(postData.getCode())) {
			missingParameters.add("code");
		}
		handleMissingParametersAndValidate(postData);

		MeveoInstance meveoInstance = meveoInstanceService.findByCode(postData.getCode());
		if (meveoInstance == null) {
			throw new EntityDoesNotExistsException(MeveoInstance.class, postData.getCode());
		}
		meveoInstance.setCode(StringUtils.isBlank(postData.getUpdatedCode()) ? postData.getCode() : postData.getUpdatedCode());
		meveoInstance.setDescription(postData.getDescription());
		meveoInstance.setProductName(postData.getProductName());
		meveoInstance.setProductVersion(postData.getProductVersion());
		meveoInstance.setOwner(postData.getOwner());
		meveoInstance.setMd5(postData.getMd5());
		meveoInstance.setStatus(postData.getStatus());
		meveoInstance.setCreationDate(postData.getCreationDate());
		meveoInstance.setUpdateDate(postData.getUpdateDate());
		meveoInstance.setKeyEntreprise(postData.getKeyEntreprise());
		meveoInstance.setMacAddress(postData.getMacAddress());
		meveoInstance.setMachineVendor(postData.getMachineVendor());
		meveoInstance.setInstallationMode(postData.getInstallationMode());
		meveoInstance.setNbCores(postData.getNbCores());
		meveoInstance.setMemory(postData.getMemory());
		meveoInstance.setHdSize(postData.getHdSize());
		meveoInstance.setOsName(postData.getOsName());
		meveoInstance.setOsVersion(postData.getOsVersion());
		meveoInstance.setOsArch(postData.getOsArch());
		meveoInstance.setJavaVmName(postData.getJavaVmName());
		meveoInstance.setJavaVmVersion(postData.getJavaVmVersion());
		meveoInstance.setAsVendor(postData.getAsVendor());
		meveoInstance.setAsVersion(postData.getAsVersion());
		if (!StringUtils.isBlank(postData.getUrl())) {
			meveoInstance.setUrl(postData.getUrl());
		}
		meveoInstance.setAuthUsername(postData.getAuthUsername());
		meveoInstance.setClearPassword(postData.getAuthPassword());
		if (!StringUtils.isBlank(postData.getUser())) {
			User user = userService.findByUsername(postData.getUser());
			if (user == null) {
				throw new EntityDoesNotExistsException(User.class, postData.getUser());
			}
			meveoInstance.setUser(user);
		}
		meveoInstanceService.update(meveoInstance);
	}

	public MeveoInstanceDto find(String meveoInstanceCode) throws MeveoApiException {
		if (StringUtils.isEmpty(meveoInstanceCode)) {
			missingParameters.add("meveoInstanceCode");
		}
		handleMissingParameters();

		MeveoInstance meveoInstance = meveoInstanceService.findByCode(meveoInstanceCode);

		if (meveoInstance == null) {
			throw new EntityDoesNotExistsException(MeveoInstance.class, meveoInstanceCode);
		}

		return new MeveoInstanceDto(meveoInstance);
	}

	public void remove(String meveoInstanceCode) throws MeveoApiException, BusinessException {
		if (StringUtils.isBlank(meveoInstanceCode)) {
			missingParameters.add("meveoInstanceCode");
		}
		handleMissingParameters();
		MeveoInstance meveoInstance = meveoInstanceService.findByCode(meveoInstanceCode);

		if (meveoInstance == null) {
			throw new EntityDoesNotExistsException(MeveoInstance.class, meveoInstanceCode);
		}

		meveoInstanceService.remove(meveoInstance);
	}

	public List<MeveoInstanceDto> list() throws MeveoApiException {

		List<MeveoInstanceDto> result = new ArrayList<MeveoInstanceDto>();
		List<MeveoInstance> meveoInstances = meveoInstanceService.list();
		if (meveoInstances != null) {
			for (MeveoInstance meveoInstance : meveoInstances) {
				result.add(new MeveoInstanceDto(meveoInstance));
			}
		}

		return result;
	}

	public void createOrUpdate(MeveoInstanceDto meveoInstanceDto) throws MeveoApiException, BusinessException {
		MeveoInstance meveoInstance = meveoInstanceService.findByCode(meveoInstanceDto.getCode());
		if (meveoInstance == null) {
			create(meveoInstanceDto);
		} else {
			update(meveoInstanceDto);
		}
	}

	public void syncRepositories(String code) throws EntityDoesNotExistsException, BusinessException {

		MeveoInstance meveoInstance = meveoInstanceService.findByCode(code);
		if (meveoInstance == null) {
			throw new EntityDoesNotExistsException(MeveoInstance.class, code);
		}

		Response response = meveoInstanceService.getRemoteRepositories("api/rest/mavenConfiguration", meveoInstance);
		MavenConfigurationResponseDto result = response.readEntity(MavenConfigurationResponseDto.class);
		mavenConfigurationService.updateRepository(result.getMavenConfiguration().getMavenRepositories());
	}
}
