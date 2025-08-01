# PrefillAndOneClickSendingAgreement Sample Application

## Use Case Overview

This sample application demonstrates how to use the SignNow platform to implement a **Form and One Click Send** workflow. It is designed for scenarios where a single recipient needs to sign a document based on form input. The application streamlines the process by allowing users to fill out a form, send an invitation for signing, and monitor the signing status with minimal steps.

## Scenario: ISV with Form and One Click Send

### Step-by-step:

1. **Form Input**
    - The user is presented with a web form containing the following fields:
        - **Name**: A required text input field.
        - **Email**: A required email input field.
    - After completing the form, the user clicks the **Continue** button to proceed.

2. **Document Pre-fill and Invitation Sending**
    - Upon clicking **Continue**, the application pre-fills the **Name** field in the document using the form input.
    - An invitation is sent to the email address provided in the form.
    - The user is then automatically redirected to the next step.

3. **Send Status Page**
    - The user is directed to a status monitoring page to track the progress of the signing process.
    - A **Refresh** button is available for manually updating the status.
    - The page displays the current status of the document (e.g., sent, viewed, signed).
    - Once the document is signed, the user can download the final signed document.

## Technical Flow

1. **Form Submission**
    - The application captures form data upon submission and uses it to pre-fill document fields.
    - The backend processes the data, clones the document from a predefined template, and sends an invitation to the recipient's email.

2. **Invitation Sending**
    - The application utilizes the template named "Membership Program Agreement (Retail)" with the TemplateId: 'e30d6e58c82d43f598e365420f3c665a048a7d81'.
    - The invitation is sent to the email address provided in the form.

3. **Status Monitoring**
    - The user is redirected to the status page to monitor the signing process.
    - The application periodically checks the status of the invitation and updates the user interface.
    - The user can manually refresh the status by clicking the **Refresh** button.

4. **Document Download**
    - Once the document is signed, the user can download the signed document.
    - The user clicks the **Download Document** button to retrieve the completed document.

## User Interaction Summary

- **Initial Interaction**: User fills out the form with their name and email.
- **Invitation Process**: The application pre-fills the document and sends an invitation to the provided email.
- **Status Monitoring**: User monitors the signing status and refreshes the page as needed.
- **Completion**: User downloads the signed document once the process is complete.

This flow ensures a seamless experience for users who need to quickly send documents for signing with minimal steps involved.

## Configuration Notes

- **Template ID**: The template used is "Membership Program Agreement (Retail)" with TemplateId: 'e30d6e58c82d43f598e365420f3c665a048a7d81'.
- **SignNow SDK**: This application leverages the SignNow platform for document management and signing processes.

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

This sample application is provided **as-is** for demonstration purposes. It is not production-ready and should be reviewed for proper error handling, input validation, security best practices, and customized to fit real-world business requirements.

## Java Example Code

The Java example code for the PrefillAndOneClickSendingAgreement application is structured to handle form submissions, send invitations, monitor signing status, and download signed documents. The key components include:

- **IndexController**: Handles HTTP GET and POST requests, processes form data, and manages the document workflow.
- **ApiClient**: Authenticates and communicates with the SignNow API to perform operations like cloning templates, pre-filling fields, sending invites, and downloading documents.

The code demonstrates how to integrate with the SignNow API using Java, providing a foundation for building a robust document signing application.