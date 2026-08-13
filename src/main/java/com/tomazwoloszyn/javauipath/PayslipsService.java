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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    // To generate credentials:
    // https://docs.uipath.com/automation-cloud/automation-cloud/latest/admin-guide/managing-external-applications
    @Value("${uipath.app-id}")
    private String APP_ID;
    @Value("${uipath.app-secret}")
    private String APP_SECRET;
//    @Value("${uipath.platrofm-url}")
//    private String PLATFORM_URL;
//    @Value("${uipath.organization}")
//    private String ORGANIZATION_NAME;
//    @Value("${uipath.tenant}")
//    private String TENANT_NAME;
//    @Value("${uipath.project-id}")
//    private String PROJECT_ID;
//    private String APP_ID;
//    private String APP_SECRET;
    private String PLATFORM_URL = "https://cloud.uipath.com";
    private String ORGANIZATION_NAME = "tomaszrpa";
    private String TENANT_NAME = "defaulttenant";
    private String PROJECT_ID = "b9446211-aa89-f111-b337-002248a375c1";

//    private static final String APP_ID = "45013995-8d99-4528-8067-ac133130ee20";
//    private static final String APP_SECRET = "a8e$CuKNrSF8_DC0bI0mpTT9skPy?5XCmWB^2@HF(5Ja7)du$qUj?K%k8nKH~W$X";

    private static HttpClient duHttpClient = HttpClient.newBuilder().build();
    private static String file = "<File Path>";
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

    private final String BASE_URI =
            PLATFORM_URL + "/" +
                    ORGANIZATION_NAME + "/" +
                    TENANT_NAME +
                    "/du_/api/framework/projects/" +
                    PROJECT_ID + "/";

    private String send(HttpRequest request)
            throws IOException, InterruptedException {

        return duHttpClient
            .send(request, HttpResponse.BodyHandlers.ofString())
            .body();
    }

    public Map<String, String> processPayslip(MultipartFile file) throws Exception {
        System.out.println("ID: "+APP_ID+", Secret: "+APP_SECRET);
        Path temp_payslip_path = Paths.get("D:/Kodowanie/UIPath/JobBoardsLogger/Dunnes Payslips Reader/Data/Email_Payslip_Processed/5040202_02DEC16.pdf");

        String authToken = new PayslipsService().authenticate(APP_ID, APP_SECRET);
        System.out.println("Token: " + authToken);

        String documentId = digitize(file, authToken);
        System.out.println("Document ID: " + documentId);

//        Extractors List is a list of extractors Deployed Versions.
        PayslipsService.ExtractorList extractorsList = new PayslipsService().getExtractorsList(authToken);
        System.out.println("Available extractors: "+extractorsList.extractors.size());

//        Extract data from the Payslip
        String temp_extractor = "2929ed78-f58b-f111-b339-000d3a673b82";
        PayslipsService.ExtractionResponse extractionResponse = new PayslipsService().extractData(
                temp_extractor, documentId, authToken);
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

        System.out.println("Processing extraction results ...");
        for (ResultsDataPoint field : fields) {
            String value = null;

            if (!field.isMissing &&
                    field.values != null &&
                    !field.values.isEmpty()) {
                value = field.values.get(0).value;
            }
            payslipData.put(field.fieldName, value);
        }
        System.out.println("Extraction results: " + payslipData);
        return payslipData;
    }


    String authenticate(String appId, String appSecret) throws Exception {
        System.out.println("Authentication");
        System.out.println("PLATFORM_URL = [" + PLATFORM_URL + "]");
        String tokenEndpoint = PLATFORM_URL + "/identity_/connect/token";
        System.out.println("tokenEndpoint = [" + tokenEndpoint + "]");
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
                    .uri(URI.create(BASE_URI + "digitization/start?api-version=1")) // <-- BASE_URI instead of baseUri
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
                .uri(URI.create(BASE_URI + "digitization/start?api-version=1")) // <-- BASE_URI instead of baseUri
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
        System.out.println("Response body: " + responseBody);

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
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " +  token);
//        HttpRequest request = requestBuilder.GET().build();
//        HttpResponse<String> response = duHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String url = BASE_URI + "extractors/?api-version=1";

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

        String url = BASE_URI
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

//        String requestBody = "{ \"documentId\" : \""+documentId+"\"}";
//        HttpRequest request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();
        HttpResponse<String> response = duHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();
        ExtractionResponse parsedResponse = mapper.readValue(
                responseBody,
                ExtractionResponse.class
        );

        System.out.println("Response body: " + response.body());
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
