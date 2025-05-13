# PrefillAndEmbeddedSendingAgreement Sample Application

## Use Case Overview

This sample application demonstrates how to use the SignNow SDK to implement a workflow that allows users to fill out a form, send an invitation for signing, and monitor the status of the document. It is designed for scenarios where a single recipient needs to sign a document based on form input.

## Scenario: ISV with Form and One Click Send

### Step-by-Step:

1. **Form Input**
    - The user is presented with a web form containing a single required field: **Name**.
    - The user fills in the **Name** field and clicks the **Continue** button to proceed to the next step.

2. **Send Invite**
    - The application opens an embedded sender interface.
    - The **Name** field in the document is pre-filled with the value from the form's **Name** field.
    - The user clicks **Send Invite** to send the document for signing.
    - After sending the invite, the application redirects the user to the status monitoring page.

3. **Send Status Page**
    - The user is redirected to a page where they can monitor the status of the document.
    - The page includes a **Refresh** button that the user can click to update the status of the document.
    - The user can see the current status of the document (e.g., sent, viewed, signed).
    - Once the document is signed, the user has the option to download the final signed document.

4. **Document Download**
    - After the document is signed, a **Download Document** button becomes available.
    - The user can click this button to download the signed document as a PDF.

## Technical Flow

1. **Form Submission**
    - The initial form is rendered when the user accesses the application.
    - Upon form submission, the application captures the input and proceeds to the embedded sender interface.

2. **Template Cloning & Invite Creation**
    - The backend clones a document from the predefined template using the specified Template ID.
    - The form fields are pre-filled, and an invitation is created for the recipient.

3. **Status Monitoring**
    - The application provides a status monitoring interface where the user can manually refresh to check the status of the document.
    - The status of the document is displayed to the user, indicating whether it has been sent, viewed, or signed.

4. **Document Download**
    - Once the document is signed, the user can download the document.
    - The application provides a download link that triggers the download of the signed document in PDF format.

## Configuration Notes

- **Template ID**: The template used for the document is predefined and configured in the application. The Template ID is `e30d6e58c82d43f598e365420f3c665a048a7d81`.
- **SignNow SDK**: The application uses the SignNow SDK to handle document creation, sending, and status monitoring.

## Disclaimer

This application is provided for demonstration purposes and is not production-ready. It should be reviewed and customized to fit specific business requirements, including proper error handling, input validation, and security best practices.

## Java Implementation

The Java implementation of this sample application utilizes the SignNow SDK to manage the document workflow. The main components of the application include:

- **IndexController**: Handles HTTP requests for the application, including form submission, invite creation, status monitoring, and document download.
- **ApiClient**: Used to interact with the SignNow API for document operations.
- **Document and Template Operations**: Methods to clone templates, prefill fields, create embedded sending links, and download signed documents.

### Key Methods

- `createEmbeddedInviteAndReturnSendingLink`: Clones a document from a template, pre-fills fields, and generates an embedded signing link.
- `createDocumentFromTemplate`: Clones a document from a specified template ID.
- `prefillFields`: Prefills document fields with user input.
- `getEmbeddedSendingLink`: Retrieves an embedded sending link for the document.
- `getDocumentStatuses`: Fetches the current status of the document invites.
- `downloadDocument`: Downloads the signed document as a PDF.

This Java application serves as a practical example of integrating SignNow's e-signature capabilities into a custom workflow, demonstrating how to handle document lifecycle events programmatically.