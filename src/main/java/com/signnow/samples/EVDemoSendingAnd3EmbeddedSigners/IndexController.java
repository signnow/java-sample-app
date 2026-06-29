package com.signnow.samples.EVDemoSendingAnd3EmbeddedSigners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentDownloadGetRequest;
import com.signnow.api.document.response.DocumentDownloadGetResponse;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.documentfield.request.DocumentPrefillPutRequest;
import com.signnow.api.documentfield.request.data.Field;
import com.signnow.api.documentfield.request.data.FieldCollection;
import com.signnow.api.embeddedinvite.request.DocumentInviteLinkPostRequest;
import com.signnow.api.embeddedinvite.request.DocumentInvitePostRequest;
import com.signnow.api.embeddedinvite.request.data.Invite;
import com.signnow.api.embeddedinvite.request.data.InviteCollection;
import com.signnow.api.embeddedinvite.response.DocumentInviteLinkPostResponse;
import com.signnow.api.embeddedinvite.response.DocumentInvitePostResponse;
import com.signnow.api.template.request.CloneTemplatePostRequest;
import com.signnow.api.template.response.CloneTemplatePostResponse;
import com.signnow.core.ApiClient;
import com.signnow.core.exception.SignNowApiException;
import com.signnow.javasampleapp.ExampleInterface;
import com.signnow.javasampleapp.config.AppConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {

    private static final String TEMPLATE_ID = "34009a3d21b5468d86d886cd715658c453335c61";
    private static final String SAMPLE_NAME = "EVDemoSendingAnd3EmbeddedSigners";

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
            case "start-workflow":
                return startWorkflow(client, data);
            case "next-signer":
                return nextSigner(client, data);
            case "download":
                return download(client, data);
            case "invite-status":
                return inviteStatus(client, data);
            default:
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid action");
                return ResponseEntity.badRequest().body(new ObjectMapper().writeValueAsString(error));
        }
    }

    private ResponseEntity<?> startWorkflow(ApiClient client, Map<String, Object> data) throws SignNowApiException, IOException {
        String agentName   = (String) data.getOrDefault("agent_name", "");
        String agentEmail  = (String) data.getOrDefault("agent_email", "");
        String signer1Name  = (String) data.getOrDefault("signer1_name", "");
        String signer1Email = (String) data.getOrDefault("signer1_email", "");
        String signer2Name  = (String) data.getOrDefault("signer2_name", "");
        String signer2Email = (String) data.getOrDefault("signer2_email", "");

        // 1. Clone template
        CloneTemplatePostRequest cloneReq = new CloneTemplatePostRequest();
        cloneReq.withTemplateId(TEMPLATE_ID);
        CloneTemplatePostResponse cloneResp = (CloneTemplatePostResponse) client.send(cloneReq).getResponse();
        String documentId = cloneResp.getId();

        // 2. Prefill signer name fields
        FieldCollection fields = new FieldCollection();
        if (!signer1Name.isEmpty()) {
            fields.add(new Field("Signer 1 Name", signer1Name));
            fields.add(new Field("Text Field 18", signer1Name));
        }
        if (!signer2Name.isEmpty()) {
            fields.add(new Field("Signer 2 Name", signer2Name));
            fields.add(new Field("Text Field 19", signer2Name));
        }
        DocumentPrefillPutRequest prefillReq = new DocumentPrefillPutRequest(fields);
        prefillReq.withDocumentId(documentId);
        client.send(prefillReq);

        // 3. Get role IDs
        String agentRoleId   = getRoleId(client, documentId, "Contract Preparer");
        String signer1RoleId = getRoleId(client, documentId, "Recipient 1");
        String signer2RoleId = getRoleId(client, documentId, "Recipient 2");

        // 4. Create embedded invites for all three roles
        InviteCollection invites = new InviteCollection();
        invites.add(new Invite(agentEmail,   agentRoleId,   1, firstName(agentName),   lastName(agentName)));
        invites.add(new Invite(signer1Email, signer1RoleId, 2, firstName(signer1Name), lastName(signer1Name)));
        invites.add(new Invite(signer2Email, signer2RoleId, 3, firstName(signer2Name), lastName(signer2Name)));

        DocumentInvitePostRequest inviteReq = new DocumentInvitePostRequest(invites, null);
        inviteReq.withDocumentId(documentId);
        DocumentInvitePostResponse inviteResp = (DocumentInvitePostResponse) client.send(inviteReq).getResponse();

        // 5. Build roleId → inviteId map
        Map<String, String> inviteMap = new HashMap<>();
        for (var item : inviteResp.getData()) {
            inviteMap.put(item.getRoleId(), item.getId());
        }

        // 6. Get invite link for agent (first signer)
        String agentInviteId = inviteMap.get(agentRoleId);
        String redirectUrl = makeRedirectUrl(documentId, "signer1");
        String agentLink = getInviteLink(client, documentId, agentInviteId, redirectUrl);

        Map<String, Object> result = new HashMap<>();
        result.put("document_id", documentId);
        result.put("embedded_link", agentLink);
        result.put("message", "Agent embedded signing link created. Agent can now sign.");
        return ResponseEntity.ok().header("Content-Type", "application/json")
                .body(new ObjectMapper().writeValueAsString(result));
    }

    private ResponseEntity<?> nextSigner(ApiClient client, Map<String, Object> data) throws SignNowApiException, IOException {
        String documentId = (String) data.get("document_id");
        String roleName   = (String) data.get("roleName");
        String redirectKey = "Recipient 1".equals(roleName) ? "signer2" : "finish";

        String inviteId = getInviteIdForRoleName(client, documentId, roleName);
        String signingLink = getInviteLink(client, documentId, inviteId, makeRedirectUrl(documentId, redirectKey));

        Map<String, Object> result = new HashMap<>();
        result.put("embedded_link", signingLink);
        result.put("message", "Embedded link for " + roleName + " created. Ready for signing.");
        return ResponseEntity.ok().header("Content-Type", "application/json")
                .body(new ObjectMapper().writeValueAsString(result));
    }

    private ResponseEntity<?> download(ApiClient client, Map<String, Object> data) throws SignNowApiException, IOException {
        String documentId = (String) data.get("document_id");
        File file = downloadDocument(client, documentId);
        String filename = file.getName();
        byte[] content = Files.readAllBytes(file.toPath());
        file.delete();
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(content);
    }

    private ResponseEntity<?> inviteStatus(ApiClient client, Map<String, Object> data) throws SignNowApiException, IOException {
        String documentId = (String) data.get("document_id");

        DocumentGetRequest docReq = new DocumentGetRequest();
        docReq.withDocumentId(documentId);
        DocumentGetResponse docResp = (DocumentGetResponse) client.send(docReq).getResponse();

        List<Map<String, Object>> statuses = new LinkedList<>();
        for (var invite : docResp.getFieldInvites()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("email", invite.getEmail());
            entry.put("status", invite.getStatus());
            entry.put("timestamp", "");
            statuses.add(entry);
        }
        return ResponseEntity.ok().header("Content-Type", "application/json")
                .body(new ObjectMapper().writeValueAsString(statuses));
    }

    // --- helpers ---

    private String getRoleId(ApiClient client, String documentId, String roleName) throws SignNowApiException {
        DocumentGetRequest req = new DocumentGetRequest();
        req.withDocumentId(documentId);
        DocumentGetResponse resp = (DocumentGetResponse) client.send(req).getResponse();
        return resp.getRoles().stream()
                .filter(r -> roleName.equals(r.getName()))
                .findFirst()
                .map(r -> r.getUniqueId())
                .orElseThrow(() -> new IllegalArgumentException("Role '" + roleName + "' not found"));
    }

    private String getInviteLink(ApiClient client, String documentId, String inviteId, String redirectUrl)
            throws SignNowApiException, IOException {
        DocumentInviteLinkPostRequest linkReq = new DocumentInviteLinkPostRequest("none", 15);
        linkReq.withFieldInviteId(inviteId);
        linkReq.withDocumentId(documentId);
        DocumentInviteLinkPostResponse linkResp = (DocumentInviteLinkPostResponse) client.send(linkReq).getResponse();
        return linkResp.getData().getLink() + "&redirect_uri=" + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8);
    }

    private String getInviteIdForRoleName(ApiClient client, String documentId, String roleName)
            throws SignNowApiException {
        String roleId = getRoleId(client, documentId, roleName);
        DocumentGetRequest req = new DocumentGetRequest();
        req.withDocumentId(documentId);
        DocumentGetResponse resp = (DocumentGetResponse) client.send(req).getResponse();
        for (var invite : resp.getFieldInvites()) {
            if (roleId.equals(invite.getRoleId())) {
                return invite.getId();
            }
        }
        throw new IllegalArgumentException("Invite for role '" + roleName + "' not found");
    }

    private File downloadDocument(ApiClient client, String documentId) throws SignNowApiException {
        DocumentDownloadGetRequest req = new DocumentDownloadGetRequest();
        req.withDocumentId(documentId).withType("collapsed");
        DocumentDownloadGetResponse resp = (DocumentDownloadGetResponse) client.send(req).getResponse();
        return resp.getFile();
    }

    private String makeRedirectUrl(String documentId, String nextStep) {
        return AppConfig.sampleUrl(SAMPLE_NAME) + "?document_id=" + documentId + "&step=" + nextStep;
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";
        String[] parts = fullName.trim().split("\\s+");
        return parts[0];
    }

    private static String lastName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 1 ? parts[parts.length - 1] : parts[0];
    }
}
