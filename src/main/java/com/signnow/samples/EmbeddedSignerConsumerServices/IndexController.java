package com.signnow.samples.EmbeddedSignerConsumerServices;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentDownloadGetRequest;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentDownloadGetResponse;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.document.response.data.Role;
import com.signnow.api.document.response.data.RoleCollection;
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
        String html = new String(Files.readAllBytes(Paths.get("src/main/resources/static/samples/EmbeddedSignerConsumerServices/templates/index.html")));
        return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(html);
    }

    @Override
    public ResponseEntity<String> handleSubmission(String formData) throws IOException, SignNowApiException {
        Map<String, String> data = new ObjectMapper().readValue(formData, Map.class);
        String documentId = data.get("document_id");

        Sdk sdk = new Sdk();
        ApiClient client = sdk.build().authenticate().getApiClient();

        byte[] file = this.downloadDocument(client, documentId);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"completed_document.pdf\"")
                .body(new String(file));
    }

    public String initiateEmbeddedFlow() throws IOException, SignNowApiException {
        Sdk sdk = new Sdk();
        ApiClient client = sdk.build().authenticate().getApiClient();

        String templateId = "da62bd76f1864e1fadff6251eca8152977ee3486";
        String signerEmail = "signer_email@example.com";
        String redirectUrl = "http://localhost:8080/samples/EmbeddedSignerConsumerServices?page=finish";

        CloneTemplatePostResponse clonedDoc = this.createDocumentFromTemplate(client, templateId);
        String documentId = clonedDoc.getId();

        String roleId = this.getSignerUniqueRoleId(client, documentId, "Recipient 1");

        DocumentInvitePostResponse inviteResponse = this.createEmbeddedInviteForOneSigner(client, documentId, signerEmail, roleId);
        String inviteId = this.findInviteId(inviteResponse, signerEmail);

        return this.getEmbeddedInviteLink(client, documentId, inviteId, redirectUrl);
    }

    private CloneTemplatePostResponse createDocumentFromTemplate(ApiClient client, String templateId) throws SignNowApiException {
        CloneTemplatePostRequest request = new CloneTemplatePostRequest();
        request.withTemplateId(templateId);
        return (CloneTemplatePostResponse) client.send(request).getResponse();
    }

    private String getSignerUniqueRoleId(ApiClient client, String documentId, String signerRole) throws SignNowApiException {
        DocumentGetRequest request = new DocumentGetRequest();
        request.withDocumentId(documentId);
        DocumentGetResponse response = (DocumentGetResponse) client.send(request).getResponse();

        RoleCollection roles = response.getRoles();
        for (Role role : roles) {
            if (role.getName().equals(signerRole)) {
                return role.getUniqueId();
            }
        }
        throw new IllegalArgumentException("Role not found: " + signerRole);
    }

    private DocumentInvitePostResponse createEmbeddedInviteForOneSigner(ApiClient client, String documentId, String signerEmail, String roleId) throws SignNowApiException {
        InviteCollection invites = new InviteCollection();
        invites.add(new Invite(signerEmail, roleId, 1, null, null));
        DocumentInvitePostRequest request = new DocumentInvitePostRequest(invites, null);
        request.withDocumentId(documentId);
        return (DocumentInvitePostResponse) client.send(request).getResponse();
    }

    private String findInviteId(DocumentInvitePostResponse inviteResponse, String signerEmail) {
        DataInviteCollection dataInvites = inviteResponse.getData();
        for (DataInvite invite : dataInvites) {
            if (invite.getEmail().equalsIgnoreCase(signerEmail)) {
                return invite.getId();
            }
        }
        throw new IllegalArgumentException("Invite for email not found: " + signerEmail);
    }

    private String getEmbeddedInviteLink(ApiClient client, String documentId, String inviteId, String redirectUrl) throws SignNowApiException {
        DocumentInviteLinkPostRequest request = new DocumentInviteLinkPostRequest("none", 15);
        request.withDocumentId(documentId);
        request.withFieldInviteId(inviteId);

        DocumentInviteLinkPostResponse response = (DocumentInviteLinkPostResponse) client.send(request).getResponse();
        return response.getData().getLink() + "&redirect_uri=" + redirectUrl;
    }

    private byte[] downloadDocument(ApiClient client, String documentId) throws SignNowApiException, IOException {
        DocumentDownloadGetRequest request = new DocumentDownloadGetRequest().withDocumentId(documentId);
        DocumentDownloadGetResponse response = (DocumentDownloadGetResponse) client.send(request).getResponse();
        File file = response.getFile();
        byte[] content = Files.readAllBytes(file.toPath());
        file.delete();
        return content;
    }
}
