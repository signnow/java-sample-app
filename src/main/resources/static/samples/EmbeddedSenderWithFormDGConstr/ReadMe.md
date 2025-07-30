# Embedded Sender With Form DG

This sample demonstrates how to create a streamlined document signing workflow for ISV (Independent Software Vendor) applications using SignNow Java SDK. The sample shows how to collect user information through a custom form, create a document group from a template, populate fields with user data, send the document for signing using embedded sending, and then proceed directly to status tracking.

## Use Case Overview

This sample addresses the common requirement of integrating electronic signature capabilities into existing business applications using embedded sending workflows. It demonstrates a streamlined workflow where:

- Users fill out a simple form with their information
- The system automatically creates a document group from a template
- Document fields are populated with user data
- The document is prepared using embedded sending interface
- Users are redirected directly to status tracking page
- Users can track the status and download completed documents

This approach is ideal for applications that want to provide seamless document signing experiences within their platform without requiring users to leave their application, with a simplified workflow that eliminates intermediate signers.

## Scenario Description

### Step 1: Form Collection
- User opens the application and sees a simple form
- Form contains Name and Email fields
- User fills out the form and clicks "Continue"
- System validates the input and proceeds to document preparation

### Step 2: Document Preparation
- System automatically creates a document group from a predefined template
- Document fields are populated with the user's name
- Recipients are added to the document group with the provided email
- User is redirected to embedded sending interface

### Step 3: Embedded Sending
- User is redirected to SignNow embedded sending interface
- User can review and configure the document for sending
- After completing embedded sending, user is redirected directly to status page

### Step 4: Status Tracking
- User can view the current status of the document group
- System provides download functionality for completed documents
- Status page shows creation and update timestamps

## Page Flow Documentation

### HTML Container Pages
- **Form Page** (`<div id="form-page">`): User input form for collecting name and email
- **Status Page** (`<div id="status-page">`): Displays document status, signer information, and provides download functionality

### Embedded SignNow Pages
- **Embedded Sending**: External SignNow URL for document preparation and sending configuration

### Page State Management
- **Query Parameters**: All pages use `?page=` parameter for routing
- **Data Persistence**: `document_group_id` is passed between pages via URL parameters
- **Error Handling**: Each page includes proper error handling and user feedback
- **Loading States**: All pages show loading indicators during API calls

### Page Navigation Flow
```
Form Page → Embedded Sending → Status Page
```

- **Form Page**: Collect user data, prepare document group
- **Embedded Sending**: SignNow interface for document preparation (external URL)
- **Status Page**: Show completion status and provide download

### Data Flow Between Pages
- **Form Page → Embedded Sending**: `document_group_id` passed via redirect URL
- **Embedded Sending → Status Page**: `document_group_id` returned via redirect URL

## Technical Flow

| Action | Responsibility | Code Location |
|--------|---------------|---------------|
| Form submission | Frontend validation and API call | `initFormPage()` in index.html |
| Document group creation | Backend SDK integration | `createDocumentGroupFromTemplate()` in IndexController.java |
| Field population | Backend SDK document update | `updateDocumentFields()` in IndexController.java |
| Recipient addition | Backend SDK recipient management | `updateDocumentGroupRecipients()` in IndexController.java |
| Embedded sending creation | Backend SDK embedded sending | `createEmbeddedSendingUrl()` in IndexController.java |
| Status tracking | Backend SDK status API | `getDocumentGroupSignersStatus()` in IndexController.java |
| Invite status tracking | Backend SDK invite API | `getInviteStatus()` and `getDocumentGroupSignersStatus()` in IndexController.java |
| Document download | Backend SDK download API | `downloadDocumentGroup()` and `downloadDocumentGroupFile()` in IndexController.java |

## Sequence of Function Calls

1. **Frontend Form Submission**
   - `initFormPage()` → Form validation
   - `fetch('/api/samples/EmbeddedSenderWithFormDGConstr')` → POST with action: 'prepare_dg'

2. **Backend Document Preparation**
   - `prepareDocumentGroup()` → Main orchestration
   - `createDocumentGroupFromTemplate()` → Create DG from template
   - `updateDocumentFields()` → Populate document fields
   - `updateDocumentGroupRecipients()` → Add recipients
   - `createEmbeddedSendingUrl()` → Generate embedded sending URL

3. **Frontend Embedded Sending**
   - Redirect to embedded sending URL from backend
   - User completes embedded sending in SignNow interface
   - Redirect back to status page with document_group_id

4. **Frontend Status Tracking**
   - `initStatusPage()` → Status page initialization
   - `fetch('/api/samples/EmbeddedSenderWithFormDGConstr')` → POST with action: 'invite-status'

5. **Backend Status Management**
   - `getInviteStatus()` → Handle invite status request
   - `getDocumentGroupSignersStatus()` → Get signers status for document group
   - `downloadDocumentGroup()` → Handle download request
   - `downloadDocumentGroupFile()` → Download merged PDF file

## Template Info

This sample requires a Document Group Template (DGT) to be configured in your SignNow account. The template should include:

- **Template ID**: `8e36720a436041ea837dc543ec00a3bc3559df45` (configured in IndexController.java)
- **Required Fields**: The template should include fields named "CustomerName" or "CustomerFN" for populating user data
- **Document Structure**: The template should contain the documents that need to be signed
- **Recipient Roles**: Template should include recipients with roles for signing

### Template Configuration
- Template must be accessible via the SignNow API
- Template should include appropriate signing fields
- Template should be configured for document group workflows
- Template should have recipients configured for signing

## Configuration

### Required Environment Variables
```bash
SIGNNOW_CLIENT_ID=your_client_id
SIGNNOW_CLIENT_SECRET=your_client_secret
SIGNNOW_USERNAME=your_username
SIGNNOW_PASSWORD=your_password
```

### Additional Configuration
- `USER_EMAIL` constant in IndexController.java for the preparer email address

### SDK Classes Used
- `com.signnow.api.documentgrouptemplate.request.DocumentGroupTemplatePostRequest` - Create document group from template
- `com.signnow.api.documentfield.request.DocumentPrefillPutRequest` - Update document fields
- `com.signnow.api.documentgroup.request.DocumentGroupRecipientsPutRequest` - Update document group recipients
- `com.signnow.api.embeddedsending.request.DocumentGroupEmbeddedSendingLinkPostRequest` - Create embedded sending
- `com.signnow.api.documentgroup.request.DocumentGroupGetRequest` - Get document group status
- `com.signnow.api.documentgroup.request.DownloadDocumentGroupPostRequest` - Download document group
- `com.signnow.api.documentgroupinvite.request.GroupInviteGetRequest` - Get invite status for document group
- `com.signnow.api.documentgroup.request.DocumentGroupRecipientsGetRequest` - Get document group recipients

## Quick Start (TL;DR)

1. **Configure the sample**
   - Set up SignNow credentials in environment variables
   - Ensure template ID is correctly configured in IndexController.java

2. **Run the sample**
   - Navigate to the sample URL
   - Fill out the form with name and email
   - Click "Continue" to prepare the document

3. **Complete embedded sending**
   - Use the embedded sending interface to configure and send the document
   - Complete the sending process in SignNow interface

4. **Track completion**
   - Monitor the status page for document completion
   - View individual signer statuses and timestamps
   - Download the completed document when ready
   - Refresh status to get latest updates
   - Parent application is notified when workflow is complete

## SDK Integration Details

### Document Group Creation from Template
```java
// Create document group directly from Document Group Template
var request = new DocumentGroupTemplatePostRequest(
    "ISV Form Document Group", null, null
);
request.withTemplateGroupId(DOCUMENT_GROUP_TEMPLATE_ID);

var response = (DocumentGroupTemplatePostResponse) client.send(request).getResponse();
String documentGroupId = response.getData().getUniqueId();
```

### Field Population
```java
// Try to fill "CustomerName" field first, then "CustomerFN" as fallback
FieldCollection fieldValues = new FieldCollection();
if (existingFields.contains(CUSTOMER_NAME_FIELD)) {
    fieldValues.add(new Field(CUSTOMER_NAME_FIELD, name));
} else if (existingFields.contains(CUSTOMER_FN_FIELD)) {
    fieldValues.add(new Field(CUSTOMER_FN_FIELD, name));
}

if (!fieldValues.isEmpty()) {
    var prefillRequest = new DocumentPrefillPutRequest(fieldValues);
    prefillRequest.withDocumentId(docId);
    client.send(prefillRequest);
}
```

### Recipient Update with Different Emails
```java
// Assign email based on recipient role/name
String emailToUse = getEmailForRole(recipientName, customerEmail, preparerEmail);

updatedRecipients.add(new Recipient(
    recipientName,
    emailToUse,
    recipient.getOrder(),
    requestDocumentCollection,
    null,
    null
));
```

### Embedded Sending
```java
// Create embedded sending URL with redirect to status page
String redirectUrl = REDIRECT_BASE_URL + "?" +
    "page=status-page&document_group_id=" + documentGroupId;

var embeddedSendingRequest = new DocumentGroupEmbeddedSendingLinkPostRequest(
    redirectUrl,  // Redirect back to status page
    15,           // 15 minute expiration
    "self",       // Self-hosted embedded experience
    "send-invite" // Action type
);
embeddedSendingRequest.withDocumentGroupId(documentGroupId);
```

### Invite Status Tracking
```java
// Get invite status for document group
var inviteStatusRequest = new GroupInviteGetRequest()
    .withDocumentGroupId(documentGroupId)
    .withInviteId(inviteId);

var inviteStatusResponse = (GroupInviteGetResponse) 
    client.send(inviteStatusRequest).getResponse();

// Process signer statuses
Map<String, String> statuses = new HashMap<>();
for (var step : inviteStatusResponse.getInvite().getSteps()) {
    for (var action : step.getActions()) {
        statuses.put(action.getRoleName(), action.getStatus());
    }
}
```

### Document Download
```java
// Download merged PDF file
var orderColl = new DocumentOrderCollection();
var downloadRequest = new DownloadDocumentGroupPostRequest(
    "merged", "no", orderColl
).withDocumentGroupId(documentGroupId);

var response = (DownloadDocumentGroupPostResponse) 
    client.send(downloadRequest).getResponse();

byte[] content = Files.readAllBytes(response.getFile().toPath());
response.getFile().delete();

return ResponseEntity.ok()
    .header("Content-Type", "application/pdf")
    .header("Content-Disposition", "attachment; filename=\"final_document_group.pdf\"")
    .body(content);
```

### Parent Application Notification
```javascript
// Notify parent application when workflow is complete
parent.postMessage({type: "SAMPLE_APP_FINISHED"}, location.origin);
```

## Error Handling

The sample includes comprehensive error handling for:
- Missing or invalid form data
- API authentication failures
- Document group creation errors
- Field update failures
- Recipient addition errors
- Embedded sending creation issues
- Status retrieval problems
- Invite status tracking errors
- Document download failures

All errors are logged and returned to the frontend with appropriate user-friendly messages. The frontend displays error messages to users and provides retry functionality for failed operations.

## Security Considerations

- All API calls use secure HTTPS connections
- User data is validated before processing
- Error messages don't expose sensitive information
- Embedded URLs are generated with proper security parameters
- Access tokens are managed securely through SignNow SDK

## Disclaimer

This sample is designed for educational and demonstration purposes. Before using in production:

- Implement proper session management
- Add comprehensive input validation
- Implement proper error logging and monitoring
- Add rate limiting and security measures
- Test thoroughly with your specific use case
- Ensure compliance with data protection regulations
- Customize the workflow to match your business requirements

The sample demonstrates core SignNow API functionality but should be adapted and enhanced for production use. 