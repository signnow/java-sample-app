package com.signnow.samples.HROnboardingSystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentDownloadGetResponse;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.document.response.data.Field;
import com.signnow.api.documentfield.request.DocumentPrefillPutRequest;
import com.signnow.api.documentfield.request.data.FieldCollection;
import com.signnow.api.documentgroup.request.DocumentGroupGetRequest;
import com.signnow.api.documentgroup.request.DocumentGroupPostRequest;
import com.signnow.api.documentgroup.request.DownloadDocumentGroupPostRequest;
import com.signnow.api.documentgroup.request.data.DocumentIdCollection;
import com.signnow.api.documentgroup.request.data.DocumentOrderCollection;
import com.signnow.api.documentgroup.response.DocumentGroupGetResponse;
import com.signnow.api.documentgroup.response.DocumentGroupPostResponse;
import com.signnow.api.documentgroupinvite.request.GroupInviteGetRequest;
import com.signnow.api.documentgroupinvite.response.GroupInviteGetResponse;
import com.signnow.api.documentgroupinvite.response.GroupInvitePostResponse;
import com.signnow.api.embeddedgroupinvite.request.GroupInviteLinkPostRequest;
import com.signnow.api.embeddedgroupinvite.request.GroupInvitePostRequest;
import com.signnow.api.embeddedgroupinvite.request.data.invite.Invite;
import com.signnow.api.embeddedgroupinvite.request.data.invite.InviteCollection;
import com.signnow.api.embeddedgroupinvite.request.data.invite.Signer;
import com.signnow.api.embeddedgroupinvite.request.data.invite.SignerCollection;
import com.signnow.api.embeddedgroupinvite.request.data.invite.Document;
import com.signnow.api.embeddedgroupinvite.request.data.invite.DocumentCollection;
import com.signnow.api.embeddedgroupinvite.response.GroupInviteLinkPostResponse;
import com.signnow.api.template.request.CloneTemplatePostRequest;
import com.signnow.api.template.response.CloneTemplatePostResponse;
import com.signnow.core.ApiClient;
import com.signnow.core.exception.SignNowApiException;
import com.signnow.javasampleapp.ExampleInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Controller
public class IndexController implements ExampleInterface {

    @Override
    public ResponseEntity<String> serveExample() throws IOException {
        String html = new String(Files.readAllBytes(Paths.get("src/main/resources/static/samples/HROnboardingSystem/templates/index.html")));
        return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(html);
    }

    @Override
    public ResponseEntity<String> handleSubmission(String formData) throws IOException, SignNowApiException {
        Map<String, Object> data = new ObjectMapper().readValue(formData, Map.class);
        String action = (String) data.get("action");

        Sdk sdk = new Sdk();
        ApiClient client = sdk.build().authenticate().getApiClient();

        if ("create-embedded-invite".equals(action)) {
            String employeeName = (String) data.get("employee_name");
            String employeeEmail = (String) data.get("employee_email");
            String hrManagerEmail = (String) data.get("hr_manager_email");
            List<String> templateIds = (List<String>) data.get("template_ids");

            String documentGroupId = createDocumentGroup(client, templateIds, Map.of(
                    "Name", employeeName,
                    "Text Field 2", employeeName,
                    "Text Field 156", employeeName,
                    "Email", employeeEmail
            ));

            String contractPreparerEmail = "contract_preparer@example.com";
            GroupInvitePostResponse embeddedInviteResponse = createEmbeddedInvite(client, documentGroupId, employeeEmail, hrManagerEmail, contractPreparerEmail);
            String inviteLink = getEmbeddedInviteLink(client, documentGroupId, embeddedInviteResponse.getId(), contractPreparerEmail);

            return ResponseEntity.ok("{\"link\":\"" + inviteLink + "\"}");
        } else if ("invite-status".equals(action)) {
            String documentGroupId = (String) data.get("document_group_id");
            String status = getDocumentGroupInviteStatus(client, documentGroupId);
            return ResponseEntity.ok("{\"status\":\"" + status + "\"}");
        } else {
            String documentGroupId = (String) data.get("document_group_id");
            byte[] file = downloadDocumentGroup(client, documentGroupId);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=\"result.pdf\"")
                    .body(new String(file));
        }
    }

    private String createDocumentGroup(ApiClient client, List<String> templateIds, Map<String, String> fields) throws SignNowApiException {
        var clonedIds = new DocumentIdCollection();
        for (String templateId : templateIds) {
            CloneTemplatePostResponse doc = cloneTemplate(client, templateId);
            clonedIds.add(doc.getId());
            prefillFields(client, doc.getId(), fields);
        }

        DocumentGroupPostRequest request = new DocumentGroupPostRequest(clonedIds, "HR Onboarding System");
        DocumentGroupPostResponse response = (DocumentGroupPostResponse) client.send(request).getResponse();
        return response.getId();
    }

    private CloneTemplatePostResponse cloneTemplate(ApiClient client, String templateId) throws SignNowApiException {
        CloneTemplatePostRequest request = new CloneTemplatePostRequest();
        request.withTemplateId(templateId);
        return (CloneTemplatePostResponse) client.send(request).getResponse();
    }

    private void prefillFields(ApiClient client, String documentId, Map<String, String> fieldsValues) throws SignNowApiException {
        DocumentGetRequest request = new DocumentGetRequest().withDocumentId(documentId);
        DocumentGetResponse response = (DocumentGetResponse) client.send(request).getResponse();

        Set<String> fieldNames = new HashSet<>();
        for (Object f : response.getFields()) {
            Object jsonAttributes = ((Map<?, ?>) f).get("json_attributes");
            Object name = ((Map<?, ?>) jsonAttributes).get("name");
            fieldNames.add((String) name);
        }

        FieldCollection prefilledFields = new FieldCollection();
        for (var entry : fieldsValues.entrySet()) {
            if (fieldNames.contains(entry.getKey())) {
                prefilledFields.add(new com.signnow.api.documentfield.request.data.Field(entry.getKey(), entry.getValue()));
            }
        }

        if (!prefilledFields.isEmpty()) {
            DocumentPrefillPutRequest prefillRequest = new DocumentPrefillPutRequest(prefilledFields).withDocumentId(documentId);
            client.send(prefillRequest);
        }
    }

    private GroupInvitePostResponse createEmbeddedInvite(ApiClient client, String documentGroupId, String employeeEmail, String hrManagerEmail, String contractPreparerEmail) throws SignNowApiException {
        var invites = new InviteCollection();

        var signers_contractPreparer = new SignerCollection();
        signers_contractPreparer.add(new Signer(contractPreparerEmail, "none", new DocumentCollection(), "http://localhost","self"));
        invites.add(new Invite(1, signers_contractPreparer));

        var signers_hrManager = new SignerCollection();
        signers_hrManager.add(new Signer(hrManagerEmail, "none", new DocumentCollection(), "http://localhost","self"));
        invites.add(new Invite(2, signers_hrManager));

        var signers_employee = new SignerCollection();
        signers_employee.add(new Signer(employeeEmail, "none", new DocumentCollection(), "http://localhost","self"));
        invites.add(new Invite(3, signers_employee));

        GroupInvitePostRequest request = new GroupInvitePostRequest(invites, true).withDocumentGroupId(documentGroupId);

        return (GroupInvitePostResponse) client.send(request).getResponse();
    }

    private String getEmbeddedInviteLink(ApiClient client, String documentGroupId, String inviteId, String email) throws SignNowApiException {
        GroupInviteLinkPostRequest linkReq = new GroupInviteLinkPostRequest(email, "none", 15)
                .withDocumentGroupId(documentGroupId).withEmbeddedInviteId(inviteId);
        GroupInviteLinkPostResponse resp = (GroupInviteLinkPostResponse) client.send(linkReq).getResponse();
        return resp.getData().getLink();
    }

    private String getDocumentGroupInviteStatus(ApiClient client, String documentGroupId) throws SignNowApiException {
        DocumentGroupGetRequest groupGet = new DocumentGroupGetRequest().withDocumentGroupId(documentGroupId);
        DocumentGroupGetResponse group = (DocumentGroupGetResponse) client.send(groupGet).getResponse();
        GroupInviteGetRequest inviteGet = new GroupInviteGetRequest().withDocumentGroupId(documentGroupId).withInviteId(group.getInviteId());
        GroupInviteGetResponse resp = (GroupInviteGetResponse) client.send(inviteGet).getResponse();
        return resp.getInvite().getStatus();
    }

    private byte[] downloadDocumentGroup(ApiClient client, String documentGroupId) throws IOException, SignNowApiException {
        DocumentOrderCollection orders = new DocumentOrderCollection();
        orders.add("1");
        DownloadDocumentGroupPostRequest request = new DownloadDocumentGroupPostRequest("merged", "no", orders)
                .withDocumentGroupId(documentGroupId);
        DocumentDownloadGetResponse response = (DocumentDownloadGetResponse) client.send(request).getResponse();
        File file = response.getFile();
        byte[] content = Files.readAllBytes(file.toPath());
        file.delete();
        return content;
    }
}
