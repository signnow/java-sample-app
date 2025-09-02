package com.signnow.samples.HROnboardingSystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.documentfield.request.DocumentPrefillPutRequest;
import com.signnow.api.documentfield.request.data.FieldCollection;
import com.signnow.api.documentfield.request.data.Field;
import com.signnow.api.template.request.CloneTemplatePostRequest;
import com.signnow.api.template.response.CloneTemplatePostResponse;
import com.signnow.api.documentgroup.request.DocumentGroupPostRequest;
import com.signnow.api.documentgroup.request.data.DocumentIdCollection;
import com.signnow.api.documentgroup.response.DocumentGroupGetResponse;
import com.signnow.api.documentgroup.response.DocumentGroupPostResponse;
import com.signnow.api.documentgroupinvite.request.GroupInvitePostRequest;
import com.signnow.api.documentgroupinvite.request.data.invitestep.InviteAction;
import com.signnow.api.documentgroupinvite.request.data.invitestep.InviteActionCollection;
import com.signnow.api.documentgroupinvite.request.data.invitestep.InviteEmail;
import com.signnow.api.documentgroupinvite.request.data.invitestep.InviteEmailCollection;
import com.signnow.api.documentgroupinvite.request.data.invitestep.InviteStep;
import com.signnow.api.documentgroupinvite.request.data.invitestep.InviteStepCollection;
import com.signnow.api.documentgroupinvite.request.data.CcCollection;
import com.signnow.api.documentgroupinvite.request.GroupInviteGetRequest;
import com.signnow.api.documentgroupinvite.response.GroupInviteGetResponse;
import com.signnow.api.documentgroup.request.DownloadDocumentGroupPostRequest;
import com.signnow.api.documentgroup.response.DownloadDocumentGroupPostResponse;
import com.signnow.api.document.response.data.Role;
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
import java.util.Set;

/**
 * Demo controller showing HR Onboarding System integration with SignNow API.
 * 
 * Features demonstrated:
 * - Creating document groups from multiple templates
 * - Pre-filling document fields with employee data
 * - Managing multiple recipients with different roles (HR Manager, Employee, Employer)
 * - Sending invites with proper signing order
 * - Status tracking and document download
 * 
 * This is a demonstration application showing how to integrate SignNow API
 * into your HR system for employee onboarding workflows.
 */
@Controller
public class IndexController implements ExampleInterface {

    // Demo configuration - in production, these would come from environment/config
    private static final String USER_EMAIL = "example@example.com"; // Demo user
    private static final String USER_PASSWORD = "example"; // Demo password

    // Template IDs for HR onboarding documents
    private static final String I9_FORM_TEMPLATE_ID = "940989288b8b4c62a950b908333b5b21efd6a174";
    private static final String NDA_TEMPLATE_ID = "a4f523d0cb234ffc99b0badc9e6f59111f76abc2";
    private static final String EMPLOYEE_CONTRACT_TEMPLATE_ID = "1a12d3e00a54457ca1bf7bde5fa37d38ede866ed";

    // Demo field names that might exist in templates
    private static final String NAME_FIELD = "Name";
    private static final String TEXT_FIELD_2 = "Text Field 2";
    private static final String TEXT_FIELD_156 = "Text Field 156";
    private static final String EMAIL_FIELD = "Email";

    // Demo URL constants
    private static final String REDIRECT_BASE_URL = "http://localhost:8080/samples/HROnboardingSystem";

    @Override
    public ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException {
        // Demo: Simple HTML page serving - use classpath resource
        try (var inputStream = getClass().getResourceAsStream("/static/samples/HROnboardingSystem/index.html")) {
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
            case "create-invite" -> createInvite(data, client);
            case "invite-status" -> getInviteStatus(data, client);
            case "download-doc-group" -> downloadDocumentGroup(data, client);
            default -> {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid action: " + action);
                errorResponse.put("available_actions", List.of("create-invite", "invite-status", "download-doc-group"));
                yield ResponseEntity.badRequest().body(errorResponse);
            }
        };
    }

    private ResponseEntity<?> createInvite(Map<String, Object> data, ApiClient client) throws SignNowApiException {
        String employeeName = (String) data.get("employee_name");
        String employeeEmail = (String) data.get("employee_email");
        String hrManagerEmail = (String) data.get("hr_manager_email");
        String employerEmail = (String) data.get("employer_email");
        @SuppressWarnings("unchecked")
        List<String> templateIds = (List<String>) data.get("template_ids");

        if (employeeName == null || employeeName.isEmpty() || 
            employeeEmail == null || employeeEmail.isEmpty() ||
            hrManagerEmail == null || hrManagerEmail.isEmpty() ||
            employerEmail == null || employerEmail.isEmpty() ||
            templateIds == null || templateIds.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "All fields are required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // Step 1: Create Document Group from templates
        Map<String, Object> dgResponse = createDocumentGroup(client, templateIds, Map.of(
            NAME_FIELD, employeeName,
            TEXT_FIELD_2, employeeName,
            TEXT_FIELD_156, employeeName,
            EMAIL_FIELD, employeeEmail
        ));
        if (!(Boolean) dgResponse.get("success")) {
            return ResponseEntity.status(500).body(dgResponse);
        }

        String documentGroupId = (String) dgResponse.get("document_group_id");

        // Step 2: Send invite to recipients
        Map<String, Object> inviteResponse = sendInvite(client, documentGroupId, employeeEmail, hrManagerEmail, employerEmail);
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
        Map<String, Object> status = getDocumentGroupSignersStatus(client, documentGroupId);
        return ResponseEntity.ok(status);
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
        DownloadDocumentGroupPostRequest downloadRequest = new DownloadDocumentGroupPostRequest(
            "merged", "no", orderColl
        ).withDocumentGroupId(documentGroupId);

        DownloadDocumentGroupPostResponse response = (DownloadDocumentGroupPostResponse) 
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

    /**
     * Creates a document group from selected templates with prefilled data.
     *
     * Steps performed:
     * 1. Clones each selected template into a signable document
     * 2. For each cloned document, fills in predefined fields with provided data
     * 3. Combines all documents into a single document group
     */
    private Map<String, Object> createDocumentGroup(ApiClient client, List<String> templateIds, Map<String, String> fields) throws SignNowApiException {
        List<CloneTemplatePostResponse> documentsFromTemplate = new LinkedList<>();
        
        // Clone templates into documents
        for (String templateId : templateIds) {
            documentsFromTemplate.add(createDocumentFromTemplate(client, templateId));
        }

        // Prefill fields in each document
        for (CloneTemplatePostResponse documentFromTemplate : documentsFromTemplate) {
            prefillFields(client, documentFromTemplate.getId(), fields);
        }

        // Create document group from documents
        DocumentGroupPostResponse documentGroupPostResponse = createDocumentGroupFromDocuments(
            client,
            documentsFromTemplate.stream().map(CloneTemplatePostResponse::getId).toList(),
            "HR Onboarding System"
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("document_group_id", documentGroupPostResponse.getId());
        return result;
    }

    /**
     * Sends invites to HR Manager, Employee and Employer for document signing.
     *
     * Steps performed:
     * 1. Retrieves document group metadata
     * 2. Creates invite actions for each document in the group
     * 3. Creates invite emails for all recipients with appropriate subjects and messages
     * 4. Creates invite steps with proper ordering
     * 5. Sends the invite request
     */
    private Map<String, Object> sendInvite(ApiClient client, String documentGroupId, String employeeEmail, String hrManagerEmail, String employerEmail) throws SignNowApiException {
        DocumentGroupGetResponse documentGroupGetResponse = getDocumentGroup(client, documentGroupId);

        // Create invite actions for each document
        List<InviteAction> inviteActions = new LinkedList<>();
        List<InviteEmail> inviteEmails = new LinkedList<>();

        // Define role mappings
        Map<String, String> roleMappings = Map.of(
            "Contract Preparer", hrManagerEmail,
            "Employee", employeeEmail,
            "Employer", employerEmail
        );

        for (var document : documentGroupGetResponse.getDocuments()) {
            List<String> documentRoles = getDocumentRoles(client, document.getId());

            for (Map.Entry<String, String> entry : roleMappings.entrySet()) {
                String roleName = entry.getKey();
                String email = entry.getValue();

                if (documentRoles.contains(roleName)) {
                    inviteActions.add(new InviteAction(
                        email,
                        roleName,
                        "sign",
                        document.getId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        REDIRECT_BASE_URL + "?page=status-page&document_group_id=" + documentGroupId,
                        null,
                        "self",
                        null
                    ));
                } else {
                    // If role is not in mappings, assign as viewer
                    inviteActions.add(new InviteAction(
                        email,
                        roleName,
                        "view",
                        document.getId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                    ));
                }
            }
        }

        // Create invite emails for all recipients
        for (Map.Entry<String, String> entry : roleMappings.entrySet()) {
            String roleName = entry.getKey();
            String email = entry.getValue();
            
            inviteEmails.add(new InviteEmail(
                email,
                "HR Onboarding Documents - Action Required",
                "Please review and sign the onboarding documents as " + roleName + ".",
                null,
                30, // expirationDays
                10 // reminderDays
            ));
        }

        InviteActionCollection inviteActionCollection = new InviteActionCollection();
        inviteActions.forEach(inviteActionCollection::add);

        InviteEmailCollection inviteEmailCollection = new InviteEmailCollection();
        inviteEmails.forEach(inviteEmailCollection::add);

        // Create invite step
        InviteStep inviteStep = new InviteStep(
            1, // order
            inviteActionCollection,
            inviteEmailCollection
        );

        InviteStepCollection inviteStepCollection = new InviteStepCollection();
        inviteStepCollection.add(inviteStep);

        // Create empty collections
        CcCollection ccCollection = new CcCollection();

        // Create and send invite request
        GroupInvitePostRequest inviteRequest = new GroupInvitePostRequest(
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

    /**
     * Retrieves the current status of document group signers.
     */
    private Map<String, Object> getDocumentGroupSignersStatus(ApiClient client, String documentGroupId) throws SignNowApiException {
        DocumentGroupGetResponse documentGroupGetResponse = getDocumentGroup(client, documentGroupId);
        String inviteId = documentGroupGetResponse.getInviteId();

        if (inviteId == null || inviteId.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "pending");
            return result;
        }

        GroupInviteGetRequest inviteStatusRequest = new GroupInviteGetRequest()
            .withDocumentGroupId(documentGroupId)
            .withInviteId(inviteId);

        GroupInviteGetResponse inviteStatusResponse = (GroupInviteGetResponse) client.send(inviteStatusRequest).getResponse();

        Map<String, Object> result = new HashMap<>();
        result.put("status", inviteStatusResponse.getInvite().getStatus());
        return result;
    }



    /**
     * Create a document from a template by template ID.
     */
    private CloneTemplatePostResponse createDocumentFromTemplate(ApiClient client, String templateId) throws SignNowApiException {
        CloneTemplatePostRequest cloneTemplate = new CloneTemplatePostRequest()
            .withTemplateId(templateId);

        return (CloneTemplatePostResponse) client.send(cloneTemplate).getResponse();
    }

    /**
     * Prefills specific fields in a given document with provided values.
     */
    private void prefillFields(ApiClient client, String documentId, Map<String, String> fieldsValue) throws SignNowApiException {
        DocumentGetResponse document = getDocument(client, documentId);

        // Extract existing field names from document
        List<String> existingFields = extractFieldNames(document);

        // Build field collection with only valid fields
        FieldCollection fieldValues = new FieldCollection();

        for (Map.Entry<String, String> entry : fieldsValue.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();

            if (existingFields.contains(fieldName) && fieldValue != null && !fieldValue.isEmpty()) {
                fieldValues.add(new Field(fieldName, fieldValue));
            }
        }

        // If there are valid fields to fill, send the request
        if (!fieldValues.isEmpty()) {
            DocumentPrefillPutRequest prefillRequest = new DocumentPrefillPutRequest(fieldValues);
            prefillRequest.withDocumentId(documentId);
            client.send(prefillRequest);
        }
    }

    /**
     * Gets available roles from a document.
     */
    private List<String> getDocumentRoles(ApiClient client, String documentId) throws SignNowApiException {
        DocumentGetResponse document = getDocument(client, documentId);
        List<String> roles = new LinkedList<>();

        for (var role : document.getRoles()) {
            String roleName = role.getName();
            if (roleName != null && !roles.contains(roleName)) {
                roles.add(roleName);
            }
        }

        return roles;
    }

    /**
     * Creates a document group from multiple documents.
     */
    private DocumentGroupPostResponse createDocumentGroupFromDocuments(ApiClient client, List<String> documentIds, String groupName) throws SignNowApiException {
        DocumentIdCollection documentIdCollection = new DocumentIdCollection();
        documentIds.forEach(documentIdCollection::add);

        DocumentGroupPostRequest documentGroupPost = new DocumentGroupPostRequest(
            documentIdCollection,
            groupName
        );

        return (DocumentGroupPostResponse) client.send(documentGroupPost).getResponse();
    }

    /**
     * Fetches metadata and details of a document group by ID.
     */
    private DocumentGroupGetResponse getDocumentGroup(ApiClient client, String documentGroupId) throws SignNowApiException {
        var request = new com.signnow.api.documentgroup.request.DocumentGroupGetRequest()
            .withDocumentGroupId(documentGroupId);
        return (DocumentGroupGetResponse) client.send(request).getResponse();
    }

    /**
     * Retrieves detailed information about a specific document by ID.
     */
    private DocumentGetResponse getDocument(ApiClient client, String documentId) throws SignNowApiException {
        DocumentGetRequest request = new DocumentGetRequest().withDocumentId(documentId);
        return (DocumentGetResponse) client.send(request).getResponse();
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
        @SuppressWarnings("unchecked")
        Map<String, Object> fieldMap = (Map<String, Object>) field;
        Object jsonAttributes = fieldMap.get("jsonAttributes");
        
        if (jsonAttributes instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = (Map<String, Object>) jsonAttributes;
            Object nameValue = attributes.get("name");
            return nameValue != null ? nameValue.toString() : null;
        }
        
        return null;
    }
} 