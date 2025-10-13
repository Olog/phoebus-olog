Change Log
==========

A list of API changes, major features, and bug fixes included in each release.

service-olog-5.1.1
------------------
Date: Oct 2025

* Support for web sockets. Clients shall connect over web sockets as information on changes (new or edited log entries) are pushed
from the service to all connected clients.

service-olog-2.0.3
------------------------------------------------
Date: TBD

* Add support for initializing default logbooks, properties, and tags on service startup
* Throw exception when invalid start and end time are requested in the log search parameters. Client will receive HTTP 400 (bad request) status.

service-olog-2.0.3
------------------
Date: Jan 10, 2022

service-olog-2.0.2
------------------
Date: Jan 9, 2022

* Remove vulnerabilities in dependencies


service-olog-2.0.1
------------------
Date: Dec 29, 2021

* Support for rich client text using markup


service-olog-2.0.0
------------------
Date:  Oct 27, 2021

* First release of service on the phoebus framework - springboot framework
* Switch backend to elastic and mongodb
