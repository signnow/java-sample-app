package com.signnow.javasampleapp.examples.sampleapp1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.api.documentfield.request.DocumentPrefillPutRequest;
import com.signnow.javasampleapp.examples.ExampleInterface;
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
import com.signnow.api.embeddedinvite.response.data.DataInviteCollection;
import com.signnow.core.ApiClient;
import com.signnow.core.exception.SignNowApiException;
import com.signnow.core.factory.SdkFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {

    @Override
    public ResponseEntity<String> serveExample() throws IOException {
        String html = new String(Files.readAllBytes(Paths.get("src/main/resources/static/examples/sampleapp1/templates/index.html")));
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
        String redirectUrl = "http://localhost:8080/examples/sampleapp1?page=thank-you";
        int embeddedInviteLinkExpirationTime = 45;

        String bearerToken = "";
        ApiClient client = SdkFactory.createApiClientWithBearerToken(bearerToken);

        // Step 1: Upload 1st document
        DocumentPostRequest request = new DocumentPostRequest(new File("src/main/resources/static/examples/sampleapp1/sample.pdf"));
        DocumentPostResponse response = (DocumentPostResponse) client.send(request).getResponse();
        String documentId = response.getId();

        // Step 2.1: Add fields with roles to the document
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

        // Step 2.2: Prefill fields with values
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

        // 3. Get the document by id to retrieve the role IDs
        DocumentGetRequest documentRequest = new DocumentGetRequest();
        documentRequest.withDocumentId(documentId);
        DocumentGetResponse documentResponse = (DocumentGetResponse) client.send(documentRequest).getResponse();

        // 4. find the roleID by role name
        RoleCollection roles = documentResponse.getRoles();
        String roleId = "";
        for (Role role : roles) {
            if (role.getName().equals(signerRole)) {
                roleId = role.getUniqueId();
                break;
            }
        }

        // 5. Send embedded invite
        InviteCollection invites = new InviteCollection();
        invites.add(new Invite(signerEmail, roleId, 1, signerFirstName, signerLastName, redirectUrl, redirectUrl, "blank"));
        DocumentInvitePostRequest inviteRequest = new DocumentInvitePostRequest(invites, null);
        inviteRequest.withDocumentId(documentId);
        DocumentInvitePostResponse inviteResponse =
                (DocumentInvitePostResponse) client.send(inviteRequest).getResponse();
        DataInviteCollection embeddedInvites = inviteResponse.getData();

        // 6. Find invite ID
        String embeddedInviteId = "";
        for (DataInvite embeddedInvite : embeddedInvites) {
            if (embeddedInvite.getEmail().equals(signerEmail)) {
                embeddedInviteId = embeddedInvite.getId();
                break;
            }
        }

        // 7. Create an embedded invite link for the embedded invite
        DocumentInviteLinkPostRequest linkRequest =
                new DocumentInviteLinkPostRequest("none", embeddedInviteLinkExpirationTime)
                        .withDocumentId(documentId)
                        .withFieldInviteId(embeddedInviteId);
        DocumentInviteLinkPostResponse linkResponse =
                (DocumentInviteLinkPostResponse) client.send(linkRequest).getResponse();

        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(new ObjectMapper().writeValueAsString(linkResponse));
    }
}
