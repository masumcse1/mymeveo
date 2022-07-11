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

package org.meveo.admin.action.admin.custom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.admin.web.interceptor.ActionMethod;
import org.meveo.jpa.CurrentRepositoryProvider;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldValues;
import org.meveo.model.customEntities.CustomEntityInstance;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.model.persistence.JacksonUtil;
import org.meveo.model.persistence.sql.SQLStorageConfiguration;
import org.meveo.model.storage.Repository;
import org.meveo.service.base.NativePersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.crm.impl.CustomFieldInstanceService;
import org.meveo.service.crm.impl.CustomFieldTemplateService;
import org.meveo.service.custom.CustomEntityTemplateService;
import org.meveo.service.custom.CustomTableService;
import org.meveo.service.custom.DataImportExportStatistics;
import org.meveo.util.view.NativeTableBasedDataModel;
import org.omnifaces.cdi.Cookie;
import org.omnifaces.util.Faces;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.event.CellEditEvent;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.primefaces.model.UploadedFile;

/**
 * @author Clement Bareth
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @version 6.9.0
 */
@Named
@ViewScoped
public class CustomTableBean extends BaseBean<CustomEntityTemplate> {

	private static final long serialVersionUID = -2748591950645172132L;

	@Inject
	private transient CustomTableService customTableService;

	@Inject
	private CustomEntityTemplateService customEntityTemplateService;

	@Inject
	private CustomFieldTemplateService customFieldTemplateService;

	@Inject
	protected CustomFieldDataEntryBean customFieldDataEntryBean;

	@Inject
	private CustomFieldInstanceService customFieldInstanceService;

	@Inject
	protected CurrentRepositoryProvider repositoryProvider;
	/**
	 * Custom table name. Determined from customEntityTemplate.code value.
	 */
	private String customTableName;

	/**
	 * Custom table fields. Come from custom fields defined in custom entity.
	 */
	private Map<String, CustomFieldTemplate> fields;

	private List<CustomFieldTemplate> quickAddFields;

	private List<CustomFieldTemplate> summaryFields;

	private List<CustomFieldTemplate> filterFields;

	private List<CustomFieldTemplate> customFieldTemplateList = new ArrayList<>();

	private LazyDataModel<Map<String, Object>> customTableBasedDataModel;

	private Map<String, Object> newValues = new HashMap<>();

	private boolean appendImportedData;

	private List<Map<String, Object>> selectedValues;

	private Future<DataImportExportStatistics> exportFuture;

	private Future<DataImportExportStatistics> importFuture;

	private String cet;

	private Map<String, Object> selectedRow;

	private int selectedRowIndex;

	private CustomFieldTemplate selectedRowField;

	private int listSize = 0;

	@Cookie
	private Repository repository;

	public CustomTableBean() {
		super(CustomEntityTemplate.class);
	}

	@Override
	public CustomEntityTemplate initEntity() {

		if (cet != null) {
			entity = customEntityTemplateService.findByCode(cet);
		} else if (getObjectId() != null) {
			entity = customEntityTemplateService.findById(getObjectId());
		} else {
			return null;
		}

		customTableName = SQLStorageConfiguration.getDbTablename(entity);

		// Get fields and sort them by GUI order
		Map<String, CustomFieldTemplate> cfts = customFieldTemplateService.findByAppliesTo(entity.getAppliesTo());
		if (cfts != null) {
			GroupedCustomField groupedCFTAndActions = new GroupedCustomField(cfts.values(), "Custom fields", true);
			List<GroupedCustomField> groupedCustomFields = groupedCFTAndActions.getChildren();
			if (groupedCustomFields != null) {
				int i = 0;
				for (GroupedCustomField groupedCustomField : groupedCustomFields.get(i).getChildren()) {
					List<CustomFieldTemplate> list = new ArrayList<>();
					if (groupedCustomField != null) {
						CustomFieldTemplate cft = (CustomFieldTemplate) groupedCustomField.getData();
						list.add(cft);
					}
					i++;
					customFieldTemplateList.addAll(list);
				}
			}
			fields = cfts;
			summaryFields = customFieldTemplateList.stream().filter(c -> c.isSummary()).collect(Collectors.toList());
			filterFields = customFieldTemplateList.stream().filter(c -> c.isFilter()).collect(Collectors.toList());
			quickAddFields = customFieldTemplateList.stream().filter(c -> c.isSummary() && c.isValueRequired()).collect(Collectors.toList());
		}

		return entity;
	}

	public int getSelectedRowIndex() {
		return selectedRowIndex;
	}

	public void setSelectedRowIndex(int selectedRowIndex) {
		this.selectedRowIndex = selectedRowIndex;
	}

	public CustomFieldTemplate getSelectedRowField() {
		return selectedRowField;
	}

	public void setSelectedRowField(CustomFieldTemplate selectedRowField) {
		this.selectedRowField = selectedRowField;
	}

	public Map<String, Object> getSelectedRow() {
		return selectedRow;
	}

	public void setSelectedRow(Map<String, Object> selectedRow) {
		this.selectedRow = selectedRow;
	}

	public List<Map<String, Object>> list() {
		List<Map<String, Object>> list = customTableService.list(getSqlConnectionCode(), entity);
		listSize = list.size();
		return list;
	}

	/**
	 * DataModel for primefaces lazy loading datatable component.
	 * 
	 * @return LazyDataModel implementation.
	 */
	public LazyDataModel<Map<String, Object>> getDataModel() {
		LazyDataModel<Map<String, Object>> dataModel2 = getDataModel(filters);
		if (dataModel2 != null) {
			listSize = dataModel2.getRowCount();
		}
		return dataModel2;
	}

	/**
	 * DataModel for primefaces lazy loading datatable component.
	 * 
	 * @param inputFilters Search criteria
	 * @return LazyDataModel implementation.
	 */
	public LazyDataModel<Map<String, Object>> getDataModel(Map<String, Object> inputFilters) {
		if (customTableBasedDataModel == null && customTableName != null) {

			final Map<String, Object> filters = inputFilters;

			customTableBasedDataModel = new NativeTableBasedDataModel() {

				private static final long serialVersionUID = 6682319740448829853L;

				@Override
				protected Map<String, Object> getSearchCriteria() {
					return filters;
				}

				@Override
				protected CustomTableService getPersistenceServiceImpl() {
					return customTableService;
				}

				@Override
				protected String getTableName() {
					return CustomTableBean.this.getCustomTableName();
				}

				@Override
				protected CustomEntityTemplate getCet() {
					return entity;
				}

				@Override
				protected String getSqlConnectionCode() {
					return CustomTableBean.this.getSqlConnectionCode();
				}
			};
		}

		return customTableBasedDataModel;
	}

	public List<CustomFieldTemplate> getSummaryFields() {
		if (entity == null) {
			initEntity();
		}
		return summaryFields;
	}

	public List<CustomFieldTemplate> getQuickAddFields() {
		if (entity == null) {
			initEntity();
		}
		return quickAddFields;
	}

	public List<CustomFieldTemplate> getFilterFields() {
		if (entity == null) {
			initEntity();
		}
		return filterFields;
	}

	/**
	 * Clean search fields in datatable.
	 */
	@Override
	public void clean() {
		customTableBasedDataModel = null;
		filters = new HashMap<>();
	}

	/**
	 * @return Custom table name
	 */
	public String getCustomTableName() {
		if (customTableName == null) {
			initEntity();
		}
		return customTableName;
	}

	/**
	 * @param customTableName Custom table name
	 */
	public void setCustomTableName(String customTableName) {
		this.customTableName = customTableName;
	}

	/**
	 * @return Custom table fields
	 */
	public Collection<CustomFieldTemplate> getFields() {
		if (entity == null) {
			initEntity();
		}
		return customFieldTemplateList;
	}

	@Override
	protected IPersistenceService<CustomEntityTemplate> getPersistenceService() {
		return customEntityTemplateService;
	}

	/**
	 * @param event the Value in Datatable edit event
	 * @throws BusinessException General exception
	 */
	@SuppressWarnings("unchecked")
	@ActionMethod
	public void onCellEdit(CellEditEvent event) throws BusinessException {
		DataTable o = (DataTable) event.getSource();
		Map<String, Object> mapValue = (Map<String, Object>) o.getRowData();

		CustomEntityInstance cei = new CustomEntityInstance();
		cei.setCet(entity);
		cei.setCetCode(entity.getCode());
		cei.setUuid((String) mapValue.get("uuid"));
		customFieldInstanceService.setCfValues(cei, entity.getCode(), mapValue);

		customTableService.update(getSqlConnectionCode(), entity, cei);
		messages.info(new BundleKey("messages", "customTable.valuesSaved"));
	}

	@SuppressWarnings("unchecked")
	@ActionMethod
	public void onEntityReferenceSelected(SelectEvent event) throws BusinessException {
		Map<String, Object> selectedEntityInPopup = (Map<String, Object>) event.getObject();
		Object newId = selectedEntityInPopup.get("uuid");
		selectedRow.put(selectedRowField.getDbFieldname(), newId);

		CustomEntityInstance cei = new CustomEntityInstance();
		cei.setCet(entity);
		cei.setCetCode(entity.getCode());
		cei.setUuid((String) selectedRow.get("uuid"));
		customFieldInstanceService.setCfValues(cei, entity.getCode(), selectedRow);

		customTableService.update(getSqlConnectionCode(), entity, cei);
		messages.info(new BundleKey("messages", "customTable.valuesSaved"));
	}

	public void onChildEntityUpdated(CustomFieldValues cfValues) throws BusinessException {
		String serializedValues = JacksonUtil.toString(cfValues.getValues());
		selectedRow.put(selectedRowField.getDbFieldname(), serializedValues);

		CustomEntityInstance cei = new CustomEntityInstance();
		cei.setCet(entity);
		cei.setCetCode(entity.getCode());
		cei.setUuid((String) selectedRow.get("uuid"));
		customFieldInstanceService.setCfValues(cei, entity.getCode(), selectedRow);

		customTableService.update(getSqlConnectionCode(), entity, cei);
		messages.info(new BundleKey("messages", "customTable.valuesSaved"));
	}

	public Map<String, Object> getNewValues() {
		return newValues;
	}

	public void setNewValues(Map<String, Object> newValues) {
		this.newValues = newValues;
	}

	/**
	 * Add new values to a map of values, setting a default value if applicable
	 * 
	 * @throws BusinessException General exception
	 */
	@ActionMethod
	public void addNewValues() throws BusinessException {

		Map<String, Object> convertedValues = customTableService.convertValue(newValues, fields, false, null);
		CustomEntityInstance cei = new CustomEntityInstance();
		cei.setCet(entity);
		cei.setCetCode(entity.getCode());
		cei.setUuid((String) convertedValues.get("uuid"));
		customFieldInstanceService.setCfValues(cei, entity.getCode(), convertedValues);

		customTableService.create(getSqlConnectionCode(), entity, cei);
		messages.info(new BundleKey("messages", "customTable.valuesSaved"));
		newValues = new HashMap<>();
		customTableBasedDataModel = null;
	}

	@ActionMethod
	public void update(Map<String, Object> values) throws BusinessException {
		CustomEntityInstance cei = new CustomEntityInstance();
		cei.setCet(entity);
		cei.setCetCode(entity.getCode());
		cei.setUuid((String) values.get("uuid"));
		customFieldInstanceService.setCfValues(cei, entity.getCode(), values);

		customTableService.update(getSqlConnectionCode(), entity, cei);
		messages.info(new BundleKey("messages", "customTable.valuesSaved"));
	}

	/**
	 * Handle a file upload and import the file
	 * 
	 * @param event File upload event
	 */
	@ActionMethod
	public void handleFileUpload(FileUploadEvent event) {
		UploadedFile file = event.getFile();

		if (file == null) {
			messages.warn(new BundleKey("messages", "customTable.importFile.fileRequired"));
			return;
		}

		try {
			if (!appendImportedData) {
				// Delete current data first if in override mode
				customTableService.remove(repositoryProvider.getRepository().getSqlConfigurationCode(), entity);
			}

			importFuture = customTableService.importDataAsync(getSqlConnectionCode(), entity, file.getInputstream(), appendImportedData);
			messages.info(new BundleKey("messages", "customTable.importFile.started"));

		} catch (Exception e) {
			log.error("Failed to initialize custom table data import", e);
			messages.info(new BundleKey("messages", "customTable.importFile.startFailed"), e.getMessage());
		}
	}

	public boolean isAppendImportedData() {
		return appendImportedData;
	}

	public void setAppendImportedData(boolean appendImportedData) {
		this.appendImportedData = appendImportedData;
	}

	public List<Map<String, Object>> getSelectedValues() {
		return selectedValues;
	}

	public void setSelectedValues(List<Map<String, Object>> selectedValues) {
		this.selectedValues = selectedValues;
	}

	/**
	 * Construct a CSV file format (header) for file import
	 * 
	 * @return CSV file field order
	 */
	public String getCsvFileFormat() {
		StringBuilder format = new StringBuilder();

		format.append(NativePersistenceService.FIELD_ID).append("(optional)");

		for (CustomFieldTemplate field : fields.values()) {
			format.append(",");
			format.append(field.getDbFieldname());
		}

		return format.toString();
	}

	@Override
	public void delete(Long id) {
		// Should not be used
	}

	@ActionMethod
	public void delete(String uuid) throws BusinessException {
		customTableService.remove(repositoryProvider.getRepository().getSqlConfigurationCode(), entity, uuid);
		customTableBasedDataModel = null;
		messages.info(new BundleKey("messages", "delete.successful"));
	}

	@Override
	@ActionMethod
	public void deleteMany() throws BusinessException {

		if (selectedValues == null || selectedValues.isEmpty()) {
			messages.info(new BundleKey("messages", "delete.entitities.noSelection"));
			return;
		}
		Set<String> ids = new HashSet<>();

		for (Map<String, Object> values : selectedValues) {

			Object uuid = values.get(NativePersistenceService.FIELD_ID);
			ids.add((String) uuid);

		}

		customTableService.remove(repositoryProvider.getRepository().getSqlConfigurationCode(), entity, ids);
		customTableBasedDataModel = null;
		messages.info(new BundleKey("messages", "delete.entitities.successful"));
	}

	@ActionMethod
	public void exportData() {
		exportFuture = null;

		PaginationConfiguration config = new PaginationConfiguration(filters, "uuid", SortOrder.ASCENDING);

		try {
			exportFuture = customTableService.exportData(getSqlConnectionCode(), entity, config);
			messages.info(new BundleKey("messages", "customTable.exportFile.started"));

		} catch (Exception e) {
			log.error("Failed to initialize custom table data export", e);
			messages.info(new BundleKey("messages", "customTable.exportFile.startFailed"), e.getMessage());
		}
	}

	public void handleRepositoryChangeEvent() {
		dataModel = null;
		filters = null;
		Faces.addResponseCookie("repository", getRepository().getCode(), Integer.MAX_VALUE);
		repositoryProvider.setRepository(getRepository());
	}

	public Future<DataImportExportStatistics> getExportFuture() {
		return exportFuture;
	}

	public Future<DataImportExportStatistics> getImportFuture() {
		return importFuture;
	}

	public String getCet() {
		return cet;
	}

	public int getListSize() {
		return listSize;
	}

	public void setListSize(int listSize) {
		this.listSize = listSize;
	}

	public void setCet(String cet) {
		this.cet = cet;
	}

	public Repository getRepository() {
		return repository;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public String getSqlConnectionCode() {
		return repository == null ? null : repository.getSqlConfigurationCode();
	}
}