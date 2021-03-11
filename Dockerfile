FROM maven:3.6.3-openjdk-11
RUN apt update && apt clean
# Used for waiting for services before starting Olog.
RUN apt install -y wait-for-it
RUN mkdir olog-es
# Run commands as user 'olog'
RUN useradd -ms /bin/bash olog
RUN chown olog:olog olog-es
USER olog
WORKDIR /olog-es
COPY . .
RUN mvn clean install -DskipTests=true -Pdeployable-jar
EXPOSE 8080
EXPOSE 8181
CMD java -jar target/olog-es*.jar
