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

import org.phoebus.olog.entity.ServiceConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.phoebus.olog.OlogResourceDescriptors.SERVICE_CONFIGURATION_URI;

import java.util.List;

@RestController
@RequestMapping(SERVICE_CONFIGURATION_URI)
public class ServiceConfigurationResource {

    @Autowired
    private LogbookRepository logbookRepository;

    @Autowired
    private TagRepository tagRepository;

    @Value("#{'${levels:Urgent,Suggestion,Info,Request,Problem}'.split(',')}")
    private List<String> levels;

    @GetMapping
    public ServiceConfiguration serviceConfiguration(){
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
        serviceConfiguration.setLevels(levels);
        serviceConfiguration.setLogbooks(logbookRepository.findAll());
        serviceConfiguration.setTags(tagRepository.findAll());
        return serviceConfiguration;
    }
}
