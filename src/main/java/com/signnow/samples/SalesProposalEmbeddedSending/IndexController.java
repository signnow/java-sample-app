package com.signnow.samples.SalesProposalEmbeddedSending;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentDownloadGetRequest;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentDownloadGetResponse;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.document.response.data.Role;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {

    @Override
    public ResponseEntity<String> serveExample() throws IOException {
        String page = System.getProperty("sn.page", "");

        if ("download-container".equals(page)) {
            String html = new String(Files.readAllBytes(
                    new File("src/main/resources/static/samples/SalesProposalEmbeddedSending/templates/index.html").toPath()));
            return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
        } else {
            Sdk sdk = new Sdk();
            try {
                ApiClient client = sdk.build().authenticate().getApiClient();
                String templateId = "59b3ff2c50f240b69a3e50412ea3c32453ce8003";
                String link = createEmbeddedSenderAndReturnSigningLink(client, templateId);
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", link);
                return ResponseEntity.status(302).headers(headers).build();
            } catch (SignNowApiException e) {
                return ResponseEntity.status(403).body(e.getMessage());
            }

        }
    }

    @Override
    public ResponseEntity<String> handleSubmission(String formData) throws IOException, SignNowApiException {
        Map<String, Object> data = new ObjectMapper().readValue(formData, Map.class);
        String documentId = (String) data.get("document_id");

        Sdk sdk = new Sdk();
        ApiClient client = sdk.build().authenticate().getApiClient();

        byte[] fileBytes = downloadDocument(client, documentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=result.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new String(fileBytes));
    }

    private String createEmbeddedSenderAndReturnSigningLink(ApiClient client, String templateId) throws SignNowApiException, UnsupportedEncodingException {
        CloneTemplatePostRequest cloneRequest = new CloneTemplatePostRequest().withTemplateId(templateId);
        CloneTemplatePostResponse cloneResponse = (CloneTemplatePostResponse) client.send(cloneRequest).getResponse();
        String documentId = cloneResponse.getId();

        String signerEmail = "first@signnow.com";
        String roleId = getSignerUniqueRoleId(client, documentId, "Recipient 1");

        InviteCollection invites = new InviteCollection();
        invites.add(new Invite(signerEmail, roleId, 1, null, null));
        DocumentInvitePostRequest inviteRequest = new DocumentInvitePostRequest(invites, null).withDocumentId(documentId);
        DocumentInvitePostResponse inviteResponse = (DocumentInvitePostResponse) client.send(inviteRequest).getResponse();

        String inviteId = inviteResponse.getData().get(0).getId();

        String redirectUrl = "http://localhost:8080/samples/SalesProposalEmbeddedSending?page=status-container&document_id=" + documentId;
        DocumentInviteLinkPostRequest linkRequest = new DocumentInviteLinkPostRequest("none", 15)
                .withDocumentId(documentId).withFieldInviteId(inviteId);
        DocumentInviteLinkPostResponse linkResponse = (DocumentInviteLinkPostResponse) client.send(linkRequest).getResponse();

        return linkResponse.getData().getLink() + "&redirect_uri=" + URLEncoder.encode(redirectUrl, "UTF-8");
    }

    private String getSignerUniqueRoleId(ApiClient client, String documentId, String roleName) throws SignNowApiException {
        DocumentGetRequest get = new DocumentGetRequest().withDocumentId(documentId);
        DocumentGetResponse doc = (DocumentGetResponse) client.send(get).getResponse();
        for (Role role : doc.getRoles()) {
            if (role.getName().equals(roleName)) {
                return role.getUniqueId();
            }
        }
        return null;
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
