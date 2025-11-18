# Use Maven image to execute build.
FROM maven:3.6.3-openjdk-17 AS maven-build
RUN mkdir phoebus-olog
WORKDIR /phoebus-olog
COPY . .
RUN mvn clean install \
    -DskipTests=true \
    -Dmaven.javadoc.skip=true \
    -Dmaven.source.skip=true \
    -Pdeployable-jar

# Use smaller openjdk image for running.
FROM eclipse-temurin:17-jdk
# Run commands as user 'olog'
RUN useradd -ms /bin/bash olog
# Use previous maven-build image.
COPY --from=maven-build /phoebus-olog/target /olog-target
RUN chown olog:olog /olog-target
USER olog
WORKDIR /olog-target
EXPOSE 8080
EXPOSE 8181
CMD java -jar service-olog-5.0.2.jar
