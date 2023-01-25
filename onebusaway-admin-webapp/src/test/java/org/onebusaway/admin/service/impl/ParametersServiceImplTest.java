/**
 * Copyright (C) 2015 Cambridge Systematics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.admin.service.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.admin.service.ParametersService;
import org.onebusaway.admin.service.impl.ParametersServiceImpl;
import org.onebusaway.admin.util.ConfigurationKeyTranslator;
import org.onebusaway.util.services.configuration.ConfigurationService;

/**
 * Tests {@link ParametersService}
 * @author abelsare
 *
 */
public class ParametersServiceImplTest {

	@Mock
	private ConfigurationService configurationService;
	
	private ConfigurationKeyTranslator keyTranslator;
	
	private ParametersServiceImpl service;
	
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		keyTranslator = new ConfigurationKeyTranslator();
		
		service = new ParametersServiceImpl();
		service.setConfigurationService(configurationService);
		service.setKeyTranslator(keyTranslator);
	}
	
	@Test
	public void testGetParameters() {
		Map<String, String> configParameters = new HashMap<String, String>();
		configParameters.put("agency_1.hideScheduleInfo", "true");
		configParameters.put("admin.showVehicleStatus", "true");
		
		when(configurationService.getConfiguration()).thenReturn(configParameters);
		
		Map<String,String> displayParameters = service.getParameters();
		
		assertTrue("Expecting translated key to be present in the map", 
				displayParameters.containsKey("admin.showVehicleStatus"));
		assertEquals("Expecting value to be associated with the translated key", "true",
				displayParameters.get("admin.showVehicleStatus"));
		
		assertTrue("Expecting translated key to be present in the map", 
				displayParameters.containsKey("agency_1.hideScheduleInfo"));
		assertEquals("Expecting value to be associated with the translated key", "true",
				displayParameters.get("agency_1.hideScheduleInfo"));
	}
	
	@Test
	public void testSaveParameters() throws Exception {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("agency_1.hideScheduleInfo", "true");
		parameters.put("admin.showVehicleStatus", "true");
		
		boolean success = service.saveParameters(parameters);
		
		assertTrue("Expecting save operation to be successful", success);
		
		verify(configurationService).setConfigurationValue("agency_1", "agency_1.hideScheduleInfo", "true");
		verify(configurationService).setConfigurationValue("admin", "admin.showVehicleStatus",
				"true");
	}
	
	@Test
	public void testSaveParametersException() throws Exception {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("agency_1.hideScheduleInfo", "true");
		parameters.put("admin.showVehicleStatus", "true");
		
		doThrow(new Exception()).when(configurationService).
						setConfigurationValue("agency_1", "agency_1.hideScheduleInfo", "true");
		// force an exception and then confirm success is fale
		boolean success = service.saveParameters(parameters);
		
		assertFalse("Expecting save operation to fail", success);
		
		verify(configurationService).setConfigurationValue("admin", "admin.showVehicleStatus",
				"true");
	}

}
