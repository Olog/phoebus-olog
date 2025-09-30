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


import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import static org.phoebus.olog.OlogResourceDescriptors.WEB_SOCKET_MESSAGES_TOPIC;

/**
 * This {@link Controller} defines an echo endpoint, i.e. for testing or health check purposes...
 */
@Controller
@SuppressWarnings("unused")
public class WebSocketController {

    /**
     *
     * @param message Will be echoed back to subscribers to the {@link org.phoebus.olog.OlogResourceDescriptors#WEB_SOCKET_MESSAGES_TOPIC} topic
     * @return The message received in the call.
     */
    @MessageMapping("/echo")
    @SendTo(WEB_SOCKET_MESSAGES_TOPIC)
    public String echo(String message) {
        return message;
    }
}
