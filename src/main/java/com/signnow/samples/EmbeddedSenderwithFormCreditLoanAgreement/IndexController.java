package com.signnow.samples.EmbeddedSenderwithFormCreditLoanAgreement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.Sdk;
import com.signnow.api.document.request.DocumentDownloadGetRequest;
import com.signnow.api.document.request.DocumentGetRequest;
import com.signnow.api.document.response.DocumentDownloadGetResponse;
import com.signnow.api.document.response.DocumentGetResponse;
import com.signnow.api.documentfield.request.DocumentPrefillPutRequest;
import com.signnow.api.documentfield.request.data.Field;
import com.signnow.api.documentfield.request.data.FieldCollection;
import com.signnow.api.embeddedsending.request.DocumentEmbeddedSendingLinkPostRequest;
import com.signnow.api.embeddedsending.response.DocumentEmbeddedSendingLinkPostResponse;
import com.signnow.api.template.request.CloneTemplatePostRequest;
import com.signnow.api.template.response.CloneTemplatePostResponse;
import com.signnow.core.ApiClient;
import com.signnow.core.exception.SignNowApiException;
import com.signnow.javasampleapp.ExampleInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Controller
public class IndexController implements ExampleInterface {
    private static final String TEMPLATE_ID = "f8768c13d2f34774b6ce059e3008d9fc04d24378";

    @Override
    public ResponseEntity<String> serveExample() throws IOException {
        String html = new String(Files.readAllBytes(Paths.get("src/main/resources/static/samples/EmbeddedSenderwithFormCreditLoanAgreement/templates/index.html")));
        return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
    }

    @Override
    public ResponseEntity<String> handleSubmission(String formData) throws IOException, SignNowApiException {
        Map<String, String> data = new ObjectMapper().readValue(formData, Map.class);
        String action = data.get("action");

        Sdk sdk = new Sdk();
        ApiClient client = sdk.build().authenticate().getApiClient();

        if ("create-embedded-invite".equals(action)) {
            String fullName = data.get("full_name");
            String link = createEmbeddedInviteAndReturnSendingLink(client, TEMPLATE_ID, Map.of("Name", fullName));
            return ResponseEntity.ok("{\"link\":\"" + link + "\"}");
        }

        String documentId = data.get("document_id");
        byte[] file = downloadDocument(client, documentId);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"result.pdf\"")
                .body(new String(file));
    }

    private byte[] downloadDocument(ApiClient client, String documentId) throws IOException, SignNowApiException {
        DocumentDownloadGetRequest request = new DocumentDownloadGetRequest().withDocumentId(documentId);
        DocumentDownloadGetResponse response = (DocumentDownloadGetResponse) client.send(request).getResponse();
        File file = response.getFile();
        byte[] content = Files.readAllBytes(file.toPath());
        file.delete();
        return content;
    }

    private String createEmbeddedInviteAndReturnSendingLink(ApiClient client, String templateId, Map<String, String> fields) throws SignNowApiException, IOException {
        CloneTemplatePostResponse document = createDocumentFromTemplate(client, templateId);
        prefillFields(client, document.getId(), fields);
        return getEmbeddedSendingLink(client, document.getId());
    }

    private CloneTemplatePostResponse createDocumentFromTemplate(ApiClient client, String templateId) throws SignNowApiException {
        CloneTemplatePostRequest request = new CloneTemplatePostRequest();
        request.withTemplateId(templateId);
        return (CloneTemplatePostResponse) client.send(request).getResponse();
    }

    private void prefillFields(ApiClient client, String documentId, Map<String, String> fieldsValue) throws SignNowApiException {
        FieldCollection fields = new FieldCollection();
        fieldsValue.forEach((name, value) -> {
            if (value != null) fields.add(new Field(name, value));
        });
        DocumentPrefillPutRequest request = new DocumentPrefillPutRequest(fields);
        request.withDocumentId(documentId);
        client.send(request);
    }

    private String getEmbeddedSendingLink(ApiClient client, String documentId) throws SignNowApiException, IOException {
        String redirectUrl = "http://localhost:8080/samples/EmbeddedSenderwithFormCreditLoanAgreement?page=download-container&document_id=" + documentId;

        DocumentEmbeddedSendingLinkPostRequest request = new DocumentEmbeddedSendingLinkPostRequest("document", redirectUrl, 16, "blank").withDocumentId(documentId);
        DocumentEmbeddedSendingLinkPostResponse response = (DocumentEmbeddedSendingLinkPostResponse) client.send(request).getResponse();
        String url = response.getData().getUrl();
        return updateSignNowLink(url, client.getBearerToken().token(), 0);
    }

    private String updateSignNowLink(String url, String newAccessToken, int embeddedValue) throws IOException {
        java.net.URL u = new java.net.URL(url);
        Map<String, String> query = splitQuery(u.getQuery());
        query.put("access_token", newAccessToken);
        query.put("embedded", String.valueOf(embeddedValue));

        if (query.containsKey("redirect_uri")) {
            String decoded = URLDecoder.decode(query.get("redirect_uri"), "UTF-8");
            java.net.URL redirUrl = new java.net.URL(decoded);
            Map<String, String> redirParams = splitQuery(redirUrl.getQuery());
            if (redirParams.containsKey("access_token")) {
                redirParams.put("access_token", newAccessToken);
            }
            String rebuiltRedirect = redirUrl.getProtocol() + "://" + redirUrl.getHost() + ':' + redirUrl.getPort() + redirUrl.getPath();
            if (!redirParams.isEmpty()) {
                rebuiltRedirect += "?" + buildQueryString(redirParams);
            }
            query.put("redirect_uri", rebuiltRedirect);
        }

        return u.getProtocol() + "://" + u.getHost() + u.getPath() + "?" + buildQueryString(query);
    }

    private Map<String, String> splitQuery(String query) throws IOException {
        Map<String, String> queryPairs = new HashMap<>();
        if (query == null) return queryPairs;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=");
            if (parts.length > 1) {
                queryPairs.put(parts[0], parts[1]);
            } else {
                queryPairs.put(parts[0], "");
            }
        }
        return queryPairs;
    }

    private String buildQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue()));
        }
        return sb.toString();
    }

    public List<Map<String, String>> getDocumentStatuses(ApiClient client, String documentId) throws SignNowApiException {
        DocumentGetRequest request = new DocumentGetRequest();
        request.withDocumentId(documentId);
        DocumentGetResponse response = (DocumentGetResponse) client.send(request).getResponse();

        List<Map<String, String>> statuses = new ArrayList<>();
        for (var invite : response.getFieldInvites()) {
            var email = invite.getEmail();
            var statusEntry = invite.getEmailStatuses().isEmpty() ? null : invite.getEmailStatuses().get(0);
            statuses.add(Map.of(
                    "name", email,
                    "timestamp", statusEntry != null ? String.valueOf(statusEntry.getCreatedAt()) : "",
                    "status", statusEntry != null ? statusEntry.getStatus() : "unknown"
            ));
        }
        return statuses;
    }
}
