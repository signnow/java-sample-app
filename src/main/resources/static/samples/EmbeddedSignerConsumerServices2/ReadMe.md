# Embedded Signer Consumer Services 2: Veterinary Clinic Intake Form

## Use Case Overview

"Embedded Signer Consumer Services 2" is a sample Java application demonstrating how to implement an **Embedded Signer** workflow using the SignNow API. This app allows users to complete and sign a "Veterinary Clinic Intake Form" seamlessly within the application. The process is designed to provide a smooth user experience from form completion to document download.

## Scenario: Embedded Signer Without Form

### Step-by-Step User Interaction Flow

1. **Access the Application**
   - **User Action**: The user opens the application on their device.
   - **System Response**: The app checks for any query parameters to determine the user's current stage. If none are present, it assumes the user is beginning the signing process.

2. **Initiate Embedded Signing Session**
   - **User Action**: The user is directed to start the signing process for the "Veterinary Clinic Intake Form."
   - **System Response**: The app clones the predefined template to create a new, signable document instance, as templates cannot be signed directly.

3. **Complete the Document**
   - **User Action**: The user fills out all necessary fields in the form using the embedded interface.
   - **System Response**: The interface ensures a user-friendly environment for entering required information, ensuring all fields are completed before proceeding.

4. **Sign the Document**
   - **User Action**: After completing the form, the user signs the document using the embedded signing feature.
   - **System Response**: The app integrates the signing process seamlessly, allowing the user to sign the document directly within the application.

5. **Redirect to Finish Screen**
   - **User Action**: Upon signing the document, the user is automatically redirected to a "Finish" page.
   - **System Response**: This page confirms that the document has been successfully signed and provides further instructions for downloading the document.

6. **Download the Completed Document**
   - **User Action**: On the "Finish" page, the user is presented with an option to download the completed and signed PDF document.
   - **System Response**: The user clicks the "Download" button, which triggers a request to fetch the signed document.

7. **Document Delivery**
   - **User Action**: The user waits for the document to be processed and delivered.
   - **System Response**: The app processes the download request, retrieves the signed PDF, and delivers it to the user, allowing them to save it locally.

## Technical Flow

1. **GET Request Initiation**
   - The user accesses the app, which checks for a `page` query parameter.
   - If `page=finish`, the app displays the "Finish" page with a download option.
   - Otherwise, the app initiates an embedded signing session:
     - Clones the "Veterinary Clinic Intake Form" template.
     - Creates an embedded invite for the user.
     - Generates a secure signing link and redirects the user to it.

2. **Template Cloning**
   - The app clones the predefined template to create a signable document instance.
   - This step is necessary as templates cannot be signed directly.

3. **Embedded Invite Generation**
   - The app retrieves the signer's role ID from the cloned document.
   - An embedded invite is created and assigned to the user's email.

4. **Signing Link Generation**
   - A signing link is generated using the SignNow API.
   - The user is redirected to this link to complete and sign the document.

5. **POST Request for Download**
   - After signing, the user is redirected to the "Finish" page.
   - Clicking the download button sends a `POST` request with the `document_id`.
   - The app fetches the signed PDF and delivers it to the user for download.

## Additional Notes
- **Template ID**: The app uses the template ID `da62bd76f1864e1fadff6251eca8152977ee3486` for the "Veterinary Clinic Intake Form."
- **Security**: All interactions with the SignNow API are conducted using secure credentials to ensure data integrity and user privacy.
- **Customization**: This flow is designed for demonstration purposes and can be customized for specific production needs, including additional security measures and user-specific configurations.

## Disclaimer
This flow is designed for demonstration purposes. The embedded signing process relies on a static template and should be customized for production use.