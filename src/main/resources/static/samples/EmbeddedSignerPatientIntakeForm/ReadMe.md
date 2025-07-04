# Embedded Signer Sample: Patient Intake Form

## Use Case Overview

This sample demonstrates how to use the SignNow PHP SDK to implement an **Embedded Signer** workflow. In this flow, the user signs a pre-defined document within your application and is then redirected to a confirmation screen where they can download the completed document.

This specific example mimics a real-world healthcare scenario where a **Patient Intake Form** must be completed and signed digitally.

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
    - The document is cloned from a **preloaded template** on our **demo SignNow account**

3. **Embedded Invite Generation**
    - In order to get an **embedded invite link**, an **embedded invite** must be explicitly created.
    - The app retrieves the signer's role ID from the cloned document.
    - An invite is created and assigned to the signer's email.

4. **Signing Link Generation**
    - A signing link is generated using `DocumentInviteLinkPost`, and the redirect URL is appended.
    - The user is redirected to this link for signing.

5. **POST Request for Download**
    - After signing, the user is redirected to the `download-container` view.
    - Clicking the download button sends a `POST` request with the `document_id`.
    - The app fetches the signed PDF and sends it to the user for download.

## Notes
- The template ID used in this demo: `4078899bbf4446eea7ae0e157e742bbd93be191f`.
- The template is a "Traditional Chinese Medicine Patient Intake Form".
- Signer's email is pulled from config: `config('signnow.api.signer_email')`.
- All API requests are made via the official SignNow SDK using the `.env` credentials.

## Disclaimer
This example is for demonstration purposes only. The embedded flow relies on a static template hosted on our demo SignNow account and should not be used in production without appropriate customization.
