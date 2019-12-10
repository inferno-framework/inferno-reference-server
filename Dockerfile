FROM maven:3.6-jdk-11 AS mavenbuild

COPY pom.xml /home/app/
#RUN mvn -f /home/app/pom.xml verify --fail-never
COPY src /home/app/src
RUN mvn -f /home/app/pom.xml package

FROM jetty:9.4-jre11

# Download MITRE certificates.

USER root

ADD --chown=jetty:jetty \
    http://pki.mitre.org/MITRE%20BA%20ROOT.crt \
    http://pki.mitre.org/MITRE%20BA%20NPE%20CA-3.crt \
    http://pki.mitre.org/MITRE%20BA%20NPE%20CA-4.crt \
    "$JETTY_HOME/"

# Add MITRE certificates to java keystore.

#RUN keytool -import -file "$JETTY_HOME/MITRE BA ROOT.crt" -alias mitre-ba-root -keystore "/etc/ssl/certs/java/cacerts" -noprompt -storepass changeit
#RUN keytool -import -file "$JETTY_HOME/MITRE BA NPE CA-3.crt" -alias mitre-npe-3 -keystore "/etc/ssl/certs/java/cacerts" -noprompt -storepass changeit
#RUN keytool -import -file "$JETTY_HOME/MITRE BA NPE CA-4.crt" -alias mitre-npe-4 -keystore "/etc/ssl/certs/java/cacerts" -noprompt -storepass changeit

# Switch to jetty user and configure jetty.

USER jetty:jetty

COPY --from=mavenbuild /home/app/target/inferno-fhir-reference-server.war /var/lib/jetty/webapps/root.war

RUN java -jar "$JETTY_HOME/start.jar" --create-startd --add-to-start=http-forwarded

EXPOSE 8080

# Add MITRE certificates to jetty keystore.

#RUN keytool -import -file "$JETTY_HOME/MITRE BA ROOT.crt" -alias mitre-ba-root -keystore "$JETTY_BASE/lib/keystore" -noprompt -storepass changeit
#RUN keytool -import -file "$JETTY_HOME/MITRE BA NPE CA-3.crt" -alias mitre-npe-3 -keystore "$JETTY_BASE/lib/keystore" -noprompt -storepass changeit
#RUN keytool -import -file "$JETTY_HOME/MITRE BA NPE CA-4.crt" -alias mitre-npe-4 -keystore "$JETTY_BASE/lib/keystore" -noprompt -storepass changeit
