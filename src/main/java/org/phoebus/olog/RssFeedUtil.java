package org.phoebus.olog;

import com.rometools.rome.feed.rss.Category;
import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Description;
import com.rometools.rome.feed.rss.Item;
import org.apache.commons.lang3.StringUtils;
import org.phoebus.olog.entity.Log;

import java.util.Date;
import java.util.List;

import static org.phoebus.olog.OlogResourceDescriptors.LOG_RESOURCE_URI;

public class RssFeedUtil {

    public static String logUrl(String baseUrl, Long logId) {
        return baseUrl + LOG_RESOURCE_URI + "/" + logId;
    }

    public static Item fromLog(Log logEntry, String baseUrl) {
        Item item = new Item();
        item.setTitle(logEntry.getTitle());

        // Construct the link to the individual channel resource
        String entryUrl = logUrl(baseUrl, logEntry.getId());
        item.setLink(entryUrl);

        item.setCategories(logEntry.getTags()
            .stream()
            .map(t -> {
                Category c = new Category();
                c.setValue(t.getName());
                return c;
            })
            .toList()
        );

        item.setAuthor(logEntry.getOwner());

        Description description = new Description();
        description.setType("text/plain");
        if (!StringUtils.isEmpty(logEntry.getSource())) {
            description.setValue(logEntry.getSource());
        } else if (!StringUtils.isEmpty(logEntry.getDescription())) {
            description.setValue(logEntry.getDescription());
        }

        item.setDescription(description);

        if (logEntry.getCreatedDate() != null) {
            item.setPubDate(Date.from(logEntry.getCreatedDate()));
        }
        return item;
    }

    public static Channel fromLogEntries(List<Log> logs, String baseUrl) {
        Channel feed = new Channel();
        feed.setFeedType("rss_2.0");
        feed.setTitle("Olog Service - Log Updates");
        feed.setDescription("Latest logs created or updated in the Olog Service");
        feed.setLink(baseUrl);
        List<Item> items = logs.stream().map(logEntry -> fromLog(logEntry, baseUrl)).toList();
        feed.setItems(items);
        return feed;

    }
}