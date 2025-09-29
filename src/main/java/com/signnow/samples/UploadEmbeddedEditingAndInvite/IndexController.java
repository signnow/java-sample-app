package com.signnow.samples.UploadEmbeddedEditingAndInvite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.document.request.DocumentPostRequest;
import com.signnow.api.document.response.DocumentPostResponse;
import com.signnow.api.document.request.DocumentDownloadGetRequest;
import com.signnow.api.document.response.DocumentDownloadGetResponse;
import com.signnow.api.documentgroup.request.DocumentGroupGetRequest;
import com.signnow.api.documentgroup.request.DocumentGroupPostRequest;
import com.signnow.api.documentgroup.request.DownloadDocumentGroupPostRequest;
import com.signnow.api.documentgroup.request.data.DocumentIdCollection;
import com.signnow.api.documentgroup.response.DocumentGroupGetResponse;
import com.signnow.api.documentgroup.response.DocumentGroupPostResponse;
import com.signnow.api.documentgroup.response.DownloadDocumentGroupPostResponse;
import com.signnow.api.documentgroupinvite.request.GroupInviteGetRequest;
import com.signnow.api.documentgroupinvite.response.GroupInviteGetResponse;
import com.signnow.api.documentgroupinvite.response.data.invite.Action;
import com.signnow.api.documentgroupinvite.response.data.invite.Step;
import com.signnow.api.documentinvite.request.data.To;
import com.signnow.api.documentinvite.request.data.ToCollection;
import com.signnow.api.documentinvite.request.SendInvitePostRequest;
import com.signnow.api.documentgroup.request.DocumentGroupRecipientsGetRequest;
import com.signnow.api.documentgroup.response.DocumentGroupRecipientsGetResponse;
import com.signnow.api.embeddededitor.request.DocumentEmbeddedEditorLinkPostRequest;
import com.signnow.api.embeddededitor.response.DocumentEmbeddedEditorLinkPostResponse;
import com.signnow.core.ApiClient;
import com.signnow.core.exception.SignNowApiException;
import com.signnow.javasampleapp.ExampleInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {

    @Override
    public ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException {
        // Always return the HTML page and let the client-side JS decide which sub-page to show.
        try (var inputStream = getClass().getResourceAsStream("/static/samples/UploadEmbeddedEditingAndInvite/index.html")) {
            if (inputStream == null) {
                throw new IOException("HTML file not found in classpath");
            }
            String html = new String(inputStream.readAllBytes());
            return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
        }
    }

    @Override
    public ResponseEntity<?> handlePost(String formData) throws IOException, SignNowApiException {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = new ObjectMapper().readValue(formData, Map.class);
        String action = (String) data.getOrDefault("action", "");

        ApiClient client = new Sdk().build().authenticate().getApiClient();

        switch (action) {
            case "upload_and_create_dg":
                return uploadAndCreateDocumentGroup(data, client);
            case "create_embedded_edit":
                return createEmbeddedEditLink(data, client);
            case "create_invite":
                return createInvite(data, client);
            case "invite-status":
                return getInviteStatus(data, client);
            case "download-document":
                return downloadDocument(data, client);
            case "get-recipients":
                return getDocumentRecipientsApi(data, client);
            case "add-recipient":
                return addDocumentRecipient(data, client);
            case "get-document-roles":
                return getDocumentRolesApi(data, client);
            default:
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid action");
                return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    public ResponseEntity<?> handlePost(MultipartHttpServletRequest request) throws IOException, SignNowApiException {
        String action = request.getParameter("action");
        
        if (action == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Action parameter is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        ApiClient client = new Sdk().build().authenticate().getApiClient();

        switch (action) {
            case "upload_and_create_dg":
                return uploadAndCreateDocumentGroupWithFile(request, client);
            default:
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid action for file upload");
                return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    private ResponseEntity<?> getDocumentRecipientsApi(Map<String, Object> data, ApiClient client) throws SignNowApiException {
        String documentId = (String) data.get("document_id");

        if (documentId == null || documentId.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Document ID is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        List<Map<String, Object>> recipients = getDocumentRecipients(client, documentId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("recipients", recipients);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> getDocumentRolesApi(Map<String, Object> data, ApiClient client) throws SignNowApiException {
        String documentId = (String) data.get("document_id");

        if (documentId == null || documentId.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Document ID is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // Get document info
        DocumentGetResponse documentGetResponse = getDocument(client, documentId);
        List<Map<String, Object>> rolesData = new LinkedList<>();

        for (var role : documentGetResponse.getRoles()) {
            Map<String, Object> roleData = new HashMap<>();
            roleData.put("name", role.getName());
            roleData.put("unique_id", role.getUniqueId());
            roleData.put("signing_order", role.getSigningOrder());
            rolesData.add(roleData);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("roles", rolesData);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> addDocumentRecipient(Map<String, Object> data, ApiClient client) throws SignNowApiException {
        String documentId = (String) data.get("document_id");
        String recipientName = (String) data.get("recipient_name");
        String recipientEmail = (String) data.get("recipient_email");
        String recipientRole = (String) data.get("recipient_role");

        if (documentId == null || documentId.isEmpty() || 
            recipientName == null || recipientName.isEmpty() ||
            recipientEmail == null || recipientEmail.isEmpty() ||
            recipientRole == null || recipientRole.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "All fields are required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // Get document info
        DocumentGetResponse documentGetResponse = getDocument(client, documentId);
        var roles = documentGetResponse.getRoles();

        // Find existing role or create a new one
        var targetRole = roles.stream()
                .filter(role -> recipientRole.equals(role.getName()))
                .findFirst()
                .orElse(null);

        if (targetRole == null) {
            String availableRoles = roles.stream()
                    .map(role -> role.getName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none");
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Role '" + recipientRole + "' not found in document. Available roles: " + availableRoles);
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // Create invite for the recipient
        ToCollection to = new ToCollection();
        to.add(new To(
                recipientEmail,
                targetRole.getUniqueId(),
                targetRole.getName(),
                Integer.parseInt(targetRole.getSigningOrder()),
                "Document Signing Request - Action Required",
                "Dear " + recipientName + ", please review and sign the uploaded document."
        ));

        // Create and send invite request for document
        SendInvitePostRequest inviteRequest = new SendInvitePostRequest(
                documentId,
                to,
                "sender@signnow.com", // You can use your own sender email
                "Document Signing Request - Action Required",
                "Dear " + recipientName + ", please review and sign the uploaded document."
        );
        inviteRequest.withDocumentId(documentId);

        client.send(inviteRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Recipient added and invite sent successfully");
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> uploadAndCreateDocumentGroup(Map<String, Object> data, ApiClient client) throws SignNowApiException, IOException {
        // This method is for JSON requests without file upload
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "File upload required for this action");
        return ResponseEntity.badRequest().body(errorResponse);
    }

    private ResponseEntity<?> uploadAndCreateDocumentGroupWithFile(MultipartHttpServletRequest request, ApiClient client) throws SignNowApiException, IOException {
        MultipartFile file = request.getFile("document_file");
        
        if (file == null || file.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "No file uploaded");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        String documentName = file.getOriginalFilename();
        
        // Validate file type
        if (documentName == null || !documentName.toLowerCase().endsWith(".pdf")) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Only PDF files are allowed");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // Check file size (max 50MB)
        if (file.getSize() > 50 * 1024 * 1024) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "File size too large. Maximum 50MB allowed");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // Create temporary file
        Path tempFile = Files.createTempFile("upload", ".pdf");
        file.transferTo(tempFile.toFile());

        try {
            // Upload PDF file to SignNow
            Map<String, Object> documentResponse = uploadDocument(client, tempFile.toFile(), documentName);
            if (!(Boolean) documentResponse.get("success")) {
                return ResponseEntity.status(500).body(documentResponse);
            }

            String documentId = (String) documentResponse.get("document_id");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document uploaded successfully");
            response.put("document_id", documentId);
            return ResponseEntity.ok(response);
        } finally {
            // Clean up temporary file
            Files.deleteIfExists(tempFile);
        }
    }

    private ResponseEntity<?> createEmbeddedEditLink(Map<String, Object> data, ApiClient client) throws SignNowApiException {
        String documentId = (String) data.get("document_id");

        if (documentId == null || documentId.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Document ID is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // Create embedded editor link for document
        String redirectUrl = "http://localhost:8080/samples/UploadEmbeddedEditingAndInvite?" +
                "page=invite-page&document_id=" + documentId;

        DocumentEmbeddedEditorLinkPostRequest editLinkReq = new DocumentEmbeddedEditorLinkPostRequest(
                redirectUrl,
                "self",
                15
        ).withDocumentId(documentId);

        DocumentEmbeddedEditorLinkPostResponse response = (DocumentEmbeddedEditorLinkPostResponse) client.send(editLinkReq).getResponse();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("edit_link", response.getData().getUrl());
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<?> createInvite(Map<String, Object> data, ApiClient client) throws SignNowApiException {
        String documentId = (String) data.get("document_id");
        String signerEmail = (String) data.get("signer_email");
        String signerName = (String) data.get("signer_name");

        if (documentId == null || documentId.isEmpty() || 
            signerEmail == null || signerEmail.isEmpty() ||
            signerName == null || signerName.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Document ID, signer email and name are required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // Get document info and recipients
        DocumentGetResponse documentGetResponse = getDocument(client, documentId);
        List<Map<String, Object>> recipients = getDocumentRecipients(client, documentId);

        // Create invite for the document using real recipients
        var roles = documentGetResponse.getRoles();
        ToCollection to = new ToCollection();
        
        for (var role : roles) {
            // Find recipient for this role
            Map<String, Object> recipient = recipients.stream()
                    .filter(rec -> role.getUniqueId().equals(rec.get("role_id")))
                    .findFirst()
                    .orElse(null);
            
            // Use recipient email or fallback to provided email
            String emailToUse = recipient != null ? (String) recipient.get("email") : signerEmail;
            String nameToUse = recipient != null ? (String) recipient.get("role") : signerName;
            
            to.add(new To(
                    emailToUse,
                    role.getUniqueId(),
                    role.getName(),
                    Integer.parseInt(role.getSigningOrder()),
                    "Document Signing Request - Action Required",
                    "Dear " + nameToUse + ", please review and sign the uploaded document."
            ));
        }

        // Create and send invite request for document
        SendInvitePostRequest inviteRequest = new SendInvitePostRequest(
                documentId,
                to,
                "sender@signnow.com", // You can use your own sender email
                "Document Signing Request - Action Required",
                "Dear " + signerName + ", please review and sign the uploaded document."
        );
        inviteRequest.withDocumentId(documentId);

        client.send(inviteRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Invite sent successfully");
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> getInviteStatus(Map<String, Object> data, ApiClient client) throws SignNowApiException {
        String documentId = (String) data.get("document_id");
        List<Map<String, Object>> signers = getDocumentSignersStatus(client, documentId);
        return ResponseEntity.ok(signers);
    }

    private ResponseEntity<?> downloadDocument(Map<String, Object> data, ApiClient client) throws SignNowApiException, IOException {
        String documentId = (String) data.get("document_id");
        byte[] fileContents = downloadDocumentFile(client, documentId);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"final_document.pdf\"")
                .body(fileContents);
    }

    private Map<String, Object> uploadDocument(ApiClient client, File file, String documentName) throws SignNowApiException {
        // Create document upload request
        DocumentPostRequest documentPost = new DocumentPostRequest(file, documentName);

        DocumentPostResponse response = (DocumentPostResponse) client.send(documentPost).getResponse();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("document_id", response.getId());
        return result;
    }

    private String createDocumentGroup(ApiClient client, String documentId) throws SignNowApiException {
        // Create document group with the uploaded document
        DocumentIdCollection documentIdCollection = new DocumentIdCollection();
        documentIdCollection.add(documentId);

        DocumentGroupPostRequest documentGroupPost = new DocumentGroupPostRequest(
                documentIdCollection,
                "Uploaded Document Group"
        );

        DocumentGroupPostResponse response = (DocumentGroupPostResponse) client.send(documentGroupPost).getResponse();
        return response.getId();
    }

    private DocumentGroupGetResponse getDocumentGroup(ApiClient client, String documentGroupId) throws SignNowApiException {
        DocumentGroupGetRequest request = new DocumentGroupGetRequest();
        request.withDocumentGroupId(documentGroupId);
        return (DocumentGroupGetResponse) client.send(request).getResponse();
    }

    private DocumentGetResponse getDocument(ApiClient client, String documentId) throws SignNowApiException {
        DocumentGetRequest request = new DocumentGetRequest();
        request.withDocumentId(documentId);
        return (DocumentGetResponse) client.send(request).getResponse();
    }

    private List<String> getDocumentRoles(ApiClient client, String documentId) throws SignNowApiException {
        DocumentGetResponse document = getDocument(client, documentId);
        List<String> roles = new LinkedList<>();

        for (var role : document.getRoles()) {
            String roleName = role.getName();
            if (roleName != null && !roleName.isEmpty() && !roles.contains(roleName)) {
                roles.add(roleName);
            }
        }

        return roles;
    }

    private List<Map<String, Object>> getDocumentRecipients(ApiClient client, String documentId) throws SignNowApiException {
        // Get document info
        DocumentGetResponse documentGetResponse = getDocument(client, documentId);
        List<Map<String, Object>> recipients = new LinkedList<>();
        
        // Get roles and create basic recipient info
        var roles = documentGetResponse.getRoles();
        for (var role : roles) {
            Map<String, Object> recipient = new HashMap<>();
            recipient.put("email", ""); // Empty email, will be filled when adding recipients
            recipient.put("role", role.getName());
            recipient.put("role_id", role.getUniqueId());
            recipient.put("signing_order", Integer.parseInt(role.getSigningOrder()));
            recipient.put("inviter_role", false); // Default value
            recipients.add(recipient);
        }
        
        return recipients;
    }

    private List<Map<String, Object>> getDocumentSignersStatus(ApiClient client, String documentId) throws SignNowApiException {
        DocumentGetRequest request = new DocumentGetRequest();
        request.withDocumentId(documentId);

        DocumentGetResponse response = (DocumentGetResponse) client.send(request).getResponse();
        var invites = response.getFieldInvites();

        List<Map<String, Object>> statuses = new LinkedList<>();
        for (var invite : invites) {
            Map<String, Object> status = new HashMap<>();
            status.put("name", invite.getEmail() != null ? invite.getEmail() : "");
            status.put("timestamp", invite.getUpdated() != null ? 
                java.time.Instant.ofEpochSecond(Long.parseLong(invite.getUpdated())).toString() : "");
            status.put("status", invite.getStatus());
            statuses.add(status);
        }
        return statuses;
    }

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

    private DocumentGroupRecipientsGetResponse getDocumentGroupRecipients(ApiClient client, String documentGroupId) throws SignNowApiException {
        DocumentGroupRecipientsGetRequest recipientsRequest = new DocumentGroupRecipientsGetRequest()
                .withDocumentGroupId(documentGroupId);

        return (DocumentGroupRecipientsGetResponse) client.send(recipientsRequest).getResponse();
    }

    private byte[] downloadDocumentGroupFile(ApiClient client, String documentGroupId) throws SignNowApiException, IOException {
        var orderColl = new com.signnow.api.documentgroup.request.data.DocumentOrderCollection();
        DownloadDocumentGroupPostRequest downloadRequest = new DownloadDocumentGroupPostRequest(
                "merged",
                "no",
                orderColl
        ).withDocumentGroupId(documentGroupId);

        DownloadDocumentGroupPostResponse response = (DownloadDocumentGroupPostResponse) client.send(downloadRequest).getResponse();

        byte[] content = Files.readAllBytes(response.getFile().toPath());
        response.getFile().delete();

        return content;
    }

    private byte[] downloadDocumentFile(ApiClient client, String documentId) throws SignNowApiException, IOException {
        // For single document download, we'll use DocumentDownloadGet
        DocumentDownloadGetRequest downloadRequest = new DocumentDownloadGetRequest();
        downloadRequest.withDocumentId(documentId)
                .withType("collapsed")
                .withHistory("no");

        DocumentDownloadGetResponse response = (DocumentDownloadGetResponse) client.send(downloadRequest).getResponse();

        byte[] content = Files.readAllBytes(response.getFile().toPath());
        response.getFile().delete();

        return content;
    }
}
