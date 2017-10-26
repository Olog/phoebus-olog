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
      "name" : {"type" : "string",
                "analyzer" : "whitespace"},
      "owner" : {"type" : "string",
                "analyzer" : "whitespace"},
      "state" : {"type" : "string",
                "analyzer" : "whitespace"}
    }
}'

#Create the Index
curl -XPUT 'http://localhost:9200/olog_logbooks'
#Set the mapping
curl -XPUT 'http://localhost:9200/olog_logbooks/_mapping/logbook' -d'
{
  "logbook" : {
    "properties" : {
      "name" : {"type" : "string",
                "analyzer" : "whitespace"},
      "owner" : {"type" : "string",
                "analyzer" : "whitespace"},
      "state" : {"type" : "string",
                "analyzer" : "whitespace"}
    }
  }
}'
