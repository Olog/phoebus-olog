package org.phoebus.olog;

/**
 * Utility class to assist in handling of text.
 *
 * @author Lars Johansson
 */
public class TextUtil {

    // common
    // olog
    // elastic
    // attachment
    // attribute
    // logbook
    // log
    // property
    // tag

    // ----------------------------------------------------------------------------------------------------

    public static final String COUNT_NOT_IMPLEMENTED                    = "Count is not implemented";
    public static final String SEARCH_CANNOT_PARSE_FROM_VALUE           = "Cannot parse from value {0} as number";
    public static final String SEARCH_CANNOT_PARSE_SIZE_VALUE           = "Cannot parse size value {0} as number";
    public static final String SEARCH_FAILED_PARSE_PARAMETERS_INVALID_START_END = "Failed to parse search parameters: {0}, CAUSE: Invalid start and end times";
    public static final String SEARCH_NOT_COMPLETED                     = "Failed to complete search";
    public static final String SEARCH_UNBALANCED_QUOTES                 = "Unbalanced quotes in search query";

    public static final String CONTENT_TYPE_NOT_DETERMINED              = "Unable to determine content type from file name {0}";
    public static final String GROUPING_NOT_ALLOWED                     = "Grouping not allowed due to conflicting log entry groups";
    public static final String GROUPING_ENTRIES_IN_DIFFERENT_GROUPS     = "Cannot group: at least two entries already contained in different groups";
    public static final String HELP_REQUEST_WHAT_FOR_LANGUAGE           = "Requesting {0} for language {1}";
    public static final String HELP_UNABLE_READ_FOR_LANGUAGE_DEFAULT    = "Unable to read {0} resource for language {1}, defaulting to 'en'";
    public static final String HELP_UNABLE_FIND_RESOURCE                = "Unable to read find resource {0}_en.html";
    public static final String PROPERTY_PROVIDER_FAILED_TO_RETURN       = "A property provider failed to return in time or threw exception";
    public static final String QUERY_FROM_CLIENT                        = "Query {0} from client {1}";
    public static final String UNSUPPORTED_DATE_TIME                    = "Unsupported date/time specified {0}";
    public static final String USER_NOT_AUTHENTICATED_THROUGH_AUTHORIZATION_HEADER = "User {0} not authenticated through authorization header";

    // ----------------------------------------------------------------------------------------------------

    public static final String OLOG_STARTING                            = "Starting Olog Service";
    public static final String OLOG_FAILED_CONFIGURE_TRUSTSTORE         = "failed to configure olog truststore";
    public static final String OLOG_FAILED_CREATE_SERVICE               = "Failed to create Olog service info resource";

    public static final String ELASTIC_CREATED_INDEX_ACKNOWLEDGED       = "Created index {0} acknowledged {1}";
    public static final String ELASTIC_FAILED_TO_CONNECT                = "Failed to connect to elastic {0}";
    public static final String ELASTIC_FAILED_TO_CREATE_INDEX           = "Failed to create index {0}";
    public static final String ELASTIC_FAILED_TO_INITIALIZE_LOGBOOK     = "Failed to initialize logbook {0}";
    public static final String ELASTIC_FAILED_TO_INITIALIZE_LOGBOOKS    = "Failed to initialize logbooks";
    public static final String ELASTIC_FAILED_TO_INITIALIZE_PROPERTY    = "Failed to initialize property {0}";
    public static final String ELASTIC_FAILED_TO_INITIALIZE_PROPERTIES  = "Failed to initialize properties";
    public static final String ELASTIC_FAILED_TO_INITIALIZE_TAG         = "Failed to initialize tag {0}";
    public static final String ELASTIC_FAILED_TO_INITIALIZE_TAGS        = "Failed to initialize tags";

    // ----------------------------------------------------------------------------------------------------

    public static final String ATTACHMENT_DATA_INVALID                  = "Attachment data invalid: file count does not match attachment count";
    public static final String ATTACHMENT_FILE_NOT_MATCHED_META_DATA    = "File {0} not matched with attachment meta-data";
    public static final String ATTACHMENT_NOT_FOUND                     = "Attachment with id {0} not found";
    public static final String ATTACHMENT_NOT_PERSISTED                 = "Unable to persist attachment {0}";
    public static final String ATTACHMENT_NOT_RETRIEVED                 = "Unable to retrieve attachment with id {0}";
    public static final String ATTACHMENT_REQUEST                       = "Requesting attachment {0}";
    public static final String ATTACHMENT_REQUEST_DETAILS               = "Requesting attachment {0} : {1}";
    public static final String ATTACHMENT_UNABLE_TO_RETRIEVE_FOR_ID     = "Unable to retrieve attachment {0} for log id {1}";

    public static final String ATTACHMENTS_NAMED_FOUND_FOR_ID           = "Found {0} attachments named {1} for log id {2}";

    public static final String ATTRIBUTE_NAME_CANNOT_BE_NULL_OR_EMPTY   = "The attribute name cannot be null or empty {0}";

    public static final String LOGBOOK_DELETE                           = "Deleted logbook {0}";
    public static final String LOGBOOK_EXISTS_FAILED                    = "Failed to check if logbook {0} exists";
    public static final String LOGBOOK_INVALID                          = "Logbook {0} is invalid";
    public static final String LOGBOOK_NAME_CANNOT_BE_NULL_OR_EMPTY     = "The logbook name cannot be null or empty {0}";
    public static final String LOGBOOK_NOT_EXISTS                       = "The logbook {0} does not exist";
    public static final String LOGBOOK_NOT_CREATED                      = "Failed to created logbook {0}";
    public static final String LOGBOOK_NOT_DELETED                      = "Failed to delete logbook {0}";
    public static final String LOGBOOK_NOT_FOUND                        = "Failed to find logbook {0}";

    public static final String LOGBOOKS_DELETE_ALL_NOT_ALLOWED          = "Deleting all logbooks is not allowed";
    public static final String LOGBOOKS_NOT_CREATED                     = "Failed to created logbooks {0}";
    public static final String LOGBOOKS_NOT_FOUND                       = "Failed to find logbooks";
    public static final String LOGBOOKS_NOT_FOUND_1                     = "Failed to find logbooks {0}";
    public static final String LOGBOOKS_NOT_SPECIFIED                   = "No logbooks specified";

    public static final String LOG_ENTRY_CANNOT_REPLY_NOT_EXISTS        = "Cannot reply to log entry {0} as it does not exist";
    public static final String LOG_ENTRY_NOTIFIER                       = "LogEntryNotifier {0} throws exception";
    public static final String LOG_ENTRY_ID_CREATED_FROM                = "Entry id {0} created from {1}";
    public static final String LOG_ENTRY_NOT_MATCH_PATH                 = "Log entry id does not match path variable";

    public static final String LOG_EXISTS_FAILED                        = "Failed to check if log {0} exists";
    public static final String LOG_ID_NOT_FOUND                         = "Log id {0} not found";
    public static final String LOG_INVALID_LOGBOOKS                     = "One or more invalid logbook name(s)";
    public static final String LOG_INVALID_TAGS                         = "One or more invalid tag name(s)";
    public static final String LOG_MUST_HAVE_LOGBOOK                    = "A log entry must specify at least one logbook";
    public static final String LOG_MUST_HAVE_TITLE                      = "A log entry must specify a title";
    public static final String LOG_NOT_ARCHIVED                         = "Failed to archive log with id {0}";
    public static final String LOG_NOT_FOUND                            = "Failed to find log {0}";
    public static final String LOG_NOT_RETRIEVED                        = "Failed to retrieve log with id {0}";
    public static final String LOG_NOT_SAVED                            = "Failed to save log entry {0}";
    public static final String LOG_NOT_TITLE                            = "Log title empty";
    public static final String LOG_NOT_UPDATED                          = "Failed to update log entry {0}";

    public static final String LOGS_DELETE_NOT_SUPPORTED                = "Deleting log entries is not supported";
    public static final String LOGS_NOT_FOUND                           = "Failed to find logs {0}";
    public static final String LOGS_RETRIEVE_ALL_NOT_SUPPORTED          = "Retrieving all log entries is not supported. Use Search with scroll";
    public static final String LOGS_SEARCH_NOT_COMPLETED                = "Failed to complete search for archived logs";

    public static final String PROPERTY_ATTRIBUTE_DELETE                = "Deleted property attribute {0}";
    public static final String PROPERTY_ATTRIBUTE_CANNOT_DELETE         = "Cannot delete attribute {0} from property {1} as the property does not exist";
    public static final String PROPERTY_DELETE                          = "Deleted property {0}";
    public static final String PROPERTY_EXISTS_FAILED                   = "Failed to check if property {0} exists";
    public static final String PROPERTY_NAME_CANNOT_BE_NULL_OR_EMPTY    = "The property name cannot be null or empty {0}";
    public static final String PROPERTY_NOT_EXISTS                      = "The property {0} does not exist";
    public static final String PROPERTY_NOT_CREATED                     = "Failed to create property {0}";
    public static final String PROPERTY_NOT_DELETED                     = "Failed to delete property {0}";
    public static final String PROPERTY_NOT_FOUND                       = "Failed to find property {0}";

    public static final String PROPERTIES_DELETE_ALL_NOT_ALLOWED        = "Deleting all properties is not allowed";
    public static final String PROPERTIES_NOT_CREATED                   = "Failed to create properties {0}";
    public static final String PROPERTIES_NOT_FOUND                     = "Failed to find properties";
    public static final String PROPERTIES_NOT_FOUND_1                   = "Failed to find properties {0}";

    public static final String TAG_DELETE                               = "Deleted tag {0}";
    public static final String TAG_EXISTS_FAILED                        = "Failed to check if tag {0} exists";
    public static final String TAG_INVALID                              = "Tag {0} is invalid";
    public static final String TAG_NAME_CANNOT_BE_NULL_OR_EMPTY         = "The tag name cannot be null or empty {0}";
    public static final String TAG_NOT_EXISTS                           = "The tag {0} does not exist";
    public static final String TAG_NOT_CREATED                          = "Failed to create tag {0}";
    public static final String TAG_NOT_DELETED                          = "Failed to delete tag {0}";
    public static final String TAG_NOT_FOUND                            = "Failed to find tag {0}";

    public static final String TAGS_DELETE_ALL_NOT_ALLOWED              = "Deleting all tags is not allowed";
    public static final String TAGS_NOT_CREATED                         = "Failed to create tags {0}";
    public static final String TAGS_NOT_FOUND                           = "Failed to find tags";
    public static final String TAGS_NOT_FOUND_1                         = "Failed to find tags {0}";

    /**
     * This class is not to be instantiated.
     */
    private TextUtil() {
        throw new IllegalStateException("Utility class");
    }

}
