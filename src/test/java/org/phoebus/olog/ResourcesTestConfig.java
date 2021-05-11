/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.olog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.gridfs.GridFSBucket;

import gov.bnl.olog.AttachmentRepository;
import gov.bnl.olog.LogRepository;
import gov.bnl.olog.LogSearchUtil;
import gov.bnl.olog.LogbookRepository;
import gov.bnl.olog.PropertyRepository;
import gov.bnl.olog.TagRepository;
import gov.bnl.olog.WebSecurityConfig;

import org.elasticsearch.client.RestHighLevelClient;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.h2.H2ConsoleProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@TestConfiguration
@Import(WebSecurityConfig.class)
// Must exclude explicitly for IT tests. Documentation claims that TestConfigurations should not
// contribute beans unless explicilty included, but component scan may (?) override this.
@Profile("!ITtest")
public class ResourcesTestConfig {

    @Bean
    public LogbookRepository logbookRepository() {
        return Mockito.mock(LogbookRepository.class);
    }

    @Bean
    public PropertyRepository propertyRepository() {
        return Mockito.mock(PropertyRepository.class);
    }

    @Bean
    public LogRepository logRepository() {
        return Mockito.mock(LogRepository.class);
    }

    @Bean
    public AttachmentRepository attachmentRepository() {
        return Mockito.mock(AttachmentRepository.class);
    }

    @Bean("indexClient")
    public RestHighLevelClient client() {
        return Mockito.mock(RestHighLevelClient.class);
    }

    @Bean
    public GridFsOperations gridOperation() {
        return Mockito.mock(GridFsOperations.class);
    }

    @Bean
    public GridFsTemplate gridFsTemplate() {
        return Mockito.mock(GridFsTemplate.class);
    }

    @Bean
    public LogSearchUtil logSearchUtil() {
        return Mockito.mock(LogSearchUtil.class);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public PlatformTransactionManager platformTransactionManager() {
        return Mockito.mock(PlatformTransactionManager.class);
    }

    @Bean
    public DataSource dataSource() {
        return Mockito.mock(DataSource.class);
    }

    @Bean
    public H2ConsoleProperties h2ConsoleProperties() {
        return Mockito.mock(H2ConsoleProperties.class);
    }

    @Bean
    public TagRepository tagRepository(){
        return Mockito.mock(TagRepository.class);
    }

    @Bean
    public GridFSBucket gridFSBucket(){
        return Mockito.mock(GridFSBucket.class);
    }
}
