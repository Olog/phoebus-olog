# olog-es   [![Build Status](https://travis-ci.org/shroffk/olog-es.svg?branch=master)](https://travis-ci.org/shroffk/olog-es)

An online logbook for recroding logs 

[Olog-es Documentation](https://olog-es.readthedocs.io/)

### Installation
Olog 

* Prerequisites

  * JDK 11 or newer
  * Elastic version 6.3
  * mongo gridfs


 **Download links for the prerequisites**   
 Download and install elasticsearch (verision 6.3) from [elastic.com](https://www.elastic.co/downloads/past-releases/elasticsearch-6-3-1)    
 Download and install mongodb from [mongodb](https://www.mongodb.com/download-center/community)    
  
  
* Configure the service   
The configuration files for olog-es are present under `olog-es/tree/master/src/main/resources` 


* Build 
```
mvn clean install
``` 

#### Start the service  

Using spring boot  

```
mvn org.springframework.boot:spring-boot-maven-plugin:run
```
