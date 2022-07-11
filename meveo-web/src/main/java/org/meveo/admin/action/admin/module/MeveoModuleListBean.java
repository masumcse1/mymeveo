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
package org.meveo.admin.action.admin.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.pagination.EntityListDataModelPF;
import org.meveo.api.dto.BaseEntityDto;
import org.meveo.api.dto.module.MeveoModuleDto;
import org.meveo.api.dto.module.MeveoModuleItemDto;
import org.meveo.api.exception.ActionForbiddenException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.module.MeveoModuleApi;
import org.meveo.api.module.OnDuplicate;
import org.meveo.commons.utils.ReflectionUtils;
import org.meveo.model.module.MeveoModule;
import org.meveo.model.persistence.JacksonUtil;
import org.meveo.model.storage.Repository;
import org.meveo.service.admin.impl.MeveoModuleService;
import org.meveo.service.admin.impl.ModuleUninstall;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 * @author Clément Bareth
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @version 6.9.0
 **/
@Named
@ConversationScoped
public class MeveoModuleListBean extends MeveoModuleBean {

    private static final long serialVersionUID = 1L;

    @Inject
    private MeveoModuleApi moduleApi;

    @Inject
    private MeveoModuleService meveoModuleService;

    private MeveoModuleDto selectedModuleDto;

    private TreeNode selectedModuleItems;

    private EntityListDataModelPF<MeveoModuleDto> moduleDtos = null;
    
	@Override
	public void delete() throws BusinessException {
		entity = meveoModuleService.findById(entity.getId(), Arrays.asList("moduleItems", "patches", "releases", "moduleDependencies", "moduleFiles"));
		if(entity.isInstalled()) {
			moduleUninstall.module(entity);
			try {
				List<MeveoModule> uninstalledModules = moduleApi.uninstall(MeveoModule.class, moduleUninstall.build());
				for (var module : uninstalledModules) {
					meveoModuleService.remove(module);
				}
			} catch (MeveoApiException e) {
				throw new BusinessException(e);
			}
		} else {
			meveoModuleService.remove(entity);
		}
	}

    public EntityListDataModelPF<MeveoModuleDto> getModuleDtos() {
        return moduleDtos;
    }

    public MeveoModuleDto getSelectedModuleDto() {
        return selectedModuleDto;
    }

    @SuppressWarnings("unchecked")
	public void setSelectedModuleDto(MeveoModuleDto selectedModuleDto) {
        this.selectedModuleDto = selectedModuleDto;
        selectedModuleItems = new DefaultTreeNode("Root");
        if (selectedModuleDto == null) {
            return;
        }

        if (selectedModuleDto.getModuleItems() != null) {
            for (MeveoModuleItemDto moduleItemDto : selectedModuleDto.getModuleItems()) {
                try {
                    Class<? extends BaseEntityDto> dtoClass = (Class<? extends BaseEntityDto>) Class.forName(moduleItemDto.getDtoClassName());
                    BaseEntityDto item = JacksonUtil.convert(moduleItemDto.getDtoData(), dtoClass);
                    TreeNode classNode = getOrCreateNodeByClass(item.getClass().getSimpleName());
                    new DefaultTreeNode("item", item, classNode);
                } catch (ClassNotFoundException e) {
                    log.error("Cannot find dto class", e);
                }
            }
        }
    }

    public TreeNode getSelectedModuleItems() {

        return selectedModuleItems;
    }

    public void loadModulesFromInstance() {
        log.debug("start loadModulesFromInstance {}", meveoInstance.getUrl());
        try {
            moduleDtos = new EntityListDataModelPF<MeveoModuleDto>(new ArrayList<MeveoModuleDto>());
            moduleDtos.addAll(meveoModuleService.downloadModulesFromMeveoInstance(meveoInstance));

        } catch (Exception e) {
            log.error("Error when retrieve modules from {}. Reason {}", meveoInstance.getCode(), e.getMessage(), e);
            messages.error(new BundleKey("messages", "meveoModule.retrieveRemoteMeveoInstanceException"), meveoInstance.getCode(), e.getMessage());
            this.moduleDtos = null;
        }
    }

    public void downloadModule() {
        if (selectedModuleDto != null) {
            try {
                moduleApi.createOrUpdate(selectedModuleDto);
                messages.info(new BundleKey("messages", "meveoModule.downloadSuccess"), selectedModuleDto.getCode());

            } catch (ActionForbiddenException e) {
                if (e.getReason() != null) {
                    messages.error(e.getReason());
                } else {
                    messages.error(new BundleKey("messages", "meveoModule.downloadFailed"), selectedModuleDto.getCode(),
                        (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                }

            } catch (Exception e) {
                log.error("Failed to download meveo module {} from meveoInstance {}", selectedModuleDto.getCode(), meveoInstance.getCode(), e);
                messages.error(new BundleKey("messages", "meveoModule.downloadFailed"), selectedModuleDto.getCode(),
                    (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            }
        }
    }

    public void downloadAndInstallModule() {
        if (selectedModuleDto != null) {
            try {
                moduleApi.install(repositoriesDM.getTarget(), selectedModuleDto, OnDuplicate.OVERWRITE);
                messages.info(new BundleKey("messages", "meveoModule.installSuccess"), selectedModuleDto.getCode());

            } catch (ActionForbiddenException e) {
                if (e.getReason() != null) {
                    messages.error(e.getReason());
                } else {
                    messages.error(new BundleKey("messages", "meveoModule.downloadFailed"), selectedModuleDto.getCode(),
                        (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                }

            } catch (Exception e) {
                log.error("Failed to download and install meveo module {} ", selectedModuleDto.getCode(), e);
                messages.error(new BundleKey("messages", "meveoModule.installFailed"), selectedModuleDto.getCode(),
                    (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            }
        }
    }

    @Override
    protected String getDefaultSort() {
        return "code";
    }

    private TreeNode getOrCreateNodeByClass(String classname) {

        classname = classname.replaceAll("Dto", "");
        classname = classname.replaceAll("DTO", "");
        for (TreeNode node : selectedModuleItems.getChildren()) {
            if (classname.equals(node.getType())) {
                return node;
            }
        }

        TreeNode node = new DefaultTreeNode(classname, ReflectionUtils.getHumanClassName(classname), selectedModuleItems);
        node.setExpanded(true);
        return node;
    }
}