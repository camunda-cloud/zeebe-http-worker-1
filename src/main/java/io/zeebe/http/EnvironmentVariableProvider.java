package io.zeebe.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Helper to load environment variables from a configured URL as JSON map.
 *
 * <p>This can be e.g. used to hand over cloud worker configurations.
 */
@Component
public class EnvironmentVariableProvider {
  
  @Autowired
  private ZeebeHttpWorkerConfig config;

  private final HttpClient client = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  private Instant lastUpdate = Instant.MIN;
  private Map<String, String> cachedVariables = null;
  
  private String cachedToken = null;
  
  public Map<String, String> getVariables() {
    // only read if M2M variables are set, otherwise return empty map
    if (!config.isEnvironmentVariableUrlSet() ||
    	!config.isM2MBaseUrlSet() ||
    	!config.isM2MClientIdSet() ||
    	!config.isM2MClientSecretSet() ||
    	!config.isM2MAudienceSet()
    		) {
      return Map.of();
    }
    
    if (cachedToken == null) {
    	try {
			refreshToken();
		} catch (Exception e) {
			throw new RuntimeException("Could not fetch token: " + e.getMessage(), e);
		}
    }
    
    // if cached values are there and up-to-date, return them
    if (cachedVariables != null
        && Duration.between(lastUpdate, Instant.now()).toMillis() < config.getEnvironmentVariableReloadInterval().toMillis()) {
      return cachedVariables;
    }
    // otherwise reload cache and return the new values
    try {
      HttpRequest getVariablesRequest =
          HttpRequest.newBuilder()		
              .uri(URI.create(config.getEnvironmentVariableUrl()))
              .headers("Accept", "application/json", "Authorization", cachedToken)
              .GET()
              .build();

      String jsonResponse = client.send(getVariablesRequest, BodyHandlers.ofString()).body();
      // TODO: is we get 401 or 403 the provided token is invalid, 
      // we need to fetch a new token here and re-request the envVars

      lastUpdate = Instant.now();
      cachedVariables = objectMapper.readValue(jsonResponse, Map.class);

      return cachedVariables;
    } catch (Exception e) {
      throw new RuntimeException(
          "Could not load variables from '" + config.getEnvironmentVariableUrl() + "': " + e.getMessage(), e);
    }
  }
  private void refreshToken() throws IOException, InterruptedException {
	  JsonObject bodyAsJson = new JsonObject();
	  bodyAsJson.addProperty("client_id", config.getM2MClientId());
	  bodyAsJson.addProperty("client_secret", config.getM2MClientSecret());
	  bodyAsJson.addProperty("audience", config.getM2mAudience());
	  bodyAsJson.addProperty("grant_type", "client_credentials");
	  
	  HttpRequest getTokenRequest = 
	      HttpRequest.newBuilder()
	      	  .uri(URI.create(config.getM2MBaseUrl()))
	      	  .POST(BodyPublishers.ofString(bodyAsJson.toString()))
	      	  .build();
	  String reponse = client.send(getTokenRequest, BodyHandlers.ofString()).body();
	  JsonObject responseJson = (JsonObject) JsonParser.parseString(reponse);
	  cachedToken = responseJson.get("token_type").getAsString() + " " + responseJson.get("access_token").getAsString();
  }
}
