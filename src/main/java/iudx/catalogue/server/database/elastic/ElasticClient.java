package iudx.catalogue.server.database.elastic;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;

import java.io.IOException;

public class ElasticClient {

  private final ElasticsearchAsyncClient client;
  private final RestClient rsClient;

  public ElasticClient(
      String databaseIp, int databasePort, String index,
      String databaseUser, String databasePassword) {

    CredentialsProvider credentials = new BasicCredentialsProvider();
    credentials.setCredentials(AuthScope.ANY,
        new UsernamePasswordCredentials(databaseUser, databasePassword));

    this.rsClient = RestClient.builder(new HttpHost(databaseIp, databasePort))
        .setHttpClientConfigCallback(
            httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentials)
        ).build();

    ElasticsearchTransport transport = new RestClientTransport(rsClient, new JacksonJsonpMapper());
    this.client = new ElasticsearchAsyncClient(transport);
  }

  // to get the client for external use
  public ElasticsearchAsyncClient getClient() {
    return client;
  }

  public void close() throws IOException {
    rsClient.close();
  }
}
