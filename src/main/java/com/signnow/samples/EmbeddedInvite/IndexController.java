package com.signnow.samples.EmbeddedInvite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.documentfield.request.DocumentPrefillPutRequest;
import com.signnow.javasampleapp.ExampleInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.request.DocumentPostRequest;
import com.signnow.api.document.request.DocumentPutRequest;
import com.signnow.api.document.request.data.Field;
import com.signnow.api.document.request.data.FieldCollection;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.document.response.DocumentPostResponse;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {

    @Override
    public ResponseEntity<String> serveExample() throws IOException {
        String html = new String(Files.readAllBytes(Paths.get("src/main/resources/static/samples/EmbeddedInvite/templates/index.html")));
        return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(html);
    }

    @Override
    public ResponseEntity<String> handleSubmission(String formData) throws IOException, SignNowApiException {
        Map<String, String> data = new ObjectMapper().readValue(formData, Map.class);
        String signerFirstName = data.get("first_name");
        String signerLastName = data.get("last_name");
        String comment = data.get("comment");
        String signerEmail = "first@signnow.com";
        String signerRole = "Customer";

        Sdk sdk = new Sdk();
        ApiClient client = sdk.build().authenticate().getApiClient();

        // Step 1: Upload 1st document
        DocumentPostResponse document = this.uploadDocument(client, "src/main/resources/static/samples/EmbeddedInvite/sample.pdf");

        // Step 2.1: Add fields with roles to the document
        this.addFieldsToDocument(client, document.getId(), signerRole);

        // Step 2.2: Prefill fields with values
        this.prefillFields(client, document.getId(), signerFirstName, signerLastName, comment);

        // 3. find the roleID by role name
        String roleId = this.getSignerUniqueRoleId(client, document.getId(), signerRole);

        // 4. Send embedded invite
        DocumentInvitePostResponse inviteResponse = this.createEmbeddedInvite(client, document.getId(), signerEmail, roleId, signerFirstName, signerLastName);

        // 5. Find invite ID
        String embeddedInviteId = this.findInviteId(inviteResponse, signerEmail);

        // 6. Create an embedded invite link for the embedded invite
        String embeddedSigningLink = this.getEmbeddedInviteSigningLink(client, document.getId(), embeddedInviteId);

        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(new ObjectMapper().writeValueAsString(Map.of(
                        "link", embeddedSigningLink
                )));
    }

    private DocumentPostResponse uploadDocument(ApiClient client, String filepath) throws SignNowApiException {
        DocumentPostRequest request = new DocumentPostRequest(new File(filepath));
        return (DocumentPostResponse) client.send(request).getResponse();
    }

    private void addFieldsToDocument(ApiClient client, String documentId, String signerRole) throws SignNowApiException {
        FieldCollection fields = new FieldCollection();
        fields.add(
                new Field(
                        160, 70, 210, 20,
                        "text", 0, true,
                        signerRole, "SignerFirstName",
                        "First Name"
                ));
        fields.add(
                new Field(
                        160, 100, 210, 20,
                        "text", 0, true,
                        signerRole, "SignerLastName",
                        "Last Name"
                ));
        fields.add(
                new Field(
                        160, 130, 210, 50,
                        "text", 0, false,
                        signerRole, "Comment",
                        "Comment"
                ));
        fields.add(
                new Field(
                        160, 190, 100, 50,
                        "signature", 0, true,
                        signerRole, "Signature",
                        "Signature"
                ));

        DocumentPutRequest putRequest = new DocumentPutRequest(fields);
        putRequest.withDocumentId(documentId);
        client.send(putRequest);
    }

    private void prefillFields(ApiClient client, String documentId, String signerFirstName, String signerLastName, String comment) throws SignNowApiException {
        com.signnow.api.documentfield.request.data.FieldCollection prefilledFields = new com.signnow.api.documentfield.request.data.FieldCollection();
        prefilledFields.add(
                new com.signnow.api.documentfield.request.data.Field(
                        "SignerFirstName", signerFirstName
                ));
        prefilledFields.add(
                new com.signnow.api.documentfield.request.data.Field(
                        "SignerLastName", signerLastName
                ));
        if (comment != null) {
            prefilledFields.add(
                    new com.signnow.api.documentfield.request.data.Field(
                            "Comment", comment
                    ));
        }

        DocumentPrefillPutRequest prefillRequest = new DocumentPrefillPutRequest(prefilledFields);
        prefillRequest.withDocumentId(documentId);
        client.send(prefillRequest);
    }

    private String getSignerUniqueRoleId(ApiClient client, String documentId, String signerRole) throws SignNowApiException {
        DocumentGetRequest documentRequest = new DocumentGetRequest();
        documentRequest.withDocumentId(documentId);
        DocumentGetResponse documentResponse = (DocumentGetResponse) client.send(documentRequest).getResponse();

        RoleCollection roles = documentResponse.getRoles();
        String roleId = "";
        for (Role role : roles) {
            if (role.getName().equals(signerRole)) {
                roleId = role.getUniqueId();
                break;
            }
        }
        return roleId;
    }

    private DocumentInvitePostResponse createEmbeddedInvite(ApiClient client, String documentId, String signerEmail, String roleId, String signerFirstName, String signerLastName) throws SignNowApiException {
        String redirectUrl = "http://localhost:8080/samples/EmbeddedInvite?page=thank-you";
        InviteCollection invites = new InviteCollection();
        invites.add(new Invite(signerEmail, roleId, 1, signerFirstName, signerLastName, redirectUrl, redirectUrl, "blank"));
        DocumentInvitePostRequest inviteRequest = new DocumentInvitePostRequest(invites, null);
        inviteRequest.withDocumentId(documentId);
        return (DocumentInvitePostResponse) client.send(inviteRequest).getResponse();
    }

    private String findInviteId(DocumentInvitePostResponse invite, String signerEmail) {
        String embeddedInviteId = "";
        for (DataInvite embeddedInvite : invite.getData()) {
            if (embeddedInvite.getEmail().equals(signerEmail)) {
                embeddedInviteId = embeddedInvite.getId();
                break;
            }
        }
        return embeddedInviteId;
    }

    private String getEmbeddedInviteSigningLink(ApiClient client, String documentId, String embeddedInviteId) throws SignNowApiException {
        DocumentInviteLinkPostRequest linkRequest =
                new DocumentInviteLinkPostRequest("none", 15)
                        .withDocumentId(documentId)
                        .withFieldInviteId(embeddedInviteId);
        DocumentInviteLinkPostResponse linkResponse =
                (DocumentInviteLinkPostResponse) client.send(linkRequest).getResponse();
        return linkResponse.getData().getLink();
    }
}
