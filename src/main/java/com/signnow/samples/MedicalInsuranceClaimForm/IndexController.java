package com.signnow.samples.MedicalInsuranceClaimForm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentDownloadGetRequest;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentDownloadGetResponse;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.document.response.data.Role;
import com.signnow.api.document.response.data.RoleCollection;
import com.signnow.api.documentfield.request.DocumentPrefillPutRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {

    private static final String TEMPLATE_ID = "60d8e92f12004fda8985d4574237507e6407530d";

    /**
     * Handles GET requests to the form or confirmation screen.
     * <p>
     * If `page=finish` or no `page` is provided, serves the static HTML finish page.
     * Otherwise, clones the document, pre-fills fields, creates an invite,
     * and redirects the user to the embedded signing session.
     *
     * @param queryParams HTTP query parameters
     * @return HTML response or redirect to embedded signing session
     * @throws IOException If reading the HTML file fails
     * @throws SignNowApiException If any SignNow API call fails
     * @throws UnsupportedEncodingException If redirect URL encoding fails
     */
    @Override
    public ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException, SignNowApiException, UnsupportedEncodingException {
        String page = queryParams.get("page");
        if (page == null || page.equals("finish")) {
            String html = new String(Files.readAllBytes(Paths.get("src/main/resources/static/samples/MedicalInsuranceClaimForm/index.html")));
            return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
        } else {
            String link = createEmbeddedInviteAndReturnSigningLink(TEMPLATE_ID, queryParams.get("full_name"), queryParams.get("email"));
            return ResponseEntity.status(302)
                    .header("Location", link)
                    .build();
        }
    }

    /**
     * Handles POST requests for two possible actions:
     * <ul>
     *     <li>`create-embedded-invite` — initiates the signing flow with pre-filled fields</li>
     *     <li>otherwise — treats the request as a download of a signed document</li>
     * </ul>
     *
     * @param formData JSON-encoded form data with user input or document ID
     * @return JSON response with signing link or PDF download
     * @throws IOException If formData parsing fails
     * @throws SignNowApiException If any SignNow API call fails
     */
    @Override
    public ResponseEntity<?> handlePost(String formData) throws IOException, SignNowApiException {
        Map<String, String> data = new ObjectMapper().readValue(formData, Map.class);
        String action = data.get("action");

        Sdk sdk = new Sdk();
        ApiClient client = sdk.build().authenticate().getApiClient();

        if ("create-embedded-invite".equals(action)) {
            String fullName = data.get("full_name");
            String email = data.get("email");
            String templateId = "c78e902aa6834af6ba92e8a6f92b603108e1bbbb";

            String link = createEmbeddedInviteAndReturnSigningLink(templateId, fullName, email);

            return ResponseEntity.ok().body("{\"link\": \"" + link + "\"}");
        } else {
            String documentId = data.get("document_id");
            byte[] file = downloadDocument(client, documentId);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=\"result.pdf\"")
                    .body(file);
        }
    }

    /**
     * Creates an embedded invite with pre-filled document fields and returns the signing link.
     * <p>
     * Steps:
     * 1. Clone template
     * 2. Fill in fields (e.g., full name, email)
     * 3. Find signer role
     * 4. Create embedded invite
     * 5. Generate embedded signing link
     *
     * @param templateId Template ID to clone
     * @param fullName Full name to pre-fill
     * @param email Email to pre-fill
     * @return Embedded signing URL
     * @throws SignNowApiException If API request fails
     * @throws UnsupportedEncodingException If redirect URL encoding fails
     */
    private String createEmbeddedInviteAndReturnSigningLink(String templateId, String fullName, String email) throws SignNowApiException, UnsupportedEncodingException {
        Sdk sdk = new Sdk();
        ApiClient client = sdk.build().authenticate().getApiClient();

        CloneTemplatePostResponse cloneTemplateResponse = createDocumentFromTemplate(client, templateId);

        prefillFields(client, cloneTemplateResponse.getId(), Map.of("Name", fullName, "Email", email));

        String signerEmail = "signer@example.com"; // Assume this is configured somewhere
        String roleId = getSignerUniqueRoleId(client, cloneTemplateResponse.getId(), "Recipient 1");

        DocumentInvitePostResponse documentInviteResponse = createEmbeddedInviteForOneSigner(client, cloneTemplateResponse.getId(), signerEmail, roleId);

        return getEmbeddedInviteLink(client, cloneTemplateResponse.getId(), documentInviteResponse.getData().get(0).getId());
    }

    /**
     * Clones a document template to create a new signable document.
     *
     * @param client Authenticated SignNow API client
     * @param templateId ID of the template to clone
     * @return CloneTemplatePostResponse containing the new document ID
     * @throws SignNowApiException If the cloning operation fails
     */
    private CloneTemplatePostResponse createDocumentFromTemplate(ApiClient client, String templateId) throws SignNowApiException {
        CloneTemplatePostRequest cloneTemplate = new CloneTemplatePostRequest();
        cloneTemplate.withTemplateId(templateId);

        return (CloneTemplatePostResponse) client.send(cloneTemplate).getResponse();
    }

    /**
     * Generates a secure embedded signing link using the document and invite IDs.
     * <p>
     * The link includes a redirect URI back to the app after signing is complete.
     *
     * @param client Authenticated SignNow API client
     * @param documentId ID of the cloned document
     * @param inviteId ID of the created invite
     * @return URL to the embedded signing session
     * @throws SignNowApiException If the API call fails
     * @throws UnsupportedEncodingException If the redirect URL cannot be encoded
     */
    private String getEmbeddedInviteLink(ApiClient client, String documentId, String inviteId) throws SignNowApiException, UnsupportedEncodingException {
        DocumentInviteLinkPostRequest embeddedInvite = new DocumentInviteLinkPostRequest("none", 15);
        embeddedInvite.withFieldInviteId(inviteId);
        embeddedInvite.withDocumentId(documentId);

        DocumentInviteLinkPostResponse embeddedInviteResponse = (DocumentInviteLinkPostResponse) client.send(embeddedInvite).getResponse();

        String redirectUrl = "http://localhost:8080/samples/MedicalInsuranceClaimForm?page=download-container&document_id=" + documentId;

        return embeddedInviteResponse.getData().getLink() + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUrl, "UTF-8");
    }

    /**
     * Creates an embedded signing invite for a single signer using email and role ID.
     *
     * @param client Authenticated SignNow API client
     * @param documentId ID of the document to sign
     * @param signerEmail Signer's email address
     * @param roleId Unique role ID to assign to signer
     * @return DocumentInvitePostResponse containing invite data
     * @throws SignNowApiException If invite creation fails
     */
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

    /**
     * Retrieves the unique role ID for a signer role by name.
     * <p>
     * This ID is required to properly assign the signer in the invite.
     *
     * @param client Authenticated SignNow API client
     * @param documentId ID of the cloned document
     * @param signerRole Role name to search for (e.g., "Recipient 1")
     * @return Unique role ID or null if not found
     * @throws SignNowApiException If document retrieval fails
     */
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

    /**
     * Downloads a completed and signed document as a PDF byte array.
     *
     * @param client Authenticated SignNow API client
     * @param documentId ID of the signed document
     * @return PDF file content as byte array
     * @throws SignNowApiException If download fails
     */
    private byte[] downloadDocument(ApiClient client, String documentId) throws SignNowApiException, IOException {
        DocumentDownloadGetRequest downloadRequest = new DocumentDownloadGetRequest();
        downloadRequest.withDocumentId(documentId).withType("collapsed");

        DocumentDownloadGetResponse response = (DocumentDownloadGetResponse) client.send(downloadRequest).getResponse();

        byte[] fileBytes = Files.readAllBytes(response.getFile().toPath());
        response.getFile().delete();
        return fileBytes;
    }

    /**
     * Pre-fills fields in the document with provided values (e.g., name, email).
     * <p>
     * This ensures the signer sees their data pre-filled before signing.
     *
     * @param client Authenticated SignNow API client
     * @param documentId ID of the document to prefill
     * @param fieldsValue Map of field names to values to insert
     * @throws SignNowApiException If the prefill API call fails
     */
    private void prefillFields(ApiClient client, String documentId, Map<String, String> fieldsValue) throws SignNowApiException {
        FieldCollection fields = new FieldCollection();

        fieldsValue.forEach((fieldName, fieldValue) -> {
            if (fieldValue != null) {
                fields.add(new com.signnow.api.documentfield.request.data.Field(fieldName, fieldValue));
            }
        });

        DocumentPrefillPutRequest patchFields = new DocumentPrefillPutRequest(fields);
        patchFields.withDocumentId(documentId);
        client.send(patchFields);
    }
}
