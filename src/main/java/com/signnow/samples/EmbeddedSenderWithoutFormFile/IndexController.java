package com.signnow.samples.EmbeddedSenderWithoutFormFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentDownloadGetRequest;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentDownloadGetResponse;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Controller
public class IndexController implements ExampleInterface {
    private static final String TEMPLATE_ID = "40d7a874baa043d881e3bc6bdab561445d64ad36";

    @Override
    public ResponseEntity<String> serveExample() throws IOException {
        String html = new String(Files.readAllBytes(Paths.get("src/main/resources/static/samples/EmbeddedSenderWithoutFormFile/templates/index.html")));
        return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(html);
    }

    @Override
    public ResponseEntity<String> handleSubmission(String formData) throws IOException, SignNowApiException {
        Map<String, String> data = new ObjectMapper().readValue(formData, Map.class);
        String action = data.get("action");
        String documentId = data.get("document_id");

        Sdk sdk = new Sdk();
        ApiClient client = sdk.build().authenticate().getApiClient();

        byte[] file = downloadDocument(client, documentId);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"document.pdf\"")
                .body(new String(file));
    }

    public String handleGet(String page, String documentId) throws IOException, SignNowApiException {
        Sdk sdk = new Sdk();
        ApiClient client = sdk.build().authenticate().getApiClient();

        if ("download-container".equals(page)) {
            return renderStatusPage(getDocumentStatuses(client, documentId));
        }
        return redirectToEmbeddedSending(client);
    }

    private CloneTemplatePostResponse createDocumentFromTemplate(ApiClient client) throws SignNowApiException {
        CloneTemplatePostRequest request = new CloneTemplatePostRequest();
        request.withTemplateId(TEMPLATE_ID);
        return (CloneTemplatePostResponse) client.send(request).getResponse();
    }

    private String redirectToEmbeddedSending(ApiClient client) throws SignNowApiException {
        CloneTemplatePostResponse cloneResponse = createDocumentFromTemplate(client);
        String documentId = cloneResponse.getId();
        String redirectUrl = "http://localhost:8080/samples/EmbeddedSenderWithoutFormFile?page=download-container&document_id=" + documentId;

        DocumentEmbeddedSendingLinkPostRequest request = new DocumentEmbeddedSendingLinkPostRequest("document", redirectUrl, 16, "blank").withDocumentId(documentId);
        DocumentEmbeddedSendingLinkPostResponse response = (DocumentEmbeddedSendingLinkPostResponse) client.send(request).getResponse();
        return response.getData().getUrl();
    }

    private byte[] downloadDocument(ApiClient client, String documentId) throws SignNowApiException, IOException {
        DocumentDownloadGetRequest request = new DocumentDownloadGetRequest().withDocumentId(documentId);
        DocumentDownloadGetResponse response = (DocumentDownloadGetResponse) client.send(request).getResponse();
        File file = response.getFile();
        byte[] content = Files.readAllBytes(file.toPath());
        file.delete();
        return content;
    }

    private List<Map<String, String>> getDocumentStatuses(ApiClient client, String documentId) throws SignNowApiException {
        DocumentGetRequest request = new DocumentGetRequest();
        request.withDocumentId(documentId);
        DocumentGetResponse response = (DocumentGetResponse) client.send(request).getResponse();

        List<Map<String, String>> statuses = new ArrayList<>();
        for (var invite : response.getFieldInvites()) {
            var email = invite.getEmail();
            var statusEntry = invite.getEmailStatuses().isEmpty() ? null : invite.getEmailStatuses().get(0);
            statuses.add(Map.of(
                    "name", email,
                    "timestamp", statusEntry != null ? String.valueOf(statusEntry.getCreatedAt()) : "",
                    "status", statusEntry != null ? statusEntry.getStatus() : "unknown"
            ));
        }
        return statuses;
    }

    private String renderStatusPage(List<Map<String, String>> statusList) throws IOException {
        // Assumes use of a basic HTML template renderer or static page for simplicity.
        // Replace with actual HTML rendering as needed.
        StringBuilder html = new StringBuilder("<html><body><h1>Document Status</h1><ul>");
        for (Map<String, String> status : statusList) {
            html.append("<li>")
                    .append(status.get("name"))
                    .append(" - ")
                    .append(status.get("status"))
                    .append(" at ")
                    .append(status.get("timestamp"))
                    .append("</li>");
        }
        html.append("</ul></body></html>");
        return html.toString();
    }
}
