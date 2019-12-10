/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package gov.bnl.olog;

import java.io.IOException;
import java.util.Optional;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import com.mongodb.client.gridfs.model.GridFSFile;
import gov.bnl.olog.entity.Attachment;

@Repository
public class AttachmentRepository implements CrudRepository<Attachment, String>
{

    @Autowired
    private GridFsTemplate gridFsTemplate;
    @Autowired
    private GridFsOperations gridOperation;

    @Override
    public <S extends Attachment> S save(S entity)
    {
        try
        {
            Document document = new Document();
            document.put("meta-data", entity.getFileMetadataDescription());
            ObjectId id = gridFsTemplate.store(entity.getAttachment().getInputStream(),
                                               entity.getFilename(),
                                               document);
            entity.setId(id.toString());
            return entity;
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public <S extends Attachment> Iterable<S> saveAll(Iterable<S> entities)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<Attachment> findById(String id)
    {
        Attachment attachment = new Attachment();
        GridFSFile found = gridOperation.find(new Query(Criteria.where("_id").is(id))).first();
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
