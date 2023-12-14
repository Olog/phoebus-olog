/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.MgetRequest;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@Repository
public class PropertyRepository implements CrudRepository<Property, String> {

    @SuppressWarnings("unused")
    @Value("${elasticsearch.property.index:olog_properties}")
    private String ES_PROPERTY_INDEX;
    @SuppressWarnings("unused")
    @Value("${elasticsearch.result.size.properties:10}")
    private int propertiesResultSize;

    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

    private final Logger logger = Logger.getLogger(PropertyRepository.class.getName());

    @Override
    public <S extends Property> S save(S property) {
        try {
            IndexRequest<Property> indexRequest =
                    IndexRequest.of(i ->
                            i.index(ES_PROPERTY_INDEX)
                                    .id(property.getName())
                                    .document(property)
                                    .refresh(Refresh.True));
            IndexResponse response = client.index(indexRequest);

            if (response.result().equals(Result.Created) ||
                    response.result().equals(Result.Updated)) {
                GetRequest getRequest =
                        co.elastic.clients.elasticsearch.core.GetRequest.of(g ->
                                g.index(ES_PROPERTY_INDEX).id(response.id()));
                GetResponse<Property> resp =
                        client.get(getRequest, Property.class);
                return (S) resp.source();
            }
            return null;
        } catch (Exception e) {
            String message = MessageFormat.format(TextUtil.PROPERTY_NOT_CREATED, property);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
    }

    @Override
    public <S extends Property> Iterable<S> saveAll(Iterable<S> properties) {
        List<BulkOperation> bulkOperations = new ArrayList<>();
        properties.forEach(property -> bulkOperations.add(IndexOperation.of(i ->
                i.index(ES_PROPERTY_INDEX).document(property).id(property.getName()))._toBulkOperation()));

        BulkRequest bulkRequest = BulkRequest.of(r ->
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
                String message = MessageFormat.format(TextUtil.PROPERTIES_NOT_CREATED, properties);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
            } else {

                return properties;
            }
        } catch (IOException e) {
            String message = MessageFormat.format(TextUtil.PROPERTIES_NOT_CREATED, properties);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
    }

    @Override
    public Optional<Property> findById(String propertyName) {
        try {
            GetRequest getRequest =
                    GetRequest.of(g ->
                            g.index(ES_PROPERTY_INDEX).id(propertyName));
            GetResponse<Property> resp =
                    client.get(getRequest, Property.class);
            if (resp.found()) {
                return Optional.of(resp.source());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            String message = MessageFormat.format(TextUtil.PROPERTY_NOT_FOUND, propertyName);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    @Override
    public boolean existsById(String propertyName) {
        try {
            ExistsRequest.Builder builder = new ExistsRequest.Builder();
            builder.index(ES_PROPERTY_INDEX).id(propertyName);
            return client.exists(builder.build()).value();
        } catch (ElasticsearchException | IOException e) {
            String message = MessageFormat.format(TextUtil.PROPERTY_EXISTS_FAILED, propertyName);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
        }
    }

    @Override
    public Iterable<Property> findAll() {
        return findAll(false);
    }

    public Iterable<Property> findAll(boolean includeInactive) {
        try {
            SearchRequest searchRequest;
            Query query;
            if (includeInactive) {
                query = new MatchAllQuery.Builder().build()._toQuery();
            } else {
                query = MatchQuery.of(t -> t.field("state").query(State.Active.toString()))._toQuery();
            }
            searchRequest = SearchRequest.of(s ->
                    s.index(ES_PROPERTY_INDEX)
                            .query(query)
                            .timeout("10s")
                            .size(propertiesResultSize)
                            .sort(SortOptions.of(so -> so.field(FieldSort.of(f -> f.field("name"))))));

            SearchResponse<Property> response =
                    client.search(searchRequest, Property.class);

            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (Exception e) {
            logger.log(Level.SEVERE, TextUtil.PROPERTIES_NOT_FOUND, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, TextUtil.PROPERTIES_NOT_FOUND);
        }
    }

    @Override
    public Iterable<Property> findAllById(Iterable<String> propertyNames) {
        List<String> ids = new ArrayList<>();
        propertyNames.forEach(ids::add);
        MgetRequest mgetRequest = MgetRequest.of(r -> r.index(ES_PROPERTY_INDEX).ids(ids));
        try {
            List<Property> foundProperties = new ArrayList<>();
            MgetResponse<Property> resp = client.mget(mgetRequest, Property.class);
            for (MultiGetResponseItem<Property> multiGetResponseItem : resp.docs()) {
                if (!multiGetResponseItem.isFailure()) {
                    foundProperties.add(multiGetResponseItem.result().source());
                }
            }
            return foundProperties;
        } catch (Exception e) {
            String message = MessageFormat.format(TextUtil.PROPERTIES_NOT_FOUND_1, propertyNames);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    @Override
    public long count() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void deleteById(String propertyName) {
        try {
            GetRequest getRequest =
                    GetRequest.of(g ->
                            g.index(ES_PROPERTY_INDEX).id(propertyName));
            GetResponse<Property> resp =
                    client.get(getRequest, Property.class);
            if (resp.found()) {
                Property property = resp.source();
                property.setState(State.Inactive);
                UpdateRequest<Property, Property> updateRequest =
                        UpdateRequest.of(u ->
                                u.index(ES_PROPERTY_INDEX).id(propertyName)
                                        .doc(property));
                UpdateResponse<Property> updateResponse =
                        client.update(updateRequest, Property.class);
                if (updateResponse.result().equals(co.elastic.clients.elasticsearch._types.Result.Updated)) {
                    String message = MessageFormat.format(TextUtil.PROPERTY_DELETE, propertyName);
                    logger.log(Level.INFO, () -> message);
                }
            }
        } catch (Exception e) {
            String message = MessageFormat.format(TextUtil.PROPERTY_NOT_DELETED, propertyName);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
    }

    public void deleteAttribute(String propertyName, String attributeName) {
        try {
            Optional<Property> optional = findById(propertyName);
            if (optional.isEmpty()) {
                logger.log(Level.SEVERE, () -> MessageFormat.format(TextUtil.PROPERTY_ATTRIBUTE_CANNOT_DELETE, attributeName, propertyName));
                return;
            }
            Property property = optional.get();
            property.setAttributes(property.getAttributes().stream().map(p -> {
                if (p.getName().equals(attributeName)) {
                    p.setState(State.Inactive);
                }
                return p;
            }).collect(Collectors.toSet()));

            UpdateRequest<Property, Property> updateRequest =
                    UpdateRequest.of(u ->
                            u.index(ES_PROPERTY_INDEX).id(propertyName)
                                    .doc(property));
            UpdateResponse<Property> updateResponse =
                    client.update(updateRequest, Property.class);

            if (updateResponse.result().equals(Result.Updated)) {
                GetRequest getRequest =
                        co.elastic.clients.elasticsearch.core.GetRequest.of(g ->
                                g.index(ES_PROPERTY_INDEX).id(updateResponse.id()));
                GetResponse<Property> resp =
                        client.get(getRequest, Property.class);
                Property deletedProperty = resp.source();
                logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.PROPERTY_ATTRIBUTE_DELETE, deletedProperty.toLogger()));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public void delete(Property property) {
        deleteById(property.getName());
    }

    @Override
    public void deleteAll(Iterable<? extends Property> properties) {
        properties.forEach(property -> deleteById(property.getName()));
    }

    @Override
    public void deleteAll() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, TextUtil.PROPERTIES_DELETE_ALL_NOT_ALLOWED);
    }

    @Override
    public void deleteAllById(Iterable ids) {
        while (ids.iterator().hasNext()) {
            deleteById((String) ids.iterator().next());
        }
    }

}
