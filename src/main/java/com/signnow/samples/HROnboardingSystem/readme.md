# HR Onboarding System Sample: Embedded Signing with Document Group

## Use Case Overview

This sample demonstrates how to use the SignNow PHP SDK to implement an **Embedded Signing** flow involving multiple users and multiple documents. It mimics a real-world HR onboarding process in which new employees must complete and sign several HR documents.

## Scenario: Multi-Document, Multi-Recipient Embedded Signing

### Step-by-step:
1. **User opens the onboarding form** — They input Employee Name, Employee Email, and HR Manager Email.
2. **User selects one or more HR templates** — e.g., NDA, Contract, I-9 Form.
3. **Application clones selected templates** — Each is turned into a signable document.
4. **Fields are pre-filled** — The Name and Email fields are populated with user input.
5. **Document Group is created** — All documents are grouped for unified processing.
6. **Embedded editor session is created** — For Recipient 1 (HR Manager) to review and optionally edit.
7. **Embedded signing session starts** — HR Manager signs.
8. **Employee is invited to sign** — After HR Manager completes their part.
9. **Signing status is polled** — The app checks if signing is complete.
10. **Documents become available for download** — After all signatures are collected.

## Technical Flow

1. **GET Request to `handleGet()`**
    - Renders the onboarding HTML form.

2. **POST Request to `handlePost()`**
    - Depending on `action` parameter, it routes to one of the following:
        - `create-embedded-editor`
            - Calls `createEmbeddedEditorLink()`
                - Clones each selected template via `createDocumentFromTemplate()`
                - Pre-fills fields via `prefillFields()`
                - Creates a document group via `createDocumentGroupFromDocuments()`
                - Generates embedded editor link via `createDocumentGroupEmbeddedEditorLink()`
        - `create-embedded-invite`
            - Calls `createEmbeddedInvite()` to create a multi-recipient invite
            - Then `getEmbeddedInviteLink()` to generate a signing URL
        - `invite-status`
            - Calls `getDocumentGroupInviteStatus()` to check current invite status
        - _default_
            - Calls `downloadDocumentGroup()` to provide the signed document as a PDF download

## Sequence of PHP Function Calls

1. **handleGet()** (on page load)
2. **handlePost()** (depending on user interaction):
    - If `create-embedded-editor`:
        - createEmbeddedEditorLink()
            - createDocumentFromTemplate()
            - prefillFields()
            - createDocumentGroupFromDocuments()
            - createDocumentGroupEmbeddedEditorLink()
    - If `create-embedded-invite`:
        - createEmbeddedInvite()
            - getDocumentGroup()
        - getEmbeddedInviteLink()
    - If `invite-status`:
        - getDocumentGroupInviteStatus()
            - getDocumentGroup()
    - If downloading PDF:
        - downloadDocumentGroup()

## Template Info
- This demo uses preloaded templates hosted on our **demo SignNow account**.
- Templates must be cloned to documents before use.
- Field and role mapping is handled programmatically.

## Disclaimer
This sample application is for **demonstration purposes only**. It uses static templates available in our **demo SignNow account**. Do not use this sample in production without appropriate adjustments for authentication, security, and dynamic document management.
