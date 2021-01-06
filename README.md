# Inferno US Core R4 Reference Server

This is a FHIR reference implementation server supporting the US Core R4 IG and SMART Launches.

By default, you can browse the server at

[http://localhost:8080/reference-server](http://localhost:8080/reference-server), and the FHIR endpoint is at
[http://localhost:8080/reference-server/r4](http://localhost:8080/reference-server/r4)

## Running with Docker

The server runs using two containers, one for the server, and one for the
database. You can build the containers with `docker-compose build` and 
run both containers with `docker-compose up`.

Note that sometimes on the initial start up, the database initialization might cause the inferno reference server container to not start correctly, so you may need to stop the container with `docker-compose down` and restart it with `docker-compose up` .

## Resetting the server

You can delete the server's data by stopping the containers with `docker-compose down` and then running `docker volume rm inferno-reference-server_fhir-pgdata` to remove the existing volume. Note that the default data will be reloaded when starting the containers.


The database will be initially populated by the default initdb.sql script. To update the default initial data with the data in the current db container, run `docker-compose exec db pg_dump -U postgres postgres  > initdb.sql`

## Running without Docker

**System Requirements:**
The reference server requires Java 11 or above.

If you cannot run docker, you will need to create a postgres database.

Once you have done that, update the `src/main/resources/hapi.properties` to connect datasource.url, datasource.username, datasource.password, datasource.schema (or make your existing postgres db have the provided values).

Once that is done, you can run an instance of the fhir-reference server using `mvn jetty:run`.  You should be able to go to localhost:8080 to see information about the fhir server.

To populate the database with sample data, run `bundle install` then `bundle exec ruby upload.rb` *Note*: make sure the jetty server is running, and that the FHIR_SERVER variable at the top of upload.rb corresponds to your running fhir reference server.

## Using with Apps

Currently, there is no registration process. To use with an app, use the default client ids:

To use as a public client, use `SAMPLE_PUBLIC_CLIENT_ID` as the client id.

To use as a confidential client, use `SAMPLE_CONFIDENTIAL_CLIENT_ID` as the client id, and `SAMPLE_CONFIDENTIAL_CLIENT_SECRET` as the client secret.

To launch an app from the EHR go to `reference-server/app/app-launch`

## Custom Authentication Token

If you would like to execute requests without going through the process to get a token, you can set the environment variable `CUSTOM_BEARER_TOKEN` to a value of your choice. 

## Revoking a token

To revoke a token, go to `reference-server/oauth/token/revoke-token`

The tokens are currently saved in memory, so if the reference server is restarted, all existing tokens will be invalid

## Running Tests

Tests can be run with:

```shell
mvn test
```

## Running Checkstyle

Checkstyle can be run with:

```shell
mvn checkstyle:check
```

## Contact Us
The Inferno development team can be reached by email at inferno@groups.mitre.org. Inferno also has a dedicated [HL7 FHIR chat channel](https://chat.fhir.org/#narrow/stream/153-inferno).

## License
Copyright 2020 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
