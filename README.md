# SignNow Java SDK Sample Application

This is a demonstration project that showcases how to use the [SignNow Java SDK](https://github.com/signnow/SNJavaSDK) to interact with the SignNow API.

Each sample represents a standalone use case and is implemented in a separate directory under:

```
src/main/java/com/signnow/samples
```

The static frontend resources for each sample (e.g. HTML, JS, CSS) are located in:

```
src/main/resources/static/samples
```

## Getting Started

### 1. Set Up Your Environment

Copy the provided `.env.example` file to `.env`:

```bash
cp .env.example .env
```

Edit the `.env` file and replace the placeholder values with your actual SignNow API credentials and settings.

### 2. Build and Run the Application

You can build and run the application using Docker:

```bash
docker build -t java-sample-app .
docker run -v $(pwd)/.env:/.env -p 8080:8080 java-sample-app
```

Once the app is running, access it via:

```
http://localhost:8080/samples/{exampleName}
```

Replace `{exampleName}` with the name of the specific example you want to run (e.g. `DocumentInvite`, `PrefillForm`, etc.).

## Routing Logic

All requests are routed through a single controller: `RoutingController`.

* **GET /samples/{exampleName}**: Loads and executes the corresponding `IndexController` class from the example folder.
* **POST /api/samples/{exampleName}**: Handles form submissions for the given example.

Only example names matching `^[a-zA-Z0-9_]+$` are allowed for security reasons.

## Structure of a Sample Example

Each sample should implement the `ExampleInterface` and provide its own `IndexController` class with `handleGet()` and `handlePost()` methods.

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
