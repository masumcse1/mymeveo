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
package org.meveo.service.base;

import java.util.Random;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.meveo.model.crm.Provider;
import org.meveo.security.CurrentUser;
import org.meveo.security.MeveoUser;
import org.meveo.util.ApplicationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseService {
	private static final Random RANDOM = new Random();

    @Inject
    @CurrentUser
    protected MeveoUser currentUser;
    
    @Inject
    @ApplicationProvider
    protected Provider appProvider;

	protected Logger log = LoggerFactory.getLogger(getClass());

	@Inject
	@Default
	private BeanManager beanManager;

    protected String generateRequestId() {
        return "MEVEOADMIN-" + RANDOM.nextInt();
    }

	@SuppressWarnings("unchecked")
	protected <E> E getManagedBeanInstance(Class<E> beanClazz) {
		Bean<E> bean = (Bean<E>) beanManager.getBeans(beanClazz).iterator().next();
		CreationalContext<E> ctx = beanManager.createCreationalContext(bean);
		return (E) beanManager.getReference(bean, beanClazz, ctx);
	}
}
