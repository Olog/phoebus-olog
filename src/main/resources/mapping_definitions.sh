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
curl -XPUT 'http://localhost:9200/olog_tags'
#Set the mapping

curl -XPUT 'http://localhost:9200/olog_tags/_mapping/tag' -d'
{
  "tag" : {
    "properties" : {
      "name"  : {"type" : "string", analyzer" : "whitespace"},
      "owner" : {"type" : "string", "analyzer" : "whitespace"},
      "state" : {"type" : "string", "analyzer" : "whitespace"}
    }
}'

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

#Create the Index
curl -XPUT 'http://localhost:9200/olog_properties'
#Set the mapping
curl -XPUT 'http://localhost:9200/olog_properties/_mapping/property' -d'
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
curl -XPUT 'http://localhost:9200/olog_sequence/?pretty=1'  -d '
{
   "settings" : {
      "number_of_shards"     : 1,
      "auto_expand_replicas" : "0-all"
   },
   "mappings" : {
      "sequence" : {
         "_source" : { "enabled" : 0 },
         "_all"    : { "enabled" : 0 },
         "_type"   : { "index" : "no" },
         "enabled" : 0
      }
   }
}
'
