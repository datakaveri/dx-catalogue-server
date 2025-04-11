package iudx.catalogue.server.apiserver.item.service;

import io.vertx.core.Future;
import iudx.catalogue.server.apiserver.item.model.Item;
import iudx.catalogue.server.common.util.DbResponseMessageBuilder;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import iudx.catalogue.server.database.elastic.util.ResponseFilter;
import java.util.List;

/**
 * Repository interface for item-related operations in Elasticsearch.
 */
public interface ItemService {

  /**
   * Searches for items based on the query model and response filter.
   *
   * @param queryModel The query model defining search criteria.
   * @param filter The response filter determining result format.
   * @return A Future containing search results wrapped in a DbResponseMessageBuilder.
   */
  Future<DbResponseMessageBuilder> search(QueryModel queryModel, ResponseFilter filter);

  Future<DbResponseMessageBuilder> search(String id, List<String> includeFields);
  /**
   * Creates a new item in the Elasticsearch index.
   *
   * @param item The item to be created.
   * @return A Future containing the created item.
   */
  Future<Item> create(Item item);

  /**
   * Updates an existing item in the Elasticsearch index.
   *
   * @param item The updated item data.
   * @return A Future containing the updated item.
   */
  Future<Item> update(Item item);

  /**
   * Deletes an item from the Elasticsearch index.
   *
   * @param itemId The unique identifier of the item to delete.
   * @return A Future containing the ID of the deleted item.
   */
  Future<String> delete(String itemId);

  /**
   * Counts the number of items matching the given query model.
   *
   * @param queryModel The query model defining count criteria.
   * @return A Future containing an instance of DbResponseMessageBuilder with the count result.
   */
  Future<DbResponseMessageBuilder> count(QueryModel queryModel);
}
