/**
 * 
 */
package edu.msu.nscl.olog;

/**
 * @author Kunal Shroff
 *
 */
public class LogManager {
//
//    public static final String ologLogIndex = "olog_logs";
//    // instance a json mapper
//    private static final ObjectMapper mapper = new ObjectMapper();
//
//    private static final Logger log = Logger.getLogger(LogManager.class.getName());
//
//    /**
//     * Return a list of the logs ordered by time and limited by the default query
//     * size limit of 10k
//     * 
//     * @return returns the
//     */
//    public static List<Log> listRecentLogs() {
//        try {
//            SearchResponse response = getSearchClient().prepareSearch(ologLogIndex).setSize(1000).get();
//            List<Log> result = new ArrayList<Log>();
//            response.getHits().forEach(h -> {
//                BytesReference b = h.getSourceRef();
//                try {
//                    Log log = mapper.readValue(b.streamInput(), Log.class);
//                    result.add(log);
//                } catch (IOException e) {
//                    log.log(Level.SEVERE, e.getMessage(), e);
//                }
//            });
//            return result;
//        } catch (Exception e) {
//            log.log(Level.SEVERE, e.getMessage(), e);
//        }
//        return Collections.emptyList();
//    }
//
//    public static Log createLog(Log log) {
//        try {
//            long id = SequenceGenerator.getID();
//            log.setId(id);
//            IndexResponse response = ElasticSearchClient.getIndexClient().prepareIndex(ologLogIndex, "log", String.valueOf(id))
//                    .setSource(mapper.writeValueAsBytes(log), XContentType.JSON).get();
//            if (response.getResult().equals(Result.UPDATED)) {
//                BytesReference ref = getSearchClient().prepareGet(ologLogIndex, "log", response.getId()).get()
//                        .getSourceAsBytesRef();
//                Log deletedTag = mapper.readValue(ref.streamInput(), Log.class);
//                return deletedTag;
//            }
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return null;
//    }
}
