# Embedded Editing & Signing with Document Group Template (DGT)

This README describes an end‑to‑end sample that demonstrates how to build an **Embedded Editing → Embedded Signing** workflow around a **Document Group Template (DGT)** using the SignNow API & Java SDK.

---

## Use Case Overview

This sample covers a common scenario where an application needs to clone a multi‑document template, pre‑fill signer data, let an operator make last‑minute edits **in‑place** (*embedded editor*), then immediately sign as the first recipient while the remaining recipients receive **email** invites. Finally, the app tracks signing status and offers a one‑click download of the merged, signed PDF.

---

## Scenario: Multi‑Signer Document Group with Embedded Editing & Signing

### Pages & Flow (High‑Level)

1. **Page 1: Collect Signer Information**
   User enters Signer 1 and Signer 2 names/emails. After submit, the backend clones the DGT, pre‑fills fields, assigns recipients, and returns an **Embedded Edit Link**.
2. **Page 2: Embedded Editing**
   Browser is redirected straight to the embedded editor so the user can review the Document Group content.
3. **Page 3: Auto‑Create Embedded Invite & Signing**
   When the user clicks **Send** in the editor, SignNow returns to `page2-embedded-sending`. The page shows a spinner while the backend automatically creates an embedded invite for the **Contract Preparer** and instantly redirects the browser to the signing session. *Recipient 1* and *Recipient 2* are set to `deliveryType: email`, so they sign via email later.
4. **Page 4: Status & Download**
   The app polls invite status and enables **Download Document** once all recipients finish.

---

## Step‑by‑Step Overview

1. **Fill the signer form** (`/samples/EmbeddedEditingAndSigningDG`).
2. **Embedded editing** – redirected to SignNow editor for the entire Document Group.
3. **Send** inside the editor → backend **creates embedded invite** for Contract Preparer and redirects to signing link.
4. **Embedded signing** – Contract Preparer signs immediately; other recipients receive email invites.
5. **Status page** – track progress and download the final PDF.

---

## Technical Flow

| #     | Action                                                                | Responsibility                              | Code Location              |
| ----- | --------------------------------------------------------------------- | ------------------------------------------- | -------------------------- |
| **1** | Collect signer names/emails                                           | Frontend form                               | `index.blade.php`          |
|       | Clone DGT, pre‑fill fields, update recipients, build **Edit Link**    | `SampleController::submitSignerInfo()`      | `SampleController.php`     |
| **2** | Redirect → embedded editor                                            | Frontend JS                                 | `index.blade.php`          |
| **3** | Editor **Send** → return to `page2-embedded-sending`                  | SignNow                                     | Redirect URL in controller |
|       | POST `action=create-embedded-invite`                                  | Frontend JS                                 | `index.blade.php`          |
|       | Create embedded invite for Contract Preparer, generate `signing_link` | `SampleController::createEmbeddedInvite()`  | `SampleController.php`     |
|       | Redirect → embedded signing                                           | Frontend JS                                 | `index.blade.php`          |
| **4** | Signer completes → redirect to `page4-status-download`                | SignNow                                     | Redirect URL in invite     |
| **5** | Poll invite status & enable **Download**                              | Frontend JS                                 | `index.blade.php`          |
|       | Stream signed PDF (`/download-doc-group`)                             | `SampleController::downloadDocumentGroup()` | `SampleController.php`     |

---

## Sequence of Possible Function Calls

1. **handleGet()** → serves Page 1 form.
2. **submitSignerInfo()** → clone DGT, pre‑fill fields, update recipients, create Edit Link.
3. **createEmbeddedInvite()** → create embedded invite for Contract Preparer, return signing link.
4. **inviteStatus()** → poll signer status.
5. **downloadDocumentGroup()** → stream final PDF.

---

## Template Info

* The sample uses a pre‑configured DGT stored in a demo SignNow account.
* Field names **“Signer 1 Name”** & **“Signer 2 Name”** (previously “Text Field 18/19”) and roles **Recipient 1/2** & **Contract Preparer** must exist in your template or be adjusted accordingly.

---

## Configuration

Before running the sample you need to set several environment variables in your `.env` (or export them in the shell that starts the application).  
These values come from your SignNow developer account:

| Variable | Description |
| -------- | ----------- |
| `SIGNNOW_CLIENT_ID` | OAuth2 client ID for your app |
| `SIGNNOW_CLIENT_SECRET` | OAuth2 client secret |
| `SIGNNOW_USERNAME` | Username (e-mail) of the SignNow user used for API calls |
| `SIGNNOW_PASSWORD` | Password of that user |

```

> **Note**: The sample uses a **hard-coded demo Document Group Template ID** inside `SampleController`.  

---

## Disclaimer

This code is provided **for demonstration purposes only**. Add authentication, input validation, comprehensive error handling, and persistent storage before deploying to production.

---

## Quick Start (TL;DR)

```text
GET  /samples/EmbeddedEditingAndSigningDG           # signer form
POST /api/samples/EmbeddedEditingAndSigningDG       # submit signer info
→ redirect  (Embedded Editor)
→ redirect  ?page=page2-embedded-sending            # auto‑invite & redirect
→ redirect  (Embedded Signing — Contract Preparer)
→ redirect  ?page=page4-status-download             # status + download
```
