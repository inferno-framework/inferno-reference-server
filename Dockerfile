FROM maven:3.6-jdk-11 AS mavenbuild

COPY pom.xml /home/app/
COPY src /home/app/src
COPY config /home/app/config
RUN mvn -q -f /home/app/pom.xml package

FROM jetty:9.4-jre11

# Download MITRE certificates.

USER root

# Switch to jetty user and configure jetty.

USER jetty:jetty

COPY --from=mavenbuild /home/app/target/inferno-fhir-reference-server.war /var/lib/jetty/webapps/root.war

RUN java -jar "$JETTY_HOME/start.jar" --create-startd --add-to-start=http-forwarded

EXPOSE 8080

