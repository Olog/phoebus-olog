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

    public static final String WEB_SOCKET_CONNECT_ENDPOINT = "/" + OLOG_SERVICE + "/web-socket";
    public static final String WEB_SOCKET_MESSAGES_TOPIC = "/" + OLOG_SERVICE + "/messages";
}
