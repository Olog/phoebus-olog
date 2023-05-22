#!/bin/bash

if [ $# -eq 0 ]; then
    echo "Invalid usage. Please specify path to the file log_entry_mapping.json as first argument."
    exit 1
fi

echo "Creating temporary index"
curl -XPUT "http://localhost:9200/olog_logs_tmp"  -H 'Content-Type: application/json' --data @$1
echo

echo "Copying data to temporary index"
curl -XPOST "http://localhost:9200/_reindex"  -H 'Content-Type: application/json' -d '{"source":{"index": "olog_logs"}, "dest":{"index":"olog_logs_tmp"}}'
echo

echo "Flushing data to temporary index"
curl -XPOST "http://localhost:9200/olog_logs_tmp/_flush"
echo

echo "Delete original index"
curl -XDELETE "http://localhost:9200/olog_logs"
echo

echo "Re-creating original index"
curl -XPUT "http://localhost:9200/olog_logs"  -H 'Content-Type: application/json' --data @$1
echo

echo "Flushing data to temporary index"
curl -XPOST "http://localhost:9200/olog_logs/_flush"
echo

echo "Copying data to original index"
curl -XPOST "http://localhost:9200/_reindex"  -H 'Content-Type: application/json' -d '{"source":{"index": "olog_logs_tmp"}, "dest":{"index":"olog_logs"}}'
echo

echo "Delete temporary index"
curl -XDELETE "http://localhost:9200/olog_logs_tmp"
echo