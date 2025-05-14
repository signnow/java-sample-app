package com.signnow.samples.PrefillAndEmbeddedSendingAgreement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.documentfield.request.DocumentPrefillPutRequest;
import com.signnow.api.documentfield.request.data.FieldCollection;
import com.signnow.api.documentfield.request.data.Field;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {

    public ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException {
        String html = new String(Files.readAllBytes(Paths.get("src/main/resources/static/samples/PrefillAndEmbeddedSendingAgreement/index.html")));
        return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
    }

    public ResponseEntity<String> handlePost(String formData) throws IOException, SignNowApiException {
        Map<String, String> data = new ObjectMapper().readValue(formData, Map.class);
        String action = data.get("action");

        ApiClient client = new Sdk().build().authenticate().getApiClient();

        if ("create-embedded-invite".equals(action)) {
            String name = data.get("name");
            String templateId = "e30d6e58c82d43f598e365420f3c665a048a7d81";

            String link = createEmbeddedInviteAndReturnSendingLink(client, templateId, name);

            return ResponseEntity.ok().body("{\"link\": \"" + link + "\"}");
        } else if ("invite-status".equals(action)) {
            String documentId = data.get("document_id");
            Map<String, Object> statusList = getDocumentStatuses(client, documentId);
            return ResponseEntity.ok().body(new ObjectMapper().writeValueAsString(statusList));
        }

        String documentId = data.get("document_id");
        byte[] file = downloadDocument(client, documentId);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"signed_document.pdf\"")
                .body(new String(file));
    }

    private String createEmbeddedInviteAndReturnSendingLink(ApiClient client, String templateId, String name) throws SignNowApiException {
        CloneTemplatePostResponse document = createDocumentFromTemplate(client, templateId);

        prefillFields(client, document.getId(), name);

        return getEmbeddedSendingLink(client, document.getId());
    }

    private CloneTemplatePostResponse createDocumentFromTemplate(ApiClient client, String templateId) throws SignNowApiException {
        CloneTemplatePostRequest cloneTemplate = new CloneTemplatePostRequest();
        cloneTemplate.withTemplateId(templateId);

        return (CloneTemplatePostResponse) client.send(cloneTemplate).getResponse();
    }

    private void prefillFields(ApiClient client, String documentId, String name) throws SignNowApiException {
        FieldCollection fields = new FieldCollection();
        fields.add(new Field("Name", name));

        DocumentPrefillPutRequest prefillRequest = new DocumentPrefillPutRequest(fields);
        prefillRequest.withDocumentId(documentId);
        client.send(prefillRequest);
    }

    private String getEmbeddedSendingLink(ApiClient client, String documentId) throws SignNowApiException {
        String redirectUrl = "http://localhost:8080/samples/PrefillAndEmbeddedSendingAgreement?page=download-with-status&document_id=" + documentId;

        DocumentEmbeddedSendingLinkPostRequest request = new DocumentEmbeddedSendingLinkPostRequest("document", redirectUrl, 16, "self");
        request.withDocumentId(documentId);

        DocumentEmbeddedSendingLinkPostResponse response = (DocumentEmbeddedSendingLinkPostResponse) client.send(request).getResponse();
        return response.getData().getUrl();
    }

    private Map<String, Object> getDocumentStatuses(ApiClient client, String documentId) throws SignNowApiException {
        DocumentGetRequest documentRequest = new DocumentGetRequest();
        documentRequest.withDocumentId(documentId);
        DocumentGetResponse documentResponse = (DocumentGetResponse) client.send(documentRequest).getResponse();

        Map<String, Object> statuses = new HashMap<>();
        documentResponse.getFieldInvites().forEach(invite -> {
            Map<String, String> status = new HashMap<>();
            status.put("name", invite.getEmail());
            status.put("status", invite.getStatus());
            statuses.put(invite.getEmail(), status);
        });

        return statuses;
    }

    private byte[] downloadDocument(ApiClient client, String documentId) throws SignNowApiException, IOException {
        DocumentDownloadGetRequest downloadRequest = new DocumentDownloadGetRequest();
        downloadRequest.withDocumentId(documentId);

        DocumentDownloadGetResponse response = (DocumentDownloadGetResponse) client.send(downloadRequest).getResponse();

        byte[] fileBytes = Files.readAllBytes(response.getFile().toPath());
        response.getFile().delete();
        return fileBytes;
    }
}