/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.phoebus.olog.entity.Attachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public class AttachmentRepository implements CrudRepository<Attachment, String>
{

    @Autowired
    private GridFsTemplate gridFsTemplate;
    @Autowired
    private GridFsOperations gridOperation;
    @Autowired
    private GridFSBucket gridFSBucket;

    /**
     * Saves an attachment.
     *
     * Client code may set an id on the entity to store, and it must be unique. In this manner a client
     * may pre-define a search path or URL to the persisted entity.
     *
     * If the client does not set the id of the entity (or if it is an empty string), the id of the persisted
     * entity will  be set by GridFs and then on the entity before it is returned.
     * @param entity
     * @param <S>
     * @return The persisted entity with non-null and non-empty id.
     */
    @Override
    public <S extends Attachment> S save(S entity)
    {
        try
        {
            GridFSUploadOptions options = new GridFSUploadOptions()
                    .metadata(new Document("meta-data", entity.getFileMetadataDescription()));
            if(entity.getId() != null && !entity.getId().isEmpty()){
                BsonString id = new BsonString(entity.getId());
                gridFSBucket.uploadFromStream(id, entity.getFilename(), entity.getAttachment().getInputStream(), options);
            }
            else{
                ObjectId objectId = gridFSBucket.uploadFromStream(entity.getFilename(), entity.getAttachment().getInputStream(), options);
                entity.setId(objectId.toString());
            }
            return entity;
        } catch (IOException e)
        {
            Logger.getLogger(AttachmentRepository.class.getName())
                    .log(Level.WARNING, String.format("Unable to persist attachment %s", entity.getFilename()), e);
        }
        return null;
    }

    @Override
    public <S extends Attachment> Iterable<S> saveAll(Iterable<S> entities)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     *
     * @param id The unique GridFS id of an attachment.
     * @return {@link Optional<Attachment>} or - if the specified id is invalid - {@link Optional#empty()}.
     */
    @Override
    public Optional<Attachment> findById(String id)
    {
        Attachment attachment = new Attachment();
        GridFSFile found = gridOperation.find(new Query(Criteria.where("_id").is(id))).first();
        if(found == null){
            return Optional.empty();
        }
        attachment.setId(id);
        attachment.setAttachment(gridOperation.getResource(found));
        attachment.setFilename(found.getFilename());
        attachment.setFileMetadataDescription(found.getMetadata().getString("meta-data"));
        return Optional.of(attachment);
    }

    @Override
    public boolean existsById(String id)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterable<Attachment> findAll()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<Attachment> findAllById(Iterable<String> ids)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long count()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void deleteById(String id)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(Attachment entity)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll(Iterable<? extends Attachment> entities)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll()
    {
        // TODO Auto-generated method stub

    }

}
