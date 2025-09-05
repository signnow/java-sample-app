# Embedded Signer Sample: Consent Form for Dinosaur Grooming

## Use Case Overview

This sample demonstrates how to use the SignNow Java SDK to implement an Embedded Signer workflow. In this flow, the user signs a pre-defined Consent Form within your application and is then redirected to a confirmation screen where they can download the completed document.

This specific example simulates a scenario where a dinosaur owner provides consent for various grooming services by signing a digital liability waiver and service agreement.

## Scenario: Embedded Signer Without Form

### Step-by-step:
1. **User opens the signing page** — The app redirects them to the embedded signing session.
2. **User signs the document** — The embedded interface allows them to sign directly within the app.
3. **Redirect to finish screen** — After signing, the user is redirected to a "Thank You" page.
4. **Download option** — The user can download the completed PDF.

## Technical Flow

1. **GET Request Initiation**
    - Route is accessed with or without a `page` query parameter.
    - If `page=download-container`, it shows the "Thank You" view with a download button.
    - Otherwise, the flow initiates an embedded signing session:
        - Clones a predefined template (see below).
        - Creates an embedded invite for a single signer.
        - Generates a secure signing link.
        - Redirects the user to the embedded signing session.

2. **Template Cloning**
   - A **template** cannot be used directly for signing.
   - It must first be **cloned** to create a **document** instance that can be signed.
   - The document is cloned from a **preloaded template** on our **demo SignNow account**.

3. **Embedded Invite Generation**
    - In order to get an **embedded invite link**, an **embedded invite** must be explicitly created.
    - The app retrieves the signer's role ID from the cloned document.
    - An invite is created and assigned to the signer's email.

4. **Signing Link Generation**
    - A signing link is generated using `DocumentInviteLinkPostRequest`, and the redirect URL is appended.
    - The user is redirected to this link for signing.

5. **POST Request for Download**
    - After signing, the user is redirected to the `download-container` view.
    - Clicking the download button sends a `POST` request with the `document_id`.
    - The app fetches the signed PDF and sends it to the user for download.

## Notes
- The template ID used in this demo: 3d28c78de8ec43ccab81a3e7dde07925cb5a1d29.
- The template is a "Beauty Procedures Consent Form" for Luxe Dinosaur Grooming Salon.
- Signer's email is hardcoded in the example as "signer@example.com".
- All API requests are made via the official SignNow SDK using credentials from the environment configuration.

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
This example is for demonstration purposes only. The embedded flow relies on a static template hosted on our demo SignNow account and should not be used in production without appropriate customization.
