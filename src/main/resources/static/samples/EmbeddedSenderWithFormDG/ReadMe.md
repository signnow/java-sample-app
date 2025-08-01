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
   - `fetch('/api/samples/EmbeddedSenderWithFormDG')` → POST with action: 'prepare_dg'

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
   - `fetch('/api/samples/EmbeddedSenderWithFormDG')` → POST with action: 'invite-status'

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

# ISV with Form and One Click Send - Merge Fields

This sample demonstrates how to create a document group from a document group template, process merge fields by adding permanent text elements, and send invites to multiple recipients with different roles. The sample showcases SignNow SDK integration for document group management, merge field processing using DocumentPut, and automated invite sending.

## Use Case Overview

This sample addresses the common business scenario where ISVs (Independent Software Vendors) need to:
- Collect customer information through a web form
- Create documents from templates with merge fields
- Process merge fields by adding permanent, read-only text elements at field coordinates
- Send documents to multiple recipients with different roles
- Track signing status and provide download functionality

The sample is particularly useful for businesses that need to automate document workflows with multiple signers and merge field processing using permanent text elements.

## Scenario Description

The sample implements a complete document workflow where:

1. **Customer Information Collection**: Users fill out a form with customer name, company name, and email
2. **Document Creation**: A document group is created from a template with merge fields
3. **Merge Fields Processing**: Customer and company names are added as permanent, read-only text elements at merge field coordinates using DocumentPut, making them part of the document content
4. **Recipient Assignment**: Different recipients are assigned based on roles with specific email addresses
5. **Invite Sending**: Documents are automatically sent to all recipients
6. **Status Tracking**: Users can monitor signing progress and download completed documents

### Step-by-step:

1. **Form Page**: User enters customer name, company name, and email address
2. **Document Group Creation**: Backend creates a document group from the specified template
3. **Merge Fields Processing**: Customer and company names are added as permanent text elements at merge field coordinates using DocumentPut, making them part of the document content
4. **Recipient Configuration**: 
   - "Prepare Contract" role gets the email from the form
   - "Customer to Sign" role gets a specific test email address
5. **Invite Sending**: Documents are sent to all recipients automatically
6. **Status Page**: User is redirected to status page to monitor progress
7. **Download**: Completed documents can be downloaded as merged PDF

## Technical Flow

| Action | Responsibility | Code Location |
|--------|---------------|---------------|
| Form submission | Frontend | `index.blade.php` - `initFormPage()` |
| Document group creation | Backend | `SampleController.php` - `createDocumentGroupFromTemplate()` |
| Merge fields processing | Backend | `SampleController.php` - `processMergeFields()` |
| Invite sending | Backend | `SampleController.php` - `sendInvite()` |
| Status tracking | Backend | `SampleController.php` - `getDocumentGroupSignersStatus()` |
| Document download | Backend | `SampleController.php` - `downloadDocumentGroupFile()` |

## Sequence of Function Calls

1. **Frontend Form Submission**:
   - User submits form with customer data
   - Frontend sends POST request to `/api/samples/ISVWithFormAndOneClickSendMergeFields`
   - Action: `prepare_dg`

2. **Backend Document Group Creation**:
   - `prepareDocumentGroup()` - Main orchestration method
   - `createDocumentGroupFromTemplate()` - Creates document group from template
   - `processMergeFields()` - Processes merge fields by adding permanent text elements using DocumentPut

   - `sendInvite()` - Sends invites to all recipients

3. **Frontend Status Page**:
   - User redirected to status page with document group ID
   - `initStatusPage()` - Initializes status tracking
   - `updateStatuses()` - Polls for signing status
   - Download functionality available when documents are complete

## Template Info

- **Template Type**: Document Group Template
- **Template ID**: `8e36720a436041ea837dc543ec00a3bc3559df45`
- **Template Name**: Sales Proposal
- **Merge Fields Processing**:
  - `CustomerName` - Converted to permanent text element at merge field coordinates using DocumentPut
  - `CompanyName` - Converted to permanent text element at merge field coordinates using DocumentPut
- **Recipient Roles**:
  - "Prepare Contract" - Email from form
  - "Customer to Sign" - Specific test email address

## Configuration

### Required Environment Variables

```env
SIGNNOW_CLIENT_ID=your_client_id
SIGNNOW_CLIENT_SECRET=your_client_secret
SIGNNOW_USERNAME=your_username
SIGNNOW_PASSWORD=your_password
```

### Setup Instructions

1. Configure SignNow API credentials in your environment
2. Ensure the document group template exists and is accessible
3. Verify the merge field names match the template fields
4. The sample uses a hardcoded test email for "Customer to Sign" role

## Quick Start (TL;DR)

1. **Access the sample**: Navigate to `/samples/ISVWithFormAndOneClickSendMergeFields`
2. **Fill the form**: Enter customer name, company name, and email
3. **Submit**: Click "Send Documents" to create and send documents
4. **Monitor**: Check status page for signing progress
5. **Download**: Download completed documents when all signatures are complete

**Expected Flow**:
```
Form Page → Document Creation → Merge Fields Processing → Invite Sending → Status Page → Download
```

## Page Flow Documentation

### HTML Container Pages

1. **Form Page** (`<div id="form-page">`)
   - **Purpose**: Collect customer information
   - **Fields**: Customer Name, Company Name, Email (all required)
   - **Action**: Submit form to create document group and send invites
   - **Transition**: Redirects to status page after successful submission

2. **Status Page** (`<div id="status-page">`)
   - **Purpose**: Monitor signing progress and download completed documents
   - **Features**: Real-time status polling, download functionality
   - **Data Source**: Document group ID passed via query parameter
   - **Completion**: Notifies parent application when download is initiated

### Page Navigation Flow

- **Form Page → Status Page**: Direct navigation via `?page=status-page&document_group_id=123`
- **Status Page Updates**: AJAX polling for real-time status updates
- **Download Flow**: Direct file download with parent notification

### Data Passing Between Pages

- **Form Data**: Passed via POST request to backend
- **Document Group ID**: Passed via query parameter to status page
- **Status Data**: Retrieved via AJAX calls from backend
- **Download Data**: Streamed directly from backend to browser

## Technical Implementation Details

### Merge Fields Processing

The sample processes merge fields by:
1. **Identifying merge fields**: Locates `CustomerName` and `CompanyName` fields in documents
2. **Creating text elements**: Adds permanent text elements at merge field coordinates using DocumentPut
3. **Preserving other fields**: Maintains all other fields in the document
4. **Making content permanent**: Text elements become part of the document and cannot be modified by signers

### Recipient Email Assignment

The sample assigns emails based on recipient roles:
- **"Prepare Contract"**: Uses email from the form
- **"Customer to Sign"**: Uses a specific test email address for demonstration

### DocumentPut Implementation

The sample uses DocumentPut to:
- Create permanent text elements at merge field coordinates
- Preserve document structure and other fields
- Make merge field content part of the document itself

____

## Ready to build eSignature integrations with SignNow API? Get the SignNow extension for GitHub Copilot

Use AI-powered code suggestions to generate SignNow API code snippets in your IDE with GitHub Copilot. Get examples for common integration tasks—from authentication and sending documents for signature to handling webhooks, and building branded workflows.

###  **🚀 Why use SignNow with GitHub Copilot**

* **Relevant code suggestions**: Get AI-powered, up-to-date code snippets for SignNow API calls. All examples reflect the latest API capabilities and follow current best practices.
* **Faster development**: Reduce time spent searching documentation.
* **Fewer mistakes**: Get context-aware guidance aligned with the SignNow API.
* **Smooth onboarding**: Useful for both new and experienced developers working with the API.

### **Prerequisites:**

1\. GitHub Copilot installed and enabled.  
2\. SignNow account. [Register here](https://www.signnow.com/developers)

### ⚙️ **How to use it**

1\. Install the [SignNow extension](https://github.com/apps/signnow).

2\. Start your prompts with [@signnow](https://github.com/signnow) in the Copilot chat window. The first time you use the extension, you may need to authorize it.

3\. Enter a prompt describing the integration scenario.   
Example: @signnow Generate a Java code example for sending a document group to two signers.

4\. Modify the generated code to match your app’s requirements—adjust parameters, headers, and workflows as needed.

### **Troubleshooting**
**The extension doesn’t provide code examples for the SignNow API**

Make sure you're using `@signnow` in the Copilot chat and that the extension is installed and authorized.

____

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