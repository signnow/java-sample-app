package com.signnow.samples.EmbeddedSignerPatientIntakeForm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.request.DocumentDownloadGetRequest;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.document.response.DocumentDownloadGetResponse;
import com.signnow.api.document.response.data.Role;
import com.signnow.api.document.response.data.RoleCollection;
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
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {

    @Override
    public ResponseEntity<String> serveExample(Map<String, String> queryParams) throws IOException, SignNowApiException, UnsupportedEncodingException {
        String page = queryParams.get("page");
        if ("download-container".equals(page)) {
            String html = new String(Files.readAllBytes(Paths.get("src/main/resources/static/samples/EmbeddedSignerPatientIntakeForm/index.html")));
            return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
        } else {
            String templateId = "2450f8a154f5450a93ea48ef795f6b679b92af1d";
            String link = createEmbeddedInviteAndReturnSigningLink(templateId);
            return ResponseEntity.status(302)
                    .header("Location", link)
                    .build();
        }
    }

    @Override
    public ResponseEntity<String> handleSubmission(String formData) throws IOException, SignNowApiException {
        Map<String, String> data = new ObjectMapper().readValue(formData, Map.class);
        String documentId = data.get("document_id");

        Sdk sdk = new Sdk();
        ApiClient client = sdk.build().authenticate().getApiClient();

        byte[] file = downloadDocument(client, documentId);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"result.pdf\"")
                .body(new String(file));
    }

    private String createEmbeddedInviteAndReturnSigningLink(String templateId) throws SignNowApiException, UnsupportedEncodingException {
        Sdk sdk = new Sdk();
        ApiClient client = sdk.build().authenticate().getApiClient();

        CloneTemplatePostResponse cloneTemplateResponse = createDocumentFromTemplate(client, templateId);

        String signerEmail = "signer@example.com"; // Assume this is configured somewhere
        String roleId = getSignerUniqueRoleId(client, cloneTemplateResponse.getId(), "Recipient 1");

        DocumentInvitePostResponse documentInviteResponse = createEmbeddedInviteForOneSigner(client, cloneTemplateResponse.getId(), signerEmail, roleId);

        return getEmbeddedInviteLink(client, cloneTemplateResponse.getId(), documentInviteResponse.getData().get(0).getId());
    }

    private CloneTemplatePostResponse createDocumentFromTemplate(ApiClient client, String templateId) throws SignNowApiException {
        CloneTemplatePostRequest cloneTemplate = new CloneTemplatePostRequest();
        cloneTemplate.withTemplateId(templateId);

        return (CloneTemplatePostResponse) client.send(cloneTemplate).getResponse();
    }

    private String getEmbeddedInviteLink(ApiClient client, String documentId, String inviteId) throws SignNowApiException, UnsupportedEncodingException {
        DocumentInviteLinkPostRequest embeddedInvite = new DocumentInviteLinkPostRequest("none", 15);
        embeddedInvite.withFieldInviteId(inviteId);
        embeddedInvite.withDocumentId(documentId);

        DocumentInviteLinkPostResponse embeddedInviteResponse = (DocumentInviteLinkPostResponse) client.send(embeddedInvite).getResponse();

        String redirectUrl = "http://localhost:8080/samples/EmbeddedSignerPatientIntakeForm?page=download-container&document_id=" + documentId;

        return embeddedInviteResponse.getData().getLink() + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUrl, "UTF-8");
    }

    private DocumentInvitePostResponse createEmbeddedInviteForOneSigner(ApiClient client, String documentId, String signerEmail, String roleId) throws SignNowApiException {
        InviteCollection invites = new InviteCollection();
        invites.add(new Invite(
                signerEmail, // email
                roleId, // roleId
                1, // order
                null, // firstName
                null // lastName
        ));

        DocumentInvitePostRequest documentInvite = new DocumentInvitePostRequest(
                invites, // invites
                null // nameFormula
        );
        documentInvite.withDocumentId(documentId);

        return (DocumentInvitePostResponse) client.send(documentInvite).getResponse();
    }

    private String getSignerUniqueRoleId(ApiClient client, String documentId, String signerRole) throws SignNowApiException {
        DocumentGetRequest documentRequest = new DocumentGetRequest();
        documentRequest.withDocumentId(documentId);
        DocumentGetResponse documentResponse = (DocumentGetResponse) client.send(documentRequest).getResponse();

        RoleCollection roles = documentResponse.getRoles();
        for (Role role : roles) {
            if (role.getName().equals(signerRole)) {
                return role.getUniqueId();
            }
        }
        return null;
    }

    private byte[] downloadDocument(ApiClient client, String documentId) throws SignNowApiException {
        DocumentDownloadGetRequest downloadDoc = new DocumentDownloadGetRequest();
        downloadDoc.withDocumentId(documentId);

        DocumentDownloadGetResponse response = (DocumentDownloadGetResponse) client.send(downloadDoc).getResponse();

        return response.getFile().getBytes();
    }
}