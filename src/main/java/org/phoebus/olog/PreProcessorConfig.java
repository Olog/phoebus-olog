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

import org.phoebus.olog.entity.preprocess.LogPropertyProvider;
import org.phoebus.olog.entity.preprocess.MarkupCleaner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

@Configuration
public class PreProcessorConfig {

    @Bean
    public List<MarkupCleaner> markupCleaners() {
        List<MarkupCleaner> cleaners = new ArrayList<>();
        ServiceLoader<MarkupCleaner> loader = ServiceLoader.load(MarkupCleaner.class);
        loader.stream().forEach(p -> {
            MarkupCleaner cleaner = p.get();
            cleaners.add(cleaner);
        });
        return cleaners;
    }

    @Bean
    public List<LogPropertyProvider> propertyProviders() {
        List<LogPropertyProvider> providers = new ArrayList<>();
        ServiceLoader<LogPropertyProvider> loader = ServiceLoader.load(LogPropertyProvider.class);
        loader.stream().forEach(p -> {
            LogPropertyProvider provider = p.get();
            providers.add(provider);
        });
        return providers;
    }
}
