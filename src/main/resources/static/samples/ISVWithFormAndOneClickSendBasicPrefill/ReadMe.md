# ISV with Form and One Click Send - Basic Pre-fill

This sample demonstrates how to create a streamlined document signing workflow for ISV (Independent Software Vendor) applications using SignNow Java SDK. The sample shows how to collect user information through a custom form, create a document group from a template, prefill document fields with user data, send invites to signers, and provide status tracking functionality.

## Use Case Overview

This sample addresses the common requirement of integrating electronic signature capabilities into existing business applications using direct API calls with basic field prefill. It demonstrates a streamlined workflow where:

- Users fill out a simple form with their information
- The system automatically creates a document group from a template
- Document fields are pre-filled with user data (Name and Email)
- Recipients are added to the document group with the provided email
- Users can track the status and download completed documents

This approach is ideal for applications that want to provide seamless document signing experiences within their platform with basic field prefill capabilities.

## Scenario Description

### Step 1: Form Collection
- User opens the application and sees a simple form
- Form contains Name and Email fields
- User fills out the form and clicks "Continue"
- System validates the input and proceeds to document preparation

### Step 2: Document Preparation
- System automatically creates a document group from a predefined template
- Document fields are pre-filled with the user's name and email
- Recipients are added to the document group with the provided email
- User is redirected directly to status page

### Step 3: Status Tracking
- User can view the current status of the document group
- System provides download functionality for completed documents
- Status page shows creation and update timestamps

### Step-by-step:

1. **User fills out the form**: Enter name and email in the provided form fields
2. **Form submission**: Click "Continue" to submit the form data
3. **Document group creation**: System creates a document group from the template
4. **Field prefill**: Document fields are automatically filled with user data
5. **Recipient addition**: User's email is added as a recipient to the document group
6. **Invite sending**: System sends an email invite to the user
7. **Status page redirect**: User is automatically redirected to the status page
8. **Status monitoring**: User can monitor signing progress and download completed documents
9. **Document download**: Download the completed document when all signers have signed

## Page Flow Documentation

### HTML Container Pages
- **Form Page** (`<div id="form-page">`): User input form for collecting name and email
- **Status Page** (`<div id="status-page">`): Displays document status, signer information, and provides download functionality

### Page State Management
- **Query Parameters**: All pages use `?page=` parameter for routing
- **Data Persistence**: `document_group_id` is passed between pages via URL parameters
- **Error Handling**: Each page includes proper error handling and user feedback
- **Loading States**: All pages show loading indicators during API calls

### Page Navigation Flow
```
Form Page → Status Page
```

- **Form Page**: Collect user data, prepare document group, prefill fields, send invite
- **Status Page**: Show completion status and provide download

### Data Flow Between Pages
- **Form Page → Status Page**: `document_group_id` passed via URL parameters

## Technical Flow

| Action | Responsibility | Code Location |
|--------|---------------|---------------|
| Form submission | Frontend validation and API call | `initFormPage()` in index.html |
| Document group creation | Backend SDK integration | `createDocumentGroupFromTemplate()` in IndexController.java |
| Field prefill | Backend SDK document update | `updateDocumentFields()` in IndexController.java |
| Recipient addition | Backend SDK recipient management | `sendInvite()` in IndexController.java |
| Status tracking | Backend SDK status API | `getInviteStatus()` and `getDocumentGroupSignersStatus()` in IndexController.java |
| Document download | Backend SDK download API | `downloadDocumentGroup()` and `downloadDocumentGroupFile()` in IndexController.java |

## Sequence of Function Calls

1. **Frontend Form Submission**
   - `initFormPage()` → Form validation
   - `fetch('/api/samples/ISVWithFormAndOneClickSendBasicPrefill')` → POST with action: 'prepare_dg'

2. **Backend Document Preparation**
   - `prepareDocumentGroup()` → Main orchestration
   - `createDocumentGroupFromTemplate()` → Create DG from template
   - `updateDocumentFields()` → Prefill document fields with name and email
   - `sendInvite()` → Add recipients and send invites

3. **Frontend Status Tracking**
   - Redirect to status page with document_group_id
   - `initStatusPage()` → Initialize status page
   - `updateStatuses()` → Poll for status updates
   - Display recipient status and download button

4. **Frontend Document Download**
   - Download request to backend
   - File download to user's device
   - Notify parent that sample is finished

## Template Info

### Document Group Template
- **Template ID**: `6e79b9e6f9624984a7f054a7171d1644d0fb9934`
- **Template Name**: "Membership Program Agreement"
- **Template Type**: Document Group Template (DGT)

### Document Fields
- **Name Field**: Pre-filled with user's name from form
- **Email Field**: Pre-filled with user's email from form

### Recipient Configuration
- **Role**: "Recipient 1"
- **Email**: User's email from form
- **Invite**: Automatically sent to user's email address
- **Redirect URL**: Returns to status page after signing

## Configuration

### Required Environment Variables
- `SIGNNOW_CLIENT_ID`: SignNow API client ID
- `SIGNNOW_CLIENT_SECRET`: SignNow API client secret
- `SIGNNOW_USERNAME`: SignNow account username
- `SIGNNOW_PASSWORD`: SignNow account password

### Setup Instructions
1. Configure SignNow API credentials in environment variables
2. Ensure the document group template is accessible
3. Verify the template contains "Name" and "Email" fields
4. Test the sample with valid email addresses

## Quick Start (TL;DR)

1. **Open the sample**: Navigate to `/samples/ISVWithFormAndOneClickSendBasicPrefill`
2. **Fill out the form**: Enter name and email
3. **Submit the form**: Click "Continue" to create document group and send invite
4. **Check status**: Monitor signing progress on status page
5. **Download document**: Download completed document when all signers have signed

### URL Patterns
- **Form Page**: `/samples/ISVWithFormAndOneClickSendBasicPrefill`
- **Status Page**: `/samples/ISVWithFormAndOneClickSendBasicPrefill?page=status-page&document_group_id={id}`

### Expected Flow
1. User fills form → Document group created → Fields pre-filled → Recipients added → Status tracking → Download

## Java Implementation Details

### Controller Structure
- **Package**: `com.signnow.samples.ISVWithFormAndOneClickSendBasicPrefill`
- **Class**: `IndexController` implements `ExampleInterface`
- **Framework**: Spring Boot with `@Controller` annotation

### Key Methods
- `handleGet()`: Serves HTML pages based on query parameters
- `handlePost()`: Routes API actions (prepare_dg, invite-status, download-doc-group)
- `prepareDocumentGroup()`: Main orchestration method
- `createDocumentGroupFromTemplate()`: Creates document group from template
- `updateDocumentFields()`: Prefills document fields with user data
- `sendInvite()`: Sends invites to recipients
- `getInviteStatus()`: Retrieves signing status
- `downloadDocumentGroup()`: Downloads completed documents

### SDK Integration
- Uses SignNow Java SDK for all API operations
- Implements proper error handling and response mapping
- Follows Java best practices for API client usage

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

- Implement proper error handling and validation
- Add security measures for user authentication
- Configure proper logging and monitoring
- Test thoroughly with your specific use case
- Ensure compliance with data protection regulations
- Customize the workflow to match your business requirements 