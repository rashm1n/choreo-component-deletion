package org.rashm1n;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class App 
{
    public static void main( String[] args ) throws URISyntaxException, IOException {
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10)).build();

        String access_token = "";

        Yaml yaml = new Yaml();
        InputStream in = null;
        try {
            in = new FileInputStream(new File("config.yaml"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader input = new BufferedReader(isr);
        Map<String, Object> yamlObject = yaml.load(input);

        String accessToken = yamlObject.get("accessToken").toString();
        String projectId = yamlObject.get("projectId").toString();
        String orgHandler = yamlObject.get("orgHandler").toString();
        String choreoCpProjectsEndpoint = yamlObject.get("choreoCpProjectsEndpoint").toString();
//        String choreoCpProjectsEndpointProd = yamlObject.get("choreoCpProjectsEndpointProd").toString();

        String tokenURL = yamlObject.get("tokenURL").toString();
        String tokenScope = yamlObject.get("tokenScope").toString();
        String clientId = yamlObject.get("clientId").toString();
        String clientSecret = yamlObject.get("clientSecret").toString();

        Map<String, String> formData = new HashMap<>();

        formData.put("grant_type", "client_credentials");
        formData.put("scope", tokenScope);
        formData.put("client_id", clientId);
        formData.put("client_secret", clientSecret);
        formData.put("orgHandle", orgHandler);

        try {
            System.out.println("Obtaining access token");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(tokenURL))
                    .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
                    .header("accept","application/json")
                    .header("content-type","application/x-www-form-urlencoded")
                    .build();
//            log.info("request to sts: {}", request);
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(response.body());

            access_token = element.getAsJsonObject().get("access_token").getAsString();

            System.out.println("Access token obtained.");
            System.out.println(access_token);

        } catch ( InterruptedException | IOException e) {
//            log.error(e.getMessage());
        }

        System.out.println("Fetching components ...");

        JsonObject response = callGraphQL(access_token,Utils.getComponentsQuery(orgHandler,projectId),choreoCpProjectsEndpoint);

        JsonArray componentArray = response.getAsJsonObject("data").
                getAsJsonArray("components");

        System.out.println("Components fetched ...");

        File deletedIdsFile = new File(orgHandler+"-"+"deleted-ids.txt");
        deletedIdsFile.createNewFile();
        FileWriter writer = new FileWriter(deletedIdsFile);

        for (int i=0;i<componentArray.size();i++) {
            JsonObject component = componentArray.get(i).getAsJsonObject();
            if (component.get("displayName").getAsString().startsWith("Rest API")) {
                System.out.println("Component - "+component.toString());
                Instant createdTime = Instant.parse(component.get("createdAt").getAsString());

                Instant currentTime = Instant.now();
                long timeDifference = ChronoUnit.DAYS.between(createdTime,currentTime);

                if (timeDifference>0) {
//                    writer.write("Component - "+component.toString()+"\n"+"Created Date: "+component.get("createdAt").getAsString()+"\n"+"Time Difference in days - "+timeDifference+"\n\n");
                }

                if (timeDifference>0) {
                    System.out.println("Deleting component - "+component.get("id").getAsString());
                    callGraphQL(access_token,Utils.getDeleteComponentMutation(
                            component.get("id").getAsString(),
                            component.get("orgHandler").getAsString(),
                            component.get("projectId").getAsString()), choreoCpProjectsEndpoint);

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    writer.write("Component - "+component.toString()+"\n"+"Created Date: "+component.get("createdAt").getAsString()+"\n"+"Time Difference in days - "+timeDifference+"\n\n");
                }
            }
        }

        writer.close();
        System.out.println(componentArray.size());
    }

    public static JsonObject callGraphQL(String accessToken, String gqlQuery, String choreoCpProjectsEndpoint)  {
        HashMap<String, String> gqlRequestPayload = new HashMap<>() {
            {
                put("query", gqlQuery);
            }
        };
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            String requestBody = objectMapper.writeValueAsString(gqlRequestPayload);
            HttpPost request = new HttpPost(choreoCpProjectsEndpoint.concat("/graphql"));
            request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer "+accessToken);
            StringEntity requestEntity = new StringEntity(
                    requestBody,
                    ContentType.APPLICATION_JSON);
            request.setEntity(requestEntity);

            try (CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                    CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                System.out.println(responseBody);
                if (statusCode != HttpStatus.SC_OK) {
                }

                return new JsonParser().parse(responseBody).getAsJsonObject();
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        return null;
    }
    private static String getFormDataAsString(Map<String, String> formData) {
        StringBuilder formBodyBuilder = new StringBuilder();
        for (Map.Entry<String, String> singleEntry : formData.entrySet()) {
            if (formBodyBuilder.length() > 0) {
                formBodyBuilder.append("&");
            }
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getKey(), StandardCharsets.UTF_8));
            formBodyBuilder.append("=");
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getValue(), StandardCharsets.UTF_8));
        }
        return formBodyBuilder.toString();
    }
}
