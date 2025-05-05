```markdown
# EmbeddedSenderWithoutFormFile2 Sample Application

## Use Case Overview

The "EmbeddedSenderWithoutFormFile" sample app demonstrates how to streamline the document sending process using embedded links and minimal user input. This app is designed to efficiently manage the sending and tracking of documents, ensuring a smooth user experience.

## Scenario: Sales Proposal with Smart Fields

### Step-by-step Flow:

1. **Initiate the Process**
   - **User Action**: The user opens the app and selects the option to start the embedded sending process.
   - **System Response**: The app automatically opens the link from `DocumentEmbeddedSendingLinkPost`, covering both the addition of recipients and fillable fields without displaying any forms directly in the app interface.

2. **Send Invite**
   - **User Action**: The user ensures that the recipient's email is entered and clicks the "Send Invite" button.
   - **System Response**: The invite is sent to the recipient's email, and the user is automatically redirected to the next step.

3. **Monitor Send Status**
   - **User Action**: The user is redirected to the Send Status page via `$redirectUrl`.
   - **System Response**: The Send Status page displays a list of document statuses, including document names, dates, and current statuses. Users can see a preview image and document details for each document.
   - **User Interaction**:
     - The user can click the "Refresh" button to update the status of the documents.
     - The user can click the "Download Document" button for any document to download it.
     - The user can click the "Finish Demo" button to proceed to the final step.

4. **Complete the Process**
   - **User Action**: After clicking the "Finish Demo" button, the user is redirected to the Finish page.
   - **System Response**: The Finish page confirms the completion of the process, and the user can exit the app or start a new session if needed.

## Technical Flow

1. **Initiate Process**
   - The app opens the embedded link from `DocumentEmbeddedSendingLinkPost`, allowing users to add recipients and fillable fields.

2. **Send Invite**
   - The app sends an invite to the recipient's email and redirects the user to the Send Status page.

3. **Monitor Send Status**
   - The Send Status page displays document statuses, and users can interact with the page to refresh statuses or download documents.

4. **Complete Process**
   - The user completes the process by clicking the "Finish Demo" button, which redirects them to the Finish page.

## Template Info
- **Template Name**: 'Sales Proposal Smart Fields'
- **Template ID**: '40d7a874baa043d881e3bc6bdab561445d64ad36'

## User Interaction Summary
- The app guides the user through the process with minimal steps, focusing on efficiency and ease of use.
- Users can monitor the status of their documents and download them as needed, ensuring control over the process from start to finish.

## Disclaimer
This sample is for **demonstration purposes only**. It is not intended for production use without adjustments for authentication, document management, error handling, and security.
```
