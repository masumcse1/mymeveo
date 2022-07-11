package org.meveo.api.dwh;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.BaseCrudApi;
import org.meveo.api.dto.dwh.MeasurableQuantityDto;
import org.meveo.api.dto.dwh.MeasuredValueDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.InvalidParameterException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.dwh.MeasurableQuantity;
import org.meveo.model.dwh.MeasuredValue;
import org.meveo.model.dwh.MeasurementPeriodEnum;
import org.meveo.service.base.local.IPersistenceService;
import org.meveocrm.services.dwh.MeasurableQuantityService;
import org.meveocrm.services.dwh.MeasuredValueService;

/**
 * @author Andrius Karpavicius
 * @author Edward P. Legaspi | edward.legaspi@manaty.net
 * @version 6.10
 **/
@Stateless
public class MeasurableQuantityApi extends BaseCrudApi<MeasurableQuantity, MeasurableQuantityDto> {

    @Inject
    private MeasurableQuantityService measurableQuantityService;
    
    @Inject
    private MeasuredValueService mvService;
    
    public MeasurableQuantityApi() {
    	super(MeasurableQuantity.class, MeasurableQuantityDto.class);
    }

    public MeasurableQuantity create(MeasurableQuantityDto postData) throws MeveoApiException, BusinessException {

        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("code");            
        }
        
        handleMissingParametersAndValidate(postData);

        if (measurableQuantityService.findByCode(postData.getCode()) != null) {
            throw new EntityAlreadyExistsException(MeasurableQuantity.class, postData.getCode());
        }

        MeasurableQuantity measurableQuantity = fromDTO(postData, null);
        measurableQuantityService.create(measurableQuantity);

        return measurableQuantity;
    }

    public MeasurableQuantity update(MeasurableQuantityDto postData) throws MeveoApiException, BusinessException {
        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("measurableQuantityCode");            
        }
        
        handleMissingParametersAndValidate(postData);

        MeasurableQuantity measurableQuantity = measurableQuantityService.findByCode(postData.getCode());
        if (measurableQuantity == null) {
            throw new EntityDoesNotExistsException(MeasurableQuantity.class, postData.getCode());
        }

        measurableQuantity = fromDTO(postData, measurableQuantity);
        measurableQuantity = measurableQuantityService.update(measurableQuantity);

        return measurableQuantity;

    }

    /* (non-Javadoc)
     * @see org.meveo.api.ApiService#find(java.lang.String)
     */
    @Override
    public MeasurableQuantityDto find(String code) throws EntityDoesNotExistsException, MissingParameterException, InvalidParameterException, MeveoApiException {

        if (StringUtils.isBlank(code)) {
            missingParameters.add("code");
            handleMissingParameters();
        }

        MeasurableQuantity measurableQuantity = measurableQuantityService.findByCode(code);
        if (measurableQuantity == null) {
            throw new EntityDoesNotExistsException(MeasurableQuantity.class, code);
        }

        MeasurableQuantityDto result = new MeasurableQuantityDto(measurableQuantity);

        return result;
    }
    
    public void remove(String code) throws MeveoApiException, BusinessException {

        if (StringUtils.isBlank(code)) {
            missingParameters.add("code");
            handleMissingParameters();
        }

        MeasurableQuantity measurableQuantity = measurableQuantityService.findByCode(code);
        if (measurableQuantity == null) {
            throw new EntityDoesNotExistsException(MeasurableQuantity.class, code);
        }

        measurableQuantityService.remove(measurableQuantity);
    }

    @Override
    public MeasurableQuantity createOrUpdate(MeasurableQuantityDto postData) throws MeveoApiException, BusinessException {
        MeasurableQuantity measurableQuantity = measurableQuantityService.findByCode(postData.getCode());
        if (measurableQuantity == null) {
            // create
            return create(postData);
        } else {
            // update
            return update(postData);
        }
    }

    public List<MeasurableQuantityDto> list(String measurableQuantityCode) {

        List<MeasurableQuantity> measurableQuantities = null;
        if (StringUtils.isBlank(measurableQuantityCode)) {
            measurableQuantities = measurableQuantityService.list();
        } else {
            measurableQuantities = measurableQuantityService.findByCodeLike(measurableQuantityCode);
        }

        List<MeasurableQuantityDto> measurableQuantityDtos = new ArrayList<MeasurableQuantityDto>();

        for (MeasurableQuantity measurableQuantity : measurableQuantities) {
            measurableQuantityDtos.add(new MeasurableQuantityDto(measurableQuantity));
        }

        return measurableQuantityDtos;
    }

    private MeasurableQuantity fromDTO(MeasurableQuantityDto dto, MeasurableQuantity mqToUpdate) {

        MeasurableQuantity mq = new MeasurableQuantity();
        if (mqToUpdate != null) {
            mq = mqToUpdate;
        }

        mq.setCode(StringUtils.isBlank(dto.getUpdatedCode()) ? dto.getCode() : dto.getUpdatedCode());
        mq.setDescription(dto.getDescription());
        mq.setTheme(dto.getTheme());
        mq.setDimension1(dto.getDimension1());
        mq.setDimension2(dto.getDimension2());
        mq.setDimension3(dto.getDimension3());
        mq.setDimension4(dto.getDimension4());
        mq.setEditable(dto.isEditable());
        mq.setAdditive(dto.isAdditive());
        mq.setSqlQuery(dto.getSqlQuery());
        mq.setCypherQuery(dto.getCypherQuery());
        mq.setMeasurementPeriod(dto.getMeasurementPeriod());
        mq.setLastMeasureDate(dto.getLastMeasureDate());

        return mq;
    }
    
	public List<MeasuredValueDto> findMVByDateAndPeriod(String code, Date fromDate, Date toDate, MeasurementPeriodEnum period, String mqCode)
			throws MeveoApiException {

		if (StringUtils.isBlank(mqCode)) {
			missingParameters.add("mqCode");
		}

		handleMissingParameters();

		MeasurableQuantity mq = measurableQuantityService.findByCode(mqCode);
		if (mq == null) {
			throw new EntityDoesNotExistsException(MeasurableQuantity.class, mqCode);
		}

		List<MeasuredValueDto> result = new ArrayList<>();

		if(period == null){
			period = mq.getMeasurementPeriod();
		}
		List<MeasuredValue> measuredValues = mvService.getByDateAndPeriod(code, fromDate, toDate, period, mq);
		if (measuredValues != null) {
			for (MeasuredValue mv : measuredValues) {
				result.add(new MeasuredValueDto(mv));
			}
		}

		return result;
	}

	@Override
	public MeasurableQuantityDto toDto(MeasurableQuantity entity) {
		return new MeasurableQuantityDto(entity);
	}

	@Override
	public MeasurableQuantity fromDto(MeasurableQuantityDto dto) throws MeveoApiException {
		return fromDTO(dto, new MeasurableQuantity());
	}

	@Override
	public IPersistenceService<MeasurableQuantity> getPersistenceService() {
		return measurableQuantityService;
	}

	@Override
	public boolean exists(MeasurableQuantityDto dto) {
		return measurableQuantityService.findByCode(dto.getCode()) != null;
	}

	@Override
	public void remove(MeasurableQuantityDto dto) throws MeveoApiException, BusinessException {
		var mq = measurableQuantityService.findByCode(dto.getCode());
		if(mq != null) {
			measurableQuantityService.remove(mq);
		}
	}
	
}