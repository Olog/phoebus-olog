package edu.msu.nscl.olog;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "edu.msu.nscl.olog")
@ComponentScan(basePackages = { "edu.msu.nscl.olog" })
public class Config {

	@Value("${elasticsearch.home:/usr/local/Cellar/elasticsearch/5.6.0}")
	private String elasticsearchHome;

	@Value("${elasticsearch.cluster.name:elasticsearch}")
	private String clusterName;

	@Bean
	public Client client() {
		
		System.out.println("creating client");
		Settings elasticsearchSettings = Settings.builder().put("client.transport.sniff", true)
				.put("cluster.name", clusterName).build();
		TransportClient client = new PreBuiltTransportClient(elasticsearchSettings);
		try {
			client.addTransportAddress(
					new TransportAddress(new InetSocketAddress(InetAddress.getByName("130.199.219.217"), 9300)));
			client.connectedNodes().forEach(System.out::println);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("returning client");
		System.out.println(client);
		return client;
	}

	@Bean
	public ElasticsearchOperations elasticsearchTemplate() {
		return new ElasticsearchTemplate(client());
	}
}