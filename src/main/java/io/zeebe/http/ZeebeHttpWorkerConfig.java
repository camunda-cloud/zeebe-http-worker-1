package io.zeebe.http;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZeebeHttpWorkerConfig {
 
  @Value( "${ENV_VARS_URL:null}" )
  private String environmentVariableUrl;

  @Value( "${ENV_VARS_RELOAD_RATE:15000}" )  
  private long environmentVariableReloadIntervalMs;
  
  @Value( "${M2M_BASE_URL:null}" )
  private String m2mBaseUrl;
  
  @Value( "${M2M_CLIENT_ID:null}" )
  private String m2mClientId;
  
  @Value( "${M2M_CLIENT_SECRET:null}" )
  private String m2mClientSecret;
  
  @Value( "${M2M_AUDIENCE:null} ")
  private String m2mAudience;

  public boolean isEnvironmentVariableUrlSet() {
    return (getEnvironmentVariableUrl() != null && getEnvironmentVariableUrl().length() > 0);
  }

  public String getEnvironmentVariableUrl() {
    return environmentVariableUrl;
  }

  public Duration getEnvironmentVariableReloadInterval() {
    return Duration.ofMillis( environmentVariableReloadIntervalMs );
  }
  
  public boolean isM2MBaseUrlSet() {
	  return (getM2MBaseUrl() != null && getM2MBaseUrl().length() > 0);
  }
  public String getM2MBaseUrl() {
	  return m2mBaseUrl;
  }
  public boolean isM2MClientIdSet() {
	  return (getM2MClientId() != null && getM2MClientId().length() > 0);
  }
  public String getM2MClientId() {
	  return m2mClientId;
  }
  public boolean isM2MClientSecretSet() {
	  return (getM2MClientSecret() != null && getM2MClientSecret().length() > 0);
  }
  public String getM2MClientSecret() {
	  return m2mClientSecret;
  }
  public boolean isM2MAudienceSet() {
	  return (getM2mAudience() != null && getM2mAudience().length() > 0);
  }
  public String getM2mAudience() {
	  return m2mAudience;
  }
  
  
}
