package com.signnow.samples.EmbeddedSenderWithFormAndFirstSigner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.documentfield.request.DocumentPrefillPutRequest;
import com.signnow.api.documentfield.request.data.FieldCollection;
import com.signnow.api.documentfield.request.data.Field;
import com.signnow.core.ApiClient;
import com.signnow.core.exception.SignNowApiException;
import com.signnow.javasampleapp.ExampleInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Demo controller showing embedded sender integration with SignNow API
 * for document group templates and embedded signing with first signer workflow.
 * 
 * Features demonstrated:
 * - Creating document groups from templates
 * - Pre-filling document fields
 * - Managing recipients with different roles
 * - Embedded sending and signing flows
 * - Status tracking and document download
 * 
 * This is a demonstration application showing how to integrate SignNow API
 * into your application for document signing workflows with embedded sender experience.
 */
@Controller
public class IndexController implements ExampleInterface {

    // Demo configuration - in production, these would come from environment/config
    private static final String DOCUMENT_GROUP_TEMPLATE_ID = "8e36720a436041ea837dc543ec00a3bc3559df45";
    private static final String USER_EMAIL = "example@example.com"; // Demo user
    private static final String USER_PASSWORD = "example"; // Demo password

    // Demo field names that might exist in templates
    private static final String CUSTOMER_NAME_FIELD = "CustomerName";
    private static final String CUSTOMER_FN_FIELD = "CustomerFN";

    // Demo recipient roles
    private static final String PREPARE_CONTRACT_ROLE = "Prepare Contract";
    private static final String CUSTOMER_SIGN_ROLE = "Customer to Sign";

    // Demo URL constants
    private static final String REDIRECT_BASE_URL = "http://localhost:8080/samples/EmbeddedSenderWithFormAndFirstSigner";
    private static final String SIGNING_URL_BASE = "https://app.signnow.com/webapp/documentgroup/signing";

    @Override
    public ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException {
        // Demo: Simple HTML page serving
        String html = new String(Files.readAllBytes(Paths.get("src/main/resources/static/samples/EmbeddedSenderWithFormAndFirstSigner/index.html")));
        
        // Demo: Add query parameters to show different demo pages
        String page = queryParams.get("page");
        if (page != null) {
            html = html.replace("<!-- DEMO_PAGE_CONTENT -->", 
                "<div class='demo-note'>Showing demo page: " + page + "</div>");
        }
        
        return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
    }

    @Override
    public ResponseEntity<?> handlePost(String formData) throws IOException, SignNowApiException, UnsupportedEncodingException {
        Map<String, Object> data = new ObjectMapper().readValue(formData, Map.class);
        String action = (String) data.getOrDefault("action", "");

        ApiClient client = new Sdk().build().authenticate().getApiClient();

        // Demo: Simple action routing for different API operations
        return switch (action) {
            case "prepare_dg" -> prepareDocumentGroup(data, client);
            case "create_signing_url" -> createSigningUrl(data, client);
            case "invite-status" -> getInviteStatus(data, client);
            case "download-doc-group" -> downloadDocumentGroup(data, client);
            default -> {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid action: " + action);
                errorResponse.put("available_actions", List.of("prepare_dg", "create_signing_url", "invite-status", "download-doc-group"));
                yield ResponseEntity.badRequest().body(errorResponse);
            }
        };
    }

    private ResponseEntity<?> prepareDocumentGroup(Map<String, Object> data, ApiClient client) throws SignNowApiException {
        String name = (String) data.get("name");
        String email = (String) data.get("email");

        if (name == null || name.isEmpty() || email == null || email.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Name and email are required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // Demo: Step 1 - Create Document Group from Template
        Map<String, Object> dgResponse = createDocumentGroupFromTemplate(client);
        if (!(Boolean) dgResponse.get("success")) {
            return ResponseEntity.status(500).body(dgResponse);
        }

        String documentGroupId = (String) dgResponse.get("document_group_id");

        // Demo: Step 2 - Update document fields with customer name
        Map<String, Object> updateFieldsResponse = updateDocumentFields(client, documentGroupId, name);
        if (!(Boolean) updateFieldsResponse.get("success")) {
            return ResponseEntity.status(500).body(updateFieldsResponse);
        }

        // Demo: Step 3 - Add recipients to Document Group with different emails for different roles
        String customerEmail = email; // Email from form for "Customer to Sign"
        String preparerEmail = USER_EMAIL; // Email from config for "Prepare Contract"
        Map<String, Object> addRecipientsResponse = updateDocumentGroupRecipients(
            client, documentGroupId, customerEmail, preparerEmail
        );
        if (!(Boolean) addRecipientsResponse.get("success")) {
            return ResponseEntity.status(500).body(addRecipientsResponse);
        }

        // Demo: Step 4 - Create embedded sending link
        Map<String, Object> embeddedSendingResponse = createEmbeddedSendingUrl(client, documentGroupId);
        if (!(Boolean) embeddedSendingResponse.get("success")) {
            return ResponseEntity.status(500).body(embeddedSendingResponse);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Document group prepared and embedded sending link created successfully");
        response.put("embedded_url", embeddedSendingResponse.get("embedded_url"));

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> createSigningUrl(Map<String, Object> data, ApiClient client) throws SignNowApiException, UnsupportedEncodingException {
        String documentGroupId = (String) data.get("document_group_id");

        // Demo: Create signing link with limited token
        Map<String, Object> signingLinkResponse = createSigningLink(client, documentGroupId);
        if (!(Boolean) signingLinkResponse.get("success")) {
            return ResponseEntity.status(500).body(signingLinkResponse);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("signing_url", signingLinkResponse.get("signing_url"));

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> getInviteStatus(Map<String, Object> data, ApiClient client) throws SignNowApiException {
        String documentGroupId = (String) data.get("document_group_id");
        List<Map<String, Object>> signers = getDocumentGroupSignersStatus(client, documentGroupId);
        return ResponseEntity.ok(signers);
    }

    private ResponseEntity<?> downloadDocumentGroup(Map<String, Object> data, ApiClient client) throws SignNowApiException, IOException {
        String documentGroupId = (String) data.get("document_group_id");
        byte[] fileContents = downloadDocumentGroupFile(client, documentGroupId);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"final_document_group.pdf\"")
                .body(fileContents);
    }

    private Map<String, Object> createDocumentGroupFromTemplate(ApiClient client) throws SignNowApiException {
        // Demo: Create document group directly from Document Group Template
        var request = new com.signnow.api.documentgrouptemplate.request.DocumentGroupTemplatePostRequest(
            "Embedded Sender Form Document Group", null, null
        );
        request.withTemplateGroupId(DOCUMENT_GROUP_TEMPLATE_ID);

        var response = (com.signnow.api.documentgrouptemplate.response.DocumentGroupTemplatePostResponse) 
            client.send(request).getResponse();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("document_group_id", response.getData().getUniqueId());
        return result;
    }

    private Map<String, Object> updateDocumentFields(ApiClient client, String documentGroupId, String name) throws SignNowApiException {
        // Demo: Shows how to iterate through documents in a group and pre-fill fields
        var docGroup = getDocumentGroup(client, documentGroupId);

        for (var docItem : docGroup.getDocuments()) {
            String docId = docItem.getId();
            var documentData = getDocument(client, docId);

            // Demo: Extract field names from document structure
            List<String> existingFields = extractFieldNames(documentData);

            // Demo: Pre-fill customer name field (supports multiple field name variations)
            FieldCollection fieldValues = new FieldCollection();
            if (existingFields.contains(CUSTOMER_NAME_FIELD)) {
                fieldValues.add(new Field(CUSTOMER_NAME_FIELD, name));
            } else if (existingFields.contains(CUSTOMER_FN_FIELD)) {
                fieldValues.add(new Field(CUSTOMER_FN_FIELD, name));
            }

            if (!fieldValues.isEmpty()) {
                var prefillRequest = new DocumentPrefillPutRequest(fieldValues);
                prefillRequest.withDocumentId(docId);
                client.send(prefillRequest);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    private List<String> extractFieldNames(DocumentGetResponse documentData) {
        List<String> fieldNames = new LinkedList<>();
        
        for (var field : documentData.getFields()) {
            String fieldName = extractFieldName(field);
            if (fieldName != null) {
                fieldNames.add(fieldName);
            }
        }
        
        return fieldNames;
    }

    private String extractFieldName(Object field) {
        Map<String, Object> fieldMap = (Map<String, Object>) field;
        Object jsonAttributes = fieldMap.get("jsonAttributes");
        
        if (jsonAttributes instanceof Map) {
            Map<String, Object> attributes = (Map<String, Object>) jsonAttributes;
            Object nameValue = attributes.get("name");
            return nameValue != null ? nameValue.toString() : null;
        }
        
        return null;
    }

    private Map<String, Object> updateDocumentGroupRecipients(
        ApiClient client,
        String documentGroupId,
        String customerEmail,
        String preparerEmail
    ) throws SignNowApiException {
        // Demo: Get current recipients from document group
        var recipientsResponse = getDocumentGroupRecipients(client, documentGroupId);
        var currentRecipients = recipientsResponse.getData().getRecipients();

        // Demo: Create updated recipients with different email addresses based on role
        List<com.signnow.api.documentgroup.request.data.recipient.Recipient> updatedRecipients = new LinkedList<>();
        
        for (var recipient : currentRecipients) {
            String recipientName = recipient.getName();

            // Demo: Convert Response DocumentCollection to Request DocumentCollection
            List<com.signnow.api.documentgroup.request.data.recipient.Document> requestDocuments = new LinkedList<>();
            for (var document : recipient.getDocuments()) {
                requestDocuments.add(new com.signnow.api.documentgroup.request.data.recipient.Document(
                    document.getId(), document.getRole(), document.getAction()
                ));
            }
            var requestDocumentCollection = new com.signnow.api.documentgroup.request.data.recipient.DocumentCollection();
            requestDocuments.forEach(requestDocumentCollection::add);

            // Demo: Assign email based on recipient role/name
            String emailToUse = getEmailForRole(recipientName, customerEmail, preparerEmail);

            updatedRecipients.add(new com.signnow.api.documentgroup.request.data.recipient.Recipient(
                recipientName,
                emailToUse,
                recipient.getOrder(),
                requestDocumentCollection,
                null,
                null
            ));
        }

        var recipientsCollection = new com.signnow.api.documentgroup.request.data.recipient.RecipientCollection();
        updatedRecipients.forEach(recipientsCollection::add);
        var ccCollection = new com.signnow.api.documentgroup.request.data.CcCollection(); // Empty CC collection

        var updateRequest = new com.signnow.api.documentgroup.request.DocumentGroupRecipientsPutRequest(
            recipientsCollection,
            ccCollection
        );
        updateRequest.withDocumentGroupId(documentGroupId);

        client.send(updateRequest);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    private String getEmailForRole(String recipientName, String customerEmail, String preparerEmail) {
        switch (recipientName) {
            case PREPARE_CONTRACT_ROLE:
                return preparerEmail;
            case CUSTOMER_SIGN_ROLE:
                return customerEmail;
            default:
                return customerEmail; // default fallback
        }
    }

    private Map<String, Object> createEmbeddedSendingUrl(ApiClient client, String documentGroupId) throws SignNowApiException {
        // Demo: Create embedded sending URL with redirect back to demo app
        String redirectUrl = REDIRECT_BASE_URL + "?page=signing-page&document_group_id=" + documentGroupId;

        var embeddedSendingRequest = new com.signnow.api.embeddedsending.request.DocumentGroupEmbeddedSendingLinkPostRequest(
            redirectUrl,  // Demo: Redirect back to our demo app
            15,           // Demo: 15 minute expiration
            "self",       // Demo: Self-hosted embedded experience
            "send-invite" // Demo: Action type
        );
        embeddedSendingRequest.withDocumentGroupId(documentGroupId);

        var response = (com.signnow.api.embeddedsending.response.DocumentGroupEmbeddedSendingLinkPostResponse) 
            client.send(embeddedSendingRequest).getResponse();

        // Demo: Get the embedded URL from response
        String embeddedUrl = response.getData().getUrl();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("embedded_url", embeddedUrl);
        result.put("demo_note", "This URL allows embedded sending experience");
        return result;
    }

    private Map<String, Object> createSigningLink(ApiClient client, String documentGroupId) throws SignNowApiException, UnsupportedEncodingException {
        // Demo: Generate limited token for Document Group
        Map<String, Object> limitedTokenResponse = generateLimitedToken(client, documentGroupId);
        if (!(Boolean) limitedTokenResponse.get("success")) {
            return limitedTokenResponse;
        }

        String accessToken = (String) limitedTokenResponse.get("access_token");

        // Demo: Build signing URL with parameters - redirect to status page after signing
        String redirectUrl = REDIRECT_BASE_URL + "?page=status-page&document_group_id=" + documentGroupId;

        String signingUrl = SIGNING_URL_BASE + "?" +
            "document_group_id=" + documentGroupId +
            "&access_token=" + accessToken +
            "&sign=1" +
            "&embedded=1" +
            "&redirect_uri=" + java.net.URLEncoder.encode(redirectUrl, "UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("signing_url", signingUrl);
        return result;
    }

    private Map<String, Object> generateLimitedToken(ApiClient client, String documentGroupId) throws SignNowApiException {
        // Demo: Create token request with limited scope for document group
        var tokenRequest = new com.signnow.api.auth.request.TokenPostRequest(
            USER_EMAIL,
            USER_PASSWORD,
            "limited_signer_scope_token_for_document_group_invite/" + documentGroupId,
            "password",
            ""
        );

        var response = (com.signnow.api.auth.response.TokenPostResponse) client.send(tokenRequest).getResponse();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("access_token", response.getAccessToken());
        return result;
    }

    private List<Map<String, Object>> getDocumentGroupSignersStatus(ApiClient client, String documentGroupId) throws SignNowApiException {
        var recipientsResponse = getDocumentGroupRecipients(client, documentGroupId);
        List<Map<String, Object>> signers = new LinkedList<>();

        // Demo: Get invite status for the document group
        var docGroup = getDocumentGroup(client, documentGroupId);
        String inviteId = docGroup.getInviteId();

        if (inviteId == null || inviteId.isEmpty()) {
            // Demo: If no invite exists, return basic recipient info
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

        var inviteStatusRequest = new com.signnow.api.documentgroupinvite.request.GroupInviteGetRequest()
            .withDocumentGroupId(documentGroupId)
            .withInviteId(inviteId);

        var inviteStatusResponse = (com.signnow.api.documentgroupinvite.response.GroupInviteGetResponse) 
            client.send(inviteStatusRequest).getResponse();

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

    private byte[] downloadDocumentGroupFile(ApiClient client, String documentGroupId) throws SignNowApiException, IOException {
        var orderColl = new com.signnow.api.documentgroup.request.data.DocumentOrderCollection();
        var downloadRequest = new com.signnow.api.documentgroup.request.DownloadDocumentGroupPostRequest(
            "merged", "no", orderColl
        ).withDocumentGroupId(documentGroupId);

        var response = (com.signnow.api.documentgroup.response.DownloadDocumentGroupPostResponse) 
            client.send(downloadRequest).getResponse();

        byte[] content = Files.readAllBytes(response.getFile().toPath());
        response.getFile().delete();

        return content;
    }

    private com.signnow.api.documentgroup.response.DocumentGroupGetResponse getDocumentGroup(ApiClient client, String documentGroupId) throws SignNowApiException {
        var request = new com.signnow.api.documentgroup.request.DocumentGroupGetRequest()
            .withDocumentGroupId(documentGroupId);
        return (com.signnow.api.documentgroup.response.DocumentGroupGetResponse) client.send(request).getResponse();
    }

    private DocumentGetResponse getDocument(ApiClient client, String documentId) throws SignNowApiException {
        var request = new DocumentGetRequest().withDocumentId(documentId);
        return (DocumentGetResponse) client.send(request).getResponse();
    }

    private com.signnow.api.documentgroup.response.DocumentGroupRecipientsGetResponse getDocumentGroupRecipients(
        ApiClient client,
        String documentGroupId
    ) throws SignNowApiException {
        var recipientsRequest = new com.signnow.api.documentgroup.request.DocumentGroupRecipientsGetRequest()
            .withDocumentGroupId(documentGroupId);

        return (com.signnow.api.documentgroup.response.DocumentGroupRecipientsGetResponse) 
            client.send(recipientsRequest).getResponse();
    }
} 