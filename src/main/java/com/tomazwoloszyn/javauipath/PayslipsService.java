package com.tomazwoloszyn.javauipath;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PayslipsService {

    // https://docs.uipath.com/automation-cloud/automation-cloud/latest/admin-guide/managing-external-applications
    @Value("${uipath.app-id}")
    private String APP_ID;
    @Value("${uipath.app-secret}")
    private String APP_SECRET;
    @Value("${uipath.platform-url}")
    private String PLATFORM_URL;
    @Value("${uipath.organization}")
    private String ORGANIZATION_NAME;
    @Value("${uipath.tenant}")
    private String TENANT_NAME;
    @Value("${uipath.project-id}")
    private String PROJECT_ID;
    @Value("${uipath.extractor-id}")
    private String EXTRACTOR_ID;
    @Value("${uipath.classification-tag}")
    private String classificationTag;

    private static HttpClient duHttpClient = HttpClient.newBuilder().build();
    private final ObjectMapper mapper = new ObjectMapper();

    public ExtractionResponse extractPayslip(MultipartFile file) throws Exception {
        String token = authenticate(APP_ID, APP_SECRET);
        String documentId = digitize(file, token);
        ExtractorList extractorList = getExtractorsList(token);

        Extractor extractor = extractorList.extractors.stream()
                .filter(e -> "Available".equals(e.status))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No available extractor found."));

        return extractData(extractor.id, documentId, token);
    }

    private String send(HttpRequest request)
            throws IOException, InterruptedException {

        return duHttpClient
            .send(request, HttpResponse.BodyHandlers.ofString())
            .body();
    }

    private String createBaseUri(){
        return PLATFORM_URL + "/" +
                ORGANIZATION_NAME + "/" +
                TENANT_NAME +
                "/du_/api/framework/projects/" +
                PROJECT_ID + "/";
    }

    /**
     * Generates authentication token
     * digitizes input document
     * gets the list of UiPath extractors
     * extracts data from the document
     *
     * @param file
     * @return
     * @throws Exception
     */
    public Map<String, String> processPayslip(MultipartFile file) throws Exception {

//        System.out.println("PLATFORM_URL - "+PLATFORM_URL
//                    +"\n"+"ORGANIZATION_NAME - "+ORGANIZATION_NAME
//                    +"\n"+"TENANT_NAME - "+TENANT_NAME
//                    +"\n"+"PROJECT_ID - "+PROJECT_ID
//                    +"\n"+"EXTRACTOR_ID - "+EXTRACTOR_ID
//                    +"\n"+"Base url - "+createBaseUri());

        String authToken = authenticate(APP_ID, APP_SECRET);
        System.out.println("Token generated: " + (authToken != null));

        String documentId = digitize(file, authToken);
        System.out.println("Document ID: " + documentId);

//        JsonNode classifiers = getClassifiers(authToken);
//        System.out.println("Tags retrived: " + classifiers.toString());

        JsonNode classification = classifyDocument(documentId, authToken);


//        I think I only used this function to check what the Tags were.
//        I don't think I need it this method anymore'
//        JsonNode tags = getTags(authToken);
//        System.out.println("Tags retrived: " + tags.toString());

//        Extractors List is a list of extractors Deployed Versions.
        PayslipsService.ExtractorList extractorsList = getExtractorsList(authToken);
        System.out.println("Available extractors: "+extractorsList.extractors.size());
//        for(Extractor extractor : extractorsList.extractors){
//            System.out.println(extractor.name+ " - " + extractor.id+" - "+extractor.status);
//        }


//        Extract data from the Payslip

        PayslipsService.ExtractionResponse extractionResponse = extractData(
                EXTRACTOR_ID, documentId, authToken);
        System.out.println("Data extracted.");

        return convertResultsToMap(extractionResponse);
    }

    /**
     * Converts the document extraction results into a Key-value Map.
     *
     * @param extractionResponse
     * @return
     */
    Map<String, String> convertResultsToMap(PayslipsService.ExtractionResponse extractionResponse){
        Map<String, String> payslipData = new LinkedHashMap<>();
        List<PayslipsService.ResultsDataPoint> fields = extractionResponse.extractionResult.resultsDocument.fields;

        for (ResultsDataPoint field : fields) {
            String value = null;

            if (!field.isMissing &&
                    field.values != null &&
                    !field.values.isEmpty()) {
                value = field.values.get(0).value;
            }
            payslipData.put(field.fieldName, value);
        }
        System.out.println("Output generated");
//        System.out.println("Extraction results: " + payslipData);
        return payslipData;
    }


    String authenticate(String appId, String appSecret) throws Exception {

        System.out.println("Authentication");

        String tokenEndpoint = PLATFORM_URL + "/identity_/connect/token";
        List<String> formData = new ArrayList<>();
        formData.add("client_id=" + appId);
        formData.add("client_secret=" + appSecret);
        formData.add("grant_type=client_credentials");

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(tokenEndpoint))
                .header("Content-Type", "application/x-www-form-urlencoded");
        HttpRequest request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(String.join("&", formData))).build();
        HttpResponse<String> response = duHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();
        IdentityResponse parsedResponse = mapper.readValue(responseBody, IdentityResponse.class);

        return parsedResponse.token;
    }

    String digitize(MultipartFile file, String token) throws Exception {

        // Save uploaded MultipartFile to a temporary file
        File tempFile = File.createTempFile("payslip-", ".pdf");
        file.transferTo(tempFile);
        System.out.println("Digitizing file");
        try {
            HttpEntity httpEntity = MultipartEntityBuilder.create()
                    .addBinaryBody(
                            "file",
                            tempFile,                     // <-- Use the temporary File
                            ContentType.DEFAULT_BINARY,
                            tempFile.getName())           // <-- Original filename is not required
                    .build();

            Pipe pipe = Pipe.open();

            new Thread(() -> {
                try (OutputStream outputStream = Channels.newOutputStream(pipe.sink())) {
                    httpEntity.writeTo(outputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(createBaseUri() + "digitization/start?api-version=1")) // <-- BASE_URI instead of baseUri
                    .header("Content-Type", httpEntity.getContentType().getValue())
                    .header("Authorization", "Bearer " + token)
                    .POST(BodyPublishers.ofInputStream(() -> Channels.newInputStream(pipe.source())))
                    .build();

            HttpResponse<String> response =
                    duHttpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String responseBody = response.body();

            DigitizeResponse parsedResponse =
                    mapper.readValue(responseBody, DigitizeResponse.class);
            return parsedResponse.documentId;
        } finally {
            // Always delete the temporary file
            tempFile.delete();
        }
    }

    private JsonNode getClassifiers(String token) throws Exception {

        String requestUrl = createBaseUri()+"/classifiers?api-version=1.0";

        System.out.println("Retrieving UiPath classifiers...");
        System.out.println("Classifiers URL: " + requestUrl);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Classifiers response status: "+ response.statusCode());
        System.out.println("Classifiers response body: "+ response.body());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Failed to retrieve UiPath classifiers. HTTP status: "
                            + response.statusCode()
                            + ", response: "
                            + response.body()
            );
        }
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(response.body());
    }


    public JsonNode classifyDocument(String documentId, String token) throws Exception {
        System.out.println("Classifying document");
        String url = createBaseUri()
                + classificationTag
                + "/classification?api-version=1.0";

        System.out.println("Classification URL: " + url);

        String requestBody = mapper.createObjectNode()
                .put("documentId", documentId)
                .toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = duHttpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("Classification status: " + response.statusCode());
        System.out.println("Classification response: " + response.body());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException(
                    "Classification failed. Status: "
                            + response.statusCode()
                            + ", Response: "
                            + response.body()
            );
        }
        JsonNode classificationResults = mapper.readTree(response.body());
        System.out.println("Classified as: "+classificationResults);
        if (classificationResults != null){

            JsonNode result = classificationResults.get(0);

            String documentTypeId = result.get("DocumentTypeId").asText();
            double confidence = result.get("Confidence").asDouble();
            String classifierName = result.get("ClassifierName").asText();

            String documentTypeName = String.valueOf(PayslipDocumentType.fromId(documentTypeId));

            System.out.println("===== CLASSIFICATION RESULT =====");
            System.out.println("Document ID: " + documentId);
            System.out.println("Document Type: " + documentTypeName);
            System.out.println("Document Type ID: " + documentTypeId);
            System.out.println("Confidence: " + confidence);
            System.out.println("Classifier: " + classifierName);
            System.out.println("=================================");
        }
        return classificationResults;
    }

    /*
            I don't think I need it anymore
     */
    public JsonNode getTags(String authToken) throws Exception {

        String url = createBaseUri()
                + "tags"
                + "?api-version=1.0";

        System.out.println("Get Tags URL: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + authToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        System.out.println("Get Tags status: " + response.statusCode());
        System.out.println("Get Tags response: " + response.body());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException(
                    "Failed to retrieve UiPath tags. HTTP "
                            + response.statusCode()
                            + ": "
                            + response.body()
            );
        }

        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readTree(response.body());
    }

    String temp_digitize(Path file, String token) throws Exception {
        System.out.println("Digitizing file");
        HttpEntity httpEntity = MultipartEntityBuilder.create()
                .addBinaryBody(
                        "file",
                        file.toFile(),                     // <-- Use the temporary File
                        ContentType.DEFAULT_BINARY,
                        file.getFileName().toString())           // <-- Original filename is not required
                .build();

        Pipe pipe = Pipe.open();

        new Thread(() -> {
            try (OutputStream outputStream = Channels.newOutputStream(pipe.sink())) {
                httpEntity.writeTo(outputStream);
                System.out.println("Write to output stream.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(createBaseUri() + "digitization/start?api-version=1")) // <-- BASE_URI instead of baseUri
                .header("Content-Type", httpEntity.getContentType().getValue())
                .header("Authorization", "Bearer " + token)
                .POST(BodyPublishers.ofInputStream(() -> Channels.newInputStream(pipe.source())))
                .build();

        HttpResponse<String> response =
                duHttpClient.send(request, HttpResponse.BodyHandlers.ofString());

        /*
        * Checking the response and throwing error is the response status code is not
        * satisfying.
        * */
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException(
                    "UiPath digitization failed. HTTP " +
                            response.statusCode() +
                            ": " +
                            response.body()
            );
        }

        String responseBody = response.body();
        DigitizeResponse parsedResponse =
                mapper.readValue(responseBody, DigitizeResponse.class);
        return parsedResponse.documentId;
    }

    /*
        Http Request and Response taken from UiPath were deleted and replaced
        with manually added requests taken from Swagger UI
     */
    ExtractorList getExtractorsList(String token) throws Exception {
        System.out.println("Getting extractor list");
//        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
//                .header("Authorization", "Bearer " +  token);
        String url = createBaseUri() + "extractors/?api-version=1";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response =
                duHttpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );
        String responseBody = response.body();
        ExtractorList parsedResponse = mapper.readValue(responseBody, ExtractorList.class);
        return parsedResponse;
    }

    ExtractionResponse extractData(String extractorId, String documentId, String token) throws Exception {
        System.out.println("Extracting data");

        String url = createBaseUri()
                + "extractors/"
                + extractorId
                + "/extraction?api-version=1";
        String requestBody = "{ \"documentId\": \""+documentId+ "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = duHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();
        ExtractionResponse parsedResponse = mapper.readValue(
                responseBody,
                ExtractionResponse.class
        );
        System.out.println("Response status: " + response.statusCode());
        return parsedResponse;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class IdentityResponse {
        @JsonProperty("access_token")
        public String token;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DigitizeResponse {
        public String documentId;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ExtractorList {
        public List<Extractor> extractors;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Extractor {
        public String id;
        public String name;
        public String status;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ExtractionResponse {
        public ExtractionResult extractionResult;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ExtractionResult {
        @JsonProperty("ResultsDocument")
        public ResultsDocument resultsDocument;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ResultsDocument {
        @JsonProperty("Fields")
        public List<ResultsDataPoint> fields;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ResultsDataPoint {
        @JsonProperty("FieldName")
        public String fieldName;
        @JsonProperty("Values")
        public List<ResultsValue> values;
        @JsonProperty("IsMissing")
        public boolean isMissing;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ResultsValue {
        @JsonProperty("Value")
        public String value;
    }
}
