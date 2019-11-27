# Inferno US Core R4 Reference Server

This is a FHIR reference implementation server supporting the US Core R4 IG and SMART Launches.

By default, you can browse the server at
[http://localhost:8080](http://localhost:8080), and the FHIR endpoint is at
[http://localhost:8080/r4](http://localhost:8080/r4)

## Running with Docker

The server runs using two containers, one for the server, and one for the
database. You can build the containers with `docker-compose build` and 
run both containers with `docker-compose up`.

## Loading US Core

- `docker-compose up` to run the server. The database has to initialize its data
  directory the first time it runs (or if its data has been deleted). If you see
  a big stack trace the first time you run `docker-compose up`, it is because
  the server is trying to connect to the db before it's finished initializing.
  Shut it down with `CTRL-C` and `docker-compose down` then restart the server
  with `docker-compose up` if this happens
- `gem install httparty` to install the upload scripts dependencies
- `ruby upload.rb` will upload the US Core resources

## Resetting the server

You can delete the server's data with `rm -rf fhir-pgdata`. The server must be
restarted after this.

## Creating Final Docker Images

- Once data has been loaded into the server, `./build-docker-images.sh` will
  create docker images for a FHIR server containing the loaded data.
- These images can be uploaded to Mitre's Artifactory with
  `./upload-docker-images.sh`. Once uploaded to artifactory, any Mitre employee
  can access and use these images without having to rebuild them.
- The preloaded version of the server can be run with `docker-compose -f
  docker-compose.artifactory.yml up`

## Running without Docker

If you cannot run docker, you will need to create a postgres database

Once you have done that, update the src/main/resources/hapi.properties to connect datasource.url, datasource.username, datasource.password, datasource.schema (or make your existing postgres db have the provided values)

Once that is done, you can run an instance of the fhir-reference server using `mvn jetty:run`.  You should be able to go to localhost:8080 to see information about the fhir server

To populate the database with sample data, run `ruby upload.rb` Note: make sure the jetty server is running, and that the FHIR_SERVER variable at the top of upload.rb is correct

## Using with Apps

Currently, there is no regestration process. To use with an app, use the default client ids:

To use as a public client, use `SAMPLE_PUBLIC_CLIENT_ID` as the client id

To use as a confidential client, use `SAMPLE_CONFIDENTIAL_CLIENT_ID` as the client id, and `SAMPLE_CONFIDENTIAL_CLIENT_SECRET` as the client secret


## Contact Us
The Inferno development team can be reached by email at inferno@groups.mitre.org. Inferno also has a dedicated [HL7 FHIR chat channel](https://chat.fhir.org/#narrow/stream/153-inferno).

## License
Copyright 2019 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
