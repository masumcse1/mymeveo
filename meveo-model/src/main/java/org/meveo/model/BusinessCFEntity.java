/*
 * (C) Copyright 2015-2016 Opencell SAS (http://opencellsoft.com/) and contributors.
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.meveo.model.crm.custom.CustomFieldValues;
import org.meveo.model.persistence.CustomFieldValuesConverter;

@MappedSuperclass
public abstract class BusinessCFEntity extends BusinessEntity implements ICustomFieldEntity {

	private static final long serialVersionUID = -6054446440106807337L;

	@Column(name = "uuid", nullable = false, updatable = false, length = 60)
	@Size(max = 60)
	@NotNull
	private String uuid = UUID.randomUUID().toString();

	@Convert(converter = CustomFieldValuesConverter.class)
	@Column(name = "cf_values", columnDefinition = "text")
	private CustomFieldValues cfValues;

	@Override
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	@Override
	public CustomFieldValues getCfValues() {
		return cfValues;
	}

	public void setCfValues(CustomFieldValues cfValues) {
		this.cfValues = cfValues;
	}

	@Override
	public CustomFieldValues getCfValuesNullSafe() {
		if (cfValues == null) {
			cfValues = new CustomFieldValues();
		}
		return cfValues;
	}

	@Override
	public void clearCfValues() {
		cfValues = null;
	}

	/**
	 * Change UUID value. Return old value
	 * 
	 * @return Old UUID value
	 */
	@Override
	public String clearUuid() {
		String oldUuid = uuid;
		uuid = UUID.randomUUID().toString();
		return oldUuid;
	}

	@Override
	public ICustomFieldEntity[] getParentCFEntities() {
		return null;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [uuid=" + uuid + ", cfValues=" + cfValues + ", code=" + code + ", id=" + id + "]";
	}

}