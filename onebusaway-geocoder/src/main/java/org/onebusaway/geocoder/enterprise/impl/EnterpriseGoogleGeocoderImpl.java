/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
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
package org.onebusaway.geocoder.enterprise.impl;

import org.onebusaway.geocoder.enterprise.services.EnterpriseGeocoderResult;
import org.onebusaway.geocoder.impl.GoogleAddressComponent;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geocoder.enterprise.model.EnterpriseGoogleGeocoderResult;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.digester.Digester;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * A geocoder that queries against Google's REST-ful Enterprise API. 
 * @author jmaki
 *
 */
public class EnterpriseGoogleGeocoderImpl extends EnterpriseFilteredGeocoderBase {

  private static Logger _log = LoggerFactory.getLogger(EnterpriseGoogleGeocoderImpl.class);

  private static final String GEOCODE_URL_PREFIX = "http://maps.googleapis.com";
  
  private static final String GEOCODE_PATH = "/maps/api/geocode/xml";
  
  private String key;
  
  private String id;
  
  private boolean _sensor = false;

  private CoordinateBounds _resultBiasingBounds = null;
  
  public void setSensor(boolean sensor) {
    _sensor = sensor;
  }

  public void setResultBiasingBounds(CoordinateBounds bounds) {
    _resultBiasingBounds = bounds;
  }
  
  public List<EnterpriseGeocoderResult> enterpriseGeocode(String location) {
    try {
      List<EnterpriseGeocoderResult> results = new ArrayList<EnterpriseGeocoderResult>();

      StringBuilder q = new StringBuilder();
      q.append("sensor=").append(_sensor);
    
      String encodedLocation = URLEncoder.encode(location, "UTF-8");
      q.append("&address=").append(encodedLocation);
    
      if(_resultBiasingBounds != null) {
        q.append("&bounds=").append(
            _resultBiasingBounds.getMinLat() + "," + 
            _resultBiasingBounds.getMinLon() + "|" + 
            _resultBiasingBounds.getMaxLat() + "," + 
            _resultBiasingBounds.getMaxLon());
      }

      String clientId = id; 
               
      String authKey = key;  
      
      if(clientId != null && authKey != null && !StringUtils.isEmpty(clientId) && !StringUtils.isEmpty(authKey)) {
        q.append("&client=").append(clientId);
      }
    
      URL url = null;
      if(authKey != null && clientId != null && !StringUtils.isEmpty(clientId) && !StringUtils.isEmpty(authKey)) {
        url = new URL(GEOCODE_URL_PREFIX + signRequest(authKey, GEOCODE_PATH + "?" + q.toString()));
      } else {
        url = new URL(GEOCODE_URL_PREFIX + GEOCODE_PATH + "?" + q.toString());        
      }
      
      Digester digester = createDigester();
      digester.push(results);

      _log.debug("Requesting " + url.toString());
      InputStream inputStream = url.openStream();

      digester.parse(inputStream);    
      _log.debug("Got " + results.size() + " geocoder results.");

      results = filterResultsByWktPolygon(results);
      _log.debug("Have " + results.size() + " geocoder results AFTER filtering.");

      return results;
    } catch (Exception e) {
      _log.error("Geocoding error: " + e.getMessage());
      return null;
    }
  }
  
  /**
   * PRIVATE METHODS
   */
  private String signRequest(String key, String resource) throws NoSuchAlgorithmException,
    InvalidKeyException, UnsupportedEncodingException, URISyntaxException {

    key = key.replace('-', '+');
    key = key.replace('_', '/');
    byte[] base64edKey = Base64.decodeBase64(key.getBytes());

    // Get an HMAC-SHA1 signing key from the raw key bytes
    SecretKeySpec sha1Key = new SecretKeySpec(base64edKey, "HmacSHA1");

    // Get an HMAC-SHA1 Mac instance and initialize it with the HMAC-SHA1 key
    Mac mac = Mac.getInstance("HmacSHA1");
    mac.init(sha1Key);

    // compute the binary signature for the request
    byte[] sigBytes = mac.doFinal(resource.getBytes());

    // base 64 encode the binary signature
    String signature = new String(Base64.encodeBase64(sigBytes));
    
    // convert the signature to 'web safe' base 64
    signature = signature.replace('+', '-');
    signature = signature.replace('/', '_');
    
    return resource + "&signature=" + signature;
  }
  
  
  private Digester createDigester() {
    Digester digester = new Digester();

    digester.addObjectCreate("GeocodeResponse/result", EnterpriseGoogleGeocoderResult.class);

    digester.addObjectCreate("GeocodeResponse/result/address_component", GoogleAddressComponent.class);
    digester.addCallMethod("GeocodeResponse/result/address_component/long_name", "setLongName", 0);
    digester.addCallMethod("GeocodeResponse/result/address_component/short_name", "setShortName", 0);
    digester.addCallMethod("GeocodeResponse/result/address_component/type", "addType", 0);
    digester.addSetNext("GeocodeResponse/result/address_component", "addAddressComponent");
    
    Class<?>[] dType = {Double.class};
    digester.addCallMethod("GeocodeResponse/result/formatted_address", "setFormattedAddress", 0);
    digester.addCallMethod("GeocodeResponse/result/geometry/location/lat", "setLatitude", 0, dType);
    digester.addCallMethod("GeocodeResponse/result/geometry/location/lng", "setLongitude", 0, dType);
    digester.addCallMethod("GeocodeResponse/result/geometry/bounds/southwest/lat", "setSouthwestLatitude", 0, dType);
    digester.addCallMethod("GeocodeResponse/result/geometry/bounds/southwest/lng", "setSouthwestLongitude", 0, dType);
    digester.addCallMethod("GeocodeResponse/result/geometry/bounds/northeast/lat", "setNortheastLatitude", 0, dType);
    digester.addCallMethod("GeocodeResponse/result/geometry/bounds/northeast/lng", "setNortheastLongitude", 0, dType);

    digester.addSetNext("GeocodeResponse/result", "add");

    return digester;
  }

	public String getKey() {
		return key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
}
