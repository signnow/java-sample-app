package com.signnow.samples.EmbeddedSignerConsentForm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentDownloadGetRequest;
import com.signnow.api.document.response.DocumentDownloadGetResponse;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentGetResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {

    public ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException, SignNowApiException {
        String page = queryParams.get("page");
        if ("download-container".equals(page)) {
            String html = new String(Files.readAllBytes(Paths.get("src/main/resources/static/samples/EmbeddedSignerConsentForm/index.html")));
            return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
        } else {
            ApiClient client = new Sdk().build().authenticate().getApiClient();
            String templateId = "bcf0ddaea1394b969a1ce628901097b8c547cd87";
            String link = createEmbeddedSenderAndReturnSigningLink(client, templateId);
            return ResponseEntity.status(302).header("Location", link).build();
        }
    }

    public ResponseEntity<String> handlePost(String formData) throws IOException, SignNowApiException {
        Map<String, String> data = new ObjectMapper().readValue(formData, Map.class);
        String documentId = data.get("document_id");

        ApiClient client = new Sdk().build().authenticate().getApiClient();
        byte[] file = downloadDocument(client, documentId);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"result.pdf\"")
                .body(new String(file));
    }

    private String createEmbeddedSenderAndReturnSigningLink(ApiClient client, String templateId) throws SignNowApiException, IOException {
        CloneTemplatePostResponse cloneTemplateResponse = createDocumentFromTemplate(client, templateId);
        String signerEmail = "signer@example.com"; // Replace with actual signer email
        String roleId = getSignerUniqueRoleId(client, cloneTemplateResponse.getId(), "Recipient 1");
        String documentInviteResponse = createEmbeddedInviteForOneSigner(client, cloneTemplateResponse.getId(), signerEmail, roleId);
        return getEmbeddedInviteLink(client, cloneTemplateResponse.getId(), documentInviteResponse);
    }

    private CloneTemplatePostResponse createDocumentFromTemplate(ApiClient client, String templateId) throws SignNowApiException {
        CloneTemplatePostRequest cloneTemplate = new CloneTemplatePostRequest();
        cloneTemplate.withTemplateId(templateId);
        return (CloneTemplatePostResponse) client.send(cloneTemplate).getResponse();
    }

    private String getEmbeddedInviteLink(ApiClient client, String documentId, String inviteId) throws SignNowApiException, IOException {
        DocumentInviteLinkPostRequest embeddedInvite = new DocumentInviteLinkPostRequest("none", 15);
        embeddedInvite.withFieldInviteId(inviteId);
        embeddedInvite.withDocumentId(documentId);

        DocumentInviteLinkPostResponse embeddedInviteResponse = (DocumentInviteLinkPostResponse) client.send(embeddedInvite).getResponse();

        String redirectUrl = "http://localhost:8080/samples/EmbeddedSignerConsentForm?page=download-container&document_id=" + documentId;
        return embeddedInviteResponse.getData().getLink() + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUrl, "UTF-8");
    }

    private String createEmbeddedInviteForOneSigner(ApiClient client, String documentId, String signerEmail, String roleId) throws SignNowApiException {
        InviteCollection invites = new InviteCollection();
        invites.add(new Invite(signerEmail, roleId, 1, "first name", "last name"));

        DocumentInvitePostRequest documentInvite = new DocumentInvitePostRequest(invites, null);
        documentInvite.withDocumentId(documentId);

        DocumentInvitePostResponse documentInviteResponse = (DocumentInvitePostResponse) client.send(documentInvite).getResponse();
        return documentInviteResponse.getData().get(0).getId();
    }

    private String getSignerUniqueRoleId(ApiClient client, String documentId, String signerRole) throws SignNowApiException {
        DocumentGetRequest documentRequest = new DocumentGetRequest();
        documentRequest.withDocumentId(documentId);
        DocumentGetResponse documentResponse = (DocumentGetResponse) client.send(documentRequest).getResponse();

        return documentResponse.getRoles().stream()
                .filter(role -> role.getName().equals(signerRole))
                .findFirst()
                .map(role -> role.getUniqueId())
                .orElse(null);
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