#!/bin/bash

###
# #%L
# Phoebus Olog Service
# %%
# Copyright (C) 2010 - 2016 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
# %%
# Copyright (C) 2010 - 2012 Brookhaven National Laboratory
# %%
# Copyright (C) 2022 European Spallation Source
# All rights reserved. Use is subject to license terms.
# #L%
###

# The mapping definition for the Indexes associated with the olog v2

# Delete in case it exists
curl -XDELETE 'http://localhost:9200/olog_logbooks/?pretty=true'
#Create the Index
curl -H 'Content-Type: application/json' -XPUT 'http://localhost:9200/olog_logbooks/?pretty=true' -d'
{
  "mappings":{
    "properties": {
        "name": {
            "type": "keyword"
        },
        "owner": {
            "type": "keyword"
        },
        "state": {
            "type": "keyword"
        }
    }
  }
}'

# Delete in case it exists
curl -XDELETE 'http://localhost:9200/olog_tags/?pretty=true'
#Create the Index
#Set the mapping
curl -H 'Content-Type: application/json' -XPUT 'http://localhost:9200/olog_tags/?pretty=true' -d'
{
  "mappings" : {
   "properties": {
       "name": {
           "type": "keyword"
       },
       "state": {
           "type": "keyword"
       }
   }
  }
}'

# Delete in case it exists
curl -XDELETE 'http://localhost:9200/olog_properties/?pretty=true'
#Create the Index
curl -H 'Content-Type: application/json' -XPUT 'http://localhost:9200/olog_properties/?pretty=true' -d'
{
  "mappings" : {
   "properties": {
               "name": {
                   "type": "keyword"
               },
               "owner": {
                   "type": "keyword"
               },
               "state": {
                   "type": "keyword"
               },
               "attributes": {
                   "type": "nested",
                   "properties": {
                       "name": {
                           "type": "keyword"
                       },
                       "value": {
                           "type": "keyword"
                       },
                       "state": {
                           "type": "keyword"
                       }
                   }
               }
           }
  }
}'

# Delete in case it exists
curl -XDELETE 'http://localhost:9200/olog_sequence/?pretty=true'
#Create the Index
#Set the mappings
curl -H 'Content-Type: application/json' -XPUT 'http://localhost:9200/olog_sequence/?pretty=true'  -d '
{
   "settings" : {
      "number_of_shards"     : 1,
      "auto_expand_replicas" : "0-all"
   },

   "mappings" : {
           "_source": {
                  "enabled": false
               },
               "enabled": false

  }
}
'

# Delete in case it exists
curl -XDELETE 'http://localhost:9200/olog_logs/?pretty=true'
#Create the Index
#Set the mapping
curl -H 'Content-Type: application/json' -XPUT 'http://localhost:9200/olog_logs/?pretty=true' -d'
{
  "mappings" : {
        "properties": {
                "id": {
                    "type": "keyword"
                },
                "owner": {
                    "type": "keyword"
                },
                "source": {
                    "type": "text"
                },
                "description": {
                    "type": "text"
                },
                "level": {
                    "type": "text"
                },
                "title" : {
                    "type": "text"
                },
                "state": {
                    "type": "keyword"
                },
                "createdDate": {
                    "type": "date",
                    "format": "epoch_millis||yyyy-MM-dd HH:mm:ss.SSS"
                },
                "modifyDate": {
                    "type": "date",
                    "format": "epoch_millis||yyyy-MM-dd HH:mm:ss.SSS"
                },
                "events": {
                    "type": "nested",
                    "properties": {
                        "name": {
                            "type": "keyword"
                        },
                        "event": {
                            "type": "date",
                            "format": "epoch_millis||yyyy-MM-dd HH:mm:ss.SSS"
                        }
                    }
                },
                "logbooks": {
                    "type": "nested",
                    "properties": {
                        "name": {
                            "type": "keyword"
                        },
                        "owner": {
                            "type": "keyword"
                        },
                        "state": {
                            "type": "keyword"
                        }
                    }
                },
                "tags": {
                    "type": "nested",
                    "properties": {
                        "name": {
                            "type": "keyword"
                        },
                        "state": {
                            "type": "keyword"
                        }
                    }
                },
                "properties": {
                    "type": "nested",
                    "properties": {
                        "name": {
                            "type": "keyword"
                        },
                        "owner": {
                            "type": "keyword"
                        },
                        "state": {
                            "type": "keyword"
                        },
                        "attributes": {
                            "type": "nested",
                            "properties": {
                                "name": {
                                    "type": "keyword"
                                },
                                "value": {
                                    "type": "keyword"
                                },
                                "state": {
                                    "type": "keyword"
                                }
                            }
                        }
                    }
                }
            }
  }
}'
