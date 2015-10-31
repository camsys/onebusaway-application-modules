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
import org.onebusaway.transit_data.model.service_alerts.ECause;
import org.onebusaway.transit_data.model.service_alerts.ESeverity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

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

  @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
  @JoinColumn(name="servicealert_active_window_id", referencedColumnName="id")
  private Set<ServiceAlertTimeRange> activeWindows = new HashSet<ServiceAlertTimeRange>();

  @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
  @JoinColumn(name="servicealert_publication_window_id", referencedColumnName="id")
  private Set<ServiceAlertTimeRange> publicationWindows = new HashSet<ServiceAlertTimeRange>();

  @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
  @JoinColumn(name="servicealert_summary_id", referencedColumnName="id")
  private Set<ServiceAlertLocalizedString> summaries = new HashSet<ServiceAlertLocalizedString>();

  @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
  @JoinColumn(name="servicealert_description_id", referencedColumnName="id")
  private Set<ServiceAlertLocalizedString> descriptions = new HashSet<ServiceAlertLocalizedString>();

  @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
  @JoinColumn(name="servicealert_url_id", referencedColumnName="id")
  private Set<ServiceAlertLocalizedString> urls = new HashSet<ServiceAlertLocalizedString>();

  @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "serviceAlertRecord", fetch = FetchType.EAGER)
  private Set<ServiceAlertsSituationAffectsClause> allAffects = new HashSet<ServiceAlertsSituationAffectsClause>();

  @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "serviceAlertRecord", fetch = FetchType.EAGER)
  private Set<ServiceAlertSituationConsequenceClause> consequences = new HashSet<ServiceAlertSituationConsequenceClause>();

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

  private Long creationTime = 0l;

  private Long modifiedTime = 0l;

  public Long getModifiedTime() {
    return modifiedTime;
  }

  public void setModifiedTime(long modifiedTime) {
    this.modifiedTime = modifiedTime;
  }

  public Set<ServiceAlertTimeRange> getActiveWindows() {
    return activeWindows;
  }

  public void setActiveWindows(Set<ServiceAlertTimeRange> activeWindows) {
    this.activeWindows = activeWindows;
  }

  public Set<ServiceAlertTimeRange> getPublicationWindows() {
    return publicationWindows;
  }

  public void setPublicationWindows(
      Set<ServiceAlertTimeRange> publicationWindows) {
    this.publicationWindows = publicationWindows;
  }

  public Set<ServiceAlertLocalizedString> getSummaries() {
    return summaries;
  }

  public void setSummaries(Set<ServiceAlertLocalizedString> summaries) {
    this.summaries = summaries;
  }

  public Set<ServiceAlertLocalizedString> getDescriptions() {
    return descriptions;
  }

  public void setDescriptions(Set<ServiceAlertLocalizedString> descriptions) {
    this.descriptions = descriptions;
  }

  public Set<ServiceAlertLocalizedString> getUrls() {
    return urls;
  }

  public void setUrls(Set<ServiceAlertLocalizedString> urls) {
    this.urls = urls;
  }

  public Set<ServiceAlertsSituationAffectsClause> getAllAffects() {
    return allAffects;
  }

  public void setAllAffects(
      Set<ServiceAlertsSituationAffectsClause> allAffects) {
    this.allAffects = allAffects;
  }

  public Set<ServiceAlertSituationConsequenceClause> getConsequences() {
    return consequences;
  }

  public void setConsequences(
      Set<ServiceAlertSituationConsequenceClause> consequences) {
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
