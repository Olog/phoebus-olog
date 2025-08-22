package org.phoebus.olog;

import org.springframework.stereotype.Service;

@Service
public class OlogResourceDescriptors
{
    public static final String OLOG_SERVICE = "Olog";
    static final String OLOG_SERVICE_INFO = OLOG_SERVICE;
    static final String TAG_RESOURCE_URI = OLOG_SERVICE + "/tags";
    static final String LOGBOOK_RESOURCE_URI = OLOG_SERVICE + "/logbooks";
    static final String PROPERTY_RESOURCE_URI = OLOG_SERVICE + "/properties";
    public static final String LOG_RESOURCE_URI = OLOG_SERVICE + "/logs";
    static final String SERVICE_CONFIGURATION_URI = OLOG_SERVICE + "/configuration";
    static final String ATTACHMENT_URI = OLOG_SERVICE + "/attachment";
    static final String HELP_URI = OLOG_SERVICE + "/help";
    public static final String LOG_TEMPLATE_RESOURCE_URI = OLOG_SERVICE + "/templates";
    public static final String LEVEL_RESOURCE_RUI = OLOG_SERVICE + "/levels";

    /**
     * The base path element for web socket related communication.
     * <p>
     *     <b>NOTE:</b>
     *     <ul>
     *         <li>Clients will need to connect to ws(s)://&lt;host&gt;:&lt;port&gt;/Olog/web-socket</li>
     *         <li>Clients will need to subscribe to /Olog/web-socket/&lt;topic name&gt;, e.g. /Olog/web-socket/messages</li>
     *     </ul>
     * </p>
     */
    public static final String WEB_SOCKET_BASE = "/web-socket";

    /**
     * Topic for the messages pushed from the service to subscriber clients, which need to specify it in the same manner.
     */
    public static final String WEB_SOCKET_MESSAGES_TOPIC = "/" + OLOG_SERVICE + WEB_SOCKET_BASE + "/messages";

    /**
     * Prefix of endpoints for client messages, i.e. /Olog/web-socket. An endpoint named/annotated &quot;echo&quot; will then
     * be specified by client as /Olog/web-socket/echo.
     */
    public static final String WEB_SOCKET_APPLICATION_PREFIX = "/" + OLOG_SERVICE + WEB_SOCKET_BASE;

}
