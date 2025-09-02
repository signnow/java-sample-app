package com.signnow.samples.ISVWithFormAndOneClickSendMergeFields;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.request.data.*;
import com.signnow.api.document.request.data.line.LineCollection;
import com.signnow.api.document.request.data.radiobutton.RadiobuttonCollection;

import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.document.request.DocumentPutRequest;
import com.signnow.core.ApiClient;
import com.signnow.core.exception.SignNowApiException;
import com.signnow.javasampleapp.ExampleInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.nio.file.Files;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Demo controller showing ISV integration with SignNow API
 * for document group templates with merge fields and one-click sending.
 * 
 * Features demonstrated:
 * - Creating document groups from templates
 * - Processing merge fields by adding permanent text elements
 * - Managing recipients with different roles and emails
 * - One-click sending with invite creation
 * - Status tracking and document download
 * 
 * This is a demonstration application showing how to integrate SignNow API
 * into your application for document signing workflows with merge field processing.
 */
@Controller
public class IndexController implements ExampleInterface {

    // Demo configuration - in production, these would come from environment/config
    private static final String DOCUMENT_GROUP_TEMPLATE_ID = "8e36720a436041ea837dc543ec00a3bc3559df45";
    private static final String USER_EMAIL = "example@example.com"; // Demo user
    private static final String USER_PASSWORD = "example"; // Demo password

    // Demo field names that might exist in templates
    private static final String CUSTOMER_NAME_FIELD = "CustomerName";
    private static final String COMPANY_NAME_FIELD = "CompanyName";

    // Demo recipient roles
    private static final String PREPARE_CONTRACT_ROLE = "Prepare Contract";
    private static final String CUSTOMER_SIGN_ROLE = "Customer to Sign";

    // Demo URL constants
    private static final String REDIRECT_BASE_URL = "http://localhost:8080/samples/ISVWithFormAndOneClickSendMergeFields";

    @Override
    public ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException {
        // Demo: Simple HTML page serving - use classpath resource
        try (var inputStream = getClass().getResourceAsStream("/static/samples/ISVWithFormAndOneClickSendMergeFields/index.html")) {
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
        String customerName = (String) data.get("customer_name");
        String companyName = (String) data.get("company_name");
        String email = (String) data.get("email");

        // Demo: Step 1 - Create Document Group from Template
        Map<String, Object> dgResponse = createDocumentGroupFromTemplate(client);

        String documentGroupId = (String) dgResponse.get("document_group_id");
        


        // Demo: Step 2 - Process merge fields with customer and company names
        Map<String, Object> updateFieldsResponse = processMergeFields(client, documentGroupId, customerName, companyName);
        if (!(Boolean) updateFieldsResponse.get("success")) {
            return ResponseEntity.status(500).body(updateFieldsResponse);
        }

        // Demo: Step 3 - Send invite to recipients (no separate recipient update)
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

        var orderColl = new com.signnow.api.documentgroup.request.data.DocumentOrderCollection();
        var downloadRequest = new com.signnow.api.documentgroup.request.DownloadDocumentGroupPostRequest(
            "merged", "no", orderColl
        ).withDocumentGroupId(documentGroupId);

        var response = (com.signnow.api.documentgroup.response.DownloadDocumentGroupPostResponse) 
            client.send(downloadRequest).getResponse();

        // Get the actual filename from the downloaded file
        String filename = response.getFile().getName();
        byte[] content = Files.readAllBytes(response.getFile().toPath());
        response.getFile().delete();

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(content);
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

    /**
     * Process merge fields by adding permanent text elements to documents.
     * 
     * This method processes merge fields (CustomerName and CompanyName) by creating
     * permanent text elements that become part of the document itself. These text
     * elements are read-only and cannot be modified by signers, making them
     * permanent part of the document content.
     * 
     * The text elements are positioned at the same coordinates as the original
     * merge fields, effectively converting them into non-editable text content.
     * All other fields are preserved in the document.
     */
    private Map<String, Object> processMergeFields(
        ApiClient client,
        String documentGroupId,
        String customerName,
        String companyName
    ) throws SignNowApiException {
        // Fetch group info to get each doc ID
        var docGroup = getDocumentGroup(client, documentGroupId);

        // For each document in the group, add text elements instead of filling fields
        for (var docItem : docGroup.getDocuments()) {
            String docId = docItem.getId();
            var documentData = getDocument(client, docId);
            
            // Create text collection with customer name and company name
            TextCollection textCollection = new TextCollection();
            com.signnow.api.document.request.data.FieldCollection fieldCollection = new com.signnow.api.document.request.data.FieldCollection();
            
            // Get coordinates from existing fields
            Object customerNameField = null;
            Object companyNameField = null;
            
            for (var field : documentData.getFields()) {
                String fieldName = extractFieldName(field);
                if (CUSTOMER_NAME_FIELD.equals(fieldName)) {
                    customerNameField = field;
                } else if (COMPANY_NAME_FIELD.equals(fieldName)) {
                    companyNameField = field;
                } else {
                    // Create a new Field object for the request using jsonAttributes directly
                    Map<String, Object> fieldMap = (Map<String, Object>) field;
                    
                    // Extract field attributes
                    Map<String, Object> jsonAttributes = (Map<String, Object>) fieldMap.get("json_attributes");
                    
                    // Create new field for request using Field.fromMap() method with jsonAttributes directly
                    // jsonAttributes already contains all field data including coordinates
                    jsonAttributes.put("type", fieldMap.getOrDefault("type", "text"));
                    jsonAttributes.put("role", fieldMap.getOrDefault("role", ""));
                    
                    var requestField = com.signnow.api.document.request.data.Field.fromMap(jsonAttributes);
                    fieldCollection.add(requestField);
                }
            }
            
            // Add customer name text at field coordinates if field exists
            if (customerNameField != null) {
                Map<String, Object> fieldCoords = extractFieldCoordinates(customerNameField);
                textCollection.add(new Text(
                    ((Number) fieldCoords.get("x")).intValue(),
                    ((Number) fieldCoords.get("y")).intValue(),
                    (Integer) fieldCoords.getOrDefault("size", 25),
                    ((Number) fieldCoords.get("width")).intValue(),
                    ((Number) fieldCoords.get("height")).intValue(),
                    "text", // subtype
                    (Integer) fieldCoords.get("pageNumber"),
                    customerName, // data
                    "Arial", // font
                    (Integer) fieldCoords.getOrDefault("size", 25), // lineHeight
                    null // fieldId
                ));
            }
            
            // Add company name text at field coordinates if field exists
            if (companyNameField != null) {
                Map<String, Object> fieldCoords = extractFieldCoordinates(companyNameField);
                textCollection.add(new Text(
                    ((Number) fieldCoords.get("x")).intValue(),
                    ((Number) fieldCoords.get("y")).intValue(),
                    (Integer) fieldCoords.getOrDefault("size", 20),
                    ((Number) fieldCoords.get("width")).intValue(),
                    ((Number) fieldCoords.get("height")).intValue(),
                    "text", // subtype
                    (Integer) fieldCoords.get("pageNumber"),
                    companyName, // data
                    "Arial", // font
                    (Integer) fieldCoords.getOrDefault("size", 20), // lineHeight
                    null // fieldId
                ));
            }

            // Create DocumentPut request with preserved fields and text elements
            var documentPutRequest = new DocumentPutRequest(
                fieldCollection, // fields - empty for DocumentPut
                new LineCollection(), // lines
                new CheckCollection(), // checks
                new RadiobuttonCollection(), // radiobuttons
                new SignatureCollection(), // signatures
                textCollection, // texts
                new AttachmentCollection(), // attachments
                new HyperlinkCollection(), // hyperlinks
                new IntegrationObjectCollection(), // integrationObjects
                new DeactivateElementCollection(), // deactivateElements
                "ISV Form Document Group", // documentName
                "" // clientTimestamp
            );
            documentPutRequest.withDocumentId(docId);
            
            client.send(documentPutRequest);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    private String extractFieldName(Object field) {
        Map<String, Object> fieldMap = (Map<String, Object>) field;
        Object jsonAttributes = fieldMap.get("json_attributes");
        
        if (jsonAttributes instanceof Map) {
            Map<String, Object> attributes = (Map<String, Object>) jsonAttributes;
            Object nameValue = attributes.get("name");
            return nameValue != null ? nameValue.toString() : null;
        }
        
        return null;
    }

    private Map<String, Object> extractFieldCoordinates(Object field) {
        Map<String, Object> fieldMap = (Map<String, Object>) field;
        Object jsonAttributes = fieldMap.get("json_attributes");
        
        Map<String, Object> coordinates = new HashMap<>();
        if (jsonAttributes instanceof Map) {
            Map<String, Object> attributes = (Map<String, Object>) jsonAttributes;
            coordinates.put("x", attributes.get("x"));
            coordinates.put("y", attributes.get("y"));
            coordinates.put("width", attributes.get("width"));
            coordinates.put("height", attributes.get("height"));
            coordinates.put("size", attributes.get("size"));
            coordinates.put("pageNumber", attributes.get("page_number"));
        }
        
        return coordinates;
    }

    /**
     * Send invite to recipients for document group signing.
     * 
     * This method creates and sends invites to all recipients in a document group.
     * It assigns appropriate emails based on recipient roles and creates invite actions
     * for each recipient and document combination.
     * 
     * @param client The API client for making requests
     * @param documentGroupId The ID of the document group to send invites for
     * @param email The email address to use for form-based recipients
     * @return Map containing success status and any error messages
     * @throws SignNowApiException If API request fails
     */
    private Map<String, Object> sendInvite(ApiClient client, String documentGroupId, String email) throws SignNowApiException {
        // Get document group to find documents
        var documentGroupGetResponse = getDocumentGroup(client, documentGroupId);

        // Get recipients to use their roles
        var recipientsResponse = getDocumentGroupRecipients(client, documentGroupId);
        
        // Debug: Print recipients response as JSON
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonResponse = mapper.writeValueAsString(recipientsResponse);
            System.out.println("DEBUG: DocumentGroupRecipientsGetResponse JSON:");
            System.out.println(jsonResponse);
        } catch (Exception e) {
            System.out.println("DEBUG: Error serializing DocumentGroupRecipientsGetResponse: " + e.getMessage());
        }
        
        var recipients = recipientsResponse.getData().getRecipients();

        // Check if we have recipients
        if (recipients.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "No recipients found in document group");
            return errorResponse;
        }

        // Create invite actions and emails for each recipient and document
        List<com.signnow.api.documentgroupinvite.request.data.invitestep.InviteAction> inviteActions = new LinkedList<>();
        List<com.signnow.api.documentgroupinvite.request.data.invitestep.InviteEmail> inviteEmails = new LinkedList<>();
        
        for (var recipient : recipients) {
            // Assign email based on recipient role/name
            String emailToUse = email; // Default to form email
            if (PREPARE_CONTRACT_ROLE.equals(recipient.getName())) {
                emailToUse = email; // Use form email for "Prepare Contract"
            } else if (CUSTOMER_SIGN_ROLE.equals(recipient.getName())) {
                // Use config email for "Customer to Sign"
                emailToUse = USER_EMAIL;
            }

            // Create invite email for this recipient
            inviteEmails.add(new com.signnow.api.documentgroupinvite.request.data.invitestep.InviteEmail(
                emailToUse,
                "Review and sign documents",
                "Please review and sign the documents",
                null,
                30, // expirationDays
                    10 // reminderDays
            ));

            for (var document : documentGroupGetResponse.getDocuments()) {
                inviteActions.add(new com.signnow.api.documentgroupinvite.request.data.invitestep.InviteAction(
                    emailToUse,
                    recipient.getName(),
                    "sign", // action
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
        }

        var inviteActionCollection = new com.signnow.api.documentgroupinvite.request.data.invitestep.InviteActionCollection();
        inviteActions.forEach(inviteActionCollection::add);

        var inviteEmailCollection = new com.signnow.api.documentgroupinvite.request.data.invitestep.InviteEmailCollection();
        inviteEmails.forEach(inviteEmailCollection::add);

        // Create invite step
        var inviteStep = new com.signnow.api.documentgroupinvite.request.data.invitestep.InviteStep(
            1, // order
            inviteActionCollection,
            inviteEmailCollection
        );

        var inviteStepCollection = new com.signnow.api.documentgroupinvite.request.data.invitestep.InviteStepCollection();
        inviteStepCollection.add(inviteStep);

        // Create empty collections
        var ccCollection = new com.signnow.api.documentgroupinvite.request.data.CcCollection();

        // Create and send invite request
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

    private String findRoleForRecipient(DocumentGetResponse documentDetails, String recipientName) {
        // Try to find a role that matches the recipient name
        for (var role : documentDetails.getRoles()) {
            if (role.getName().equals(recipientName)) {
                return role.getName();
            }
        }
        
        // If no exact match, try to find a role that contains the recipient name
        for (var role : documentDetails.getRoles()) {
            if (role.getName().toLowerCase().contains(recipientName.toLowerCase()) || 
                recipientName.toLowerCase().contains(role.getName().toLowerCase())) {
                return role.getName();
            }
        }
        
        // If still no match, use the first available role
        if (!documentDetails.getRoles().isEmpty()) {
            return documentDetails.getRoles().get(0).getName();
        }
        
        // Fallback to recipient name if no roles found
        return recipientName;
    }

    private List<Map<String, Object>> getDocumentGroupSignersStatus(ApiClient client, String documentGroupId) throws SignNowApiException {
        var recipientsResponse = getDocumentGroupRecipients(client, documentGroupId);
        List<Map<String, Object>> signers = new LinkedList<>();

        // Get invite status for the document group
        var docGroup = getDocumentGroup(client, documentGroupId);
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