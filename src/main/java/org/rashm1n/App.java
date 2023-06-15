package org.rashm1n;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class App 
{
    public static void main( String[] args ) throws URISyntaxException {

        Yaml yaml = new Yaml();
        InputStream in = null;
        try {
            in = new FileInputStream(new File("config.yaml"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader input = new BufferedReader(isr);
        Map<String, Object> yamlObject = yaml.load(input);

        String accessToken = yamlObject.get("accessToken").toString();
        String projectId = yamlObject.get("projectId").toString();
        String orgHandler = yamlObject.get("orgHandler").toString();
        String choreoCpProjectsEndpoint = yamlObject.get("choreoCpProjectsEndpoint").toString();
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
//        try {
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(new URI(tokenURL))
//                    .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
//                    .header("accept","application/json")
//                    .header("content-type","x-www-form-urlencoded")
//                    .GET()
//                    .build();
////            log.info("request to sts: {}", request);
//            HttpResponse<String> response = httpClient.send(
//                    request, HttpResponse.BodyHandlers.ofString());
//
//            Thread.sleep(5000);
////            if (response.statusCode() == 200) {
////                return new JSONObject(response.body()).getString("access_token");
////            } else {
////            }
//        } catch ( InterruptedException | IOException e) {
////            log.error(e.getMessage());
//        }
//

        JsonObject response = callGraphQL(accessToken,Utils.getComponentsQuery(orgHandler,projectId),choreoCpProjectsEndpoint);

        JsonArray componentArray = response.getAsJsonObject("data").
                getAsJsonArray("components");

        for (int i=0;i<componentArray.size();i++) {
            JsonObject component = componentArray.get(i).getAsJsonObject();
            System.out.println(component.toString());
            Instant createdTime = Instant.parse(component.get("createdAt").getAsString());

            Instant currentTime = Instant.now();
            long timeDifference = ChronoUnit.DAYS.between(createdTime,currentTime);
            System.out.println(timeDifference);

            if (timeDifference==0) {
                callGraphQL(accessToken,Utils.getDeleteComponentMutation(
                        component.get("id").getAsString(),
                        component.get("orgHandler").getAsString(),
                        component.get("projectId").getAsString()), choreoCpProjectsEndpoint);

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Instant i = Instant.now();
        System.out.println(i);
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

                if (statusCode != HttpStatus.SC_OK) {
                }

                return new JsonParser().parse(responseBody).getAsJsonObject();
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        return null;
    }
}
