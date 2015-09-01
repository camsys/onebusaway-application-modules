package org.onebusaway.util.impl.configuration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.util.services.configuration.ConfigurationService;
import org.onebusaway.util.services.configuration.ConfigurationServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.google.gson.JsonObject;

public class ConfigurationServiceImpl implements ConfigurationService {

	private static Logger _log = LoggerFactory
			.getLogger(ConfigurationServiceImpl.class);

	@Autowired
	private ThreadPoolTaskScheduler _taskScheduler;

	private RefreshService _refreshService = null;

	private ConfigurationServiceClient _configurationServiceClient = null;

	private ConcurrentMap<String, String> _configurationKeyToValueMap = new ConcurrentHashMap<String, String>();

	private HashMap<String, Object> _localConfiguration = null;

	@Autowired
	public void setRefreshService(RefreshService refreshService) {
		this._refreshService = refreshService;
	}

	@Autowired
	public void setConfigurationServiceClient(ConfigurationServiceClient configServiceClient) {
		this._configurationServiceClient = configServiceClient;
	}

	@Autowired
	public void setTaskScheduler(ThreadPoolTaskScheduler taskScheduler) {
		this._taskScheduler = taskScheduler;
	}

	private void updateConfigurationMap(String configKey, String configValue) {
		if(!_configurationServiceClient.isLocal()){
		
			String currentValue = _configurationKeyToValueMap.get(configKey);
			_configurationKeyToValueMap.put(configKey, configValue);
	
			if (currentValue == null || !configValue.equals(currentValue)) {
				_log.info("Invoking refresh method for config key " + configKey);
	
				_refreshService.refresh(configKey);
			}
		}
	}

	public void refreshConfiguration() throws Exception {
		if(!_configurationServiceClient.isLocal()){
			List<JsonObject> configurationItems = _configurationServiceClient
					.getItemsForRequest("config", "list");
			if (configurationItems == null) {
				_log.info("no config values present!");
				return;
			}
			for (JsonObject configItem : configurationItems) {
				String configKey = configItem.get("key").getAsString();
				String configValue = configItem.get("value").getAsString();
	
				updateConfigurationMap(configKey, configValue);
			}
		}
	}

	private class UpdateThread implements Runnable {
		@Override
		public void run() {
			try {
				refreshConfiguration();
			} catch (Exception e) {
				_log.error("Error updating configuration from TDM: "
						+ e.getMessage());
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("unused")
	@PostConstruct
	private void startUpdateProcess() {
		if (_configurationServiceClient.isLocal()) {
			_log.info("using local oba configuration");
		} else {
			_log.info("using tdm configuration");
		}

		_taskScheduler
				.scheduleWithFixedDelay(new UpdateThread(), 5 * 60 * 1000); // 5m
	}

	@Override
	public String getConfigurationValueAsString(String configurationItemKey,
			String defaultValue) {
		String value = null;
		if (_configurationServiceClient.isLocal()) {
			_log.info("using localConfiguration=" + _localConfiguration);
			try {
				value = getLocalConfigurationValue(configurationItemKey);
			} catch (Exception e) {
				_log.error("lookup up local config failed:", e);
			}
			_log.info("for key=" + configurationItemKey + " found " + value);
			if (value == null) {
				return defaultValue;
			}
			return value;
		}

		if (_configurationKeyToValueMap.size() == 0) {
			_log.warn("No configuration values are present!");
		} else {
			_log.debug("Have " + _configurationKeyToValueMap.size()
					+ " configuration parameters.");
		}

		value = _configurationKeyToValueMap.get(configurationItemKey);

		if (value == null) {
			return defaultValue;
		} else {
			return value;
		}
	}

	

	@Override
	public Float getConfigurationValueAsFloat(String configurationItemKey,
			Float defaultValue) {
		try {
			String defaultValueAsString = ((defaultValue != null) ? defaultValue
					.toString() : null);

			return Float.parseFloat(getConfigurationValueAsString(
					configurationItemKey, defaultValueAsString));
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	@Override
	public Integer getConfigurationValueAsInteger(String configurationItemKey,
			Integer defaultValue) {
		try {
			String defaultValueAsString = ((defaultValue != null) ? defaultValue
					.toString() : null);

			return Integer.parseInt(getConfigurationValueAsString(
					configurationItemKey, defaultValueAsString));
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	@Override
	public void setConfigurationValue(String component,
			String configurationItemKey, String value) throws Exception {
		if (StringUtils.isBlank(value) || value.equals("null")) {
			throw new Exception(
					"Configuration values cannot be null (or 'null' as a string!).");
		}

		if (StringUtils.isBlank(configurationItemKey)) {
			throw new Exception("Configuration item key cannot be null.");
		}

		String currentValue = _configurationKeyToValueMap
				.get(configurationItemKey);

		if (StringUtils.isNotBlank(currentValue) && currentValue.equals(value)) {
			return;
		}

		if (_localConfiguration != null) {
			_log.error("setConfigurationValue not supported for _localConfiguration!");
			throw new UnsupportedOperationException();
		}

		_configurationServiceClient.setConfigItem("config", component,
				configurationItemKey, value);
		updateConfigurationMap(configurationItemKey, value);
	}

	@Override
	public Map<String, String> getConfiguration() {
		try {
			refreshConfiguration();
		} catch (Exception e) {
			_log.error("Error updating configuration from TDM: "
					+ e.getMessage());
			e.printStackTrace();
		}
		return _configurationKeyToValueMap;
	}
	
	// Local Config Methods
	
	private String getLocalConfigurationValue(String configurationItemKey)
			throws Exception {
		return _configurationServiceClient.getItem(null, configurationItemKey);
	}
}
