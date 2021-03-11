# Use Maven image to execute build.
FROM maven:3.6.3-openjdk-11 AS maven-build
RUN mkdir olog-es
WORKDIR /olog-es
COPY . .
RUN mvn clean install -DskipTests=true -Pdeployable-jar

# Use smaller openjdk image for running.
FROM openjdk:11
# apt clean is run automatically in debian-based images.
RUN apt update && apt install -y wait-for-it
# Run commands as user 'olog'
RUN useradd -ms /bin/bash olog
# Use previous maven-build image.
COPY --from=maven-build /olog-es/target /olog-target
RUN chown olog:olog /olog-target
USER olog
WORKDIR /olog-target
EXPOSE 8080
EXPOSE 8181
CMD java -jar olog-es*.jar