package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.phoebus.olog.entity.LogbookMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.phoebus.olog.ElasticConfig.ES_LOGBOOK_MEMBER_INDEX;

@Repository
public class LogbookMemberRepository {

    @Autowired
    @Qualifier("client")
    private ElasticsearchClient client;

    public LogbookMember save(LogbookMember member) {
        try {
            client.index(IndexRequest.of(i -> i.index(ES_LOGBOOK_MEMBER_INDEX)
                    .id(member.getId())
                    .document(member)
                    .refresh(Refresh.True)));
            return member;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save logbook membership", e);
        }
    }

    public Optional<LogbookMember> find(String logbook, String infnUuid) {
        try {
            GetResponse<LogbookMember> response = client.get(GetRequest.of(g -> g
                    .index(ES_LOGBOOK_MEMBER_INDEX)
                    .id(LogbookMember.id(logbook, infnUuid))), LogbookMember.class);
            return response.found() ? Optional.ofNullable(response.source()) : Optional.empty();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to retrieve logbook membership", e);
        }
    }

    public List<LogbookMember> findByLogbook(String logbook) {
        try {
            SearchRequest request = SearchRequest.of(s -> s.index(ES_LOGBOOK_MEMBER_INDEX)
                    .query(q -> q.term(t -> t.field("logbook").value(logbook)))
                    .size(1000));
            SearchResponse<LogbookMember> response = client.search(request, LogbookMember.class);
            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to list logbook memberships", e);
        }
    }

    public void delete(String logbook, String infnUuid) {
        try {
            client.delete(DeleteRequest.of(d -> d.index(ES_LOGBOOK_MEMBER_INDEX)
                    .id(LogbookMember.id(logbook, infnUuid))
                    .refresh(Refresh.True)));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to delete logbook membership", e);
        }
    }

    public void deleteByLogbook(String logbook) {
        findByLogbook(logbook).forEach(member -> delete(logbook, member.getInfnUuid()));
    }
}
