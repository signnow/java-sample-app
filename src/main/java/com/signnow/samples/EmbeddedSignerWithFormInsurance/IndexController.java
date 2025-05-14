package com.signnow.samples.EmbeddedSignerWithFormInsurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.documentfield.request.DocumentPrefillPutRequest;
import com.signnow.api.documentfield.request.data.FieldCollection;
import com.signnow.api.documentfield.request.data.Field;
import com.signnow.api.document.response.data.Role;
import com.signnow.api.embeddedinvite.request.DocumentInviteLinkPostRequest;
import com.signnow.api.embeddedinvite.request.DocumentInvitePostRequest;
import com.signnow.api.embeddedinvite.request.data.Invite;
import com.signnow.api.embeddedinvite.response.DocumentInviteLinkPostResponse;
import com.signnow.api.embeddedinvite.response.DocumentInvitePostResponse;
import com.signnow.api.template.request.CloneTemplatePostRequest;
import com.signnow.api.template.response.CloneTemplatePostResponse;
import com.signnow.core.ApiClient;
import com.signnow.core.exception.SignNowApiException;
import com.signnow.javasampleapp.ExampleInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import com.signnow.api.document.response.DocumentDownloadGetResponse;
import com.signnow.api.document.request.DocumentDownloadGetRequest;
import com.signnow.api.embeddedinvite.request.data.InviteCollection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {

    public ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException {
        String html = new String(Files.readAllBytes(Paths.get("src/main/resources/static/samples/EmbeddedSignerWithFormInsurance/index.html")));
        return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
    }

    public ResponseEntity<String> handlePost(String formData) throws IOException, SignNowApiException {
        Map<String, String> data = new ObjectMapper().readValue(formData, Map.class);
        String action = data.get("action");

        ApiClient client = new Sdk().build().authenticate().getApiClient();

        if ("create-embedded-invite".equals(action)) {
            String fullName = data.get("full_name");
            String email = data.get("email");
            String templateId = "c78e902aa6834af6ba92e8a6f92b603108e1bbbb";

            String link = createEmbeddedInviteAndReturnSigningLink(client, templateId, fullName, email);

            return ResponseEntity.ok().body("{\"link\": \"" + link + "\"}");
        }

        String documentId = data.get("document_id");
        byte[] file = downloadDocument(client, documentId);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"result.pdf\"")
                .body(new String(file));
    }

    private String createEmbeddedInviteAndReturnSigningLink(ApiClient client, String templateId, String fullName, String email) throws SignNowApiException, IOException {
        CloneTemplatePostResponse document = createDocumentFromTemplate(client, templateId);

        prefillFields(client, document.getId(), fullName, email);

        String roleId = getSignerUniqueRoleId(client, document.getId(), "Recipient 1");

        return getEmbeddedInviteLink(client, document.getId(), createEmbeddedInviteForOneSigner(client, document.getId(), email, roleId));
    }

    private CloneTemplatePostResponse createDocumentFromTemplate(ApiClient client, String templateId) throws SignNowApiException {
        CloneTemplatePostRequest cloneTemplate = new CloneTemplatePostRequest();
        cloneTemplate.withTemplateId(templateId);

        return (CloneTemplatePostResponse) client.send(cloneTemplate).getResponse();
    }

    private void prefillFields(ApiClient client, String documentId, String fullName, String email) throws SignNowApiException {
        FieldCollection fields = new FieldCollection();
        fields.add(new Field("Name", fullName));
        fields.add(new Field("Email", email));

        DocumentPrefillPutRequest prefillRequest = new DocumentPrefillPutRequest(fields);
        prefillRequest.withDocumentId(documentId);
        client.send(prefillRequest);
    }

    private String getEmbeddedInviteLink(ApiClient client, String documentId, String inviteId) throws SignNowApiException, IOException {
        DocumentInviteLinkPostRequest embeddedInvite = new DocumentInviteLinkPostRequest("none", 15);
        embeddedInvite.withFieldInviteId(inviteId);
        embeddedInvite.withDocumentId(documentId);

        DocumentInviteLinkPostResponse embeddedInviteResponse = (DocumentInviteLinkPostResponse) client.send(embeddedInvite).getResponse();

        String redirectUrl = "http://localhost:8080/samples/EmbeddedSignerWithFormInsurance?page=download-container&document_id=" + documentId;

        return embeddedInviteResponse.getData().getLink() + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUrl, "UTF-8");
    }

    private String createEmbeddedInviteForOneSigner(ApiClient client, String documentId, String signerEmail, String roleId) throws SignNowApiException {
        InviteCollection invites = new InviteCollection();
        invites.add(new Invite(signerEmail, roleId, 1, null, null));

        DocumentInvitePostRequest documentInvite = new DocumentInvitePostRequest(invites, null);
        documentInvite.withDocumentId(documentId);

        DocumentInvitePostResponse documentInviteResponse = (DocumentInvitePostResponse) client.send(documentInvite).getResponse();
        return documentInviteResponse.getData().get(0).getId();
    }

    private String getSignerUniqueRoleId(ApiClient client, String documentId, String signerRole) throws SignNowApiException {
        DocumentGetRequest documentRequest = new DocumentGetRequest();
        documentRequest.withDocumentId(documentId);
        DocumentGetResponse documentResponse = (DocumentGetResponse) client.send(documentRequest).getResponse();

        for (Role role : documentResponse.getRoles()) {
            if (role.getName().equals(signerRole)) {
                return role.getUniqueId();
            }
        }
        return null;
    }

    private byte[] downloadDocument(ApiClient client, String documentId) throws SignNowApiException, IOException {
        DocumentDownloadGetRequest downloadRequest = new DocumentDownloadGetRequest();
        downloadRequest.withDocumentId(documentId);

        DocumentDownloadGetResponse response = (DocumentDownloadGetResponse) client.send(downloadRequest).getResponse();

        byte[] fileBytes = Files.readAllBytes(response.getFile().toPath());
        response.getFile().delete();
        return fileBytes;
    }
}