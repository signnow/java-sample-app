package com.signnow.samples.ISVWithFormAndOneClickSendBasicPrefill;

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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Demo controller showing ISV integration with SignNow API
 * for document group templates with form pre-filling and one-click sending.
 * 
 * Features demonstrated:
 * - Creating document groups from templates
 * - Pre-filling document fields with form data
 * - Sending invites to recipients
 * - Status tracking and document download
 * 
 * This is a demonstration application showing how to integrate SignNow API
 * into your ISV application for document signing workflows.
 */
@Controller
public class IndexController implements ExampleInterface {

    // Demo configuration - in production, these would come from environment/config
    private static final String DOCUMENT_GROUP_TEMPLATE_ID = "6e79b9e6f9624984a7f054a7171d1644d0fb9934";
    private static final String USER_EMAIL = "example@example.com"; // Demo user
    private static final String USER_PASSWORD = "example"; // Demo password

    // Demo field names that might exist in templates
    private static final String NAME_FIELD = "Name";
    private static final String EMAIL_FIELD = "Email";

    // Demo URL constants
    private static final String REDIRECT_BASE_URL = "http://localhost:8080/samples/ISVWithFormAndOneClickSendBasicPrefill";

    @Override
        public ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException {
        // Demo: Simple HTML page serving
        try (var inputStream = getClass().getResourceAsStream("/static/samples/ISVWithFormAndOneClickSendBasicPrefill/index.html")) {
            if (inputStream == null) {
                throw new IOException("HTML file not found in classpath");
            }
            String html = new String(inputStream.readAllBytes());
            
            // Demo: Add query parameters to show different demo pages
            String page = queryParams.get("page");
            if (page != null) {
                html = html.replace("<!-- DEMO_PAGE_CONTENT -->", 
                    "<div class='demo-note'>Showing demo page: " + page + "</div>");
            }

            return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
        }
    }

    @Override
    public ResponseEntity<?> handlePost(String formData) throws IOException, SignNowApiException {
        Map<String, Object> data = new ObjectMapper().readValue(formData, Map.class);
        String action = (String) data.getOrDefault("action", "");

        ApiClient client = new Sdk().build().authenticate().getApiClient();

        // Demo: Simple action routing for different API operations
        return switch (action) {
            case "prepare_dg" -> prepareDocumentGroup(data, client);
            case "invite-status" -> getInviteStatus(data, client);
            case "download-doc-group" -> downloadDocumentGroup(data, client);
            default -> {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid action: " + action);
                errorResponse.put("available_actions", List.of("prepare_dg", "invite-status", "download-doc-group"));
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

        // Demo: Step 2 - Update document fields with names
        Map<String, Object> updateFieldsResponse = updateDocumentFields(client, documentGroupId, name, email);
        if (!(Boolean) updateFieldsResponse.get("success")) {
            return ResponseEntity.status(500).body(updateFieldsResponse);
        }

        // Demo: Step 3 - Send invite to the recipient
        Map<String, Object> inviteResponse = sendInvite(client, documentGroupId, email);
        if (!(Boolean) inviteResponse.get("success")) {
            return ResponseEntity.status(500).body(inviteResponse);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("document_group_id", documentGroupId);

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> getInviteStatus(Map<String, Object> data, ApiClient client) throws SignNowApiException {
        String documentGroupId = (String) data.get("document_group_id");
        List<Map<String, Object>> signers = getDocumentGroupSignersStatus(client, documentGroupId);
        return ResponseEntity.ok(signers);
    }

    private ResponseEntity<?> downloadDocumentGroup(Map<String, Object> data, ApiClient client) throws SignNowApiException, IOException {
        String documentGroupId = (String) data.get("document_group_id");

        if (documentGroupId == null || documentGroupId.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Document group ID is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        File file = downloadDocumentGroupFile(client, documentGroupId);
        
        String filename = file.getName();
        byte[] content = Files.readAllBytes(file.toPath());
        file.delete();

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(content);
    }

    private File downloadDocumentGroupFile(ApiClient client, String documentGroupId) throws SignNowApiException {
        var orderColl = new com.signnow.api.documentgroup.request.data.DocumentOrderCollection();
        var downloadRequest = new com.signnow.api.documentgroup.request.DownloadDocumentGroupPostRequest(
            "merged", "no", orderColl
        ).withDocumentGroupId(documentGroupId);

        var response = (com.signnow.api.documentgroup.response.DownloadDocumentGroupPostResponse) 
            client.send(downloadRequest).getResponse();

        return response.getFile();
    }

    private Map<String, Object> createDocumentGroupFromTemplate(ApiClient client) throws SignNowApiException {
        // Demo: Create document group directly from Document Group Template
        var request = new com.signnow.api.documentgrouptemplate.request.DocumentGroupTemplatePostRequest(
            "ISV Form Document Group", null, null
        );
        request.withTemplateGroupId(DOCUMENT_GROUP_TEMPLATE_ID);

        var response = (com.signnow.api.documentgrouptemplate.response.DocumentGroupTemplatePostResponse) 
            client.send(request).getResponse();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("document_group_id", response.getData().getUniqueId());
        return result;
    }

    private Map<String, Object> updateDocumentFields(ApiClient client, String documentGroupId, String name, String email) throws SignNowApiException {
        // Demo: Shows how to iterate through documents in a group and pre-fill fields
        var docGroup = getDocumentGroup(client, documentGroupId);

        for (var docItem : docGroup.getDocuments()) {
            String docId = docItem.getId();
            var documentData = getDocument(client, docId);

            // Demo: Extract field names from document structure
            List<String> existingFields = extractFieldNames(documentData);

            // Demo: Build a field collection with only valid fields
            FieldCollection fieldValues = new FieldCollection();

            // Demo: Try to fill "Name" field
            if (existingFields.contains(NAME_FIELD)) {
                fieldValues.add(new Field(NAME_FIELD, name));
            }

            // Demo: Try to fill "Email" field
            if (existingFields.contains(EMAIL_FIELD)) {
                fieldValues.add(new Field(EMAIL_FIELD, email));
            }

            // Demo: If there are any fields to fill, send the request
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

    private Map<String, Object> sendInvite(ApiClient client, String documentGroupId, String email) throws SignNowApiException {
        // Demo: Get document group to find documents
        var documentGroupGetResponse = getDocumentGroup(client, documentGroupId);

        // Demo: Create invite actions for each document
        List<com.signnow.api.documentgroupinvite.request.data.invitestep.InviteAction> inviteActions = new LinkedList<>();
        for (var document : documentGroupGetResponse.getDocuments()) {
            inviteActions.add(new com.signnow.api.documentgroupinvite.request.data.invitestep.InviteAction(
                email,
                "Recipient 1",
                "sign",
                document.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                REDIRECT_BASE_URL + "?page=status-page&document_group_id=" + documentGroupId, // redirectUri
                null,
                "self", // redirectTarget
                null
            ));
        }

        var inviteActionCollection = new com.signnow.api.documentgroupinvite.request.data.invitestep.InviteActionCollection();
        inviteActions.forEach(inviteActionCollection::add);

        // Demo: Create invite email
        var inviteEmail = new com.signnow.api.documentgroupinvite.request.data.invitestep.InviteEmail(
            email,
            "Review and sign documents",
            "Please review and sign the documents",
            null,
            30, // expirationDays
            10 // reminderDays
        );

        var inviteEmailCollection = new com.signnow.api.documentgroupinvite.request.data.invitestep.InviteEmailCollection();
        inviteEmailCollection.add(inviteEmail);

        // Demo: Create invite step
        var inviteStep = new com.signnow.api.documentgroupinvite.request.data.invitestep.InviteStep(
            1, // order
            inviteActionCollection,
            inviteEmailCollection
        );

        var inviteStepCollection = new com.signnow.api.documentgroupinvite.request.data.invitestep.InviteStepCollection();
        inviteStepCollection.add(inviteStep);

        // Demo: Create empty collections
        var ccCollection = new com.signnow.api.documentgroupinvite.request.data.CcCollection();

        // Demo: Create and send invite request
        var inviteRequest = new com.signnow.api.documentgroupinvite.request.GroupInvitePostRequest(
            inviteStepCollection,
            ccCollection,
            null,
            null,
            true, // signAsMerged
            100,
            null, null
        );
        inviteRequest.withDocumentGroupId(documentGroupId);

        client.send(inviteRequest);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
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