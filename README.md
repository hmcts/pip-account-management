# pip-account-management

## Table of Contents

- [Overview](#overview)
- [Features and Functionality](#features-and-functionality)
- [Azure B2C Integration](#azure-b2c-integration)
- [System Admin Accounts](#system-admin-accounts)
- [List Sensitivity Checking](#list-sensitivity-checking)
  - [Verified Users](#verified-users)
  - [Third Party Users](#third-party-users)
- [Account Application Process](#account-application-process)
- [Expired / Inactive Accounts](#expired--inactive-accounts)
- [Roles](#roles)
- [Architecture Diagram](#architecture-diagram)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
    - [General](#general)
    - [Local development](#local-development)
    - [Nice-to-haves](#nice-to-haves)
  - [Installation](#installation)
  - [Configuration](#configuration)
    - [Environment variables](#environment-variables)
      - [Getting all environment variables with python](#get-environment-variables-with-python-scripts)
      - [Additional Test secrets](#additional-test-secrets)
      - [Application.yaml files](#applicationyaml-files)
- [API Documentation](#api-documentation)
- [Examples](#examples)
  - [Requesting a bearer token](#requesting-a-bearer-token)
  - [Using the bearer token](#using-the-bearer-token)
- [Deployment](#deployment)
- [Creating or debugging of SQL scripts with Flyway](#creating-or-debugging-of-sql-scripts-with-flyway)
  - [Pipeline](#pipeline)
  - [Local](#local)
- [Monitoring and Logging](#monitoring-and-logging)
  - [Application Insights](#application-insights)
- [Security & Quality Considerations](#security--quality-considerations)
- [Test Suite](#test-suite)
  - [Unit tests](#unit-tests)
  - [Functional tests](#functional-tests)
- [Contributing](#contributing)
- [License](#license)

## Overview
`pip-account-management` is a microservice that deals with operations related to accounts, including interaction with Azure B2C for PI_AAD users. It also manages the audit functionality.

It sits within the Court and Tribunal Hearings Service (CaTH hereafter), written with Spring Boot/Java.

In practice, the service is usually containerized within a hosted kubernetes environment within Azure.

All interactions with `pip-account-management` are performed through the API (specified in [API Documentation](#api-documentation)) either as a standalone service or via connections to other microservices.

## Features and Functionality

- Creation of new verified PI_AAD users in Azure B2C and in the PiUser postgres table.
- Creation of new system admin accounts in Azure B2C and in the PiUser postgres table, and ensures the limit is not breached.
- For third parties and other IDAMs (e.g. CFT IDAM), creation of the user in the PiUser postgres database.
- Maintenance of the above users, such as updating roles and deletion of account and subscriptions when expired.
- Provides functionality to check if a user has permission to see a list based on the sensitivity and list type.
- Allows the creation of an audit log to log admin activities.
- Provides the ability to bulk create verified users (follows the same process as point 1)
- Triggers the sending of emails to users under various conditions (e.g New user, expiring, audit actions).
- Manages the account application process for PI_AAD users.
- Provides MI reporting endpoints, which are used to produce the MI report.
- Flyway for database modifications via SQL ingestion.
- Secure/Insecure Mode: Use of bearer tokens for authentication with the secure instance (if desired)
- OpenAPI Spec/Swagger-UI: Documents and allows users or developers to access API resources within the browser.
- Integration tests using TestContainers for dummy database operations.

## Azure B2C Integration

PI_AAD users, excluding third parties, will be stored in Azure B2C. Azure B2C provides OIDC which is used in our frontend to authenticate the users.

Account management interacts with Azure B2C - storing the user when they are created, updating them when their role changes, or deleting them if they have expired.

## System Admin Accounts

There is a limit to the number of system admin accounts that can be created. This is set using an environment variable: MAX_SYSTEM_ADMIN_ACCOUNTS.

If this is not set, the default maximum number of accounts is 4. The request will be rejected if an attempt is made that causes it to go over this limit.

## List Sensitivity Checking

### Verified Users

If a publication is PRIVATE or CLASSIFIED, account management is used to validate whether the user has permission to see the publication.

For PRIVATE, as long as the user exists in our database, then they are able to see the publication.

For CLASSIFIED, rules are applied based on the List Type and Provenance of the user. For reference, the provenance is which IDAM the user came from (PI_AAD, CFT_IDAM or CRIME_IDAM). The mappings of which provenance can see which list type is defined in the [enum file](./src/main/java/uk/gov/hmcts/reform/pip/account/management/model/ListType.java)

### Third Party Users

For third party users, a similar process is applied however is based on List Type + Third Party role. This is also defined in: [enum file](./src/main/java/uk/gov/hmcts/reform/pip/account/management/model/ListType.java)

## Account Application Process

When a user applies for a new PI_AAD verified account they go through an approval process. Account management deals with all CRUD operations associated with this process.

On Approval / Rejection, the uploaded ID file will be deleted.

## Expired / Inactive Accounts

For all users except for third party, there is an inactive account process to ensure accounts are deleted when they are no longer used.

For IDAM and Admin users, this uses the sign in date. For internal, this will use the verified date.

Users will be sent a reminder to login / re-verify 14 days before their account is due to expire.

If they do not complete this process within 14 days, their account will be deleted.

## Roles

Any endpoint that should require authentication, needs to be annotated either at controller or endpoint level with @IsAdmin.

## Architecture Diagram

![Architecture Diagram for pip-account-management](./account-man-arch.png)

The above diagram is somewhat simplified for readability (e.g. it does not include secure/insecure communications, but those are covered elsewhere).

## Getting Started

### Prerequisites

##### General

- [Java JDK 17](https://openjdk.org/projects/jdk/17/) - this is used throughout all of our services.
- REST client of some description (e.g. [Curl](https://github.com/curl/curl), [Insomnia](https://insomnia.rest/), [Postman](https://www.postman.com/)). Swagger-UI can also be used to send requests.
- Docker - used to run integration tests due to our use of [TestContainers](https://www.testcontainers.org/)

##### Local development

- [Azurite](https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azurite) - Local Azure emulator used along with Azure Storage explorer for local storage.
- [Azure Storage Explorer](https://azure.microsoft.com/en-us/products/storage/storage-explorer) - Used for viewing and storing blobs within an Azurite instance locally.

##### Nice-to-haves

- [pip-dev-env](https://github.com/hmcts/pip-dev-env) - This repo provides a development environment wherein ensure all microservices, as well as external services (e.g. postgres & redis) are all running in tandem within the service. It eases the development process and is particularly helpful when working with cross-service communication, as it also reduces strain on local performance from having many separate IDE windows open.
- PostgreSQL - for local development, it will help to install Postgres. Ensure your postgres instance matches the relevant [environment variables](#environment-variables). Most devs on the project are just using this within a docker container.
- Some means of interfacing with the postgres database either locally or remotely. Good options include [DataGrip](https://www.jetbrains.com/datagrip/), [pgAdmin](https://www.pgadmin.org/) or [psql](https://www.postgresql.org/docs/9.1/app-psql.html). This will allow you to verify the impacts of your requests on the underlying database.

### Installation

- Clone the repository.
- Ensure all required [environment variables](#environment-variables) have been set.
- Build using the command `./gradlew clean build`
- Start the service using the command `./gradlew bootrun` in the newly created directory.

### Configuration

#### Environment Variables

Environment variables are used by the service to control its behaviour in various ways.

These variables can be found within various separate CaTH Azure keyvaults. You may need to obtain access to this via a support ticket.
- Runtime secrets are stored in `pip-ss-{env}-kv` (where {env} is the environment where the given instance is running (e.g. production, staging, test, sandbox)).
- Test secrets are stored in `pip-bootstrap-{env}-kv` with the same convention.

##### Get environment variables with python scripts
Python scripts to quickly grab all environment variables (subject to Azure permissions) are available for both [runtime](https://github.com/hmcts/pip-dev-env/blob/master/get_envs.py) and [test](https://github.com/hmcts/pip-secret-grabber/blob/master/main.py) secrets.

##### Runtime secrets

Below is a table of currently used environment variables for starting the service, along with a descriptor of their purpose and whether they are optional or required.

| Variable                       | Description                                                                                                                                                                                                                                                            | Required? |
|:-------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| SPRING_PROFILES_ACTIVE         | If set equal to `dev`, the application will run in insecure mode (i.e. no bearer token authentication required for incoming requests.) *Note - if you wish to communicate with other services, you will need to set them all to run in insecure mode in the same way.* | No        |
| DB_HOST                        | Postgres Hostname                                                                                                                                                                                                                                                      | Yes       |
| DB_PORT                        | Postgres Port                                                                                                                                                                                                                                                          | Yes       |
| DB_NAME                        | Postgres Db name                                                                                                                                                                                                                                                       | Yes       |
| DB_USER                        | Postgres Username                                                                                                                                                                                                                                                      | Yes       |
| DB_PASS                        | Postgres Password                                                                                                                                                                                                                                                      | Yes       |
| APP_URI                        | Uniform Resource Identifier - the location where the application expects to receive bearer tokens after a successful authentication process. The application then validates received bearer tokens using the AUD parameter in the token.                               | No        |
| CLIENT_ID                      | Unique ID for the application within Azure AD. Used to identify the application during authentication.                                                                                                                                                                 | No        |
| TENANT_ID                      | Directory unique ID assigned to our Azure AD tenant. Represents the organisation that owns and manages the Azure AD instance.                                                                                                                                          | No        |
| CLIENT_SECRET                  | Secret key for authentication requests to the service.                                                                                                                                                                                                                 | No        |
| SUBSCRIPTION_MANAGEMENT_URL    | URL used for connecting to the pip-subscription-management service. Defaults to staging if not provided.                                                                                                                                                               | No        |
| PUBLICATION_SERVICES_URL       | URL used for connecting to the pip-publication-services service. Defaults to staging if not provided.                                                                                                                                                                  | No        |
| SUBSCRIPTION_MANAGEMENT_AZ_API | Used as part of the `scope` parameter when requesting a token from Azure. Used for service-to-service communication with the pip-subscription-management service.                                                                                                      | No        |
| PUBLICATION_SERVICES_AZ_API    | Used as part of the `scope` parameter when requesting a token from Azure. Used for service-to-service communication with the pip-publication-services service.                                                                                                         | No        |                                                                                                                                                                                      | No        |
| CLIENT_ID_B2C                  | The client id to use when authenticating with the Azure B2C instance.                                                                                                                                                                                                  | Yes       |
| CLIENT_SECRET_B2C              | The client secret to use when authenticating with the Azure B2C instance.                                                                                                                                                                                              | Yes       |
| TENANT_GUID_B2C                | The tenant id of the Azure B2C instance                                                                                                                                                                                                                                | Yes       |
| EXTENSION_ID                   | The extension ID for the users. This is used to store the role against a user.                                                                                                                                                                                         | Yes       |
| IDENTITY_ISSUER                | The identity issue of the Azure B2C instance.                                                                                                                                                                                                                          | Yes       |
| CONNECTION_STRING              | Connection string for connecting to the Azure Blob Storage service. Only required when running the application locally via Azurite.                                                                                                                                    | Yes       |
| STORAGE_ACCOUNT_NAME           | Azure storage account name used to construct the storage account endpoint. Not required when running the application locally.                                                                                                                                          | No        |
| MAX_SYSTEM_ADMIN_ACCOUNTS      | The max number of system admin accounts that can exist. Default is 4.                                                                                                                                                                                                  | No        |
| ENABLE_TESTING_SUPPORT_API     | Used to conditionally enable testing support API. Default to `false` for the production environment only.                                                                                                                                                              | No        |

##### Additional Test secrets

Secrets required for getting tests to run correctly can be found in the below table:

| Variable                       | Description                                                                                                                                                                                                                                     |
|:-------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| APP_URI                        | Uniform Resource Identifier - the location where the application expects to receive bearer tokens after a successful authentication process. The application then validates received bearer tokens using the AUD parameter in the token.        |
| CLIENT_ID                      | Unique ID for the application within Azure AD. Used to identify the application during authentication.                                                                                                                                          |
| CLIENT_SECRET                  | Secret key for authentication requests to the service.                                                                                                                                                                                          |
| PUBLICATION_SERVICES_AZ_API    | Used as part of the `scope` parameter when requesting a token from Azure. Used for service-to-service communication with the pip-publication-services service.                                                                                  |
| SUBSCRIPTION_MANAGEMENT_AZ_API | Used as part of the `scope` parameter when requesting a token from Azure. Used for service-to-service communication with the pip-subscription-management service.                                                                               |
| TENANT_ID                      | Directory unique ID assigned to our Azure AD tenant. Represents the organisation that owns and manages the Azure AD instance.                                                                                                                   |

#### Application.yaml files
The service can also be adapted using the yaml files found in the following locations:
- [src/main/resources/application.yaml](./src/main/resources/application.yaml) for changes to the behaviour of the service itself.
- [src/main/resources/application-dev.yaml](./src/main/resources/application-dev.yaml) for changes to the behaviour of the service when running locally.
- [src/test/resources/application-test.yaml](./src/test/resources/application-test.yaml) for changes to other test types (e.g. unit tests).
- [src/integrationTest/resources/application-functional.yaml](./src/functionalTest/resources/application-functional.yaml) for changes to the application when its running functional tests.

## API Documentation
Our full API specification can be found within our Swagger-UI page.
It can be accessed locally by starting the service and going to [http://localhost:6969/swagger-ui/swagger-ui/index.html](http://localhost:6969/swagger-ui/swagger-ui/index.html)
Alternatively, if you're on our VPN, you can access the swagger endpoint at our staging URL (ask a teammate to give you this).

## Examples
As mentioned, the full api documentation can be found within swagger-ui, but some of the most common operations are highlighted below.

Most of the communication with this service benefits from using secure authentication. While possible to stand up locally in insecure mode, to simulate a production environment it is better to use secure mode.
Before sending in any requests to the service, you'll need to obtain a bearer token using the following approach:

### Requesting a bearer token
To request a bearer token, sending a post request following this template:
```
curl --request POST \
  --url https://login.microsoftonline.com/{TENANT_ID}/oauth2/v2.0/token \
  --header 'Content-Type: multipart/form-data' \
  --form client_id={CLIENT_ID_FOR_ANOTHER_SERVICE} \
  --form scope={APP_URI}/.default \
  --form client_secret={CLIENT_SECRET_FOR_ANOTHER_SERVICE}\
  --form grant_type=client_credentials
```
You can copy the above curl command into either Postman or Insomnia and they will automatically be converted to the relevant formats for those programs.

*Note - the `_FOR_ANOTHER_SERVICE` variables need to be extracted from another registered microservice within the broader CaTH umbrella (e.g. [pip-data-management](https://github.com/hmcts/pip-data-management))*

### Using the bearer token
You can use the bearer token in the Authorization header when making requests. Here is an example using the create PI user endpoint:
```
curl --request POST \
  --url http://localhost:6969/account/add/pi \
  --header 'Authorization: Bearer {BEARER_TOKEN_HERE}' \
  --header 'Content-Type: application/json' \
  --header 'x-issuer-id: <UserIdOfTheUserSubmittingTheRequest>' \
  --data-raw '[
    {
    	"userProvenance": "PI_AAD",
    	"provenanceUserId": "<UserIdInTheProvenanceIDAM>",
    	"roles": "VERIFIED",
    }
  ]'
```

## Deployment
We use [Jenkins](https://www.jenkins.io/) as our CI/CD system. The deployment of this can be controlled within our application logic using the various `Jenkinsfile`-prepended files within the root directory of the repository.

Our builds run against our `dev` environment during the Jenkins build process. As this is a microservice, the build process involves standing up the service in a docker container in a Kubernetes cluster with the current staging master copies of the other interconnected microservices.

If your debugging leads you to conclude that you need to implement a pipeline fix, this can be done in the [CNP Jenkins repo](https://github.com/hmcts/cnp-jenkins-library)

## Creating or debugging of SQL scripts with Flyway
Flyway is used to apply incremental schema changes (migrations) to our database.

### Pipeline
Flyway is enabled on the pipeline, but is run at startup then switched off.

### Local
For local development, flyway is turned off by default. This is due to all tables existing within a single database locally. This can cause flyway to fail at startup due to mismatching scripts.

## Monitoring and Logging
We utilise [Azure Application Insights](https://learn.microsoft.com/en-us/azure/azure-monitor/app/app-insights-overview) to store our logs. Ask a teammate for the specific resource in Azure to access these.
Locally, we use [Log4j](https://logging.apache.org/log4j/2.x/).

In addition, this service is also monitored in production and staging environments by [Dynatrace](https://www.dynatrace.com/). The URL for viewing our specific Dynatrace instance can be had by asking a team member.

### Application Insights

Application insights is configured via the lib/applicationinsights.json file. Alongside this, the Dockerfile is configured to copy in this file and also download the app insights client.

The client at runtime is attached as a javaagent, which allows it to send the logging to app insights.

To connect to app insights a connection string is used. This is configured to read from the KV Secret mounted inside the pod.

It is possible to connect to app insights locally, although somewhat tricky. The easiest way is to get the connection string from azure, set it as an environment variable (APPLICATIONINSIGHTS_CONNECTION_STRING), and add in the javaagent as VM argument. You will also need to remove / comment out the connection string line the config.

## Security & Quality Considerations
We use a few automated tools to ensure quality and security within the service. A few examples can be found below:

- SonarCloud - provides automated code analysis, finding vulnerabilities, bugs and code smells. Quality gates ensure that test coverage, code style and security are maintained where possible.
- DependencyCheckAggregate - Ensures that dependencies are kept up to date and that those with known security vulnerabilities (based on the [National Vulnerability Database(NVD)](https://nvd.nist.gov/)) are flagged to developers for mitigation or suppression.
- JaCoCo Test Coverage - Produces code coverage metrics which allows developers to determine which lines of code are covered (or not) by unit testing. This also makes up one of SonarCloud's quality gates.
- PMD - Static code analysis tool providing code quality guidance and identifying potential issues relating to coding standards, performance or security.
- CheckStyle - Enforces coding standards and conventions such as formatting, naming conventions and structure.

## Test Suite

This microservice is comprehensively tested using both unit and functional tests.

### Unit tests

Unit tests can be run on demand using `./gradlew test`.

### Functional tests

Functional tests can be run using `./gradlew functional`

For our functional tests, we are using Square's [MockWebServer](https://github.com/square/okhttp/tree/master/mockwebserver) library. This allows us to test the full HTTP stack for our service-to-service interactions.

The functional tests also call out to Data Management in staging to retrieve publications.

## Contributing
We are happy to accept third-party contributions. See [.github/CONTRIBUTING.md](./.github/CONTRIBUTING.md) for more details.

## License
This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.
