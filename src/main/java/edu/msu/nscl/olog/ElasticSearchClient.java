/**
 * 
 */
package edu.msu.nscl.olog;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;

/*
 * #%L
 * ChannelFinder Directory Service
 * %%
 * Copyright (C) 2010 - 2015 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * %%
 * Copyright (C) 2010 - 2012 Brookhaven National Laboratory
 * All rights reserved. Use is subject to license terms.
 * #L%
 */

import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

/**
 * @author Kunal Shroff {@literal <shroffk@bnl.gov>}
 *
 */
public class ElasticSearchClient implements ServletContextListener {

    private static Logger log = Logger.getLogger(ElasticSearchClient.class.getName());

    private static Settings settings;

    private static TransportClient searchClient;
    private static TransportClient indexClient;

    public static TransportClient getSearchClient() {
        if(searchClient == null) {
            initialize();
        }
        return searchClient;
    }

    public static TransportClient getIndexClient() {
        if(indexClient == null) {
            initialize();
        }
        return indexClient;
    }

    /**
     * Returns a new {@link TransportClient} using the default settings
     * **IMPORTANT** it is the responsibility of the caller to close this client
     * 
     * @return es transport client
     */
    @SuppressWarnings("resource")
    public static TransportClient getNewClient() {
        try {
            InetAddress host = InetAddress.getByName(settings.get("network.host", "localhost"));
            int port = Integer.valueOf(settings.get("transport.tcp.port", "9300"));
            return new PreBuiltTransportClient(Settings.EMPTY)
                    .addTransportAddress(new InetSocketTransportAddress(host, port));
        } catch (ElasticsearchException | UnknownHostException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            return null;
        }
    }

    public static Settings getSettings() {
        return settings;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        initialize();
    }

    static void initialize() {
        log.info("Initializing a new Transport clients.");
        searchClient = new PreBuiltTransportClient(Settings.EMPTY);
        indexClient = new PreBuiltTransportClient(Settings.EMPTY);
        settings = searchClient.settings();
        InetAddress host;
        try {
            host = InetAddress.getByName(settings.get("network.host", "localhost"));
            int port = Integer.valueOf(settings.get("transport.tcp.port", "9300"));
            searchClient.addTransportAddress(new InetSocketTransportAddress(host, port));
            indexClient.addTransportAddress(new InetSocketTransportAddress(host, port));
        } catch (UnknownHostException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        close();
    }

    void close() {
        log.info("Closeing the default Transport clients.");
        if (searchClient != null)
            searchClient.close();
        if (indexClient != null)
            indexClient.close();
    }

}
