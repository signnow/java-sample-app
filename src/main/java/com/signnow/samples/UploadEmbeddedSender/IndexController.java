package com.signnow.samples.UploadEmbeddedSender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentPostRequest;
import com.signnow.api.document.response.DocumentPostResponse;
import com.signnow.api.documentgroup.request.DocumentGroupPostRequest;
import com.signnow.api.documentgroup.response.DocumentGroupPostResponse;
import com.signnow.api.documentgroup.request.data.DocumentIdCollection;
import com.signnow.api.embeddedsending.request.DocumentGroupEmbeddedSendingLinkPostRequest;
import com.signnow.api.embeddedsending.response.DocumentGroupEmbeddedSendingLinkPostResponse;
import com.signnow.api.documentgroup.request.DocumentGroupRecipientsGetRequest;
import com.signnow.api.documentgroup.response.DocumentGroupRecipientsGetResponse;
import com.signnow.api.documentgroup.request.DocumentGroupGetRequest;
import com.signnow.api.documentgroup.response.DocumentGroupGetResponse;
import com.signnow.api.documentgroupinvite.request.GroupInviteGetRequest;
import com.signnow.api.documentgroupinvite.response.GroupInviteGetResponse;
import com.signnow.api.documentgroup.request.DownloadDocumentGroupPostRequest;
import com.signnow.api.documentgroup.response.DownloadDocumentGroupPostResponse;
import com.signnow.core.ApiClient;
import com.signnow.core.exception.SignNowApiException;
import com.signnow.javasampleapp.ExampleInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {

    @Override
    public ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException {
        // Always return the HTML page and let the client-side JS decide which sub-page to show.
        String html = Files.readString(Paths.get("src/main/resources/static/samples/UploadEmbeddedSender/index.html"));
        return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
    }

    @Override
    public ResponseEntity<?> handlePost(String formData) throws IOException, SignNowApiException {
        Map<String, Object> data = new ObjectMapper().readValue(formData, Map.class);
        String action = (String) data.getOrDefault("action", "");

        ApiClient client = new Sdk().build().authenticate().getApiClient();

        switch (action) {
            case "upload_and_create_dg":
                return uploadAndCreateDocumentGroup(client);
            case "invite-status":
                String documentGroupId = (String) data.get("document_group_id");
                List<Map<String, Object>> signers = getDocumentGroupSignersStatus(client, documentGroupId);
                return ResponseEntity.ok(signers);
            case "download-doc-group":
                return downloadDocumentGroup(data, client);
            default:
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid action");
                return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    private ResponseEntity<?> uploadAndCreateDocumentGroup(ApiClient client) throws SignNowApiException, IOException {
        // 1. Upload PDF file to SignNow
        Map<String, Object> documentResponse = uploadDocument(client);
        if (!(Boolean) documentResponse.get("success")) {
            return ResponseEntity.status(500).body(documentResponse);
        }

        String documentId = (String) documentResponse.get("document_id");

        // 2. Create Document Group from uploaded document
        Map<String, Object> documentGroupResponse = createDocumentGroup(client, documentId);
        if (!(Boolean) documentGroupResponse.get("success")) {
            return ResponseEntity.status(500).body(documentGroupResponse);
        }

        String documentGroupId = (String) documentGroupResponse.get("document_group_id");

        // 3. Create embedded sending link
        Map<String, Object> embeddedSendingResponse = createEmbeddedSendingUrl(client, documentGroupId);
        if (!(Boolean) embeddedSendingResponse.get("success")) {
            return ResponseEntity.status(500).body(embeddedSendingResponse);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Document uploaded and embedded sending link created successfully");
        response.put("embedded_url", embeddedSendingResponse.get("embedded_url"));

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> uploadDocument(ApiClient client) throws SignNowApiException, IOException {
        // Read the PDF file from samples directory
        Path pdfPath = Paths.get("src/main/resources/static/samples/UploadEmbeddedSender/Sales Proposal.pdf");

        if (!Files.exists(pdfPath)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "PDF file not found");
            return error;
        }

        // Create document upload request
        DocumentPostRequest documentPost = new DocumentPostRequest(
                pdfPath.toFile(),
                "Sales Proposal"
        );

        DocumentPostResponse response = (DocumentPostResponse) client.send(documentPost).getResponse();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("document_id", response.getId());
        return result;
    }

    private Map<String, Object> createDocumentGroup(ApiClient client, String documentId) throws SignNowApiException {
        // Create document group with the uploaded document
        DocumentIdCollection documentIdCollection = new DocumentIdCollection();
        documentIdCollection.add(documentId);

        DocumentGroupPostRequest documentGroupPost = new DocumentGroupPostRequest(
                documentIdCollection,
                "Uploaded Document Group"
        );

        DocumentGroupPostResponse response = (DocumentGroupPostResponse) client.send(documentGroupPost).getResponse();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("document_group_id", response.getId());
        return result;
    }

    private Map<String, Object> createEmbeddedSendingUrl(ApiClient client, String documentGroupId) throws SignNowApiException {
        // Return URL after embedded sending to redirect to status page
        String redirectUrl = "http://localhost:8080/samples/UploadEmbeddedSender?" +
                "page=status-page&document_group_id=" + documentGroupId;

        DocumentGroupEmbeddedSendingLinkPostRequest embeddedSendingRequest = new DocumentGroupEmbeddedSendingLinkPostRequest(
                redirectUrl,
                15, // 15 minutes
                "self",
                "edit"
        ).withDocumentGroupId(documentGroupId);

        DocumentGroupEmbeddedSendingLinkPostResponse response = (DocumentGroupEmbeddedSendingLinkPostResponse) client.send(embeddedSendingRequest).getResponse();

        // Get the embedded URL from response
        String embeddedUrl = response.getData().getUrl();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("embedded_url", embeddedUrl);
        return result;
    }

    private DocumentGroupGetResponse getDocumentGroup(ApiClient client, String documentGroupId) throws SignNowApiException {
        DocumentGroupGetRequest request = new DocumentGroupGetRequest();
        request.withDocumentGroupId(documentGroupId);
        return (DocumentGroupGetResponse) client.send(request).getResponse();
    }

    /**
     * Get the list of recipients from the Document Group.
     */
    private DocumentGroupRecipientsGetResponse getDocumentGroupRecipients(ApiClient client, String documentGroupId) throws SignNowApiException {
        DocumentGroupRecipientsGetRequest recipientsRequest = new DocumentGroupRecipientsGetRequest()
                .withDocumentGroupId(documentGroupId);

        return (DocumentGroupRecipientsGetResponse) client.send(recipientsRequest).getResponse();
    }

    /**
     * Get signers status for Document Group
     */
    private List<Map<String, Object>> getDocumentGroupSignersStatus(ApiClient client, String documentGroupId) throws SignNowApiException {
        DocumentGroupRecipientsGetResponse recipientsResponse = getDocumentGroupRecipients(client, documentGroupId);
        List<Map<String, Object>> signers = new LinkedList<>();

        // Get invite status for the document group
        DocumentGroupGetResponse docGroup = getDocumentGroup(client, documentGroupId);
        String inviteId = docGroup.getInviteId();

        if (inviteId == null || inviteId.isEmpty()) {
            // If no invite exists, return basic recipient info
            for (var recipient : recipientsResponse.getData().getRecipients()) {
                Map<String, Object> signer = new HashMap<>();
                signer.put("name", recipient.getName());
                signer.put("email", recipient.getEmail());
                signer.put("status", "not_invited");
                signer.put("order", recipient.getOrder());
                signer.put("timestamp", null);
                signers.add(signer);
            }
            return signers;
        }

        GroupInviteGetRequest inviteStatusRequest = new GroupInviteGetRequest()
                .withDocumentGroupId(documentGroupId)
                .withInviteId(inviteId);

        GroupInviteGetResponse inviteStatusResponse = (GroupInviteGetResponse) client.send(inviteStatusRequest).getResponse();

        Map<String, String> statuses = new HashMap<>();
        for (var step : inviteStatusResponse.getInvite().getSteps()) {
            for (var action : step.getActions()) {
                statuses.put(action.getRoleName(), action.getStatus());
            }
        }

        for (var recipient : recipientsResponse.getData().getRecipients()) {
            Map<String, Object> signer = new HashMap<>();
            signer.put("name", recipient.getName());
            signer.put("email", recipient.getEmail());
            signer.put("status", statuses.getOrDefault(recipient.getName(), "unknown"));
            signer.put("order", recipient.getOrder());
            signer.put("timestamp", null); // Document Group doesn't provide individual timestamps
            signers.add(signer);
        }

        return signers;
    }

    /**
     * Download the entire doc group as a merged PDF, once all are signed.
     */
    private ResponseEntity<?> downloadDocumentGroup(Map<String, Object> data, ApiClient client) throws SignNowApiException, IOException {
        String documentGroupId = (String) data.get("document_group_id");
        
        var orderColl = new com.signnow.api.documentgroup.request.data.DocumentOrderCollection();
        DownloadDocumentGroupPostRequest downloadRequest = new DownloadDocumentGroupPostRequest(
                "merged",
                "no",
                orderColl
        ).withDocumentGroupId(documentGroupId);

        DownloadDocumentGroupPostResponse response = (DownloadDocumentGroupPostResponse) client.send(downloadRequest).getResponse();

        // Get the actual filename from the downloaded file
        String filename = response.getFile().getName();
        byte[] content = Files.readAllBytes(response.getFile().toPath());
        response.getFile().delete();

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(content);
    }
} 