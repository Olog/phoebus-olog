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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.PrincipalMethodArgumentResolver;

import java.util.List;

@Configuration
@PropertySource("classpath:application.properties")
@SuppressWarnings("unused")
public class WebConfig implements WebMvcConfigurer {

    /**
     * Specifies the allowed origins for CORS requests. Defaults to http://localhost:3000,
     * which is useful during development of the web front-end in NodeJS.
     */
    @Value("#{'${cors.allowed.origins:http://localhost:3000}'.split(',')}")
    private String[] corsAllowedOrigins;

    @Value("#{'${cors.allowed.methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}'.split(',')}")
    private String[] corsAllowedMethods;

    @Value("#{'${cors.allowed.headers:*}'.split(',')}")
    private String[] corsAllowedHeaders;

    @Value("${cors.allow.credentials:true}")
    private boolean corsAllowCredentials;

    @Value("${cors.max.age:3600}")
    private long corsMaxAge;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(corsAllowedOrigins)
                .allowedMethods(corsAllowedMethods)
                .allowedHeaders(corsAllowedHeaders)
                .allowCredentials(corsAllowCredentials)
                .maxAge(corsMaxAge);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers){
        resolvers.add(new PrincipalMethodArgumentResolver());
    }
}
