#!/bin/bash

###
# #%L
# ChannelFinder Directory Service
# %%
# Copyright (C) 2010 - 2016 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
# %%
# Copyright (C) 2010 - 2012 Brookhaven National Laboratory
# All rights reserved. Use is subject to license terms.
# #L%
###

# The mapping definition for the Indexes associated with the olog v2

#Create the Index
curl -XPUT 'http://localhost:9200/olog_logbooks'
#Set the mapping
curl -XPUT 'http://localhost:9200/olog_logbooks/_mapping/logbook' -d'
{
  "logbook" : {
    "properties" : {
      "name"  : {"type" : "string", "analyzer" : "whitespace"},
      "owner" : {"type" : "string", "analyzer" : "whitespace"},
      "state" : {"type" : "string", "analyzer" : "whitespace"}
    }
  }
}'

curl -XDELETE 'http://130.199.219.217:9200/olog_tags'
#Create the Index
#Set the mapping
curl -H 'Content-Type: application/json' -XPUT 'http://130.199.219.217:9200/olog_tags' -d'
{
  "mappings" : {
  "olog_tag" : {
    "properties" : {
      "name"  : {"type" : "keyword"},
      "state" : {"type" : "keyword"}
    }
  }
  }
}'

#Create the Index
curl -XPUT 'http://130.199.219.217:9200/olog_properties'
#Set the mapping
curl -H 'Content-Type: application/json' -XPUT 'http://localhost:9200/olog_properties' -d'
{
  "property" : {
    "properties" : {
      "name" :  {"type" : "string", "analyzer" : "whitespace"},
      "state" : {"type" : "string", "analyzer" : "whitespace"},
      "attributes" : {
                "type" : "nested",
                "include_in_parent" : true,
                "properties" : {
                  "name" :  { "type" : "string", "analyzer" : "whitespace"},
                  "value" : { "type" : "string", "analyzer" : "whitespace"},
                  "state" : { "type" : "string", "analyzer" : "whitespace"}
                 }
      }
    }
  }
}'

#Create the Index
curl -XPUT 'http://localhost:9200/olog_sequence'
#Set the mappings
curl -XPUT 'http://localhost:9200/olog_sequence/?pretty=true'  -d '
{
   "settings" : {
      "number_of_shards"     : 1,
      "auto_expand_replicas" : "0-all"
   },
   "mappings" : {
      "olog_sequence" : {
         "_source" : { "enabled" : false },
         "_all"    : { "enabled" : false },
         "_type"   : { "index" : "no" },
         "enabled" : 0
      }
   }
}
'


#Create the Index
curl -XPUT 'http://localhost:9200/olog_logs'
#Set the mapping
curl -XPUT 'http://localhost:9200/olog_logs/_mapping/log' -d'
{
  "log" : {
    "properties" : {
    }
  }
}'
