# Upload Embedded Sender

This sample demonstrates uploading a PDF file to SignNow and creating an embedded sending link for document preparation and sending.

## Use Case Overview

This sample showcases how to:
- Upload a PDF document to SignNow using the Java SDK
- Create a document group from the uploaded document
- Generate an embedded sending link for document preparation (edit mode)
- Track document status and provide download functionality

**Target Audience**: Java developers who need to implement document upload functionality with embedded sending capabilities.

## Scenario Description

This sample implements a workflow where users can upload a PDF document and create an embedded sending link for document preparation and sending. The workflow consists of two main pages:

### Step-by-step:

1. **User opens the page** — The application automatically starts the upload process and shows a loading spinner
2. **Document upload** — The PDF file is uploaded to SignNow using the Java SDK
3. **Document group creation** — A document group is created from the uploaded document
4. **Embedded sending link generation** — An embedded sending link is created for document preparation (edit mode)
5. **Document preparation** — User is redirected to SignNow interface to prepare and send the document (add recipients and fields)
6. **Status tracking** — User returns to status page to monitor document progress
7. **Document download** — User can download the completed document when ready

### Page Flow Documentation

#### HTML Container Pages:
1. **Form Page** (`<div id="form-page">`): Shows loading spinner and automatically starts the upload process
2. **Status Page** (`<div id="status-page">`): Shows document status and provides download functionality

#### Embedded SignNow Pages:
- **Embedded Sending (Edit Mode)**: External SignNow URL for document preparation and sending (redirects back to status page)

### Page Navigation Flow:
```
Form Page → Embedded Sending (Edit Mode) → Status Page
```

- **Form Page**: Automatically uploads document and creates embedded sending link, shows loading spinner
- **Embedded Sending (Edit Mode)**: SignNow interface for document preparation and sending (external URL) - allows users to add recipients and fields
- **Status Page**: Shows document status and provides download when complete

## Technical Flow

| Action | Responsibility | Code Location |
|--------|---------------|---------------|
| Upload PDF file | Backend uploads file to SignNow | `uploadDocument()` method |
| Create document group | Backend creates group from uploaded document | `createDocumentGroup()` method |
| Generate embedded sending link | Backend creates embedded URL (edit mode) | `createEmbeddedSendingUrl()` method |
| Track status | Backend polls for document status | `getDocumentGroupSignersStatus()` method |
| Download completed document | Backend provides merged PDF | `downloadDocumentGroupFile()` method |

## Sequence of Function Calls

### Frontend-Backend Interaction Flow:

1. **Page Load**:
   - Frontend automatically sends POST to `/api/samples/UploadEmbeddedSender` with action `upload_and_create_dg`
   - Backend calls `uploadAndCreateDocumentGroup()` method

2. **Document Upload Process**:
   - `uploadDocument()` - Uploads PDF file to SignNow using Java SDK
   - `createDocumentGroup()` - Creates document group from uploaded document
   - `createEmbeddedSendingUrl()` - Generates embedded sending link (edit mode)

3. **Embedded Sending (Edit Mode)**:
   - Frontend redirects to embedded sending URL
   - User prepares and sends document in SignNow interface (can add recipients and fields)
   - SignNow redirects back to status page with document group ID

4. **Status Tracking**:
   - Frontend sends POST with action `invite-status`
   - Backend calls `getDocumentGroupSignersStatus()` to get current status
   - Frontend displays status and provides download functionality

5. **Document Download**:
   - Frontend sends POST with action `download-doc-group`
   - Backend calls `downloadDocumentGroupFile()` to get merged PDF
   - Frontend triggers download and notifies parent application

### Page Navigation Sequence:

1. **Frontend Page Routing**: `handlePages()` function manages page visibility
2. **Backend Redirect URL Construction**: Embedded sending URL includes redirect to status page
3. **SignNow Java SDK Calls**: Document upload, document group creation, embedded sending link generation
4. **Return Flow**: Embedded sending redirects back to status page with document group ID

## Java Implementation Details

### Controller Structure:
- **Package**: `com.signnow.samples.UploadEmbeddedSender`
- **Class**: `IndexController` implements `ExampleInterface`
- **Framework**: Spring Boot with REST endpoints

### Key Java SDK Components Used:
- `DocumentPostRequest` - For uploading PDF documents
- `DocumentGroupPostRequest` - For creating document groups
- `DocumentGroupEmbeddedSendingLinkPostRequest` - For creating embedded sending links
- `GroupInviteGetRequest` - For tracking invite status
- `DownloadDocumentGroupPostRequest` - For downloading completed documents

### Error Handling:
- Comprehensive exception handling with `SignNowApiException`
- Proper HTTP status codes for different error scenarios
- JSON response format for frontend consumption

## Template Info

This sample uses a pre-existing PDF file (`src/main/resources/static/samples/UploadEmbeddedSender/Sales Proposal.pdf`) for demonstration purposes. The file is automatically uploaded to SignNow when the page loads.

In a real implementation, you would:
- Allow users to upload their own PDF files
- Implement file validation and size limits
- Handle different file formats if needed
- Add proper security measures for file uploads

## Configuration

### Required Environment Variables:
- `SIGNNOW_CLIENT_ID` - Your SignNow API client ID
- `SIGNNOW_CLIENT_SECRET` - Your SignNow API client secret
- `SIGNNOW_USERNAME` - Your SignNow account username
- `SIGNNOW_PASSWORD` - Your SignNow account password

### Java Application Properties:
Configure in `src/main/resources/application.properties`:
```properties
# SignNow API Configuration
signnow.client.id=${SIGNNOW_CLIENT_ID}
signnow.client.secret=${SIGNNOW_CLIENT_SECRET}
signnow.username=${SIGNNOW_USERNAME}
signnow.password=${SIGNNOW_PASSWORD}
```

### Setup Instructions:
1. The PDF file is already included in the sample directory (`src/main/resources/static/samples/UploadEmbeddedSender/Sales Proposal.pdf`)
2. Configure SignNow API credentials in your environment variables
3. Make sure the application has proper file permissions
4. Build and run the Spring Boot application

## Quick Start (TL;DR)

1. **Build the application**: `mvn clean compile`
2. **Run the application**: `mvn spring-boot:run`
3. **Access the sample**: Navigate to `http://localhost:8080/samples/UploadEmbeddedSender`
4. **Wait for processing**: The page automatically uploads the PDF and creates an embedded sending link
5. **Use embedded sending (edit mode)**: Click the link to prepare and send the document (add recipients and fields)
6. **Check status**: Return to the status page to monitor progress
7. **Download document**: Download the completed document when ready

**Expected Flow**: Page Load → Embedded Sending (Edit Mode) → Status Page → Download

## Java-Specific Features

### Spring Boot Integration:
- Uses Spring Boot's `@Controller` annotation
- Implements `ExampleInterface` for consistent sample structure
- RESTful endpoints with proper HTTP status codes
- JSON response handling with Jackson ObjectMapper

### SignNow Java SDK Usage:
- Proper SDK initialization with `new Sdk().build().authenticate().getApiClient()`
- Type-safe API requests and responses
- Exception handling for API errors
- File handling with Java NIO

### Code Organization:
- Clean separation of concerns with private helper methods
- Consistent error handling patterns
- Proper resource cleanup (file deletion after download)
- Comprehensive logging and debugging support

## Disclaimer

This sample is for educational purposes and demonstrates the basic workflow. For production use, consider:

- Implementing proper error handling and validation
- Adding security measures for file uploads
- Implementing user authentication and authorization
- Adding proper logging and monitoring
- Customizing the UI to match your application's design
- Adding unit tests and integration tests
- Implementing proper configuration management
- Adding health checks and monitoring endpoints 