/*
 * (C) Copyright 2018-2019 Webdrone SAS (https://www.webdrone.fr/) and contributors.
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
package org.meveo.service.catalog.impl;

import org.meveo.model.catalog.CounterTemplate;
import org.meveo.service.base.BusinessService;

import javax.ejb.Stateless;
import javax.persistence.Query;

/**
 * Counter Template service implementation.
 * @author Clément Bareth
 */
@Stateless
public class CounterTemplateService extends BusinessService<CounterTemplate> {

    /**
     * Remove a counter template whose code start with prefix
     * @param prefix Prefix to look for
     */
    public void removeByPrefix(String prefix) {
        Query query = getEntityManager().createQuery("DELETE CounterTemplate t WHERE t.code LIKE '"+ prefix + "%'");
        query.executeUpdate();
    }

}