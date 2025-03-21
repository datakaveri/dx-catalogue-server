package iudx.catalogue.server.database.elastic.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.elastic.model.ElasticsearchResponse;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import java.util.List;

@VertxGen
@ProxyGen
public interface ElasticsearchService {

  @GenIgnore
  static ElasticsearchService createProxy(Vertx vertx, String address) {
    return new ElasticsearchServiceVertxEBProxy(vertx, address);
  }

  Future<List<ElasticsearchResponse>> search(String index, QueryModel queryModel);

  Future<JsonObject> createDocument(String index, JsonObject document);

  Future<JsonObject> updateDocument(String index, String id, JsonObject document);

  /**
   * Partially updates an existing item in the Elasticsearch index.
   *
   * @param queryModel The query instance document containing the fields to update.
   * @param docId The unique identifier of the document to be updated.
   * @return A Future containing the updated document.
   */
  Future<JsonObject> patchDocument(String index, String docId, QueryModel queryModel);

  Future<JsonObject> deleteDocument(String index, String id);

  Future<Integer> count(String index, QueryModel queryModel);
}
