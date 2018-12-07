package edu.msu.nscl.olog;

import java.util.Optional;

import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import edu.msu.nscl.olog.entity.Tag;

@Repository
public class TagRepository implements ElasticsearchRepository<Tag, String> {

	@Autowired
	Client client;
	
	public Client getClient() {

		System.out.println(client);
		return this.client;
	}
	
	@Override
	public Iterable<Tag> findAll(Sort sort) {
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<Tag> findAll(Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S extends Tag> S save(S entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S extends Tag> Iterable<S> saveAll(Iterable<S> entities) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<Tag> findById(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean existsById(String id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterable<Tag> findAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<Tag> findAllById(Iterable<String> ids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long count() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void deleteById(String id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(Tag entity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteAll(Iterable<? extends Tag> entities) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteAll() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Iterable<Tag> search(QueryBuilder query) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<Tag> search(QueryBuilder query, Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<Tag> search(SearchQuery searchQuery) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<Tag> searchSimilar(Tag entity, String[] fields, Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void refresh() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Class<Tag> getEntityClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S extends Tag> S index(S entity) {
		// TODO Auto-generated method stub
		return null;
	}
}
