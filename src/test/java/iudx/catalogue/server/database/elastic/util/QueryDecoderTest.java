package iudx.catalogue.server.database.elastic.util;

import static iudx.catalogue.server.database.elastic.util.Constants.*;
import static iudx.catalogue.server.database.elastic.util.Constants.KEYWORD_KEY;
import static iudx.catalogue.server.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.Configuration;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.database.elastic.ElasticsearchVerticle;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import java.util.stream.Stream;
import jdk.jfr.Description;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QueryDecoderTest {

  private static QueryDecoder queryDecoder;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {

    JsonObject elasticConfig = Configuration.getConfiguration("./configs/config-test.json", 0);

    vertx.deployVerticle(new ElasticsearchVerticle(), new
            DeploymentOptions().setConfig(elasticConfig), testContext.completing());
    queryDecoder = new QueryDecoder();
    testContext.completed();
  }

  static Stream<Arguments> mustQuery(){
    return Stream.of(
        Arguments.of(
            RESOURCE, ITEM_TYPE_RESOURCE),
        Arguments.arguments(
            RESOURCE_GRP, ITEM_TYPE_RESOURCE_GROUP),
        Arguments.arguments(
            RESOURCE_SVR, ITEM_TYPE_RESOURCE_SERVER),
        Arguments.arguments(
            PROVIDER, ITEM_TYPE_PROVIDER));
  }

  static Stream<Arguments> shouldQuery(){
    return Stream.of(
                        Arguments.arguments(
                    RESOURCE_GRP),
            Arguments.arguments(
                    PROVIDER),
            Arguments.arguments(
                    RESOURCE_SVR),
            Arguments.arguments(
                    COS_ITEM));
  }

  @Test
  @Order(1)
  @DisplayName("GeoPoint request to DbQuery")
  public void searchGeoPointTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(GEOPROPERTY, LOCATION)
            .put(GEORELATION, GEOREL_WITHIN)
            .put(MAX_DISTANCE, 5000)
            .put(GEOMETRY, POINT)
            .put(COORDINATES, new JsonArray().add(73.927285).add(18.502712))
            .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
            .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);
    assertTrue(json.containsKey(QUERY_KEY), "The result should contain the QUERY_KEY.");
    QueryModel query = (QueryModel) json.getValue(QUERY_KEY);

    assertEquals(GEO_CIRCLE,
        query.toJson().getJsonObject("queries").getJsonArray("filterQueries").getJsonObject(0)
            .getJsonObject("queryParameters").getString(TYPE));
    testContext.completeNow();
  }

  @Test
  @Order(2)
  @DisplayName("GeoPolygon request to DbQuery")
  public void searchGeoPolygonTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(GEOPROPERTY, LOCATION)
            .put(GEORELATION, GEOREL_WITHIN)
            .put(MAX_DISTANCE, 5000)
            .put(GEOMETRY, POLYGON)
            .put(COORDINATES,
                    new JsonArray().add(new JsonArray().add(new JsonArray().add(75.9).add(14.5))
                            .add(new JsonArray().add(72).add(13)).add(new JsonArray().add(73).add(20))
                            .add(new JsonArray().add(75.9).add(14.5))))
            .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
            .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);
    assertTrue(json.containsKey(QUERY_KEY), "The result should contain the QUERY_KEY.");
    QueryModel query = (QueryModel) json.getValue(QUERY_KEY);

    assertEquals(POLYGON,
        query.toJson().getJsonObject("queries").getJsonArray("filterQueries").getJsonObject(0)
            .getJsonObject("queryParameters").getString(TYPE));
    testContext.completeNow();
  }

  @Test
  @Order(3)
  @DisplayName("GeoBbox request to DbQuery")
  public void searchGeoBboxTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(GEOPROPERTY, LOCATION)
            .put(GEORELATION, GEOREL_WITHIN)
            .put(MAX_DISTANCE, 5000)
            .put(GEOMETRY, BBOX)
            .put(COORDINATES,
                    new JsonArray().add(new JsonArray().add(73).add(20))
                            .add(new JsonArray().add(75).add(14)))
            .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
            .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);
    assertTrue(json.containsKey(QUERY_KEY), "The result should contain the QUERY_KEY.");
    QueryModel query = (QueryModel) json.getValue(QUERY_KEY);

    assertEquals(GEO_BBOX,
        query.toJson().getJsonObject("queries").getJsonArray("filterQueries").getJsonObject(0)
            .getJsonObject("queryParameters").getString(TYPE));
    testContext.completeNow();
  }

  @Test
  @Order(4)
  @DisplayName("GeoLineString request to DbQuery")
  public void searchGeoLineStringTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(GEOPROPERTY, LOCATION)
            .put(GEORELATION, GEOREL_WITHIN)
            .put(MAX_DISTANCE, 5000)
            .put(GEOMETRY, LINESTRING)
            .put(COORDINATES,
                    new JsonArray().add(new JsonArray().add(73.874537).add(18.528311))
                            .add(new JsonArray().add(73.836808).add(18.572797))
                            .add(new JsonArray().add(73.876484).add(18.525007)))
            .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
            .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);
    assertTrue(json.containsKey(QUERY_KEY), "The result should contain the QUERY_KEY.");
    QueryModel query = (QueryModel) json.getValue(QUERY_KEY);

    assertEquals(LINESTRING,
        query.toJson().getJsonObject("queries").getJsonArray("filterQueries").getJsonObject(0)
            .getJsonObject("queryParameters").getString(TYPE));
    testContext.completeNow();
  }

  @Test
  @Order(5)
  @DisplayName("Invalid QueryParameters")
  public void searchInvalidQueryParametersTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(GEOPROPERTY, LOCATION)
            .put(GEORELATION, GEOREL_WITHIN)
            .put(MAX_DISTANCE, 5000)
            .put(GEOMETRY, SHAPE_KEY)
            .put(COORDINATES,
                    new JsonArray().add(new JsonArray().add(73.874537).add(18.528311))
                            .add(new JsonArray().add(73.836808).add(18.572797))
                            .add(new JsonArray().add(73.876484).add(18.525007)))
            .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
            .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);

    //  assertEquals(DETAIL_INVALID_GEO_PARAMETER, json.getString(ERROR));
    testContext.completeNow();
  }

  @Test
  @Order(6)
  @DisplayName("Text search request to DbQuery")
  public void searchTextTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(Q_VALUE, "Golibar Square")
            .put(SEARCH_TYPE, SEARCH_TYPE_TEXT)
            .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);
    assertTrue(json.containsKey(QUERY_KEY), "The result should contain the QUERY_KEY.");
    QueryModel query = (QueryModel) json.getValue(QUERY_KEY);

    assertEquals("Golibar Square",
        query.toJson().getJsonObject("queries").getJsonArray("mustQueries").getJsonObject(0)
            .getJsonObject("queryParameters").getString(Q_VALUE));
    testContext.completeNow();
  }

  @Test
  @Order(7)
  @DisplayName("Attribute search request to DbQuery")
  public void searchAttributeTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(PROPERTY, new JsonArray().add(ID))
            .put(VALUE,
                    new JsonArray().add(
                            new JsonArray().add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs."
                                    + "iudx.io/aqm-bosch-climo/Ambedkar society circle_29")))
            .put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE)
            .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);
    assertTrue(json.containsKey(QUERY_KEY), "The result should contain the QUERY_KEY.");
    QueryModel query = (QueryModel) json.getValue(QUERY_KEY);

    assertEquals("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs."
            + "iudx.io/aqm-bosch-climo/Ambedkar society circle_29",
        query.toJson().getJsonObject("queries").getJsonArray("mustQueries").getJsonObject(0)
            .getJsonArray("shouldQueries").getJsonObject(0)
            .getJsonObject("queryParameters").getString(VALUE));
    testContext.completeNow();
  }

  @Test
  @Order(8)
  @DisplayName("Tag search request to DbQuery")
  public void searchTagTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject().put(PROPERTY, new JsonArray().add(TAGS))
            .put(VALUE, new JsonArray().add(new JsonArray().add("pollution")))
            .put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE).put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);
    assertTrue(json.containsKey(QUERY_KEY), "The result should contain the QUERY_KEY.");
    QueryModel query = (QueryModel) json.getValue(QUERY_KEY);

    assertEquals("pollution",
        query.toJson().getJsonObject("queries").getJsonArray("mustQueries").getJsonObject(0)
            .getJsonArray("shouldQueries").getJsonObject(0)
            .getJsonObject("queryParameters").getString(VALUE));
    testContext.completeNow();
  }

  @Test
  @Order(9)
  @DisplayName("Relationship search request to DbQuery")
  public void searchRelationshipTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject().put(ID,
                            "2596264a-ff2a-40f7-90cc-17a57b2adffe")
        .put(ITEM_TYPE, "iudx:Resource")
        .put(RESOURCE_GRP, "rg-id")
            .put(RELATIONSHIP, RESOURCE_GRP);

    JsonObject json = queryDecoder.listRelationshipQueryModel(requests).toJson();

    assertEquals(ITEM_TYPE_RESOURCE_GROUP,
        json.getJsonArray("mustQueries").getJsonObject(1)
            .getJsonObject("queryParameters").getString(VALUE));
    testContext.completeNow();
  }

  @Test
  @Order(10)
  @DisplayName("Relationship search request to DbQuery")
  public void searchRelationshipTest2(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(ID, "2596264a-ff2a-40f7-90cc-17a57b2adffe")
        .put(ITEM_TYPE, "iudx:Resource")
        .put(RESOURCE_SVR, "rs-id")
            .put(RELATIONSHIP, RESOURCE_SVR);

    JsonObject json = queryDecoder.listRelationshipQueryModel(requests).toJson();
    assertEquals(ITEM_TYPE_RESOURCE_SERVER,
        json.getJsonArray("mustQueries").getJsonObject(1)
            .getJsonObject("queryParameters").getString(VALUE));
    testContext.completeNow();
  }

  @Test
  @Order(11)
  @DisplayName("Relationship search request to DbQuery")
  public void listItemTagTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(ITEM_TYPE, TAGS)
            .put(TYPE, TAGS);

    JsonObject json = queryDecoder.listItemQueryModel(requests).toJson();
    assertEquals(TAGS + KEYWORD_KEY,
        json.getJsonArray("aggregations").getJsonObject(0)
            .getJsonObject("aggregationParameters").getString(FIELD));
    testContext.completeNow();
  }

  @Test
  @Description("test listItemQuery method when itemType equals TAGS")
  public void testListItemQueryTag(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(ITEM_TYPE,TAGS);
    request.put(INSTANCE,"dummy").put(LIMIT,2).put(OFFSET,1);

    JsonObject json = queryDecoder.listItemQueryModel(request).toJson();
    assertEquals(2, json.getJsonArray("aggregations").getJsonObject(0).getJsonObject(
        "aggregationParameters").getInteger("size"));
    assertEquals("TERMS", json.getJsonArray("aggregations").getJsonObject(0).getString("aggregationType"));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test listItemQuery method when itemType not equals TAGS")
  public void testListItemQuery(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(ITEM_TYPE,"dummy")
            .put(TYPE_KEY,"dummy").put(LIMIT,2).put(OFFSET,1);
    request.put(INSTANCE,null);

    JsonObject json = queryDecoder.listItemQueryModel(request).toJson();
    assertEquals("dummy",
        json.getJsonObject("queries").getJsonArray("filterQueries").getJsonObject(0).getJsonObject("queryParameters").getString(VALUE));
    assertEquals(ID_KEYWORD, json.getJsonArray("aggregations").getJsonObject(0).getJsonObject(
        "aggregationParameters").getString(FIELD));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test listItemQuery method when itemType not equals TAGS and instanceID is not null/empty")
  public void testListItemQueryInstance(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(ITEM_TYPE,"dummy")
            .put(TYPE_KEY,"dummy")
            .put(LIMIT,2).put(OFFSET,1);
    request.put(INSTANCE,"dummy");

    JsonObject json = queryDecoder.listItemQueryModel(request).toJson();
    assertEquals(ID_KEYWORD, json.getJsonArray("aggregations").getJsonObject(0).getJsonObject(
        "aggregationParameters").getString(FIELD));
    assertEquals(INSTANCE + KEYWORD_KEY,
        json.getJsonObject("queries").getJsonArray("filterQueries").getJsonObject(1).getJsonObject("queryParameters").getString(FIELD));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test SearchQuery method when searchType equals GEOSEARCH_REGEX")
  public void testSearchQueryGeosearch(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(SEARCH_TYPE,GEOSEARCH_REGEX)
            .put(ITEM_TYPE,"dummy")
            .put(SEARCH,false);

    assertEquals(new JsonObject().put(ERROR,new RespBuilder().withType(TYPE_INVALID_GEO_PARAM).withTitle(TITLE_INVALID_GEO_PARAM).withDetail("Missing/Invalid geo parameters").getJsonResponse()),queryDecoder.searchQuery(request));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test listItemQuery method when itemType not equals TAGS and instanceID is not null/empty")
  public void testSearchQueryTextSearch(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(SEARCH_TYPE,TEXTSEARCH_REGEX)
            .put(SEARCH,false);
    assertEquals(new JsonObject().put(ERROR,new RespBuilder().withType(TYPE_BAD_TEXT_QUERY).withTitle(TITLE_BAD_TEXT_QUERY).withDetail("bad text query values").getJsonResponse()),queryDecoder.searchQuery(request));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test listItemQuery method when itemType not equals TAGS and instanceID is not null/empty")
  public void testSearchQuery(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    jsonArray.add("dummy value");
    jsonArray.add("dummy value");
    JsonArray jsonArray2=new JsonArray();
    jsonArray2.add("dummy");
    request.put(SEARCH_TYPE,ATTRIBUTE_SEARCH_REGEX)
            .put(SEARCH,false)
            .put(PROPERTY,jsonArray)
            .put(VALUE,jsonArray2);
    assertEquals(new JsonObject().put(ERROR,new RespBuilder().withType(TYPE_INVALID_PROPERTY_VALUE).withTitle(TITLE_INVALID_PROPERTY_VALUE).withDetail("Invalid Property Value").getJsonResponse()),queryDecoder.searchQuery(request));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test listItemQuery method when itemType not equals TAGS and instanceID is not null/empty")
  public void testSearchQueryAttributeSearch(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    jsonArray.add(0,KEYWORD_KEY);
    JsonArray jsonArray2=new JsonArray();
    JsonArray jsonArray3=new JsonArray();
    jsonArray3.add(0,"dummy");
    jsonArray2.add(0,jsonArray3);
    request.put(SEARCH_TYPE,ATTRIBUTE_SEARCH_REGEX)
            .put(SEARCH,false)
            .put(PROPERTY,jsonArray)
            .put(VALUE,jsonArray2);
   /* String matchQuery=MATCH_QUERY.replace("$1",request.getJsonArray(PROPERTY).getString(0)).replace("$2",request.getJsonArray(VALUE).getString(0));
     JsonArray shouldQuery=new JsonArray();
     shouldQuery.add(new JsonObject(matchQuery));
     JsonArray mustQuery=new JsonArray();
    mustQuery.add(new JsonObject(SHOULD_QUERY.replace("$1", shouldQuery.toString())));
    JsonObject elasticQuery=new JsonObject();
    JsonObject boolQuery = new JsonObject(MUST_QUERY.replace("$1", mustQuery.toString()));
    */
    queryDecoder.searchQuery(request);
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test searchquery when instanceId is not null")
  public void testSearchInstance(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(INSTANCE,"dummy")
            .put(SEARCH,false)
            .put(SEARCH_TYPE,"dummy")
            .put(OFFSET,100);
    assertEquals(new JsonObject().put(ERROR, new RespBuilder()
            .withType(TYPE_INVALID_SYNTAX)
            .withTitle(TITLE_INVALID_SYNTAX)
            .withDetail(TITLE_INVALID_SYNTAX)
            .getJsonResponse()),queryDecoder.searchQuery(request));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing listQueryRelationship test")
  public void testListRelationshipQuery(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    request.put(RELATIONSHIP,TYPE_KEY);
    request.put(ID,"dummy")
//        .put(ITEM_TYPE, "item:Resource")
            .put(LIMIT,100)
            .put(OFFSET,100)
            .put(FILTER,jsonArray)
            .put(LIMIT,2).put(OFFSET,1);

    queryDecoder.listRelationshipQueryModel(request);
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing seachquery method with limit set to 100")
  public void testsearchQuery(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(LIMIT,100).put(SEARCH_TYPE,"dummy").put(SEARCH,false);
    request.put(INSTANCE,"dummy");
    JsonObject elasticQuery=new JsonObject();
    elasticQuery.put(SIZE_KEY, request.getInteger(LIMIT));
    assertEquals(new JsonObject().put(ERROR, new RespBuilder()
            .withType(TYPE_INVALID_SYNTAX)
            .withTitle(TITLE_INVALID_SYNTAX)
            .withDetail(TITLE_INVALID_SYNTAX)
            .getJsonResponse()),queryDecoder.searchQuery(request));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing seachQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testsearchQueryFilter_Regex(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    jsonArray.add("dummy");
    request.put(SEARCH_TYPE,RESPONSE_FILTER_REGEX)
            .put(SEARCH,true).put(OFFSET,100)
            .put(ATTRIBUTE,jsonArray)
            .put(INSTANCE,"dummy");
    Integer limit =
            request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));

    JsonObject query = queryDecoder.searchQuery(request);
    QueryModel queryModel = (QueryModel) query.getValue(QUERY_KEY);

    assertEquals(limit.toString(), queryModel.toJson().getString("limit"));
    assertEquals(request.getInteger(OFFSET).toString(), queryModel.toJson().getString("offset"));
    assertEquals("dummy", queryModel.toJson().getJsonObject("queries").getJsonArray(
        "mustQueries").getJsonObject(0).getJsonObject("queryParameters").getString(INSTANCE));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing seachQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testsearchQueryFilter_Regex2(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    jsonArray.add("dummy");
    request.put(SEARCH_TYPE,RESPONSE_FILTER_REGEX)
            .put(SEARCH,true)
            .put(OFFSET,100).
            put(FILTER,jsonArray)
            .put(INSTANCE,"dummy");

    JsonObject query = queryDecoder.searchQuery(request);
    QueryModel queryModel = (QueryModel) query.getValue(QUERY_KEY);

    assertEquals("dummy", queryModel.toJson().getJsonObject("queries").getJsonArray(
        "mustQueries").getJsonObject(0).getJsonObject("queryParameters").getString(INSTANCE));
    assertEquals(queryModel.toJson().getJsonArray("includeFields"), request.getJsonArray(FILTER));

    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing seachQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testListRelationshipQueryID(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(RELATIONSHIP,RESOURCE)
            .put(ID,"dummy")
            .put(ITEM_TYPE,"iudx:ResourceGroup");
    Integer limit =
            request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
    JsonObject query =  queryDecoder.listRelationshipQueryModel(request).toJson();

    assertEquals("resourceGroup.keyword", query.getJsonArray("mustQueries").getJsonObject(0)
        .getJsonObject("queryParameters").getString(FIELD));
    assertEquals(query.getString("limit"), limit.toString());
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing seachQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testListRelationshipQueryProvider(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(RELATIONSHIP,PROVIDER)
            .put(ID,"abcd/abcd")
            .put(PROVIDER,"abcd/abc");
    String id=request.getString(ID);
    String providerId = StringUtils.substring(id, 0, id.indexOf("/", id.indexOf("/") + 1));
    Integer limit =
            request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));

    JsonObject query =  queryDecoder.listRelationshipQueryModel(request).toJson();

    assertEquals(ID_KEYWORD, query.getJsonArray("mustQueries").getJsonObject(0)
        .getJsonObject("queryParameters").getString(FIELD));
    assertEquals(providerId, query.getJsonArray("mustQueries").getJsonObject(0)
        .getJsonObject("queryParameters").getString(VALUE));
    assertEquals(query.getString("limit"), limit.toString());
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing listRelationshipQuery method when realtionshipType is resource and itemType is provider")
  public void testListRelationshipResource(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(RELATIONSHIP,RESOURCE)
            .put(ID,"dummy")
            .put(ITEM_TYPE,"iudx:Provider");
    Integer limit =
            request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
    JsonObject query =  queryDecoder.listRelationshipQueryModel(request).toJson();

    assertEquals(PROVIDER + KEYWORD_KEY, query.getJsonArray("mustQueries").getJsonObject(0)
        .getJsonObject("queryParameters").getString(FIELD));
    assertEquals("dummy", query.getJsonArray("mustQueries").getJsonObject(0)
        .getJsonObject("queryParameters").getString(VALUE));
    assertEquals(query.getString("limit"), limit.toString());
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing listRelationshipQuery method when realtionshipType is resource group and itemType is resource")
  public void testListRelationshipResourceGroup(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(RELATIONSHIP,RESOURCE_GRP)
            .put(ID,"dummy")
            .put(ITEM_TYPE,"iudx:Resource")
            .put("resourceGroup","dummy id");
    Integer limit =
            request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
    JsonObject query =  queryDecoder.listRelationshipQueryModel(request).toJson();

    assertEquals(ID_KEYWORD, query.getJsonArray("mustQueries").getJsonObject(0)
        .getJsonObject("queryParameters").getString(FIELD));
    assertEquals("dummy id", query.getJsonArray("mustQueries").getJsonObject(0)
        .getJsonObject("queryParameters").getString(VALUE));
    assertEquals(query.getString("limit"), limit.toString());
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing listRelationshipQuery method when realtionshipType is resource group and itemType is provider")
  public void testListRelationshipItemProvider(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(RELATIONSHIP,RESOURCE_GRP)
            .put(ID,"dummy")
            .put(ITEM_TYPE,"iudx:Provider")
            .put("resourceGroup","dummy id");
    Integer limit =
            request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));

    JsonObject query =  queryDecoder.listRelationshipQueryModel(request).toJson();

    assertEquals(PROVIDER + KEYWORD_KEY, query.getJsonArray("mustQueries").getJsonObject(0)
        .getJsonObject("queryParameters").getString(FIELD));
    assertEquals("dummy", query.getJsonArray("mustQueries").getJsonObject(0)
        .getJsonObject("queryParameters").getString(VALUE));
    assertEquals(query.getString("limit"), limit.toString());
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing listRelationshipQuery method when realtionshipType is resourceGrp and itemType is rescource server")
  public void testListRelationshipItemResourceServer(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request
        .put(RELATIONSHIP, RESOURCE_GRP)
        .put(ID, "dummy")
        .put("providerIds", new JsonArray().add(new JsonObject().put("id","provider-id")))
        .put(ITEM_TYPE, "iudx:ResourceServer");

    JsonObject query =  queryDecoder.listRelationshipQueryModel(request).toJson();

    assertEquals(PROVIDER + KEYWORD_KEY, query.getJsonArray("shouldQueries").getJsonObject(0)
        .getJsonObject("queryParameters").getString(FIELD));
    assertEquals("provider-id", query.getJsonArray("shouldQueries").getJsonObject(0)
        .getJsonObject("queryParameters").getString(VALUE));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing seachQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testsearchQueryGetItemType(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    jsonArray.add("dummy");
    request.put(SEARCH_TYPE,"getParentObjectInfo")
            .put(ID,"id");
    JsonObject elasticQuery =
        new JsonObject(
            GET_DOC_QUERY
                .replace("$1", request.getString(ID))
                .replace("$2", "\"type\",\"provider\",\"ownerUserId\",\"resourceGroup\",\"resourceServer\", \"resourceServerRegURL\", \"cos\", \"cos_admin\""));
    JsonObject query =  queryDecoder.searchQuery(request);
    QueryModel queryModel = (QueryModel) query.getValue(QUERY_KEY);

    assertEquals(ID_KEYWORD, queryModel.toJson().getJsonObject("queries")
        .getJsonObject("queryParameters").getString(FIELD));
    assertEquals("id", queryModel.toJson().getJsonObject("queries")
        .getJsonObject("queryParameters").getString(VALUE));

    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing listRelationshipQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testListRelationshipQueryType(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    jsonArray.add("dummy");
    request.put(RELATIONSHIP,"cos")
            .put(ID,"id")
            .put(COS_ITEM,"value");
    String cosId = request.getString(COS_ITEM);
    String  subQuery = TERM_QUERY.replace("$1", ID + KEYWORD_KEY).replace("$2", cosId);
    String elasticQuery = BOOL_MUST_QUERY.replace("$1", subQuery);
    Integer limit =
            request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
    JsonObject tempQuery = new JsonObject(elasticQuery).put(SIZE_KEY, limit.toString());
    JsonObject query = queryDecoder.listRelationshipQueryModel(request).toJson();
    assertEquals(limit.toString(), query.getString("limit"));
    assertEquals(ID_KEYWORD, query.getJsonArray("mustQueries").getJsonObject(0).getJsonObject(
        "queryParameters").getString(FIELD));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing listRelationshipQuery method with item type cos")
  public void testListRelationshipCosQueryType(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    jsonArray.add("dummy");
    request.put(ID,"id")
            .put(ITEM_TYPE,ITEM_TYPE_COS);
    assertEquals(null, queryDecoder.listRelationshipQueryModel(request));
    vertxTestContext.completeNow();
  }

  @ParameterizedTest
  @MethodSource("mustQuery")
  @Description("testing listRelationshipQuery method with item type cos and relType resource")
  public void testListRelationshipCosResourceType(String input, String actualOutput, VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    jsonArray.add("dummy");
    request.put(ID,"id")
            .put(ITEM_TYPE_COS,"value")
            .put(ITEM_TYPE,ITEM_TYPE_COS)
            .put(RELATIONSHIP, input);

    JsonObject query = queryDecoder.listRelationshipQueryModel(request).toJson();
    assertEquals("id", query.getJsonArray("mustQueries").getJsonObject(0).getJsonObject(
        "queryParameters").getString(VALUE));
    assertEquals("cos.keyword", query.getJsonArray("mustQueries").getJsonObject(0).getJsonObject(
        "queryParameters").getString(FIELD));
    vertxTestContext.completeNow();
  }
  
  @ParameterizedTest
  @MethodSource("shouldQuery")
  @Description("testing listRelationshipQuery method with item type cos and relType all")
  public void testListRelationshipCosAllType(String input, VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    jsonArray.add("dummy");
    request.put(ID,"id")
            .put(RELATIONSHIP, ALL)
            .put(input, "dummy");

    JsonObject query = queryDecoder.listRelationshipQueryModel(request).toJson();
    assertEquals(ID_KEYWORD, query.getJsonArray("shouldQueries").getJsonObject(0).getJsonObject(
        "queryParameters").getString(FIELD));
    assertEquals("dummy", query.getJsonArray("shouldQueries").getJsonObject(1).getJsonObject(
        "queryParameters").getString(VALUE));
    vertxTestContext.completeNow();
  }

}
