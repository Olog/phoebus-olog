package gov.bnl.olog;

public class OlogResourceDescriptors
{

    static final String OLOG_SERVICE = "Olog";
    static final String TAG_RESOURCE_URI = OLOG_SERVICE + "/tags";
    static final String LOGBOOK_RESOURCE_URI = OLOG_SERVICE + "/logbooks";
    static final String PROPERTY_RESOURCE_URI = OLOG_SERVICE + "/properties";
    static final String LOG_RESOURCE_URI = OLOG_SERVICE + "/logs";

    public static final String ES_TAG_INDEX = "olog_tags";
    public static final String ES_TAG_TYPE = "olog_tag";

    public static final String ES_LOGBOOK_INDEX = "olog_logbooks";
    public static final String ES_LOGBOOK_TYPE = "olog_logbook";

    public static final String ES_PROPERTY_INDEX = "olog_properties";
    public static final String ES_PROPERTY_TYPE = "olog_property";

    public static final String ES_LOG_INDEX = "olog_logs";
    public static final String ES_LOG_TYPE = "olog_log";
    
    public static final String ES_SEQ_INDEX = "olog_sequence";
    public static final String ES_SEQ_TYPE = "olog_sequence";
}
