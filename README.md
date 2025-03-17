# Inferno US Core R4 Reference Server
[![Docker Image Version](https://img.shields.io/docker/v/infernocommunity/inferno-reference-server)](https://hub.docker.com/r/infernocommunity/inferno-reference-server)

This is an HL7® FHIR® reference implementation server supporting the US Core R4
IG and SMART Launches.

By default, you can browse the server at

[http://localhost:8080/reference-server](http://localhost:8080/reference-server), and the FHIR endpoint is at
[http://localhost:8080/reference-server/r4](http://localhost:8080/reference-server/r4)

## Running with Docker

The server runs using two containers, one for the FHIR server (`infernocommunity/inferno-reference-server`), and one for the
database. You can build the containers with `docker-compose build` and 
run both containers with `docker-compose up`.

Note that sometimes on the initial start up, the database initialization might cause the inferno reference server container to not start correctly, so you may need to stop the container with `docker-compose down` and restart it with `docker-compose up` .

## Resetting the server

You can delete the server's data by stopping the containers with `docker-compose down` and then running `docker volume rm inferno-reference-server_fhir-pgdata` to remove the existing volume. Note that the default data will be reloaded when starting the containers.


The database will be initially populated with the resources in `./resources/` the next time the server starts. This folder by default contains 3 files, but you can add additional files in the form of transaction Bundles or individual resources, or you can remove the original files to start with an empty server.  
If the server contains any `Patient` resources the initial loading process will be skipped, but you can force loading the files in this folder by setting the `FORCE_LOAD_RESOURCES`  environment variable to `true`. Note that if the original files are re-loaded in this way, this will result in duplicate data being populated.

## Running without Docker

**System Requirements:**
The reference server requires Java 11 or above.

If you cannot run docker, you will need to create a postgres database.

Once you have done that, update the `src/main/resources/hapi.properties` to connect datasource.url, datasource.username, datasource.password, datasource.schema (or make your existing postgres db have the provided values).

Once that is done, you can run an instance of the fhir-reference server using `./mvnw jetty:run` (Linux/Mac) or `.\mvnw.cmd jetty:run` (Windows).  You should be able to go to localhost:8080 to see information about the fhir server.

## Using with Apps

Currently, there is no registration process. To use with an app, use the default client ids:

To use as a public client, use `SAMPLE_PUBLIC_CLIENT_ID` as the client id.

To use as a confidential client, use `SAMPLE_CONFIDENTIAL_CLIENT_ID` as the client id, and `SAMPLE_CONFIDENTIAL_CLIENT_SECRET` as the client secret.

To launch an app from the EHR go to `reference-server/app/app-launch`

The Bulk Data Token Endpoint is `/reference-server/oauth/bulk-token`

The registered Bulk Data Client ID is `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InJlZ2lzdHJhdGlvbi10b2tlbiJ9.eyJqd2tzX3VybCI6Imh0dHA6Ly8xMC4xNS4yNTIuNzMvaW5mZXJuby8ud2VsbC1rbm93bi9qd2tzLmpzb24iLCJhY2Nlc3NUb2tlbnNFeHBpcmVJbiI6MTUsImlhdCI6MTU5NzQxMzE5NX0.q4v4Msc74kN506KTZ0q_minyapJw0gwlT6M_uiL73S4`.

See [SMART Clients](#smart-clients) below for more detail on the clients.

`./resources/` provides the following resources:
 - Patients with ids `85` and `355`
 - a Group with id `1a`, containing patients `85` and `355` as members.

`upload.rb` includes the resources above, except the Group with id `1a`.

## Custom Authentication Token

If you would like to execute requests without going through the process to get a token, you can set the environment variable `CUSTOM_BEARER_TOKEN` to a value of your choice. 

## Revoking a token

To revoke a token, go to `reference-server/oauth/token/revoke-token`

The tokens are currently saved in memory, so if the reference server is restarted, all existing tokens will be invalid

## Running Read-Write Mode

By default, the Dockerized server runs in read-only mode – meaning, operations
modifying the state of the server are not supported. For example, requests to
CREATE, UPDATE, or DELETE a resource will receive a `405 Method Not Allowed`
error. To adjust this while running Docker, change the `READ_ONLY` environment
variable to `false` in `./docker-compose.yml`. If running without Docker, run
`./mvnw jetty:run -DREAD_ONLY=false` when starting the server.

## SMART Clients
By default, the server contains four sample SMART clients; one for each of four launch/authentication methods:
- Standalone Launch, Public Client
- Standalone Launch, Confidential Symmetric Authentication
- Standalone Launch, Confidential Asymmetric Authentication
- Backend Services (ie, Bulk Data)

These four clients are defined in `src/main/resources/default_clients.json` and may be customized to change the ID, client secret, etc. If the ID for a client is changed then the corresponding setting should be changed in `src/main/resources/hapi.properties`

Additional clients may be defined by putting JSON files into the `./clients` directory. It is recommended to start with one of the existing clients and modifying fields as appropriate. Customizations specific to this reference server will all be located in the `customSettings` field, which currently only contains a field `patientPickerIds` - a list of strings that represent Patient IDs which will be used to filter the Patient Picker UI. 

A brief overview of the schema:

```
{
  "clientId": "client ID",
  "authorizationGrantType": "authorization_code" | "client_credentials",
  "clientAuthenticationMethod": "none" | "client_secret_basic" | "private_key_jwt",
  "clientSecret": "client secret, only used for client_secret_basic auth method",
  "providerDetails": {
    "jwkSetUri": "location of JWKS, only used for private_key_jwt auth method"
  },
  "customSettings": {
    "patientPickerIds": ["id1", "id2", ...]
  }
}
```

Note on `providerDetails.jwkSetUri`:
- for a web-based JWKS, use the URL here
- for a file-based JWKS, use a `file:///` URL
- for a JWKS located under `src/main/resources`, use `resource:/file_path`, eg `resource:/inferno_client_jwks.json`


## Running Tests

Tests can be run with:

```shell
./mvnw test
```

## Running Checkstyle

Checkstyle can be run with:

```shell
./mvnw checkstyle:check
```

## Contact Us
The Inferno development team can be reached by email at inferno@groups.mitre.org. Inferno also has a dedicated [HL7 FHIR chat channel](https://chat.fhir.org/#narrow/stream/153-inferno).

## License

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

## Trademark Notice

HL7, FHIR and the FHIR [FLAME DESIGN] are the registered trademarks of Health
Level Seven International and their use does not constitute endorsement by HL7.
