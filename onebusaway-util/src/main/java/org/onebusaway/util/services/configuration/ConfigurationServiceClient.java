package org.onebusaway.util.services.configuration;

import java.net.URL;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

public interface ConfigurationServiceClient {


  URL buildUrl(String baseObject, String... params) throws Exception;
    
  void setConfigItem(String baseObject, String component, String key, String value) 
		  throws Exception;
  
  String log(String baseObject, String component, Integer priority, String message);
  
  List<JsonObject> getItemsForRequest(String baseObject, String... params) throws Exception;
  
  /**
   * Convenience method. Note this assumes all values coming back from the service are strings.
   */
  List<Map<String, String>> getItems(String baseObject, String... params) throws Exception;
  
  String getItem(String baseObject, String key) throws Exception;
  
  boolean isLocal();

}
