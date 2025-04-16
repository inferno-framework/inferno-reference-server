FROM maven:3-eclipse-temurin-17 AS mavenbuild

COPY pom.xml /home/app/
COPY src /home/app/src
COPY config /home/app/config

# RUN curl -ksSL https://gitlab.mitre.org/mitre-scripts/mitre-pki/raw/master/tool_scripts/install_certs.sh | sh

RUN mvn -q -f /home/app/pom.xml package -DskipTests

FROM jetty:12.0.8-jdk17

USER root
# add git for cloning data repositories
RUN apt update && apt install -y git ca-certificates

RUN curl -ksSL https://gitlab.mitre.org/mitre-scripts/mitre-pki/raw/master/os_scripts/install_certs.sh | MODE=ubuntu sh

USER jetty:jetty

COPY --from=mavenbuild /home/app/target/inferno-fhir-reference-server.war /var/lib/jetty/webapps/root.war
COPY --chown=jetty:jetty ./resources /var/lib/jetty/resources/
COPY --chown=jetty:jetty ./clients /var/lib/jetty/clients/
COPY ./scripts /var/lib/jetty/scripts

RUN java -jar "$JETTY_HOME/start.jar" --create-startd --add-to-start=http-forwarded --add-modules=server,http,ee10-deploy

CMD ["./scripts/start_reference_server.sh"]

EXPOSE 8080