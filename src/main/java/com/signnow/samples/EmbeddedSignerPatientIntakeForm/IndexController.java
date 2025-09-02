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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Controller
public class IndexController implements ExampleInterface {

    private static final String TEMPLATE_ID = "4078899bbf4446eea7ae0e157e742bbd93be191f";

    /**
     * Handles GET requests to the signing flow.
     * <p>
     * If the "page" query parameter is "download-container", serves the static confirmation page.
     * Otherwise, initiates the signing flow by cloning the template, creating an embedded invite,
     * and redirecting the user to the generated embedded signing link.
     *
     * @param queryParams Query parameters from the HTTP request
     * @return ResponseEntity containing either an HTML page or a redirect
     * @throws IOException if the HTML file cannot be read
     * @throws SignNowApiException if a SignNow API call fails
     * @throws UnsupportedEncodingException if URL encoding fails
     */
    @Override
    public ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException, SignNowApiException, UnsupportedEncodingException {
        String page = queryParams.get("page");
        if ("download-container".equals(page)) {
            try (var inputStream = getClass().getResourceAsStream("/static/samples/EmbeddedSignerPatientIntakeForm/index.html")) {
                if (inputStream == null) {
                    throw new IOException("HTML file not found in classpath");
                }
                String html = new String(inputStream.readAllBytes());
                return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
            }
        } else {
            String link = createEmbeddedInviteAndReturnSigningLink(TEMPLATE_ID);
            return ResponseEntity.status(302)
                    .header("Location", link)
                    .build();
        }
    }

    /**
     * Handles POST requests to download a completed and signed document.
     * <p>
     * Parses the document ID from form data and uses the SignNow API to retrieve
     * and return the signed document as a downloadable PDF.
     *
     * @param formData JSON-encoded form data containing the document ID
     * @return PDF file as an HTTP response
     * @throws IOException if JSON parsing fails
     * @throws SignNowApiException if a SignNow API call fails
     */
    @Override
    public ResponseEntity<?> handlePost(String formData) throws IOException, SignNowApiException {
        Map<String, String> data = new ObjectMapper().readValue(formData, Map.class);
        String documentId = data.get("document_id");

        Sdk sdk = new Sdk();
        ApiClient client = sdk.build().authenticate().getApiClient();

        File file = downloadDocumentFile(client, documentId);
        
        String filename = file.getName();
        byte[] content = Files.readAllBytes(file.toPath());
        file.delete();

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(content);
    }

    /**
     * Orchestrates the full embedded signing invitation process.
     * <p>
     * Steps:
     * 1. Authenticates via SDK
     * 2. Clones the document from a predefined template
     * 3. Retrieves the signer's role ID
     * 4. Creates an embedded invite
     * 5. Generates a signing link with redirect back to the confirmation page
     *
     * @param templateId ID of the template to use
     * @return URL to the embedded signing session
     * @throws SignNowApiException if a SignNow API call fails
     * @throws UnsupportedEncodingException if redirect URL encoding fails
     */
    private String createEmbeddedInviteAndReturnSigningLink(String templateId) throws SignNowApiException, UnsupportedEncodingException {
        Sdk sdk = new Sdk();
        ApiClient client = sdk.build().authenticate().getApiClient();

        CloneTemplatePostResponse cloneTemplateResponse = createDocumentFromTemplate(client, templateId);

        String signerEmail = "signer@example.com"; // Assume this is configured somewhere
        String roleId = getSignerUniqueRoleId(client, cloneTemplateResponse.getId(), "Recipient 1");

        DocumentInvitePostResponse documentInviteResponse = createEmbeddedInviteForOneSigner(client, cloneTemplateResponse.getId(), signerEmail, roleId);

        return getEmbeddedInviteLink(client, cloneTemplateResponse.getId(), documentInviteResponse.getData().get(0).getId());
    }

    /**
     * Clones a predefined template and returns the created document. Create document from template
     *
     * @param client Authenticated SignNow API client
     * @param templateId Template ID to clone
     * @return CloneTemplatePostResponse with new document ID
     * @throws SignNowApiException if cloning fails
     */
    private CloneTemplatePostResponse createDocumentFromTemplate(ApiClient client, String templateId) throws SignNowApiException {
        CloneTemplatePostRequest cloneTemplate = new CloneTemplatePostRequest();
        cloneTemplate.withTemplateId(templateId);

        return (CloneTemplatePostResponse) client.send(cloneTemplate).getResponse();
    }

    /**
     * Generates an embedded signing link for a signer.
     * <p>
     * The link includes a redirect URI that points to the post-signing confirmation page.
     *
     * @param client Authenticated SignNow API client
     * @param documentId ID of the document to sign
     * @param inviteId ID of the invite for the signer
     * @return URL to embedded signing interface with redirect back to the app
     * @throws SignNowApiException if link generation fails
     * @throws UnsupportedEncodingException if redirect URL encoding fails
     */
    private String getEmbeddedInviteLink(ApiClient client, String documentId, String inviteId) throws SignNowApiException, UnsupportedEncodingException {
        DocumentInviteLinkPostRequest embeddedInvite = new DocumentInviteLinkPostRequest("none", 15);
        embeddedInvite.withFieldInviteId(inviteId);
        embeddedInvite.withDocumentId(documentId);

        DocumentInviteLinkPostResponse embeddedInviteResponse = (DocumentInviteLinkPostResponse) client.send(embeddedInvite).getResponse();

        String redirectUrl = "http://localhost:8080/samples/EmbeddedSignerPatientIntakeForm?page=download-container&document_id=" + documentId;

        return embeddedInviteResponse.getData().getLink() + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUrl, "UTF-8");
    }

    /**
     * Creates an embedded invite for a single signer using email and role ID.
     *
     * @param client Authenticated SignNow API client
     * @param documentId ID of the document to sign
     * @param signerEmail Email address of the signer
     * @param roleId Role ID to assign to the signer
     * @return DocumentInvitePostResponse containing invite information
     * @throws SignNowApiException if invite creation fails
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
     * Retrieves the unique role ID for a signer based on the role name.
     *
     * @param client Authenticated SignNow API client
     * @param documentId ID of the document
     * @param signerRole Name of the role to search for (e.g., "Recipient 1")
     * @return Unique role ID if found, null otherwise
     * @throws SignNowApiException if role retrieval fails
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
     * Downloads the completed and signed document from the SignNow API.
     *
     * @param client Authenticated SignNow API client
     * @param documentId ID of the signed document
     * @return Byte array of the signed PDF
     * @throws SignNowApiException if document download fails
     */
    private File downloadDocumentFile(ApiClient client, String documentId) throws SignNowApiException, IOException {
        DocumentDownloadGetRequest downloadRequest = new DocumentDownloadGetRequest();
        downloadRequest.withDocumentId(documentId).withType("collapsed");

        DocumentDownloadGetResponse response = (DocumentDownloadGetResponse) client.send(downloadRequest).getResponse();

        return response.getFile();
    }
}
