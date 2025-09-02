package com.signnow.samples.PrefillAndOneClickSendingAgreement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.documentfield.request.DocumentPrefillPutRequest;
import com.signnow.api.documentfield.request.data.FieldCollection;
import com.signnow.api.documentfield.request.data.Field;
import com.signnow.api.documentinvite.request.SendInvitePostRequest;
import com.signnow.api.documentinvite.request.data.To;
import com.signnow.api.documentinvite.request.data.ToCollection;
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
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {

    private static final String TEMPLATE_ID = "e30d6e58c82d43f598e365420f3c665a048a7d81";

    public ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException {
        try (var inputStream = getClass().getResourceAsStream("/static/samples/PrefillAndOneClickSendingAgreement/index.html")) {
            if (inputStream == null) {
                throw new IOException("HTML file not found in classpath");
            }
            String html = new String(inputStream.readAllBytes());
            return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
        }
    }

    public ResponseEntity<?> handlePost(String formData) throws IOException, SignNowApiException {
        Map<String, String> data = new ObjectMapper().readValue(formData, Map.class);
        String action = data.get("action");

        ApiClient client = new Sdk().build().authenticate().getApiClient();

        if ("send-invite".equals(action)) {
            String name = data.get("name");
            String email = data.get("email");

            String documentId = sendInvite(client, TEMPLATE_ID, name, email);

            return ResponseEntity.ok().body("{\"status\": \"success\", \"document_id\": \"" + documentId + "\"}");
        } else if ("invite-status".equals(action)) {
            String documentId = data.get("document_id");
            Map<String, Object> statusList = getDocumentStatuses(client, documentId);
            return ResponseEntity.ok().body(new ObjectMapper().writeValueAsString(statusList));
        }

        String documentId = data.get("document_id");
        File file = downloadDocumentFile(client, documentId);
        
        String filename = file.getName();
        byte[] content = Files.readAllBytes(file.toPath());
        file.delete();

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(content);
    }

    private String sendInvite(ApiClient client, String templateId, String name, String email) throws SignNowApiException {
        CloneTemplatePostResponse document = createDocumentFromTemplate(client, templateId);

        prefillFields(client, document.getId(), name);

        String roleId = getSignerUniqueRoleId(client, document.getId(), "Recipient 1");

        ToCollection to = new ToCollection();
        to.add(new To(
                email, // email
                roleId, //roleId
                "signer", //role
                1, //order
                "Subject", //subject
                "Message" //message
        ));

        SendInvitePostRequest inviteRequest = new SendInvitePostRequest(
                document.getId(), // documentId
                to, // recipients
                "from@email.com", // sender email
                "Subject", // subject
                "Message" // message
        );
        client.send(inviteRequest);

        return document.getId();
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

    private String getSignerUniqueRoleId(ApiClient client, String documentId, String signerRole) throws SignNowApiException {
        DocumentGetRequest documentRequest = new DocumentGetRequest();
        documentRequest.withDocumentId(documentId);
        DocumentGetResponse documentResponse = (DocumentGetResponse) client.send(documentRequest).getResponse();

        return documentResponse.getRoles().stream()
                .filter(role -> signerRole.equals(role.getName()))
                .findFirst()
                .map(role -> role.getUniqueId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
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

    private File downloadDocumentFile(ApiClient client, String documentId) throws SignNowApiException, IOException {
        DocumentDownloadGetRequest downloadRequest = new DocumentDownloadGetRequest();
        downloadRequest.withDocumentId(documentId).withType("collapsed");

        DocumentDownloadGetResponse response = (DocumentDownloadGetResponse) client.send(downloadRequest).getResponse();

        return response.getFile();
    }
}
