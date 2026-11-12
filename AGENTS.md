# AGENTS.md

## Ambito

Queste istruzioni si applicano all'intero repository.

Phoebus Olog e un servizio Spring Boot per creare, archiviare e consultare log entry. Usa Java 17, Maven, Elasticsearch 8.2.x per i metadati e MongoDB GridFS per gli allegati.

## Struttura del progetto

- `src/main/java/org/phoebus/olog`: controller REST, repository e configurazione applicativa.
- `src/main/java/org/phoebus/olog/entity`: modelli del dominio.
- `src/main/java/org/phoebus/olog/security`: autenticazione tramite sessione e provider di sicurezza.
- `src/main/resources`: configurazione Spring, mapping Elasticsearch e dati predefiniti.
- `src/test/java/org/phoebus/olog`: unit test e test MVC.
- `src/test/java/org/phoebus/olog/docker`: integration test basati su container.
- `src/site/sphinx`: documentazione dell'API.

## Comandi utili

- Eseguire gli unit test: `mvn test`
- Compilare e verificare il progetto: `mvn clean install`
- Creare il JAR eseguibile: `mvn -Pdeployable-jar clean install`
- Eseguire gli integration test Docker: `mvn failsafe:integration-test -DskipITs=false -Pintegrationtest-docker`
- Avviare il servizio: `mvn org.springframework.boot:spring-boot-maven-plugin:run`

Per modifiche circoscritte, eseguire prima i test della classe interessata con `mvn -Dtest=NomeTest test`, quindi ampliare la verifica in proporzione al rischio.

## Convenzioni di modifica

- Mantieni la compatibilita con Java 17 e Spring Boot 2.7.x.
- Segui lo stile delle classi vicine; evita refactoring non collegati alla richiesta.
- Mantieni controller, repository, entity e test nei package esistenti.
- Quando cambia un endpoint, aggiorna insieme test MVC, integration test e documentazione pertinente.
- Non modificare i mapping Elasticsearch senza considerare la compatibilita dei dati esistenti.
- Non inserire credenziali, token, URL interni o altri segreti nel repository.
- Preserva le modifiche locali non correlate gia presenti nel working tree.

## Autenticazione e autorizzazione

- Le richieste `GET` sono attualmente escluse dall'autenticazione in `WebSecurityConfig`.
- Le operazioni di scrittura richiedono autenticazione tramite sessione/Basic oppure JWT, a seconda della configurazione.
- I ruoli globali dichiarati in `AuthorizationService` non sono applicati alle ACL per-logbook.
- Per i nuovi logbook, `owner` identifica il creatore autenticato e amministratore; i collaboratori sono registrati separatamente con permessi `READ` e/o `WRITE`.
- Le API sotto `/Olog/logbooks/{name}/members` sono riservate all'amministratore del logbook.
- Non assumere che i controlli `READ` e `WRITE` siano gia applicati a tutti gli endpoint storici dei log e degli allegati.
- Qualunque intervento sui permessi deve usare `403 Forbidden` per un utente autenticato ma non autorizzato e `401 Unauthorized` per una richiesta non autenticata.
- Aggiungi test positivi e negativi per utente anonimo, utente normale, membro del gruppo logbook, proprietario e amministratore quando introduci autorizzazioni granulari.
- Verifica attentamente i nomi delle property dei gruppi: `application.properties` dichiara `log-groups` e `logbook-groups`, mentre `AuthorizationService` contiene attualmente riferimenti incoerenti a `channel-groups` e `property-groups`.

## Regole specifiche per i logbook

- Gli endpoint sono sotto `/Olog/logbooks`.
- `PUT /Olog/logbooks/{logbookName}` crea o sostituisce un logbook.
- `PUT /Olog/logbooks` aggiorna una collezione di logbook.
- `DELETE /Olog/logbooks/{logbookName}` elimina un logbook esistente.
- Il nome e l'identificatore persistito del logbook: mantieni coerenti il parametro nel path e il nome nel payload.
- Per i nuovi logbook, non accettare `owner` dal client: deve essere ricavato dall'identita autenticata.
- Il client INFN Identity e generato da `src/main/openapi/infn-identity-v3.json` tramite OpenAPI Generator; non modificare i sorgenti sotto `target/generated-sources/openapi`.

## Verifica finale

Prima di concludere una modifica:

1. Controlla il diff e assicurati che contenga solo cambiamenti pertinenti.
2. Esegui almeno i test unitari/MVC direttamente interessati.
3. Per modifiche a repository, mapping o flussi end-to-end, esegui gli integration test appropriati quando Docker e disponibile.
4. Riporta chiaramente quali verifiche sono state eseguite e quali non e stato possibile eseguire.
