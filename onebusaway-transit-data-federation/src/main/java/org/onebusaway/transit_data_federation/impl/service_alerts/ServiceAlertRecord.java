/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.transit_data_federation.impl.service_alerts;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.onebusaway.transit_data.model.service_alerts.ECause;
import org.onebusaway.transit_data.model.service_alerts.ESeverity;

import javax.persistence.*;
import java.util.List;

/**
 * A Service Alert record is a database-serializable record that captures the
 * real-time service alerts from different agencies on a particular route.
 * The record includes service alert data object with service alert id.
 * 
 * This class is meant for internal use.
 * 
 * @author ckhasnis 
 */
@Entity
@Table(name = "transit_data_service_alerts_records")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ServiceAlertRecord {

  @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER, mappedBy = "serviceAlertRecord")
  @Fetch(value = FetchMode.SUBSELECT)
  private List<ServiceAlertTimeRange> activeWindows;

  @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER, mappedBy = "serviceAlertRecord")
  @Fetch(value = FetchMode.SUBSELECT)
  private List<ServiceAlertTimeRange> publicationWindows;

  @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER, mappedBy = "serviceAlertRecord")
  @Fetch(value = FetchMode.SUBSELECT)
  private List<ServiceAlertLocalizedString> summaries;

  @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER, mappedBy = "serviceAlertRecord")
  @Fetch(value = FetchMode.SUBSELECT)
  private List<ServiceAlertLocalizedString> descriptions;

  @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER, mappedBy = "serviceAlertRecord")
  @Fetch(value = FetchMode.SUBSELECT)
  private List<ServiceAlertLocalizedString> urls;

  @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "serviceAlertRecord", fetch = FetchType.EAGER)
  @Fetch(value = FetchMode.SUBSELECT)
  private List<ServiceAlertsSituationAffectsClause> allAffects;

  @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "serviceAlertRecord", fetch = FetchType.EAGER)
  @Fetch(value = FetchMode.SUBSELECT)
  private List<ServiceAlertSituationConsequenceClause> consequences;

  @Enumerated(EnumType.STRING)
  private ESeverity severity;

  @Enumerated(EnumType.STRING)
  private ECause cause;

  private String source;

	@Id
	@GeneratedValue
	private final int id = 0;

  private String agencyId;

	@Column(nullable = false, name="service_alert_id", length = 255)
	private String serviceAlertId;

  private Long creationTime;

  private Long modifiedTime;

  public Long getModifiedTime() {
    return modifiedTime;
  }

  public void setModifiedTime(long modifiedTime) {
    this.modifiedTime = modifiedTime;
  }

  public List<ServiceAlertTimeRange> getActiveWindows() {
    return activeWindows;
  }

  public void setActiveWindows(List<ServiceAlertTimeRange> activeWindows) {
    this.activeWindows = activeWindows;
  }

  public List<ServiceAlertTimeRange> getPublicationWindows() {
    return publicationWindows;
  }

  public void setPublicationWindows(
      List<ServiceAlertTimeRange> publicationWindows) {
    this.publicationWindows = publicationWindows;
  }

  public List<ServiceAlertLocalizedString> getSummaries() {
    return summaries;
  }

  public void setSummaries(List<ServiceAlertLocalizedString> summaries) {
    this.summaries = summaries;
  }

  public List<ServiceAlertLocalizedString> getDescriptions() {
    return descriptions;
  }

  public void setDescriptions(List<ServiceAlertLocalizedString> descriptions) {
    this.descriptions = descriptions;
  }

  public List<ServiceAlertLocalizedString> getUrls() {
    return urls;
  }

  public void setUrls(List<ServiceAlertLocalizedString> urls) {
    this.urls = urls;
  }

  public List<ServiceAlertsSituationAffectsClause> getAllAffects() {
    return allAffects;
  }

  public void setAllAffects(
      List<ServiceAlertsSituationAffectsClause> allAffects) {
    this.allAffects = allAffects;
  }

  public List<ServiceAlertSituationConsequenceClause> getConsequences() {
    return consequences;
  }

  public void setConsequences(
      List<ServiceAlertSituationConsequenceClause> consequences) {
    this.consequences = consequences;
  }

  public ESeverity getSeverity() {
    return severity;
  }

  public void setSeverity(ESeverity severity) {
    this.severity = severity;
  }

  public ECause getCause() {
    return cause;
  }

  public void setCause(ECause cause) {
    this.cause = cause;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public int getId() {
    return id;
  }

  public String getAgencyId() {
    return agencyId;
  }

  public void setAgencyId(String agencyId) {
    this.agencyId = agencyId;
  }

  public String getServiceAlertId() {
    return serviceAlertId;
  }

  public void setServiceAlertId(String serviceAlertId) {
    this.serviceAlertId = serviceAlertId;
  }

  public long getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(long creationTime) {
    this.creationTime = creationTime;
  }
}
