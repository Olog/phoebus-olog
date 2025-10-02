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

package org.phoebus.olog.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import static org.phoebus.olog.OlogResourceDescriptors.WEB_SOCKET_APPLICATION_PREFIX;
import static org.phoebus.olog.OlogResourceDescriptors.WEB_SOCKET_BASE;
import static org.phoebus.olog.OlogResourceDescriptors.WEB_SOCKET_MESSAGES_TOPIC;

/**
 * Sets up bare minimum web socket for STOMP clients.
 * <p>
 *     <b>NOTE:</b> Client side URL/paths are:
 *     <ul>
 *         <li>Connection established on ws(s)://host:port/Olog/web-socket</li>
 *         <li>Subscription to messages (i.e. topic name) on: /Olog/web-socket/messages</li>
 *         <li>Echo endpoint (for testing purposes): /Olog/web-socket/echo. Message is echoed to topic /Olog/web-socket/messages.</li>
 *     </ul>
 * </p>
 */
@Configuration
@EnableWebSocketMessageBroker
@SuppressWarnings("unused")
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private TaskScheduler messageBrokerTaskScheduler;

    /**
     * Specifies the allowed origins for CORS requests. Defaults to http://localhost:3000,
     * which is useful during development of the web front-end in NodeJS.
     */
    @Value("#{'${cors.allowed.origins:http://localhost:3000}'.split(',')}")
    private String[] corsAllowedOrigins;

    @Autowired
    public void setMessageBrokerTaskScheduler(@Lazy TaskScheduler taskScheduler) {
        this.messageBrokerTaskScheduler = taskScheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(WEB_SOCKET_MESSAGES_TOPIC)
                .setHeartbeatValue(new long[]{30000, 30000})
                .setTaskScheduler(this.messageBrokerTaskScheduler);
        config.setApplicationDestinationPrefixes(WEB_SOCKET_APPLICATION_PREFIX);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(WEB_SOCKET_BASE).setAllowedOriginPatterns(corsAllowedOrigins);
    }
}
