FROM ubuntu:latest
RUN apt update
# Avoid prompt for locale.
RUN DEBIAN_FRONTEND="noninteractive" apt install -y tzdata
RUN apt install -y git netcat wget
RUN apt install -y openjdk-11-jdk maven
# Used for waiting for services before starting Olog.
RUN apt install -y wait-for-it
RUN mkdir olog-es
WORKDIR /olog-es
COPY . .
RUN mvn clean install -DskipTests=true -Pdeployable-jar
EXPOSE 8080
EXPOSE 8181
CMD java -jar target/olog-es*.jar
