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
import org.phoebus.olog.entity.Property;
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
public class PropertyRepository implements CrudRepository<Property, String> {
    @Value("${elasticsearch.property.index:olog_properties}")
    private String ES_PROPERTY_INDEX;
    @Value("${elasticsearch.property.type:olog_property}")
    private String ES_PROPERTY_TYPE;

    @Value("${elasticsearch.result.size.properties:10}")
    private int propertiesResultSize;

    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

    private static final ObjectMapper mapper = new ObjectMapper();

    private Logger logger = Logger.getLogger(PropertyRepository.class.getName());

    @Override
    public <S extends Property> S save(S property) {
        try {
            co.elastic.clients.elasticsearch.core.IndexRequest indexRequest =
                    co.elastic.clients.elasticsearch.core.IndexRequest.of(i ->
                            i.index(ES_PROPERTY_INDEX)
                                    .id(property.getName())
                                    .document(property)
                                    .refresh(Refresh.True));
            co.elastic.clients.elasticsearch.core.IndexResponse response = client.index(indexRequest);

            if (response.result().equals(Result.Created) ||
                    response.result().equals(Result.Updated)) {
                co.elastic.clients.elasticsearch.core.GetRequest getRequest =
                        co.elastic.clients.elasticsearch.core.GetRequest.of(g ->
                                g.index(ES_PROPERTY_INDEX).id(response.id()));
                co.elastic.clients.elasticsearch.core.GetResponse<Property> resp =
                        client.get(getRequest, Property.class);
                return (S) resp.source();
            }
            return null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create property: " + property, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create property: " + property);
        }
    }

    @Override
    public <S extends Property> Iterable<S> saveAll(Iterable<S> properties) {
        List<BulkOperation> bulkOperations = new ArrayList<>();
        properties.forEach(property -> bulkOperations.add(IndexOperation.of(i ->
                i.index(ES_PROPERTY_INDEX).document(property))._toBulkOperation()));

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
                        "Failed to create properties: " + properties);
            } else {
                return properties;
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create properties: " + properties, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create properties: " + properties);
        }
    }

    @Override
    public Optional<Property> findById(String propertyName) {
        try {
            co.elastic.clients.elasticsearch.core.GetRequest getRequest =
                    co.elastic.clients.elasticsearch.core.GetRequest.of(g ->
                            g.index(ES_PROPERTY_INDEX).id(propertyName));
            co.elastic.clients.elasticsearch.core.GetResponse<Property> resp =
                    client.get(getRequest, Property.class);
            if (resp.found()) {
                return Optional.of(resp.source());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to find property: " + propertyName, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find property: " + propertyName);
        }
    }

    @Override
    public boolean existsById(String propertyName) {
        Optional<Property> propertyOptional = findById(propertyName);
        return propertyOptional.isPresent();
    }

    @Override
    public Iterable<Property> findAll() {
        return findAll(false);
    }

    public Iterable<Property> findAll(boolean includeInactive) {
        try {
            co.elastic.clients.elasticsearch.core.SearchRequest searchRequest =
                    co.elastic.clients.elasticsearch.core.SearchRequest.of(s ->
                            s.index(ES_PROPERTY_INDEX)
                                    .query(q -> q.match(t -> t.field("state").query(State.Active.toString())))
                                    .timeout("10s")
                                    .size(propertiesResultSize)
                                    .sort(SortOptions.of(so -> so.field(FieldSort.of(f -> f.field("name"))))));

            co.elastic.clients.elasticsearch.core.SearchResponse<Property> response =
                    client.search(searchRequest, Property.class);

            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to find properties", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find properties");
        }
    }

    @Override
    public Iterable<Property> findAllById(Iterable<String> propertyNames) {
        List<String> ids = new ArrayList<>();
        propertyNames.forEach(ids::add);
        MgetRequest mgetRequest = MgetRequest.of(r -> r.ids(ids));
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
            logger.log(Level.SEVERE, "Failed to find properties: " + propertyNames, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find properties: " + propertyNames);
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
            co.elastic.clients.elasticsearch.core.GetRequest getRequest =
                    co.elastic.clients.elasticsearch.core.GetRequest.of(g ->
                            g.index(ES_PROPERTY_INDEX).id(propertyName));
            co.elastic.clients.elasticsearch.core.GetResponse<Property> resp =
                    client.get(getRequest, Property.class);
            if (resp.found()) {
                Property property = resp.source();
                property.setState(State.Inactive);
                co.elastic.clients.elasticsearch.core.UpdateRequest updateRequest =
                        co.elastic.clients.elasticsearch.core.UpdateRequest.of(u ->
                                u.index(ES_PROPERTY_INDEX).id(propertyName)
                                        .doc(property));
                co.elastic.clients.elasticsearch.core.UpdateResponse updateResponse =
                        client.update(updateRequest, Property.class);
                if (updateResponse.result().equals(co.elastic.clients.elasticsearch._types.Result.Updated)) {
                    logger.log(Level.INFO, "Deleted property " + propertyName);
                }
            }
        } /*catch (DocumentMissingException e) {
            logger.log(Level.SEVERE, "Failed to delete property: " + propertyName + " because it does not exist", e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to delete property: " + propertyName + " because it does not exist");
        }*/ catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to delete property: " + propertyName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete property: " + propertyName);
        }
    }

    public void deleteAttribute(String propertyName, String attributeName) {
        try {
            Property property = findById(propertyName).get();
            if (property != null) {
                property.setAttributes(property.getAttributes().stream().map(p -> {
                    if (p.getName().equals(attributeName)) {
                        p.setState(State.Inactive);
                    }
                    return p;
                }).collect(Collectors.toSet()));
            }

            co.elastic.clients.elasticsearch.core.UpdateRequest updateRequest =
                    co.elastic.clients.elasticsearch.core.UpdateRequest.of(u ->
                            u.index(ES_PROPERTY_INDEX).id(propertyName)
                                    .doc(property));
            co.elastic.clients.elasticsearch.core.UpdateResponse updateResponse =
                    client.update(updateRequest, Property.class);

            if (updateResponse.result().equals(Result.Updated)) {
                co.elastic.clients.elasticsearch.core.GetRequest getRequest =
                        co.elastic.clients.elasticsearch.core.GetRequest.of(g ->
                                g.index(ES_PROPERTY_INDEX).id(updateResponse.id()));
                co.elastic.clients.elasticsearch.core.GetResponse<Property> resp =
                        client.get(getRequest, Property.class);
                Property deletedProperty = resp.source();
                logger.log(Level.INFO, "Deleted property attribute" + deletedProperty.toLogger());
            }
        } /*catch (DocumentMissingException e) {
            logger.log(Level.SEVERE, propertyName + " Does not exist and thus cannot be deleted");
        } */catch (Exception e) {
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
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Deleting all properties is not allowed");
    }

    @Override
    public void deleteAllById(Iterable ids) {
        while (ids.iterator().hasNext()) {
            deleteById((String) ids.iterator().next());
        }
    }

}
