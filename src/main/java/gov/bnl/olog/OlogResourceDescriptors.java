package gov.bnl.olog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OlogResourceDescriptors
{
    static final String OLOG_SERVICE = "Olog";
    static final String OLOG_SERVICE_INFO = OLOG_SERVICE;
    static final String TAG_RESOURCE_URI = OLOG_SERVICE + "/tags";
    static final String LOGBOOK_RESOURCE_URI = OLOG_SERVICE + "/logbooks";
    static final String PROPERTY_RESOURCE_URI = OLOG_SERVICE + "/properties";
    static final String LOG_RESOURCE_URI = OLOG_SERVICE + "/logs";
    static final String SERVICE_CONFIGURATION_URI = OLOG_SERVICE + "/configuration";
    static final String ATTACHMENT_URI = OLOG_SERVICE + "/attachment";

    // Read the elatic index and type from the application.properties
    @Value("${elasticsearch.tag.index:olog_tags}")
    String ES_TAG_INDEX;
    @Value("${elasticsearch.tag.type:olog_tag}")
    String ES_TAG_TYPE;
    @Value("${elasticsearch.logbook.index:olog_logbooks}")
    String ES_LOGBOOK_INDEX;
    @Value("${elasticsearch.logbook.type:olog_logbook}")
    String ES_LOGBOOK_TYPE;
    @Value("${elasticsearch.property.index:olog_properties}")
    String ES_PROPERTY_INDEX;
    @Value("${elasticsearch.property.type:olog_property}")
    String ES_PROPERTY_TYPE;
    @Value("${elasticsearch.log.index:olog_logs}")
    String ES_LOG_INDEX;
    @Value("${elasticsearch.log.type:olog_log}")
    String ES_LOG_TYPE;
    @Value("${elasticsearch.sequence.index:olog_sequence}")
    String ES_SEQ_INDEX;
    @Value("${elasticsearch.sequence.type:olog_sequence}")
    String ES_SEQ_TYPE;

}
