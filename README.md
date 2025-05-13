# SignNow Java SDK Sample Application

This is a demonstration project that showcases how to use the [SignNow Java SDK](https://github.com/signnow/SignNowJavaSDK) to interact with the SignNow API.

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
