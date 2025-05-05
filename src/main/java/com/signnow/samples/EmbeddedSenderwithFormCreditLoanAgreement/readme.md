
# Embedded Sender With Form: Credit Loan Agreement

## Use Case Overview

This example demonstrates how to use the SignNow Java SDK to build an Embedded Sender flow with a pre-filled form.
In this case, a user (typically an internal employee or agent) fills out their name and optionally an email, signs a Credit Loan Agreement as the first recipient, and then sends it to others.

It uses SignNow’s embedded sending capabilities with a predefined template.

## Scenario: Embedded Sender with Form

### Steps:
1. **User fills out a form**:
    - Required: Name
    - Optional: Email

2. **Document is cloned from template**:
    - Pre-fills fields with form data.

3. **Embedded sender session opens**:
    - The first recipient (the sender) signs the document.
    - Email of Recipient 2 is pre-filled from the form.

4. **Status view**:
    - After signing, a status page shows recipients and their status.
    - A document download option is available.

5. **Finish page**:
    - User can close the demo.

## Technical Flow

1. **Form (Step 1)**:
    - POST to `action=create-embedded-invite`.

2. **Template Clone & Prefill (Step 2)**:
    - Uses template ID `f8768c13d2f34774b6ce059e3008d9fc04d24378`.
    - Fields pre-filled via `DocumentPrefillPut`.

3. **Embedded Sender Link (Step 3)**:
    - Link generated using `DocumentEmbeddedSendingLinkPost`.

4. **Document Status View (Step 4)**:
    - Email statuses fetched from document object.

5. **PDF Download (Step 5)**:
    - `DocumentDownloadGet` with type = `collapsed`.

## Notes

- The signer email for Recipient 1 is pulled from `config('signnow.api.signer_email')`.
- This example uses SignNow SDK authentication.
- `updateSignNowLink()` ensures embedded session link is token-safe.

## Template

- **Template ID**: `f8768c13d2f34774b6ce059e3008d9fc04d24378`
- **Template Name**: `Credit Loan Agreement`
