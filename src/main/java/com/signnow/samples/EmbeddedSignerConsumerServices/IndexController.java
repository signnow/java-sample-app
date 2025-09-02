package com.signnow.samples.EmbeddedSignerConsumerServices;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentDownloadGetRequest;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentDownloadGetResponse;
import com.signnow.api.document.response.DocumentGetResponse;
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

    private static final String TEMPLATE_ID = "b6797f3437db4c818256560e4f68143cb99c7bc9";

    /**
     * Handles GET requests.
     * <p>
     * If the "page" query parameter is "finish", serves the static confirmation HTML.
     * Otherwise, initiates the embedded signing flow by cloning a template,
     * creating an embedded invite, and redirecting the user to the signing URL.
     *
     * @param queryParams Map of query parameters
     * @return HTML page or redirect to signing link
     * @throws IOException if reading the HTML file fails
     * @throws SignNowApiException if SignNow API call fails
     * @throws UnsupportedEncodingException if URL encoding fails
     */
    @Override
    public ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException, SignNowApiException, UnsupportedEncodingException {
        String page = queryParams.get("page");
        if ("finish".equals(page)) {
            try (var inputStream = getClass().getResourceAsStream("/static/samples/EmbeddedSignerConsumerServices/index.html")) {
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
     * Handles POST requests to download the signed document.
     * <p>
     * Expects a `document_id` in the form data, downloads the completed PDF using SignNow API,
     * and returns it as an HTTP response with proper headers.
     *
     * @param formData JSON-encoded form data containing the document ID
     * @return PDF file as HTTP response
     * @throws IOException if formData parsing fails
     * @throws SignNowApiException if SignNow API call fails
     */
    @Override
    public ResponseEntity<?> handlePost(String formData) throws IOException, SignNowApiException {
        Map<String, String> data = new ObjectMapper().readValue(formData, Map.class);
        String documentId = data.get("document_id");

        ApiClient client = new Sdk().build().authenticate().getApiClient();

        byte[] file = downloadDocument(client, documentId);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"completed_document.pdf\"")
                .body(file);
    }

    /**
     * Creates an embedded signing invite and returns the URL for the signer.
     * <p>
     * Clones the template, retrieves the signer role, sends the embedded invite,
     * and generates a signing URL with redirect.
     *
     * @param templateId ID of the SignNow template to clone
     * @return URL to start embedded signing session
     * @throws SignNowApiException if SignNow API call fails
     * @throws UnsupportedEncodingException if redirect URL encoding fails
     */
    private String createEmbeddedInviteAndReturnSigningLink(String templateId) throws SignNowApiException, UnsupportedEncodingException {
        ApiClient client = new Sdk().build().authenticate().getApiClient();

        CloneTemplatePostResponse cloneTemplateResponse = createDocumentFromTemplate(client, templateId);

        String signerEmail = "signer@example.com"; // Assume this is configured somewhere
        String roleId = getSignerUniqueRoleId(client, cloneTemplateResponse.getId(), "Recipient 1");

        DocumentInvitePostResponse documentInviteResponse = createEmbeddedInviteForOneSigner(client, cloneTemplateResponse.getId(), signerEmail, roleId);

        return getEmbeddedInviteLink(client, cloneTemplateResponse.getId(), documentInviteResponse.getData().get(0).getId());
    }

    /**
     * Clones a template to create a new document.
     *
     * @param client Authenticated SignNow API client
     * @param templateId ID of the template to clone
     * @return Response containing the new document ID
     * @throws SignNowApiException if cloning fails
     */
    private CloneTemplatePostResponse createDocumentFromTemplate(ApiClient client, String templateId) throws SignNowApiException {
        CloneTemplatePostRequest cloneTemplate = new CloneTemplatePostRequest();
        cloneTemplate.withTemplateId(templateId);

        return (CloneTemplatePostResponse) client.send(cloneTemplate).getResponse();
    }

    /**
     * Generates a URL for the embedded signing session.
     * <p>
     * Includes a redirect URI back to the app after signing is completed.
     *
     * @param client Authenticated SignNow API client
     * @param documentId ID of the cloned document
     * @param inviteId ID of the created invite
     * @return URL to embedded signing interface with redirect parameter
     * @throws SignNowApiException if SignNow API call fails
     * @throws UnsupportedEncodingException if redirect URL encoding fails
     */
    private String getEmbeddedInviteLink(ApiClient client, String documentId, String inviteId) throws SignNowApiException, UnsupportedEncodingException {
        DocumentInviteLinkPostRequest embeddedInvite = new DocumentInviteLinkPostRequest("none", 15);
        embeddedInvite.withFieldInviteId(inviteId);
        embeddedInvite.withDocumentId(documentId);

        DocumentInviteLinkPostResponse embeddedInviteResponse = (DocumentInviteLinkPostResponse) client.send(embeddedInvite).getResponse();

        String redirectUrl = "http://localhost:8080/samples/EmbeddedSignerConsumerServices?page=finish&document_id=" + documentId;

        return embeddedInviteResponse.getData().getLink() + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUrl, "UTF-8");
    }

    /**
     * Creates an embedded signing invite for a single signer.
     *
     * @param client Authenticated SignNow API client
     * @param documentId ID of the document to sign
     * @param signerEmail Email of the signer
     * @param roleId Unique ID of the signer’s role
     * @return Response containing invite details
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
     * Retrieves the unique role ID for a given role name in a document.
     *
     * @param client Authenticated SignNow API client
     * @param documentId ID of the document
     * @param signerRole Name of the signer role (e.g., "Recipient 1")
     * @return Unique ID of the role, or null if not found
     * @throws SignNowApiException if document retrieval fails
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
     * Downloads the finalized signed document as a byte array.
     *
     * @param client Authenticated SignNow API client
     * @param documentId ID of the signed document
     * @return Byte array of the downloaded PDF
     * @throws SignNowApiException if document download fails
     */
    private byte[] downloadDocument(ApiClient client, String documentId) throws SignNowApiException, IOException {
        DocumentDownloadGetRequest downloadRequest = new DocumentDownloadGetRequest();
        downloadRequest.withDocumentId(documentId).withType("collapsed");

        DocumentDownloadGetResponse response = (DocumentDownloadGetResponse) client.send(downloadRequest).getResponse();

        byte[] fileBytes = Files.readAllBytes(response.getFile().toPath());
        response.getFile().delete();
        return fileBytes;
    }
}
