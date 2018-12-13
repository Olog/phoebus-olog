package edu.msu.nscl.olog;

public class OlogResourceDescriptors {

    static final String OLOG_SERVICE = "Olog";
    static final String TAG_RESOURCE_URI = OLOG_SERVICE+"/resources/tags";
    static final String LOGBOOK_RESOURCE_URI = OLOG_SERVICE+"/logbooks";
    static final String PROPERTY_RESOURCE_URI = OLOG_SERVICE+"/properties";
    static final String LOG_RESOURCE_URI = OLOG_SERVICE+"/logs";
    
    static final String ES_TAG_INDEX = "olog_tags";
    static final String ES_TAG_TYPE = "olog_tag";
    
    static final String ES_LOGBOOK_INDEX = "olog_logbooks";
    static final String ES_LOGBOOK_TYPE = "olog_logbook";
}
