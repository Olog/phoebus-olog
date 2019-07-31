# olog-es   [![Build Status](https://travis-ci.org/shroffk/olog-es.svg?branch=master)](https://travis-ci.org/shroffk/olog-es)

An online logbook for recroding logs 

### Installation
Olog 

* Prerequisites

  * JDK 11 or newer
  * Elastic version 6.3
  * gridfs


* setup elastic search  
  **Install**  
  Download and install elasticsearch (verision 6.3) from [elastic.com](https://www.elastic.co/downloads/past-releases/elasticsearch-6-3-1)    
  Download and install mongodb from [mongodb](https://www.mongodb.com/download-center/community)    
  
* Build 
```
mvn clean install
``` 

#### Start the service  

Using spring boot  

```
mvn spring-boot:run
```
