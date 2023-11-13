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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@TestConfiguration
// Must exclude explicitly for IT tests. Documentation claims that TestConfigurations should not
// contribute beans unless explicitly included, but component scan may (?) override this.
@Profile("!ITtest")
public class LogEntryValidatorTestConfig {

    @Bean
    public LogbookRepository logbookRepository() {
        return Mockito.mock(LogbookRepository.class);
    }

    @Bean
    public TagRepository tagRepository() {
        return Mockito.mock(TagRepository.class);
    }

    @Bean("client")
    public ElasticsearchClient client() {
        return Mockito.mock(ElasticsearchClient.class);
    }

    @Bean
    public LogEntryValidator logEntryValidator() {
        return new LogEntryValidator();
    }
}
