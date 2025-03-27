package com.signnow.samples.HROnboardingSystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentDownloadGetResponse;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.documentgroup.request.DocumentGroupGetRequest;
import com.signnow.api.documentgroup.request.DocumentGroupPostRequest;
import com.signnow.api.documentgroup.request.DownloadDocumentGroupPostRequest;
import com.signnow.api.documentgroup.request.data.DocumentIdCollection;
import com.signnow.api.documentgroup.request.data.DocumentOrderCollection;
import com.signnow.api.documentgroup.response.DocumentGroupGetResponse;
import com.signnow.api.documentgroup.response.DocumentGroupPostResponse;
import com.signnow.api.documentgroup.response.DownloadDocumentGroupPostResponse;
import com.signnow.api.embeddededitor.request.DocumentGroupEmbeddedEditorLinkPostRequest;
import com.signnow.api.embeddededitor.response.DocumentGroupEmbeddedEditorLinkPostResponse;
import com.signnow.api.embeddedgroupinvite.request.GroupInviteLinkPostRequest;
import com.signnow.api.embeddedgroupinvite.request.GroupInvitePostRequest;
import com.signnow.api.embeddedgroupinvite.request.data.invite.Document;
import com.signnow.api.embeddedgroupinvite.request.data.invite.DocumentCollection;
import com.signnow.api.embeddedgroupinvite.request.data.invite.Invite;
import com.signnow.api.embeddedgroupinvite.request.data.invite.InviteCollection;
import com.signnow.api.embeddedgroupinvite.request.data.invite.Signer;
import com.signnow.api.embeddedgroupinvite.request.data.invite.SignerCollection;
import com.signnow.api.embeddedgroupinvite.response.GroupInviteLinkPostResponse;
import com.signnow.api.embeddedgroupinvite.response.GroupInvitePostResponse;
import com.signnow.api.documentgroupinvite.request.GroupInviteGetRequest;
import com.signnow.api.documentgroupinvite.response.GroupInviteGetResponse;
import com.signnow.api.template.request.CloneTemplatePostRequest;
import com.signnow.api.template.response.CloneTemplatePostResponse;
import com.signnow.api.documentfield.request.DocumentPrefillPutRequest;
import com.signnow.api.documentfield.request.data.Field;
import com.signnow.api.documentfield.request.data.FieldCollection;
import com.signnow.core.ApiClient;
import com.signnow.core.exception.SignNowApiException;
import com.signnow.javasampleapp.ExampleInterface;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@Controller
public class IndexController implements ExampleInterface {

    @Override
    public ResponseEntity<String> serveExample() throws IOException {
        String html = new String(Files.readAllBytes(
                new File("src/main/resources/static/samples/HROnboardingSystem/templates/index.html").toPath()));
        return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
    }

    @Override
    public ResponseEntity<String> handleSubmission(String formData) throws IOException, SignNowApiException {
        Map<String, Object> data = new ObjectMapper().readValue(formData, Map.class);
        String action = (String) data.get("action");

        Sdk sdk = new Sdk();
        ApiClient apiClient = sdk.build().authenticate().getApiClient();

        switch (action) {
            case "create-embedded-editor":
                String employeeName = (String) data.get("employee_name");
                String employeeEmail = (String) data.get("employee_email");
                String hrManagerEmail = (String) data.get("hr_manager_email");
                List<String> templateIds = (List<String>) data.get("template_ids");

                String editorLink = createEmbeddedEditorLink(apiClient, templateIds, Map.of(
                        "Name", employeeName,
                        "Text Field 2", employeeName,
                        "Text Field 156", employeeName,
                        "Email", employeeEmail
                ), employeeEmail, hrManagerEmail);

                return ResponseEntity.ok(new ObjectMapper().writeValueAsString(Map.of("link", editorLink)));

            case "create-embedded-invite":
                String groupId = (String) data.get("document_group_id");
                employeeEmail = (String) data.get("employee_email");
                hrManagerEmail = (String) data.get("hr_manager_email");

                GroupInvitePostResponse inviteResponse = createEmbeddedInvite(apiClient, groupId, employeeEmail, hrManagerEmail);
                String inviteLink = getEmbeddedInviteLink(apiClient, groupId, inviteResponse.getData().getId(), employeeEmail);

                return ResponseEntity.ok(new ObjectMapper().writeValueAsString(Map.of("link", inviteLink)));

            case "invite-status":
                groupId = (String) data.get("document_group_id");
                String status = getDocumentGroupInviteStatus(apiClient, groupId);
                return ResponseEntity.ok(new ObjectMapper().writeValueAsString(Map.of("status", status)));

            default:
                groupId = (String) data.get("document_group_id");
                byte[] pdf = downloadDocumentGroup(apiClient, groupId);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=result.pdf")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(Base64.getEncoder().encodeToString(pdf));
        }
    }

    private String createEmbeddedEditorLink(ApiClient apiClient, List<String> templateIds, Map<String, String> fields, String employeeEmail, String hrManagerEmail) throws SignNowApiException {
        List<CloneTemplatePostResponse> docs = new ArrayList<>();
        for (String templateId : templateIds) {
            CloneTemplatePostRequest cloneRequest = new CloneTemplatePostRequest().withTemplateId(templateId);
            docs.add((CloneTemplatePostResponse) apiClient.send(cloneRequest).getResponse());
        }

        for (CloneTemplatePostResponse doc : docs) {
            prefillFields(apiClient, doc.getId(), fields);
        }

        DocumentIdCollection docIds = new DocumentIdCollection();
        for (CloneTemplatePostResponse doc : docs) docIds.add(doc.getId());

        DocumentGroupPostRequest groupPost = new DocumentGroupPostRequest(docIds, "HR Onboarding System");
        DocumentGroupPostResponse groupResp = (DocumentGroupPostResponse) apiClient.send(groupPost).getResponse();

        String redirectUrl = "http://localhost:8080/samples/HROnboardingSystem?page=embedded-editing-callback&document_group_id=" + groupResp.getId() + "&employee_email=" + employeeEmail + "&hr_manager_email=" + hrManagerEmail;

        DocumentGroupEmbeddedEditorLinkPostRequest editorLinkReq = new DocumentGroupEmbeddedEditorLinkPostRequest(redirectUrl, "self", 15)
                .withDocumentGroupId(groupResp.getId());
        DocumentGroupEmbeddedEditorLinkPostResponse linkResp = (DocumentGroupEmbeddedEditorLinkPostResponse) apiClient.send(editorLinkReq).getResponse();

        return linkResp.getData().getUrl();
    }

    private void prefillFields(ApiClient client, String docId, Map<String, String> values) throws SignNowApiException {
        DocumentGetRequest getRequest = new DocumentGetRequest().withDocumentId(docId);
        DocumentGetResponse doc = (DocumentGetResponse) client.send(getRequest).getResponse();

        Set<String> fieldNames = new HashSet<>();
        doc.getFields().forEach(f -> fieldNames.add(((Field)f).getFieldName()));

        FieldCollection fields = new FieldCollection();
        values.forEach((name, value) -> {
            if (value != null && fieldNames.contains(name)) {
                fields.add(new Field(name, value));
            }
        });

        if (!fields.isEmpty()) {
            DocumentPrefillPutRequest prefill = new DocumentPrefillPutRequest(fields).withDocumentId(docId);
            client.send(prefill);
        }
    }

    private GroupInvitePostResponse createEmbeddedInvite(ApiClient client, String groupId, String employeeEmail, String hrManagerEmail) throws SignNowApiException {
        DocumentGroupGetRequest groupGet = new DocumentGroupGetRequest().withDocumentGroupId(groupId);
        DocumentGroupGetResponse group = (DocumentGroupGetResponse) client.send(groupGet).getResponse();

        Map<String, String> roleEmailMap = new LinkedHashMap<>();
        List<String> emails = List.of(employeeEmail, hrManagerEmail);

        Map<String, List<Document>> signerDocs = new LinkedHashMap<>();
        int emailIdx = 0;

        for (var doc : group.getDocuments()) {
            for (String role : doc.getRoles()) {
                roleEmailMap.putIfAbsent(role, emails.get(Math.min(emailIdx++, emails.size() - 1)));
                String email = roleEmailMap.get(role);
                signerDocs.computeIfAbsent(email, k -> new ArrayList<>()).add(new Document(doc.getId(), "sign", role));
            }
        }

        String redirectUrl = "http://localhost:8080/samples/HROnboardingSystem?page=download-with-status&document_group_id=" + groupId;
        InviteCollection invites = new InviteCollection();

        int order = 1;
        for (String email : emails) {
            if (!signerDocs.containsKey(email)) continue;
            DocumentCollection docCollection = new DocumentCollection();
            docCollection.addAll(signerDocs.get(email));
            Signer signer = new Signer(email, "none", docCollection, redirectUrl, "self");

            SignerCollection signCollection = new SignerCollection();
            signCollection.add(signer);
            invites.add(new Invite(order++, signCollection));
        }

        GroupInvitePostRequest request = new GroupInvitePostRequest(invites, false).withDocumentGroupId(groupId);
        return (GroupInvitePostResponse) client.send(request).getResponse();
    }

    private String getEmbeddedInviteLink(ApiClient client, String groupId, String inviteId, String email) throws SignNowApiException {
        GroupInviteLinkPostRequest linkReq = new GroupInviteLinkPostRequest(email, "none", 15)
                .withDocumentGroupId(groupId).withEmbeddedInviteId(inviteId);
        GroupInviteLinkPostResponse resp = (GroupInviteLinkPostResponse) client.send(linkReq).getResponse();
        return resp.getData().getLink();
    }

    private String getDocumentGroupInviteStatus(ApiClient client, String groupId) throws SignNowApiException {
        DocumentGroupGetRequest groupGet = new DocumentGroupGetRequest().withDocumentGroupId(groupId);
        DocumentGroupGetResponse group = (DocumentGroupGetResponse) client.send(groupGet).getResponse();
        GroupInviteGetRequest inviteGet = new GroupInviteGetRequest().withDocumentGroupId(groupId).withInviteId(group.getInviteId());
        GroupInviteGetResponse resp = (GroupInviteGetResponse) client.send(inviteGet).getResponse();
        return resp.getInvite().getStatus();
    }

    private byte[] downloadDocumentGroup(ApiClient client, String groupId) throws SignNowApiException, IOException {
        DocumentOrderCollection orders = new DocumentOrderCollection();
        orders.add("1");
        DownloadDocumentGroupPostRequest request = new DownloadDocumentGroupPostRequest("merged", "no", orders)
                .withDocumentGroupId(groupId);
        DocumentDownloadGetResponse response = (DocumentDownloadGetResponse) client.send(request).getResponse();
        //File file = (File) response.getFile();
        byte[] content = null;//Files.readAllBytes(file.toPath());
        //file.delete();
        return content;
    }

}
