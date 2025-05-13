# Embedded Signer Sample: Veterinary Clinic Intake Form (Java Version)

## Use Case Overview

This sample demonstrates how to use the SignNow **Java SDK** to implement an **Embedded Signer** workflow for a veterinary clinic. The user completes and signs a pre-defined "Veterinary Clinic Intake Form" directly within the application and is then redirected to a confirmation screen where they can download the completed document.

The use case simulates a real-world scenario in the **consumer services** domain, where intake forms must be completed and signed digitally.

## Scenario: Embedded Signer Without Form

### Step-by-Step Interaction:

1. **Launch Signing Flow**

    * The user accesses the application.
    * If the URL does not contain `?page=finish`, the app:

        * Clones a predefined template (Veterinary Clinic Intake Form).
        * Creates a document instance from it.
        * Generates an embedded invite.
        * Redirects the user to the embedded signing session.

2. **Fill Out and Sign**

    * The embedded interface allows the user to complete and sign the form directly in the app.

3. **Redirect to Finish Page**

    * After signing, the user is redirected to a confirmation page (`page=finish`).
    * The UI confirms completion and presents a download option.

4. **Download Signed Document**

    * Clicking the download button triggers a POST request containing the `document_id`.
    * The server responds with the signed PDF, which is downloaded to the user's device.

## Technical Flow

### 1. **GET Request Handling**

* If `page=finish`:

    * Static `index.html` page is served.
* Otherwise:

    * The app:

        * Clones the template (`da62bd76f1864e1fadff6251eca8152977ee3486`).
        * Retrieves the recipient role from the cloned document.
        * Sends an embedded invite.
        * Generates a secure signing link.
        * Appends a `redirect_uri` and redirects the user.

### 2. **Template Cloning**

* Uses SignNow API to clone the static template.
* Required because templates cannot be signed directly.

### 3. **Embedded Invite Creation**

* Creates a single embedded invite with a signer email.
* Retrieves the correct `role_id` for the signer.

### 4. **Generate and Redirect to Signing Link**

* Uses `DocumentInviteLinkPostRequest`.
* Includes a redirect URL back to `page=finish` with `document_id`.

### 5. **Download the Document (POST Request)**

* `document_id` is passed in the request body.
* The backend fetches the signed PDF and returns it for download.

## Notes

* Template ID used: `da62bd76f1864e1fadff6251eca8152977ee3486`.
* Template Name: **Veterinary Clinic Intake Form (Consumer Services)**.
* The signer's email is currently hardcoded (`signer@example.com`).
* All API interactions are handled via the official **SignNow Java SDK** using pre-authenticated credentials.

## Disclaimer

This example is for demonstration purposes only. The embedded signing experience is based on static demo templates and should be adapted with custom logic and dynamic configuration for production use.
