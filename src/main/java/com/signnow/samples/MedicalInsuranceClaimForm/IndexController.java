package com.signnow.samples.MedicalInsuranceClaimForm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentDownloadGetRequest;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentDownloadGetResponse;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.document.response.data.Role;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {

    @Override
    public ResponseEntity<String> serveExample() throws IOException {
        String html = new String(Files.readAllBytes(
                new File("src/main/resources/static/samples/MedicalInsuranceClaimForm/templates/index.html").toPath()));
        return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
    }

    @Override
    public ResponseEntity<String> handleSubmission(String formData) throws IOException, SignNowApiException {
        Map<String, Object> data = new ObjectMapper().readValue(formData, Map.class);
        String action = (String) data.get("action");

        Sdk sdk = new Sdk();
        ApiClient client = sdk.build().authenticate().getApiClient();

        if ("create-embedded-editor".equals(action)) {
            String fullName = (String) data.get("full_name");
            String email = (String) data.get("email");
            String templateId = "518933bacd634b82883cb232ff295ff45a8e5217";

            String link = createEmbeddedInviteAndReturnSigningLink(client, templateId, Map.of(
                    "Name", fullName,
                    "Email", email
            ));

            return ResponseEntity.ok(new ObjectMapper().writeValueAsString(Map.of("link", link)));
        } else {
            String documentId = (String) data.get("document_id");
            byte[] fileBytes = downloadDocument(client, documentId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=result.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new String(fileBytes));
        }
    }

    private byte[] downloadDocument(ApiClient client, String documentId) throws SignNowApiException, IOException {
        DocumentDownloadGetRequest request = new DocumentDownloadGetRequest().withDocumentId(documentId);
        DocumentDownloadGetResponse response = (DocumentDownloadGetResponse) client.send(request).getResponse();
        //File file = (File) response.getFile();
        byte[] content = null;//Files.readAllBytes(file.toPath());
        //file.delete();
        return content;
    }

    private String createEmbeddedInviteAndReturnSigningLink(ApiClient client, String templateId, Map<String, String> fields) throws SignNowApiException, UnsupportedEncodingException {
        CloneTemplatePostRequest cloneRequest = new CloneTemplatePostRequest().withTemplateId(templateId);
        CloneTemplatePostResponse cloneResponse = (CloneTemplatePostResponse) client.send(cloneRequest).getResponse();
        String documentId = cloneResponse.getId();

        prefillFields(client, documentId, fields);

        String signerEmail = "first@signnow.com"; // replace with actual config value
        String roleId = getSignerUniqueRoleId(client, documentId, "Recipient 1");

        InviteCollection invites = new InviteCollection();
        invites.add(new Invite(signerEmail, roleId, 1, null, null));
        DocumentInvitePostRequest inviteRequest = new DocumentInvitePostRequest(invites, null).withDocumentId(documentId);
        DocumentInvitePostResponse inviteResponse = (DocumentInvitePostResponse) client.send(inviteRequest).getResponse();

        String inviteId = inviteResponse.getData().get(0).getId();

        String redirectUrl = "http://localhost:8080/samples/MedicalInsuranceClaimForm?page=download-container&document_id=" + documentId;
        DocumentInviteLinkPostRequest linkRequest = new DocumentInviteLinkPostRequest("none", 15)
                .withDocumentId(documentId).withFieldInviteId(inviteId);
        DocumentInviteLinkPostResponse linkResponse = (DocumentInviteLinkPostResponse) client.send(linkRequest).getResponse();

        return linkResponse.getData().getLink() + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUrl, "UTF-8");
    }

    private void prefillFields(ApiClient client, String documentId, Map<String, String> fieldsValue) throws SignNowApiException {
        FieldCollection fields = new FieldCollection();
        for (Map.Entry<String, String> entry : fieldsValue.entrySet()) {
            if (entry.getValue() != null) {
                fields.add(new Field(entry.getKey(), entry.getValue()));
            }
        }
        DocumentPrefillPutRequest patch = new DocumentPrefillPutRequest(fields).withDocumentId(documentId);
        client.send(patch);
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
}
