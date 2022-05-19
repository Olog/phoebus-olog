/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.MgetRequest;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.State;
import org.phoebus.olog.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Repository
public class TagRepository implements CrudRepository<Tag, String> {

    private Logger logger = Logger.getLogger(TagRepository.class.getName());

    @Value("${elasticsearch.tag.index:olog_tags}")
    private String ES_TAG_INDEX;
    @Value("${elasticsearch.tag.type:olog_tag}")
    private String ES_TAG_TYPE;

    @Value("${elasticsearch.result.size.tags:10}")
    private int tagsResultSize;

    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     *
     */
    @Override
    public <S extends Tag> S save(S tag) {
        try {
            co.elastic.clients.elasticsearch.core.IndexRequest indexRequest =
                    co.elastic.clients.elasticsearch.core.IndexRequest.of(i ->
                            i.index(ES_TAG_INDEX)
                                    .id(tag.getName())
                                    .document(tag)
                                    .refresh(Refresh.True));
            co.elastic.clients.elasticsearch.core.IndexResponse response = client.index(indexRequest);

            if (response.result().equals(Result.Created) ||
                    response.result().equals(Result.Updated)) {
                co.elastic.clients.elasticsearch.core.GetRequest getRequest =
                        co.elastic.clients.elasticsearch.core.GetRequest.of(g ->
                                g.index(ES_TAG_INDEX).id(response.id()));
                co.elastic.clients.elasticsearch.core.GetResponse<Tag> resp =
                        client.get(getRequest, Tag.class);
                return (S) resp.source();
            }
            return null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create tag: " + tag, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create tag: " + tag);
        }
    }

    @Override
    public <S extends Tag> Iterable<S> saveAll(Iterable<S> tags) {
        List<BulkOperation> bulkOperations = new ArrayList<>();
        tags.forEach(tag -> bulkOperations.add(IndexOperation.of(i ->
                i.index(ES_TAG_INDEX).document(tag))._toBulkOperation()));
        co.elastic.clients.elasticsearch.core.BulkRequest bulkRequest =
                co.elastic.clients.elasticsearch.core.BulkRequest.of(r ->
                        r.operations(bulkOperations).refresh(Refresh.True));

        BulkResponse bulkResponse;
        try {
            bulkResponse = client.bulk(bulkRequest);
            if (bulkResponse.errors()) {
                // process failures by iterating through each bulk response item
                bulkResponse.items().forEach(responseItem -> {
                    if (responseItem.error() != null) {
                        logger.log(Level.SEVERE, responseItem.error().reason());
                    }
                });
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to create logbooks: " + tags);
            } else {
                return tags;
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create tags: " + tags, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create tags: " + tags);
        }
    }

    @Override
    public Optional<Tag> findById(String tagName) {
        try {
            co.elastic.clients.elasticsearch.core.GetRequest getRequest =
                    co.elastic.clients.elasticsearch.core.GetRequest.of(g ->
                            g.index(ES_TAG_INDEX).id(tagName));
            co.elastic.clients.elasticsearch.core.GetResponse<Tag> resp =
                    client.get(getRequest, Tag.class);
            if (resp.found()) {
                return Optional.of(resp.source());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to find tag: " + tagName, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find tag: " + tagName);
        }
    }

    @Override
    public boolean existsById(String tagName) {
        Optional<Tag> tagOptional = findById(tagName);
        return tagOptional.isPresent();
    }

    @Override
    public Iterable<Tag> findAll() {

        try {
            co.elastic.clients.elasticsearch.core.SearchRequest searchRequest =
                    co.elastic.clients.elasticsearch.core.SearchRequest.of(s ->
                            s.index(ES_TAG_INDEX)
                                    .query(q -> q.match(t -> t.field("state").query(State.Active.toString())))
                                    .timeout("10s")
                                    .size(tagsResultSize)
                                    .sort(SortOptions.of(so -> so.field(FieldSort.of(f -> f.field("name"))))));

            co.elastic.clients.elasticsearch.core.SearchResponse<Tag> response =
                    client.search(searchRequest, Tag.class);

            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to find tags", e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find tags");
        }

        /*
        try {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.termQuery("state", State.Active.toString()));
            sourceBuilder.timeout(new TimeValue(10, TimeUnit.SECONDS));
            sourceBuilder.size(tagsResultSize);
            sourceBuilder.sort(SortBuilders.fieldSort("name").order(SortOrder.ASC));

            SearchResponse response = legacyClient.search(
                    new SearchRequest(ES_TAG_INDEX).types(ES_TAG_TYPE).source(sourceBuilder), RequestOptions.DEFAULT);
            List<Tag> result = new ArrayList<Tag>();
            response.getHits().forEach(h -> {
                BytesReference b = h.getSourceRef();
                try {
                    result.add(mapper.readValue(b.streamInput(), Tag.class));
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            });
            return result;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to find tags", e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find tags");
        }

         */
    }

    @Override
    public Iterable<Tag> findAllById(Iterable<String> tagNames) {

        List<String> ids = new ArrayList<>();
        tagNames.forEach(ids::add);
        MgetRequest mgetRequest = MgetRequest.of(r -> r.ids(ids));
        try {
            List<Tag> foundTags = new ArrayList<>();
            MgetResponse<Tag> resp = client.mget(mgetRequest, Tag.class);
            for (MultiGetResponseItem<Tag> multiGetResponseItem : resp.docs()) {
                if (!multiGetResponseItem.isFailure()) {
                    foundTags.add(multiGetResponseItem.result().source());
                }
            }
            return foundTags;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to find tags: " + tagNames, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find tags: " + tagNames);
        }
    }

    @Override
    public long count() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void deleteById(String tagName) {
        try {
            co.elastic.clients.elasticsearch.core.GetRequest getRequest =
                    co.elastic.clients.elasticsearch.core.GetRequest.of(g ->
                            g.index(ES_TAG_INDEX).id(tagName));
            co.elastic.clients.elasticsearch.core.GetResponse<Tag> resp =
                    client.get(getRequest, Tag.class);
            if (resp.found()) {
                Tag tag = resp.source();
                tag.setState(State.Inactive);
                co.elastic.clients.elasticsearch.core.UpdateRequest updateRequest =
                        co.elastic.clients.elasticsearch.core.UpdateRequest.of(u ->
                                u.index(ES_TAG_INDEX).id(tagName)
                                        .doc(tag));
                co.elastic.clients.elasticsearch.core.UpdateResponse updateResponse =
                        client.update(updateRequest, Tag.class);
                if (updateResponse.result().equals(co.elastic.clients.elasticsearch._types.Result.Updated)) {
                    logger.log(Level.INFO, "Deleted tag " + tagName);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to delete tag: " + tagName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete tag: " + tagName);
        }
    }

    @Override
    public void delete(Tag tag) {
        deleteById(tag.getName());
    }

    @Override
    public void deleteAll(Iterable<? extends Tag> tags) {
        tags.forEach(tag -> deleteById(tag.getName()));
    }

    @Override
    public void deleteAll() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Deleting all tags is not allowed");
    }

    @Override
    public void deleteAllById(Iterable ids) {
        while (ids.iterator().hasNext()) {
            deleteById((String) ids.iterator().next());
        }
    }
}
