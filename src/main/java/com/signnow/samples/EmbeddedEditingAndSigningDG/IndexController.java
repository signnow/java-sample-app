package com.signnow.samples.EmbeddedEditingAndSigningDG;

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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {
    private static final String TEMPLATE_ID = "de45a9a2a6014c2c8ac0a4d9057b17a2108e77e7";
    private static final String DOCUMENT_GROUP_TEMPLATE_ID = "0d7fb734e962418bad79d8fb80bbdaaf1f8e8cd9";

    // Role names used in the sample (should match roles set inside your template)
    private static final String ROLE_CONTRACT_PREPARER = "Contract Preparer";
    private static final String ROLE_RECIPIENT_1       = "Recipient 1";
    private static final String ROLE_RECIPIENT_2       = "Recipient 2";

    @Override
    public ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException {
        // Always return the HTML page and let the client-side JS decide which sub-page to show.
        try (var inputStream = getClass().getResourceAsStream("/static/samples/EmbeddedEditingAndSigningDG/index.html")) {
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
            case "submit-signer-info": {
                String signer1Name  = (String) data.get("signer_1_name");
                String signer1Email = (String) data.get("signer_1_email");
                String signer2Name  = (String) data.get("signer_2_name");
                String signer2Email = (String) data.get("signer_2_email");

                // 1. Create a new document group from the existing DGT.
                String documentGroupId = createDocumentGroupFromTemplate(client, DOCUMENT_GROUP_TEMPLATE_ID);

                // 2. Prefill fields across all docs in the group.
                prefillDocGroupFields(client, documentGroupId, Map.of(
                        "Signer 1 Name", signer1Name,
                        "Signer 2 Name", signer2Name
                ));

                // 3. Update recipients with actual email addresses.
                updateDocumentGroupRecipients(client, documentGroupId, Map.of(
                        ROLE_RECIPIENT_1, signer1Email,
                        ROLE_RECIPIENT_2, signer2Email
                ));

                // 4. Build an embedded edit link so the user can review docs.
                String editLink = createEmbeddedEditLink(client, documentGroupId);

                Map<String, String> resp = new HashMap<>();
                resp.put("document_group_id", documentGroupId);
                resp.put("edit_link", editLink);

                return ResponseEntity.ok(resp);
            }
            case "create-embedded-invite": {
                String documentGroupId       = (String) data.get("document_group_id");
                String contractPreparerEmail = (String) data.get("contract_preparer_email");

                String signingLink = createEmbeddedInvite(client, documentGroupId, contractPreparerEmail);

                Map<String, String> resp = new HashMap<>();
                resp.put("document_group_id", documentGroupId);
                resp.put("signing_link", signingLink);

                return ResponseEntity.ok(resp);
            }
            case "invite-status": {
                String documentGroupId = (String) data.get("document_group_id");
                List<Map<String, Object>> statuses = getDocumentGroupSignersStatus(client, documentGroupId);
                return ResponseEntity.ok(statuses);
            }
            case "download-doc-group":
            default: {
                String documentGroupId = (String) data.get("document_group_id");
                
                var orderColl = new com.signnow.api.documentgroup.request.data.DocumentOrderCollection();
                var downloadReq = new com.signnow.api.documentgroup.request.DownloadDocumentGroupPostRequest("merged", "no", orderColl)
                        .withDocumentGroupId(documentGroupId);

                var downloadResp = (com.signnow.api.documentgroup.response.DownloadDocumentGroupPostResponse) client.send(downloadReq).getResponse();

                // Get the actual filename from the downloaded file
                String filename = downloadResp.getFile().getName();
                byte[] bytes = Files.readAllBytes(downloadResp.getFile().toPath());
                downloadResp.getFile().delete();
                
                return ResponseEntity.ok()
                        .header("Content-Type", "application/pdf")
                        .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                        .body(bytes);
            }
        }
    }

    /*
     * === Helper methods below ===
     * The method bodies mirror the PHP sample logic one-to-one, but use SignNow Java SDK classes.
     */

    private String createDocumentGroupFromTemplate(ApiClient client, String templateId) throws SignNowApiException {
        // Creates a Document Group from an existing DGT in a single call.
        var request = new com.signnow.api.documentgrouptemplate.request.DocumentGroupTemplatePostRequest("Embedded Editing & Signing Group", null, null)
                .withTemplateGroupId(templateId);

        var response = (com.signnow.api.documentgrouptemplate.response.DocumentGroupTemplatePostResponse) client.send(request).getResponse();
        return response.getData().getUniqueId();
    }

    private void prefillDocGroupFields(ApiClient client, String documentGroupId, Map<String, String> fieldsToFill) throws SignNowApiException {
        var docGroup = getDocumentGroup(client, documentGroupId);

        for (var docItem : docGroup.getDocuments()) {
            String docId = docItem.getId();
            var documentData = getDocument(client, docId);

            FieldCollection collection = new FieldCollection();
            documentData.getFields().forEach(fieldObj -> {
                com.signnow.api.document.response.data.Field field = (com.signnow.api.document.response.data.Field) fieldObj;
                String fieldName = field.getJsonAttributes().getName();
                if (fieldsToFill.containsKey(fieldName)) {
                    collection.add(new Field(fieldName, fieldsToFill.get(fieldName)));
                }
            });

            if (!collection.isEmpty()) {
                var prefillRequest = new DocumentPrefillPutRequest(collection).withDocumentId(docId);
                client.send(prefillRequest);
            }
        }
    }

    private void updateDocumentGroupRecipients(ApiClient client, String documentGroupId, Map<String, String> recipientEmails) throws SignNowApiException {
        var recipientsGet = new com.signnow.api.documentgroup.request.DocumentGroupRecipientsGetRequest()
                .withDocumentGroupId(documentGroupId);

        var recipientsResponse = (com.signnow.api.documentgroup.response.DocumentGroupRecipientsGetResponse) client.send(recipientsGet).getResponse();

        java.util.List<com.signnow.api.documentgroup.request.data.recipient.Recipient> updatedRecipients = new LinkedList<>();

        for (var recipient : recipientsResponse.getData().getRecipients()) {
            String email = recipientEmails.getOrDefault(recipient.getName(), null);
            java.util.List<com.signnow.api.documentgroup.request.data.recipient.Document> docs = new LinkedList<>();
            recipient.getDocuments().forEach(doc -> docs.add(new com.signnow.api.documentgroup.request.data.recipient.Document(doc.getId(), doc.getRole(), doc.getAction())));

            updatedRecipients.add(new com.signnow.api.documentgroup.request.data.recipient.Recipient(
                    recipient.getName(),
                    email,
                    recipient.getOrder(),
                    buildRecipientDocumentCollection(docs),
                    null,
                    null
            ));
        }

        com.signnow.api.documentgroup.request.data.recipient.RecipientCollection recColl = new com.signnow.api.documentgroup.request.data.recipient.RecipientCollection();
        updatedRecipients.forEach(recColl::add);
        com.signnow.api.documentgroup.request.data.CcCollection ccColl = new com.signnow.api.documentgroup.request.data.CcCollection();
        var updateReq = new com.signnow.api.documentgroup.request.DocumentGroupRecipientsPutRequest(
                recColl,
                ccColl
        ).withDocumentGroupId(documentGroupId);

        client.send(updateReq);
    }

    private String createEmbeddedEditLink(ApiClient client, String documentGroupId) throws SignNowApiException {
        String redirectUrl = "http://localhost:8080/samples/EmbeddedEditingAndSigningDG?page=page2-embedded-sending&document_group_id=" + documentGroupId;

        var request = new com.signnow.api.embeddededitor.request.DocumentGroupEmbeddedEditorLinkPostRequest(
                redirectUrl, "self", 15
        ).withDocumentGroupId(documentGroupId);

        var response = (com.signnow.api.embeddededitor.response.DocumentGroupEmbeddedEditorLinkPostResponse) client.send(request).getResponse();
        return response.getData().getUrl();
    }

    private String createEmbeddedInvite(ApiClient client, String documentGroupId, String contractPreparerEmail) throws SignNowApiException {
        // 1. Build the invite structure (roles & docs) similar to PHP implementation.
        // For brevity, we rely on a helper that constructs Signer/Invite collections.
        // -- Retrieve recipients to pick existing emails for Recipient 1 & 2
        var recipientsRespReq = new com.signnow.api.documentgroup.request.DocumentGroupRecipientsGetRequest().withDocumentGroupId(documentGroupId);
        var recipientsResp = (com.signnow.api.documentgroup.response.DocumentGroupRecipientsGetResponse) client.send(recipientsRespReq).getResponse();

        String recipient1Email = findEmailByRoleName(recipientsResp, ROLE_RECIPIENT_1);
        String recipient2Email = findEmailByRoleName(recipientsResp, ROLE_RECIPIENT_2);

        String redirectUrl = "http://localhost:8080/samples/EmbeddedEditingAndSigningDG?page=page4-status-download&document_group_id=" + documentGroupId;

        // -- Build invite list
        java.util.List<com.signnow.api.embeddedgroupinvite.request.data.invite.Invite> inviteSteps = new LinkedList<>();
        int order = 1;

        {
            var docColl = new com.signnow.api.embeddedgroupinvite.request.data.invite.DocumentCollection();
            buildDocumentsForRole(client, documentGroupId, ROLE_CONTRACT_PREPARER).forEach(docColl::add);
            var signerColl = new com.signnow.api.embeddedgroupinvite.request.data.invite.SignerCollection();
            signerColl.add(new com.signnow.api.embeddedgroupinvite.request.data.invite.Signer(
                    contractPreparerEmail, "none", docColl, null, null, null, null, redirectUrl, null, "self", null));
            inviteSteps.add(new com.signnow.api.embeddedgroupinvite.request.data.invite.Invite(order++, signerColl));
        }
        {
            var docColl = new com.signnow.api.embeddedgroupinvite.request.data.invite.DocumentCollection();
            buildDocumentsForRole(client, documentGroupId, ROLE_RECIPIENT_1).forEach(docColl::add);
            var signerColl = new com.signnow.api.embeddedgroupinvite.request.data.invite.SignerCollection();
            signerColl.add(new com.signnow.api.embeddedgroupinvite.request.data.invite.Signer(recipient1Email, "none", docColl, null, null, null, null, redirectUrl, null, "self", "email"));
            inviteSteps.add(new com.signnow.api.embeddedgroupinvite.request.data.invite.Invite(order++, signerColl));
        }
        {
            var docColl = new com.signnow.api.embeddedgroupinvite.request.data.invite.DocumentCollection();
            buildDocumentsForRole(client, documentGroupId, ROLE_RECIPIENT_2).forEach(docColl::add);
            var signerColl = new com.signnow.api.embeddedgroupinvite.request.data.invite.SignerCollection();
            signerColl.add(new com.signnow.api.embeddedgroupinvite.request.data.invite.Signer(recipient2Email, "none", docColl, null, null, null, null, redirectUrl, null, "self", "email"));
            inviteSteps.add(new com.signnow.api.embeddedgroupinvite.request.data.invite.Invite(order, signerColl));
        }

        // Send invite creation request
        com.signnow.api.embeddedgroupinvite.request.data.invite.InviteCollection inviteColl = new com.signnow.api.embeddedgroupinvite.request.data.invite.InviteCollection();
        inviteSteps.forEach(inviteColl::add);
        var inviteReq = new com.signnow.api.embeddedgroupinvite.request.GroupInvitePostRequest(
                inviteColl, true
        ).withDocumentGroupId(documentGroupId);

        var inviteResp = (com.signnow.api.embeddedgroupinvite.response.GroupInvitePostResponse) client.send(inviteReq).getResponse();

        // Create the embedded signing link for Contract Preparer
        var linkReq = new com.signnow.api.embeddedgroupinvite.request.GroupInviteLinkPostRequest(
                contractPreparerEmail, "none", 30
        ).withDocumentGroupId(documentGroupId)
         .withEmbeddedInviteId(inviteResp.getData().getId());

        var linkResp = (com.signnow.api.embeddedgroupinvite.response.GroupInviteLinkPostResponse) client.send(linkReq).getResponse();

        return linkResp.getData().getLink();
    }

    private java.util.List<com.signnow.api.embeddedgroupinvite.request.data.invite.Document> buildDocumentsForRole(ApiClient client, String documentGroupId, String roleName) throws SignNowApiException {
        var docGroup = getDocumentGroup(client, documentGroupId);
        java.util.List<com.signnow.api.embeddedgroupinvite.request.data.invite.Document> docs = new LinkedList<>();

        docGroup.getDocuments().forEach(doc -> {
            boolean rolePresent = doc.getRoles().contains(roleName);
            docs.add(new com.signnow.api.embeddedgroupinvite.request.data.invite.Document(
                    doc.getId(), rolePresent ? "sign" : "view", roleName
            ));
        });

        return docs;
    }

    private List<Map<String, Object>> getDocumentGroupSignersStatus(ApiClient client, String documentGroupId) throws SignNowApiException {
        var docGroup = getDocumentGroup(client, documentGroupId);
        String inviteId = docGroup.getInviteId();

        var inviteStatusReq = new com.signnow.api.documentgroupinvite.request.GroupInviteGetRequest()
                .withDocumentGroupId(documentGroupId)
                .withInviteId(inviteId);

        var inviteStatusResp = (com.signnow.api.documentgroupinvite.response.GroupInviteGetResponse) client.send(inviteStatusReq).getResponse();

        Map<Integer, String> stepStatuses = new HashMap<>();
        inviteStatusResp.getInvite().getSteps().forEach(step -> stepStatuses.put(step.getOrder(), step.getStatus()));

        var recipientsReq = new com.signnow.api.documentgroup.request.DocumentGroupRecipientsGetRequest().withDocumentGroupId(documentGroupId);
        var recipientsResp = (com.signnow.api.documentgroup.response.DocumentGroupRecipientsGetResponse) client.send(recipientsReq).getResponse();

        List<Map<String, Object>> result = new LinkedList<>();
        recipientsResp.getData().getRecipients().forEach(recipient -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", recipient.getName());
            map.put("email", recipient.getEmail());
            map.put("order", recipient.getOrder());
            map.put("status", stepStatuses.getOrDefault(recipient.getOrder(), "unknown"));
            map.put("timestamp", null);
            result.add(map);
        });

        return result;
    }



    private com.signnow.api.documentgroup.response.DocumentGroupGetResponse getDocumentGroup(ApiClient client, String documentGroupId) throws SignNowApiException {
        var req = new com.signnow.api.documentgroup.request.DocumentGroupGetRequest().withDocumentGroupId(documentGroupId);
        return (com.signnow.api.documentgroup.response.DocumentGroupGetResponse) client.send(req).getResponse();
    }

    private DocumentGetResponse getDocument(ApiClient client, String documentId) throws SignNowApiException {
        var req = new DocumentGetRequest().withDocumentId(documentId);
        return (DocumentGetResponse) client.send(req).getResponse();
    }

    private String findEmailByRoleName(com.signnow.api.documentgroup.response.DocumentGroupRecipientsGetResponse recipientsResponse, String roleName) {
        return recipientsResponse.getData().getRecipients().stream()
                .filter(r -> roleName.equals(r.getName()))
                .findFirst()
                .map(com.signnow.api.documentgroup.response.data.data.Recipient::getEmail)
                .orElse(null);
    }

    private com.signnow.api.documentgroup.request.data.recipient.DocumentCollection buildRecipientDocumentCollection(java.util.List<com.signnow.api.documentgroup.request.data.recipient.Document> docs) {
        var collection = new com.signnow.api.documentgroup.request.data.recipient.DocumentCollection();
        docs.forEach(collection::add);
        return collection;
    }
}
