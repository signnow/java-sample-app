package com.signnow.samples.EmbeddedSignerWithFormInsurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentDownloadGetRequest;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentDownloadGetResponse;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.document.response.data.Role;
import com.signnow.api.document.response.data.RoleCollection;
import com.signnow.api.documentfield.request.DocumentPrefillPutRequest;
import com.signnow.api.documentfield.request.data.Field;
import com.signnow.api.documentfield.request.data.FieldCollection;
import com.signnow.api.embeddedinvite.request.DocumentInviteLinkPostRequest;
import com.signnow.api.embeddedinvite.request.DocumentInvitePostRequest;
import com.signnow.api.embeddedinvite.request.data.Invite;
import com.signnow.api.embeddedinvite.request.data.InviteCollection;
import com.signnow.api.embeddedinvite.response.DocumentInviteLinkPostResponse;
import com.signnow.api.embeddedinvite.response.DocumentInvitePostResponse;
import com.signnow.api.embeddedinvite.response.data.DataInvite;
import com.signnow.api.embeddedinvite.response.data.DataInviteCollection;
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
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {

    @Override
    public ResponseEntity<String> serveExample() throws IOException {
        String html = new String(Files.readAllBytes(Paths.get("src/main/resources/static/samples/EmbeddedSignerWithFormInsurance/templates/index.html")));
        return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(html);
    }

    @Override
    public ResponseEntity<String> handleSubmission(String formData) throws IOException, SignNowApiException {
        Map<String, String> data = new ObjectMapper().readValue(formData, Map.class);
        String action = data.get("action");

        Sdk sdk = new Sdk();
        ApiClient client = sdk.build().authenticate().getApiClient();

        if ("create-embedded-invite".equals(action)) {
            String fullName = data.get("full_name");
            String email = data.get("email");
            String link = createEmbeddedInviteAndReturnSigningLink(client, "c78e902aa6834af6ba92e8a6f92b603108e1bbbb", fullName, email);
            return ResponseEntity.ok("{" + "\"link\":\"" + link + "\"}");
        } else {
            String documentId = data.get("document_id");
            byte[] file = downloadDocument(client, documentId);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=\"result.pdf\"")
                    .body(new String(file));
        }
    }

    private byte[] downloadDocument(ApiClient client, String documentId) throws IOException, SignNowApiException {
        DocumentDownloadGetRequest request = new DocumentDownloadGetRequest().withDocumentId(documentId);
        DocumentDownloadGetResponse response = (DocumentDownloadGetResponse) client.send(request).getResponse();
        File file = response.getFile();
        byte[] content = Files.readAllBytes(file.toPath());
        file.delete();
        return content;
    }

    private String createEmbeddedInviteAndReturnSigningLink(ApiClient client, String templateId, String fullName, String email) throws SignNowApiException {
        CloneTemplatePostResponse cloneResponse = createDocumentFromTemplate(client, templateId);

        prefillFields(client, cloneResponse.getId(), fullName, email);

        String roleId = getSignerUniqueRoleId(client, cloneResponse.getId(), "Recipient 1");
        DocumentInvitePostResponse inviteResponse = createEmbeddedInviteForOneSigner(client, cloneResponse.getId(), email, roleId);
        String inviteId = findInviteId(inviteResponse, email);

        return getEmbeddedInviteLink(client, cloneResponse.getId(), inviteId);
    }

    private CloneTemplatePostResponse createDocumentFromTemplate(ApiClient client, String templateId) throws SignNowApiException {
        CloneTemplatePostRequest request = new CloneTemplatePostRequest();
        request.withTemplateId(templateId);
        return (CloneTemplatePostResponse) client.send(request).getResponse();
    }

    private void prefillFields(ApiClient client, String documentId, String fullName, String email) throws SignNowApiException {
        FieldCollection fields = new FieldCollection();
        fields.add(new Field("Name", fullName));
        fields.add(new Field("Email", email));

        DocumentPrefillPutRequest request = new DocumentPrefillPutRequest(fields);
        request.withDocumentId(documentId);
        client.send(request);
    }

    private String getSignerUniqueRoleId(ApiClient client, String documentId, String roleName) throws SignNowApiException {
        DocumentGetRequest request = new DocumentGetRequest();
        request.withDocumentId(documentId);
        DocumentGetResponse response = (DocumentGetResponse) client.send(request).getResponse();

        RoleCollection roles = response.getRoles();
        for (Role role : roles) {
            if (role.getName().equals(roleName)) {
                return role.getUniqueId();
            }
        }
        throw new IllegalArgumentException("Role not found: " + roleName);
    }

    private DocumentInvitePostResponse createEmbeddedInviteForOneSigner(ApiClient client, String documentId, String email, String roleId) throws SignNowApiException {
        InviteCollection invites = new InviteCollection();
        invites.add(new Invite(email, roleId, 1, null, null));

        DocumentInvitePostRequest request = new DocumentInvitePostRequest(invites, null);
        request.withDocumentId(documentId);
        return (DocumentInvitePostResponse) client.send(request).getResponse();
    }

    private String findInviteId(DocumentInvitePostResponse inviteResponse, String email) {
        DataInviteCollection invites = inviteResponse.getData();
        for (DataInvite invite : invites) {
            if (invite.getEmail().equalsIgnoreCase(email)) {
                return invite.getId();
            }
        }
        throw new IllegalArgumentException("Invite for email not found: " + email);
    }

    private String getEmbeddedInviteLink(ApiClient client, String documentId, String inviteId) throws SignNowApiException {
        DocumentInviteLinkPostRequest request = new DocumentInviteLinkPostRequest("none", 15);
        request.withDocumentId(documentId);
        request.withFieldInviteId(inviteId);

        DocumentInviteLinkPostResponse response = (DocumentInviteLinkPostResponse) client.send(request).getResponse();
        String redirectUrl = "http://localhost:8080/samples/EmbeddedSignerWithFormInsurance?page=download-container&document_id=" + documentId;
        return response.getData().getLink() + "&redirect_uri=" + redirectUrl;
    }
}
