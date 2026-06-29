package com.signnow.samples.UploadEmbeddedEditingAndInvite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentDownloadGetRequest;
import com.signnow.api.document.response.DocumentDownloadGetResponse;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.document.request.DocumentPostRequest;
import com.signnow.api.document.response.DocumentPostResponse;
import com.signnow.api.documentinvite.request.SendInvitePostRequest;
import com.signnow.api.documentinvite.request.data.To;
import com.signnow.api.documentinvite.request.data.ToCollection;
import com.signnow.api.embeddededitor.request.DocumentEmbeddedEditorLinkPostRequest;
import com.signnow.api.embeddededitor.response.DocumentEmbeddedEditorLinkPostResponse;
import com.signnow.core.ApiClient;
import com.signnow.core.exception.SignNowApiException;
import com.signnow.javasampleapp.ExampleInterface;
import com.signnow.javasampleapp.config.AppConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

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

    private static final String SAMPLE_NAME = "UploadEmbeddedEditingAndInvite";
    private static final String SENDER_EMAIL = "sender@signnow.com";
    private static final String PDF_PATH =
            "src/main/resources/static/samples/UploadEmbeddedSender/Sales Proposal.pdf";

    @Override
    public ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException {
        try (var inputStream = getClass().getResourceAsStream("/static/samples/" + SAMPLE_NAME + "/index.html")) {
            if (inputStream == null) {
                throw new IOException("HTML file not found in classpath");
            }
            String html = new String(inputStream.readAllBytes());
            return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
        }
    }

    @Override
    public ResponseEntity<?> handlePost(String formData) throws IOException, SignNowApiException {
        Map<String, Object> data = new ObjectMapper().readValue(formData, Map.class);
        String action = (String) data.getOrDefault("action", "");

        ApiClient client = new Sdk().build().authenticate().getApiClient();

        switch (action) {
            case "upload_and_create_dg":
                return uploadDocument(client);
            case "create_embedded_edit":
                return createEmbeddedEdit(client, data);
            case "create_invite":
                return createInvite(client, data);
            case "invite-status":
                return inviteStatus(client, data);
            case "download-document":
                return downloadDocument(client, data);
            default:
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Invalid action");
                return ResponseEntity.badRequest().body(new ObjectMapper().writeValueAsString(error));
        }
    }

    private ResponseEntity<?> uploadDocument(ApiClient client) throws SignNowApiException, IOException {
        if (!Files.exists(Paths.get(PDF_PATH))) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "PDF file not found: " + PDF_PATH);
            return ResponseEntity.status(500).body(new ObjectMapper().writeValueAsString(error));
        }

        DocumentPostRequest req = new DocumentPostRequest(new File(PDF_PATH), "Sales Proposal");
        DocumentPostResponse resp = (DocumentPostResponse) client.send(req).getResponse();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Document uploaded successfully");
        result.put("document_id", resp.getId());
        return ResponseEntity.ok().header("Content-Type", "application/json")
                .body(new ObjectMapper().writeValueAsString(result));
    }

    private ResponseEntity<?> createEmbeddedEdit(ApiClient client, Map<String, Object> data)
            throws SignNowApiException, IOException {
        String documentId = (String) data.get("document_id");
        if (documentId == null || documentId.isBlank()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Document ID is required");
            return ResponseEntity.badRequest().body(new ObjectMapper().writeValueAsString(error));
        }

        String redirectUrl = AppConfig.sampleUrl(SAMPLE_NAME) + "?page=invite-page&document_id=" + documentId;

        DocumentEmbeddedEditorLinkPostRequest req =
                new DocumentEmbeddedEditorLinkPostRequest(redirectUrl, "self", 15);
        req.withDocumentId(documentId);
        DocumentEmbeddedEditorLinkPostResponse resp =
                (DocumentEmbeddedEditorLinkPostResponse) client.send(req).getResponse();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("edit_link", resp.getData().getUrl());
        return ResponseEntity.ok().header("Content-Type", "application/json")
                .body(new ObjectMapper().writeValueAsString(result));
    }

    private ResponseEntity<?> createInvite(ApiClient client, Map<String, Object> data)
            throws SignNowApiException, IOException {
        String documentId  = (String) data.get("document_id");
        String signerEmail = (String) data.getOrDefault("signer_email", "");
        String signerName  = (String) data.getOrDefault("signer_name", "");

        if (documentId == null || signerEmail.isBlank() || signerName.isBlank()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Document ID, signer email and name are required");
            return ResponseEntity.badRequest().body(new ObjectMapper().writeValueAsString(error));
        }

        DocumentGetRequest docReq = new DocumentGetRequest();
        docReq.withDocumentId(documentId);
        DocumentGetResponse docResp = (DocumentGetResponse) client.send(docReq).getResponse();

        String redirectUri = AppConfig.sampleUrl(SAMPLE_NAME) + "?page=status-page&document_id=" + documentId;

        ToCollection toCollection = new ToCollection();
        for (var role : docResp.getRoles()) {
            toCollection.add(new To(
                    signerEmail,
                    role.getUniqueId(),
                    role.getName(),
                    1,
                    "Document Signing Request - Action Required",
                    "Dear " + signerName + ", please review and sign the uploaded document."
            ));
        }

        SendInvitePostRequest inviteReq = new SendInvitePostRequest(
                documentId, toCollection, SENDER_EMAIL,
                "Document Signing Request - Action Required",
                "Dear " + signerName + ", please review and sign the uploaded document."
        );
        client.send(inviteReq);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Invite sent successfully");
        return ResponseEntity.ok().header("Content-Type", "application/json")
                .body(new ObjectMapper().writeValueAsString(result));
    }

    private ResponseEntity<?> inviteStatus(ApiClient client, Map<String, Object> data)
            throws SignNowApiException, IOException {
        String documentId = (String) data.get("document_id");

        DocumentGetRequest req = new DocumentGetRequest();
        req.withDocumentId(documentId);
        DocumentGetResponse resp = (DocumentGetResponse) client.send(req).getResponse();

        List<Map<String, Object>> statuses = new LinkedList<>();
        for (var invite : resp.getFieldInvites()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("name", invite.getEmail());
            entry.put("status", invite.getStatus());
            entry.put("timestamp", "");
            statuses.add(entry);
        }
        return ResponseEntity.ok().header("Content-Type", "application/json")
                .body(new ObjectMapper().writeValueAsString(statuses));
    }

    private ResponseEntity<?> downloadDocument(ApiClient client, Map<String, Object> data)
            throws SignNowApiException, IOException {
        String documentId = (String) data.get("document_id");

        DocumentDownloadGetRequest req = new DocumentDownloadGetRequest();
        req.withDocumentId(documentId).withType("collapsed");
        DocumentDownloadGetResponse resp = (DocumentDownloadGetResponse) client.send(req).getResponse();

        File file = resp.getFile();
        byte[] content = Files.readAllBytes(file.toPath());
        file.delete();

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"final_document.pdf\"")
                .body(content);
    }
}
