/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.kie.services.impl.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.services.api.model.DeploymentUnit;
import org.jbpm.shared.services.impl.TransactionalCommandService;
import org.jbpm.shared.services.impl.commands.MergeObjectCommand;
import org.jbpm.shared.services.impl.commands.PersistObjectCommand;
import org.jbpm.shared.services.impl.commands.QueryNameCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;

public class DeploymentStore {
	
	private static final Integer STATE_DISABLED = 0;
	private static final Integer STATE_ENABLED = 1;
	private static final Integer STATE_ACTIVATED = 2;
	private static final Integer STATE_DEACTIVATED = 3;
	private static final Integer STATE_OBSOLETE = -1;
	
	private static final Logger logger = LoggerFactory.getLogger(DeploymentStore.class);

	private final XStream xstream = new XStream();
	
	
	private TransactionalCommandService commandService;
	
	public void setCommandService(TransactionalCommandService commandService) {
        this.commandService = commandService;
    }
	
	public Collection<DeploymentUnit> getEnabledDeploymentUnits() {
		List<DeploymentUnit> activeDeployments = new ArrayList<DeploymentUnit>();
		Map<String, Object> params = new HashMap<String, Object>();
		List<Integer> states = new ArrayList<Integer>();
		states.add(STATE_ENABLED);
		states.add(STATE_ACTIVATED);
		states.add(STATE_DEACTIVATED);
        params.put("state", states);
        List<DeploymentStoreEntry> deployments = commandService.execute(
				new QueryNameCommand<List<DeploymentStoreEntry>>("getDeploymentUnitsByState", params));
        
        for (DeploymentStoreEntry entry : deployments) {
        	DeploymentUnit unit = (DeploymentUnit) xstream.fromXML(entry.getDeploymentUnit());
        	
        	activeDeployments.add(unit);
        }
        
        return activeDeployments;
	}
	
	public Collection<DeploymentUnit> getDeactivatedDeploymentUnits() {
		List<DeploymentUnit> activeDeployments = new ArrayList<DeploymentUnit>();
		Map<String, Object> params = new HashMap<String, Object>();
		List<Integer> states = new ArrayList<Integer>();
		states.add(STATE_DEACTIVATED);
        params.put("state", states);
        List<DeploymentStoreEntry> deployments = commandService.execute(
				new QueryNameCommand<List<DeploymentStoreEntry>>("getDeploymentUnitsByState", params));
        
        for (DeploymentStoreEntry entry : deployments) {
        	DeploymentUnit unit = (DeploymentUnit) xstream.fromXML(entry.getDeploymentUnit());
        	
        	activeDeployments.add(unit);
        }
        
        return activeDeployments;
	}
	
	public void getDeploymentUnitsByDate(Date date, Collection<DeploymentUnit> enabled, Collection<DeploymentUnit> disabled,
			Collection<DeploymentUnit> activated, Collection<DeploymentUnit> deactivated) {
		
		Map<String, Object> params = new HashMap<String, Object>();
        params.put("ludate", date);
        List<DeploymentStoreEntry> deployments = commandService.execute(
				new QueryNameCommand<List<DeploymentStoreEntry>>("getDeploymentUnitsByDate", params));
        
        for (DeploymentStoreEntry entry : deployments) {
        	DeploymentUnit unit = (DeploymentUnit) xstream.fromXML(entry.getDeploymentUnit());
        	if (entry.getState() == STATE_ENABLED) {
        		enabled.add(unit);
        	} else if (entry.getState() == STATE_DISABLED) {
        		disabled.add(unit);
        	} else if (entry.getState() == STATE_ACTIVATED) {
        		activated.add(unit);
        	} else if (entry.getState() == STATE_DEACTIVATED) {
        		deactivated.add(unit);
        	} else {
        		logger.warn("Unknown state of deployment store entry {} for {} will be ignored", entry.getId(), entry);
        	}
        }
        
	}
	
	public void enableDeploymentUnit(DeploymentUnit unit) {
		String unitContent = xstream.toXML(unit);
		DeploymentStoreEntry entry = findDeploymentStoreByDeploymentId(unit.getIdentifier());
		if (entry != null) {
			// update only
			entry.setState(STATE_ENABLED);// 0 - disabled, 1 - enabled, 2 - activated, 3 - deactivated
			entry.setUpdateDate(new Date());		
			entry.setDeploymentUnit(unitContent);
			
			commandService.execute(new MergeObjectCommand(entry));
			return;
		}
		
		entry = new DeploymentStoreEntry();
		entry.setDeploymentId(unit.getIdentifier());
		entry.setState(STATE_ENABLED);// 0 - disabled, 1 - enabled, 2 - activated, 3 - deactivated
		entry.setUpdateDate(new Date());		
		entry.setDeploymentUnit(unitContent);
		
		commandService.execute(new PersistObjectCommand(entry));
		
	}
	
	public void disableDeploymentUnit(DeploymentUnit unit) {
		
		DeploymentStoreEntry entry = findDeploymentStoreByDeploymentId(unit.getIdentifier());
		if (entry != null) {
			// update only
			entry.setState(STATE_DISABLED);// 0 - disabled, 1 - enabled, 2 - activated, 3 - deactivated
			entry.setUpdateDate(new Date());		
			
			commandService.execute(new MergeObjectCommand(entry));	
		}
		
	}
	
	public void deactivateDeploymentUnit(DeploymentUnit unit) {
		
		DeploymentStoreEntry entry = findDeploymentStoreByDeploymentId(unit.getIdentifier());
		if (entry != null && entry.getState() != STATE_DEACTIVATED) {
			// update only
			entry.setState(STATE_DEACTIVATED);// 0 - disabled, 1 - enabled, 2 - activated, 3 - deactivated
			entry.setUpdateDate(new Date());	
						
			commandService.execute(new MergeObjectCommand(entry));	
		}
		
	}
	
	public void activateDeploymentUnit(DeploymentUnit unit) {
		
		DeploymentStoreEntry entry = findDeploymentStoreByDeploymentId(unit.getIdentifier());
		if (entry != null && entry.getState() != STATE_ACTIVATED) {
			// update only
			entry.setState(STATE_ACTIVATED);// 0 - disabled, 1 - enabled, 2 - activated, 3 - deactivated
			entry.setUpdateDate(new Date());	
						
			commandService.execute(new MergeObjectCommand(entry));	
		}
		
	}
	
	public void markDeploymentUnitAsObsolete(DeploymentUnit unit) {
		
		DeploymentStoreEntry entry = findDeploymentStoreByDeploymentId(unit.getIdentifier());
		if (entry != null) {
			// update only
			entry.setState(STATE_OBSOLETE);// 0 - disabled, 1 - enabled, -1 - obsolete 
			entry.setUpdateDate(new Date());		
			
			commandService.execute(new MergeObjectCommand(entry));	
		}
		
	}
	
	public DeploymentStoreEntry findDeploymentStoreByDeploymentId(String deploymentId) {
		Map<String, Object> params = new HashMap<String, Object>();
        params.put("deploymentId", deploymentId);
        params.put("maxResults", 1);
        List<DeploymentStoreEntry> deployments = commandService.execute(
				new QueryNameCommand<List<DeploymentStoreEntry>>("getDeploymentUnit", params));
        
        if (!deployments.isEmpty()) {
        	return deployments.get(0);
        }
        return null;
	}


}
