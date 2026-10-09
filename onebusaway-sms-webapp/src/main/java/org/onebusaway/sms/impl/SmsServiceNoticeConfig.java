/**
 * Copyright (C) 2026 Cambridge Systematics, Inc.
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
package org.onebusaway.sms.impl;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.container.ConfigurationParameter;
import org.springframework.stereotype.Component;

/**
 * Holds optional notices shown before and after the body of SMS responses.
 * Set them in data-sources.xml through the PropertyOverrideConfigurer, e.g.
 * smsServiceNoticeConfig.textBefore=... and smsServiceNoticeConfig.textAfter=...
 */
@Component("smsServiceNoticeConfig")
public class SmsServiceNoticeConfig {

  private String _textBefore;

  private String _textAfter;

  @ConfigurationParameter
  public void setTextBefore(String textBefore) {
    _textBefore = textBefore;
  }

  @ConfigurationParameter
  public void setTextAfter(String textAfter) {
    _textAfter = textAfter;
  }

  /**
   * @return the notice shown before the body, or null when unset or blank
   */
  public String getTextBefore() {
    return StringUtils.trimToNull(_textBefore);
  }

  /**
   * @return the notice shown after the body, or null when unset or blank
   */
  public String getTextAfter() {
    return StringUtils.trimToNull(_textAfter);
  }
}
