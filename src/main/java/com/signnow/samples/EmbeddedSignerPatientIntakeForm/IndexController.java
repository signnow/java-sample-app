package com.signnow.samples.EmbeddedSignerPatientIntakeForm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentDownloadGetRequest;
import com.signnow.api.document.request.DocumentGetRequest;
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
import com.signnow.core.ApiClient;
import com.signnow.core.exception.SignNowApiException;
import com.signnow.javasampleapp.ExampleInterface;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {

    @Override
    public ResponseEntity<String> serveExample() throws IOException {
        String page = java.util.Optional.ofNullable(System.getProperty("sn.page")).orElse("");
        if (page.equals("download")) {
            String html = new String(Files.readAllBytes(Paths.get("src/main/resources/static/samples/EmbeddedSignerPatientIntakeForm/templates/index.html")));
            return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
        }

        try {
            String templateId = "2450f8a154f5450a93ea48ef795f6b679b92af1d";
            String signerEmail = "first@signnow.com";
            String signerRole = "Recipient 1";
            String signerFirstName = "FirstName";
            String signerLastName = "LastName";
            String redirectBaseUrl = "http://localhost:8080/samples/EmbeddedSignerPatientIntakeForm";

            Sdk sdk = new Sdk();
            ApiClient client = sdk.build().authenticate().getApiClient();

            // 1. Clone the template
            com.signnow.api.template.request.CloneTemplatePostRequest cloneRequest = new com.signnow.api.template.request.CloneTemplatePostRequest();
            cloneRequest.withTemplateId(templateId);
            com.signnow.api.template.response.CloneTemplatePostResponse cloneResponse = (com.signnow.api.template.response.CloneTemplatePostResponse) client.send(cloneRequest).getResponse();
            String documentId = cloneResponse.getId();

            // 2. Get signer roleId
            String roleId = getSignerUniqueRoleId(client, documentId, signerRole);

            // 3. Create embedded invite
            InviteCollection invites = new InviteCollection();
            invites.add(new Invite(signerEmail, roleId, 1, signerFirstName, signerLastName));
            DocumentInvitePostRequest inviteRequest = new DocumentInvitePostRequest(invites, null);
            inviteRequest.withDocumentId(documentId);
            DocumentInvitePostResponse inviteResponse = (DocumentInvitePostResponse) client.send(inviteRequest).getResponse();

            String embeddedInviteId = "";
            for (DataInvite data : inviteResponse.getData()) {
                if (data.getEmail().equals(signerEmail)) {
                    embeddedInviteId = data.getId();
                    break;
                }
            }

            // 4. Generate signing link
            DocumentInviteLinkPostRequest linkRequest = new DocumentInviteLinkPostRequest("none", 15)
                    .withDocumentId(documentId)
                    .withFieldInviteId(embeddedInviteId);
            DocumentInviteLinkPostResponse linkResponse = (DocumentInviteLinkPostResponse) client.send(linkRequest).getResponse();
            String redirectUrl = redirectBaseUrl + "?page=download&document_id=" + documentId;

            String link = linkResponse.getData().getLink() + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUrl, "UTF-8");
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", link);
            return ResponseEntity.status(302).headers(headers).build();

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<String> handleSubmission(String formData) throws IOException, SignNowApiException {
        Map<String, String> data = new ObjectMapper().readValue(formData, Map.class);
        String documentId = data.get("document_id");

        Sdk sdk = new Sdk();
        ApiClient client = sdk.build().authenticate().getApiClient();

        com.signnow.api.document.request.DocumentDownloadGetRequest request = new DocumentDownloadGetRequest().withDocumentId(documentId);
        com.signnow.api.document.response.DocumentDownloadGetResponse response = (com.signnow.api.document.response.DocumentDownloadGetResponse) client.send(request).getResponse();
        File file = response.getFile();
        byte[] fileBytes = FileCopyUtils.copyToByteArray(new FileInputStream(file));

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=completed_document.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new String(fileBytes));
    }

    private String getSignerUniqueRoleId(ApiClient client, String documentId, String roleName) throws SignNowApiException {
        DocumentGetRequest documentRequest = new DocumentGetRequest().withDocumentId(documentId);
        DocumentGetResponse response = (DocumentGetResponse) client.send(documentRequest).getResponse();
        RoleCollection roles = response.getRoles();
        for (Role role : roles) {
            if (role.getName().equals(roleName)) {
                return role.getUniqueId();
            }
        }
        return "";
    }
}
