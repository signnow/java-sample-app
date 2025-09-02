package com.signnow.samples.EmbeddedSenderWithoutFormFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.embeddedsending.request.DocumentEmbeddedSendingLinkPostRequest;
import com.signnow.api.embeddedsending.response.DocumentEmbeddedSendingLinkPostResponse;
import com.signnow.api.template.request.CloneTemplatePostRequest;
import com.signnow.api.template.response.CloneTemplatePostResponse;
import com.signnow.core.ApiClient;
import com.signnow.core.exception.SignNowApiException;
import com.signnow.javasampleapp.ExampleInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import com.signnow.api.document.response.DocumentDownloadGetResponse;
import com.signnow.api.document.request.DocumentDownloadGetRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {

    private static final String TEMPLATE_ID = "76713f00c106425ea8b673c49fd94c0145643c34";

    public ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException, SignNowApiException {
        String page = queryParams.get("page");
        ApiClient client = new Sdk().build().authenticate().getApiClient();

        if ("download-with-status".equals(page)) {
            try (var inputStream = getClass().getResourceAsStream("/static/samples/EmbeddedSenderWithoutFormFile/index.html")) {
                if (inputStream == null) {
                    throw new IOException("HTML file not found in classpath");
                }
                String html = new String(inputStream.readAllBytes());
                return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
            }
        }

        String link = getEmbeddedSendingLink(client);
        return ResponseEntity.status(302).header("Location", link).build();
    }

    public ResponseEntity<?> handlePost(String formData) throws IOException, SignNowApiException {
        Map<String, String> data = new ObjectMapper().readValue(formData, Map.class);
        String action = data.get("action");

        ApiClient client = new Sdk().build().authenticate().getApiClient();
        String documentId = data.get("document_id");

        if ("invite-status".equals(action)) {
            List<Object> statusList = getDocumentStatuses(client, documentId);
            return ResponseEntity.ok().body(new ObjectMapper().writeValueAsString(statusList));
        }

        File file = downloadDocumentFile(client, documentId);
        
        String filename = file.getName();
        byte[] content = Files.readAllBytes(file.toPath());
        file.delete();
        
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(content);
    }

    private String getEmbeddedSendingLink(ApiClient client) throws SignNowApiException {
        CloneTemplatePostResponse cloneResponse = createDocumentFromTemplate(client);
        String documentId = cloneResponse.getId();

        String redirectUrl = "http://localhost:8080/samples/EmbeddedSenderWithoutFormFile?page=download-with-status&document_id=" + documentId;
        DocumentEmbeddedSendingLinkPostRequest request = new DocumentEmbeddedSendingLinkPostRequest("document", redirectUrl, 16, "self");
        request.withDocumentId(documentId);

        DocumentEmbeddedSendingLinkPostResponse response = (DocumentEmbeddedSendingLinkPostResponse) client.send(request).getResponse();
        return response.getData().getUrl();
    }

    private CloneTemplatePostResponse createDocumentFromTemplate(ApiClient client) throws SignNowApiException {
        CloneTemplatePostRequest cloneTemplate = new CloneTemplatePostRequest();
        cloneTemplate.withTemplateId(TEMPLATE_ID);

        return (CloneTemplatePostResponse) client.send(cloneTemplate).getResponse();
    }

    private List<Object> getDocumentStatuses(ApiClient client, String documentId) throws SignNowApiException {
        DocumentGetRequest documentRequest = new DocumentGetRequest();
        documentRequest.withDocumentId(documentId);
        DocumentGetResponse documentResponse = (DocumentGetResponse) client.send(documentRequest).getResponse();

        List<Object> statuses = new LinkedList<>();
        documentResponse.getFieldInvites().forEach(invite -> {
            Map<String, String> status = new HashMap<>();
            status.put("name", invite.getEmail());
            status.put("status", invite.getStatus());
            statuses.add(status);
        });

        return statuses;
    }

    private File downloadDocumentFile(ApiClient client, String documentId) throws SignNowApiException, IOException {
        DocumentDownloadGetRequest downloadRequest = new DocumentDownloadGetRequest();
        downloadRequest.withDocumentId(documentId).withType("collapsed");

        DocumentDownloadGetResponse response = (DocumentDownloadGetResponse) client.send(downloadRequest).getResponse();

        return response.getFile();
    }
}
