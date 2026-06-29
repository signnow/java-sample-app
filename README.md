# SignNow Java Sample App

[![Java](https://img.shields.io/badge/java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4-brightgreen)](https://spring.io/projects/spring-boot)
[![SignNow SDK](https://img.shields.io/badge/SignNow_SDK-3.5+-light)](https://github.com/signnow/SNJavaSDK)
[![License](https://img.shields.io/badge/license-MIT-green)](./LICENSE)

A Spring Boot application demonstrating the SignNow API via the official [SignNow Java SDK](https://github.com/signnow/SNJavaSDK).

## Quick Start

### 1. Enter the directory

```bash
cd SampleApps/java-sample-app
```

### 2. Configure environment

```bash
cp .env.example .env
```

Edit `.env` and fill in your SignNow credentials:

| Variable | Example | Description |
|---|---|---|
| `SIGNNOW_API_HOST` | `https://api.signnow.com` | Production or sandbox URL |
| `SIGNNOW_API_BASIC_TOKEN` | `c2lnbk5vdy4...` | Base64 token from [API Dashboard](https://app.signnow.com/webapp/api-dashboard/keys) |
| `SIGNNOW_API_USERNAME` | `you@example.com` | Your SignNow account email |
| `SIGNNOW_API_PASSWORD` | `••••••` | Your SignNow account password |
| `SIGNNOW_DOWNLOADS_DIR` | `/tmp/signnow-downloads` | Where downloaded documents are cached |
| `APP_BASE_URL` | `http://localhost:8080` | Public base URL used in `redirect_uri` |
| `SIGNNOW_SIGNER_EMAIL` | `signer@example.com` | Default embedded signer email |
| `SIGNNOW_DEMO_USER_EMAIL` | `demo@example.com` | Demo sender email (used in some samples) |
| `SIGNNOW_DEMO_USER_PASSWORD` | `••••••` | Demo sender password |

### 3. Run with Docker

```bash
docker build -t java-sample-app .
docker run -v $(pwd)/.env:/.env -p 8080:8080 java-sample-app
```

### 4. Run locally (no Docker)

```bash
mvn spring-boot:run
```

Requires Java 17+ and Maven 3.9+ on `PATH`. The app reads `.env` from the working directory.

### 5. Open a sample

```
http://localhost:8080/samples/EmbeddedSignerConsentForm
```

`http://localhost:8080/samples` lists all available samples.

## Available Samples (20)

| Sample | Description |
|---|---|
| EmbeddedSignerConsentForm | Consent form with single embedded signer |
| EmbeddedSenderWithoutFormFile | Sales proposal — embedded sender |
| EmbeddedSenderWithFormCreditLoanAgreement | Credit loan agreement |
| EmbeddedSignerConsumerServices | Veterinary intake form |
| EmbeddedSignerPatientIntakeForm | Patient intake (healthcare) |
| EmbeddedSignerWithFormInsurance | Insurance claim form |
| MedicalInsuranceClaimForm | Medical insurance claim |
| EmbeddedEditingAndSigningDG | Document generation: edit + sign |
| EmbeddedSenderWithFormAndFirstSigner | Sender is also first signer |
| EmbeddedSenderWithFormDG | Document generation variant |
| EmbeddedSenderWithFormDGAdjunct | DG adjunct form |
| EmbeddedSenderWithFormDGConstr | DG construction form |
| ISVWithFormAndOneClickSendBasicPrefill | ISV one-click send, basic prefill |
| ISVWithFormAndOneClickSendMergeFields | ISV one-click send, merge fields |
| HROnboardingSystem | HR onboarding multi-document flow |
| UploadEmbeddedSender | Upload PDF + embedded sender |
| PrefillAndEmbeddedSendingAgreement | Prefill + embedded send |
| PrefillAndOneClickSendingAgreement | Prefill + one-click send |
| EVDemoSendingAnd3EmbeddedSigners | Real estate: 3 sequential embedded signers |
| UploadEmbeddedEditingAndInvite | Upload PDF, embedded edit, invite |

## Project Structure

```
src/main/java/com/signnow/
  javasampleapp/
    JavaSampleAppApplication.java   Spring Boot entry point
    ExampleInterface.java           Sample contract (handleGet + handlePost)
    controllers/
      RoutingController.java        GET /samples/:name, POST /api/samples/:name
    config/
      AppConfig.java                Reads .env; provides AppConfig.sampleUrl()
  samples/
    <SampleName>/
      IndexController.java          Implements ExampleInterface

src/main/resources/
  static/
    samples/<SampleName>/
      index.html                    UI served on GET /samples/<SampleName>
    css/, img/                      Shared static assets
  application.properties            Spring config

src/test/
  JavaSampleAppApplicationTests.java  Context load test
  controllers/RoutingControllerTest.java  MockMvc routing tests
```

## Routing

| Method | Path | Handler |
|---|---|---|
| GET | `/samples/{name}` | `samples/<name>/IndexController.handleGet` |
| POST | `/api/samples/{name}` | `samples/<name>/IndexController.handlePost` |
| GET | `/css/*`, `/img/*`, etc. | Shared static files |

Sample names must match `^[a-zA-Z0-9_]+$`. `RoutingController` loads each sample's `IndexController` by class name convention at request time — no whitelist or registration needed.

## Add a New Sample

1. Create `src/main/java/com/signnow/samples/MyNewSample/IndexController.java`:
   ```java
   package com.signnow.samples.MyNewSample;

   import com.signnow.javasampleapp.ExampleInterface;
   import org.springframework.http.ResponseEntity;
   import org.springframework.stereotype.Controller;
   import java.io.IOException;
   import java.util.Map;

   @Controller
   public class IndexController implements ExampleInterface {
       @Override
       public ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException {
           try (var in = getClass().getResourceAsStream("/static/samples/MyNewSample/index.html")) {
               return ResponseEntity.ok().header("Content-Type", "text/html")
                       .body(new String(in.readAllBytes()));
           }
       }

       @Override
       public ResponseEntity<?> handlePost(String formData) throws Exception {
           // parse formData as JSON and implement your logic
           return ResponseEntity.ok("{}");
       }
   }
   ```
2. Create `src/main/resources/static/samples/MyNewSample/index.html`.
3. Restart the application — `RoutingController` discovers the new controller automatically.

## Tests

```bash
mvn test
```

2 test classes:
- `JavaSampleAppApplicationTests` — Spring context loads without errors
- `RoutingControllerTest` — 6 MockMvc tests covering name validation (`^[a-zA-Z0-9_]+$`), 404 for unknown samples, and routing dispatch

Tests do **not** exercise real SignNow API calls (live credentials required).

## SDK Notes

Known constraints in the SignNow Java SDK that affect this codebase:

**No multipart support in `RoutingController`** — `handlePost` receives `@RequestBody String formData` (raw JSON), not multipart. Samples that conceptually "upload" a file instead read a pre-bundled PDF from `src/main/resources/static/samples/<Name>/`. See `UploadEmbeddedSender` for the pattern.

**`Invite` constructor order** — `new Invite(email, roleId, order, firstName, lastName)`. There is no shorter overload; all five parameters are required.

**`DocumentInvitePostResponse.getData()`** — Returns a collection; each item exposes `.getRoleId()` (to match against role IDs you looked up) and `.getId()` (the invite ID needed for `DocumentInviteLinkPostRequest`).

**`DocumentInviteLinkPostRequest` constructor** — `(authType, linkExpiration)` where `authType` is typically `"none"`. Chain `.withDocumentId()` and `.withFieldInviteId()` before sending.

**`DocumentEmbeddedEditorLinkPostRequest` constructor** — `(redirectUri, redirectTarget, linkExpiration)`. Chain `.withDocumentId()` before sending. Response: `getData().getUrl()`.

## Tech Stack

- Java 17
- Spring Boot 3.4.1
- `signnow-java-sdk` 3.5.1 (from Maven Central)
- Maven 3.9
- JUnit 5 + Spring MockMvc (tests)
- Docker: multi-stage `maven:3.9-eclipse-temurin-17` → `eclipse-temurin:17-jre`

## GitHub Copilot Extension

Get AI-powered SignNow code suggestions in your IDE:
[github.com/apps/signnow](https://github.com/apps/signnow) — start prompts with `@signnow`.

## License

See [LICENSE](./LICENSE).
