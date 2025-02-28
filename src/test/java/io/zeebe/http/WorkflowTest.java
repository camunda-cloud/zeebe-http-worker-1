package io.zeebe.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import io.zeebe.client.api.response.WorkflowInstanceEvent;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.builder.ServiceTaskBuilder;
import io.zeebe.protocol.record.Record;
import io.zeebe.protocol.record.intent.JobIntent;
import io.zeebe.protocol.record.value.JobRecordValue;
import io.zeebe.test.ZeebeTestRule;
import io.zeebe.test.util.record.RecordingExporter;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"ENV_VARS_URL=http://localhost:8089/config", "ENV_VARS_RELOAD_RATE=0"})
public class WorkflowTest {

  @ClassRule 
  public static final ZeebeTestRule TEST_RULE = new ZeebeTestRule();

  @ClassRule 
  public static WireMockRule WIRE_MOCK_RULE = new WireMockRule(8089);

  @BeforeClass
  public static void init() {
    stubFor(
        get(urlEqualTo("/config"))
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("{}")));

    System.setProperty("zeebe.client.broker.contactPoint", TEST_RULE.getClient().getConfiguration().getBrokerContactPoint());
  }

  @Before
  public void resetMock() {
    WIRE_MOCK_RULE.resetRequests();
  }

  @Test
  public void testGetRequest() {

    stubFor(
        get(urlEqualTo("/api"))
            .willReturn(
                aResponse().withHeader("Content-Type", "application/json").withBody("{\"x\":1}")));

    final WorkflowInstanceEvent workflowInstance =
        createInstance(
            serviceTask ->
                serviceTask
                    .zeebeTaskHeader("url", WIRE_MOCK_RULE.baseUrl() + "/api")
                    .zeebeTaskHeader("method", "GET"),
            Collections.emptyMap());

    ZeebeTestRule.assertThat(workflowInstance)
        .isEnded()
        .hasVariable("statusCode", 200)
        .hasVariable("body", Map.of("x", 1));

    WIRE_MOCK_RULE.verify(getRequestedFor(urlEqualTo("/api")));
  }

  @Test
  public void testPostRequest() {

    stubFor(post(urlEqualTo("/api")).willReturn(aResponse().withStatus(201)));

    final WorkflowInstanceEvent workflowInstance =
        createInstance(
            serviceTask ->
                serviceTask
                    .zeebeTaskHeader("url", WIRE_MOCK_RULE.baseUrl() + "/api")
                    .zeebeTaskHeader("method", "POST"),
            Map.of("body", Map.of("x", 1)));

    ZeebeTestRule.assertThat(workflowInstance).isEnded().hasVariable("statusCode", 201);

    WIRE_MOCK_RULE.verify(
        postRequestedFor(urlEqualTo("/api"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson("{\"x\":1}")));
  }

  @Test
  public void testPutRequest() {

    stubFor(put(urlEqualTo("/api")).willReturn(aResponse().withStatus(200)));

    final WorkflowInstanceEvent workflowInstance =
        createInstance(
            serviceTask ->
                serviceTask
                    .zeebeTaskHeader("url", WIRE_MOCK_RULE.baseUrl() + "/api")
                    .zeebeTaskHeader("method", "PUT"),
            Map.of("body", Map.of("x", 1)));

    ZeebeTestRule.assertThat(workflowInstance).isEnded().hasVariable("statusCode", 200);

    WIRE_MOCK_RULE.verify(
        putRequestedFor(urlEqualTo("/api"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson("{\"x\":1}")));
  }

  @Test
  public void testDeleteRequest() {

    stubFor(delete(urlEqualTo("/api")).willReturn(aResponse().withStatus(200)));

    final WorkflowInstanceEvent workflowInstance =
        createInstance(
            serviceTask ->
                serviceTask
                    .zeebeTaskHeader("url", WIRE_MOCK_RULE.baseUrl() + "/api")
                    .zeebeTaskHeader("method", "DELETE"),
            Collections.emptyMap());

    ZeebeTestRule.assertThat(workflowInstance).isEnded().hasVariable("statusCode", 200);

    WIRE_MOCK_RULE.verify(deleteRequestedFor(urlEqualTo("/api")));
  }

  @Test
  public void testGetNotFoundResponse() {

    stubFor(get(urlEqualTo("/api")).willReturn(aResponse().withStatus(404)));

    final WorkflowInstanceEvent workflowInstance =
        createInstance(
            serviceTask ->
                serviceTask
                    .zeebeTaskHeader("url", WIRE_MOCK_RULE.baseUrl() + "/api")
                    .zeebeTaskHeader("method", "GET"),
            Map.of());

    ZeebeTestRule.assertThat(workflowInstance).isEnded().hasVariable("statusCode", 404);
  }

  @Test
  public void testAuthorizationHeader() {

    stubFor(get(urlEqualTo("/api")).willReturn(aResponse()));

    final WorkflowInstanceEvent workflowInstance =
        createInstance(
            serviceTask ->
                serviceTask
                    .zeebeTaskHeader("url", WIRE_MOCK_RULE.baseUrl() + "/api")
                    .zeebeTaskHeader("method", "GET"),
            Map.of("authorization", "token 123"));

    ZeebeTestRule.assertThat(workflowInstance).isEnded().hasVariable("statusCode", 200);

    WIRE_MOCK_RULE.verify(
        getRequestedFor(urlEqualTo("/api")).withHeader("Authorization", equalTo("token 123")));
  }

  @Test
  public void shouldExposeJobKeyIfStatusCode202() {

    stubFor(post(urlEqualTo("/api")).willReturn(aResponse().withStatus(202)));

    final WorkflowInstanceEvent workflowInstance =
        createInstance(
            serviceTask ->
                serviceTask
                    .zeebeTaskHeader("url", WIRE_MOCK_RULE.baseUrl() + "/api")
                    .zeebeTaskHeader("method", "POST"),
            Map.of("body", Map.of("x", 1)));

    Record<JobRecordValue> job = RecordingExporter.jobRecords(JobIntent.CREATED)
        .withWorkflowInstanceKey(workflowInstance.getWorkflowInstanceKey())
        .getFirst();

    ZeebeTestRule.assertThat(workflowInstance)
        .isEnded()
        .hasVariable("statusCode", 202)
        .hasVariable("jobKey", job.getKey());
  }

  @Test
  public void shouldReplacePlaceholdersWithVariables() {

    stubFor(post(urlMatching("/api/.*")).willReturn(aResponse().withStatus(201)));

    final WorkflowInstanceEvent workflowInstance =
        createInstance(
            serviceTask ->
                serviceTask
                    .zeebeTaskHeader("url", WIRE_MOCK_RULE.baseUrl() + "/api/{{x}}")
                    .zeebeTaskHeader("method", "POST")
                    .zeebeTaskHeader("body", "{\"y\":{{y}}}"),
            Map.of("x", 1, "y", 2));

    ZeebeTestRule.assertThat(workflowInstance).isEnded().hasVariable("statusCode", 201);

    WIRE_MOCK_RULE.verify(
        postRequestedFor(urlEqualTo("/api/1"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson("{\"y\":2}")));
  }

  @Test
  public void shouldReplacePlaceholdersWithContext() {

    stubFor(post(urlMatching("/api/.*")).willReturn(aResponse().withStatus(201)));

    final WorkflowInstanceEvent workflowInstance =
        createInstance(
            serviceTask ->
                serviceTask
                    .zeebeTaskHeader("url", WIRE_MOCK_RULE.baseUrl() + "/api/{{jobKey}}")
                    .zeebeTaskHeader("method", "POST")
                    .zeebeTaskHeader("body", "{\"instanceKey\":{{workflowInstanceKey}}}"),
            Map.of());

    ZeebeTestRule.assertThat(workflowInstance).isEnded().hasVariable("statusCode", 201);

    Record<JobRecordValue> job = RecordingExporter.jobRecords(JobIntent.CREATED)
        .withWorkflowInstanceKey(workflowInstance.getWorkflowInstanceKey())
        .getFirst();

    WIRE_MOCK_RULE.verify(
        postRequestedFor(urlEqualTo("/api/" + job.getKey()))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(
                equalToJson(
                    "{\"instanceKey\":" + workflowInstance.getWorkflowInstanceKey() + "}")));
  }

  @Test
  public void shouldReplacePlaceholdersWithConfig() {

    stubFor(
        get(urlEqualTo("/config"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{ \"x\":1,\"y\":2}")));

    stubFor(post(urlMatching("/api/.*")).willReturn(aResponse().withStatus(201)));

    final WorkflowInstanceEvent workflowInstance =
        createInstance(
            serviceTask ->
                serviceTask
                    .zeebeTaskHeader("url", WIRE_MOCK_RULE.baseUrl() + "/api/{{x}}")
                    .zeebeTaskHeader("method", "POST")
                    .zeebeTaskHeader("body", "{\"y\":{{y}}}"),
            Map.of());

    ZeebeTestRule.assertThat(workflowInstance).isEnded().hasVariable("statusCode", 201);

    WIRE_MOCK_RULE.verify(
        postRequestedFor(urlEqualTo("/api/1"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson("{\"y\":2}")));
  }

  private WorkflowInstanceEvent createInstance(
      final Consumer<ServiceTaskBuilder> taskCustomizer, Map<String, Object> variables) {

    ServiceTaskBuilder processBuilder = Bpmn.createExecutableProcess("process")
        .startEvent()
        .serviceTask("task", t -> t.zeebeTaskType("http"));

    taskCustomizer.accept(processBuilder);
    processBuilder.endEvent();

    TEST_RULE
        .getClient()
        .newDeployCommand()
        .addWorkflowModel(processBuilder.done(), "process.bpmn")
        .send()
        .join();

    final WorkflowInstanceEvent workflowInstance =
        TEST_RULE
            .getClient()
            .newCreateInstanceCommand()
            .bpmnProcessId("process")
            .latestVersion()
            .variables(variables)
            .send()
            .join();

    return workflowInstance;
  }
}
