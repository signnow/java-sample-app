# java-sample-app

Copy `.env.example` to `.env` and put there your credentials.

To run it execute:

```
mvn clean install
mvn package
docker build -t java-sample-app .
docker run -v $(pwd)/.env:/.env -p 8080:8080 java-sample-app 
```
