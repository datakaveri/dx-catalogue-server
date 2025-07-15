package iudx.catalogue.server.apiserver.util;

import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.http.HttpMethod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Constants {

  /** General purpose. */
  public static final String CAT_ADMIN = "catAdmin";

  public static final String HOST = "host";

  /** Accept Headers and CORS. */
  public static final String HEADER_ACCEPT = "Accept";

  public static final String CONTENT_TYPE = "content-type";
  public static final String APPLICATION_JSON = "application/json";


  public static final String HEADER_TOKEN = "token";
  public static final String HEADER_CONTENT_LENGTH = "Content-Length";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HEADER_HOST = "Host";
  public static final String HEADER_INSTANCE = "instance";
  public static final String HEADER_ORIGIN = "Origin";
  public static final String HEADER_AUTHORIZATION = "Authorization";
  public static final String HEADER_REFERER = "Referer";
  public static final String HEADER_CORS = "Access-Control-Allow-Origin";
  public static final Set<String> ALLOWED_HEADERS =
      new HashSet<String>(
          Arrays.asList(
              HEADER_ACCEPT,
              HEADER_TOKEN,
              HEADER_CONTENT_LENGTH,
              HEADER_CONTENT_TYPE,
              HEADER_HOST,
              HEADER_INSTANCE,
              HEADER_ORIGIN,
              HEADER_AUTHORIZATION,
              HEADER_REFERER,
              HEADER_CORS));

  public static final Set<HttpMethod> ALLOWED_METHODS =
      new HashSet<HttpMethod>(
          Arrays.asList(HttpMethod.GET, HttpMethod.POST, HttpMethod.DELETE, HttpMethod.PUT));

  public static final String MIME_APPLICATION_JSON = "application/json";
  public static final String MIME_TEXT_HTML = "text/html";

  /** Routes. */
  public static final String ROUTE_STATIC_SPEC = "/apis/spec";

  public static final String ROUTE_DOC = "/apis";
  public static final String ROUTE_ITEMS = "/item";
  public static final String ROUTE_UPDATE_ITEMS = "/item";
  public static final String ROUTE_DELETE_ITEMS = "/item";

  public static final String ROUTE_ORG_ASSET = "/organisation/asset";
  public static final String ROUTE_INSTANCE = "/instance";
  public static final String ROUTE_LIST_RESOURCE_GROUP_REL = "\\/(?<id>.*)\\/resourceGroup";

  public static final String ROUTE_RELATIONSHIP = "/relationship";
  public static final String ROUTE_SEARCH = "/search";
  public static final String ROUTE_HEALTH_LIVE = "/health/live";
  public static final String ROUTE_SEARCH_MY_ASSETS = "/search/myassets";
  public static final String OWNERSHIP_TRANSFER_PATH = "/item/ownership/transfer";
  public static final String OWNERSHIP_DELETE_PATH = "/item/ownership/delete";
  public static final String ROUTE_NLP_SEARCH = "/nlpsearch";
  public static final String ROUTE_LIST_ITEMS = "/list/:itemType";
  public static final String ROUTE_LIST = "/list";

  public static final String ROUTE_RATING = "/consumer/ratings";
  public static final String ROUTE_STACK = "/stac";

  public static final String RESOURCE_ITEM = "resItem";
  public static final String RESOURCE_GRP_ITEM = "resGrpItem";
  public static final String RESOURCE_SVR_ITEM = "resSvrItem";
  public static final String PROVIDER_ORG = "prvdrOrg";
  public static final String PROVIDER_ITEM = "pvdrItem";
  public static final String DATA_DES_ITEM = "dataDesItem";
  public static final String USERID = "user_id";

  public static final String ROUTE_GET_ITEM = "/items";
  public static final String ROUTE_COUNT = "/count";
  public static final String ROUTE_REL_SEARCH = "/relsearch";

  public static final String ROUTE_GEO_COORDINATES = "/geo";
  public static final String ROUTE_GEO_REVERSE = "/reversegeo";

  public static final String PROVIDER_NAME = "provider.name";
  public static final String PARTIAL_CONTENT = "partial-content";
  public static final String GEO_PROPERTY = "location";

  public static final String UAC_DEPLOYMENT = "isUACInstance";

  public static final String TEXT = "text";

  public static final ArrayList<String> GEORELS =
      new ArrayList<String>(
          Arrays.asList(
              GEOREL_WITHIN,
              GEOREL_NEAR,
              GEOREL_COVERED_BY,
              GEOREL_INTERSECTS,
              GEOREL_EQUALS,
              GEOREL_DISJOINT));
  public static final String ROUTE_MLAYER_INSTANCE = "/internal/ui/instance";
  public static final String ROUTE_MLAYER_DOMAIN = "/internal/ui/domain";
  public static final String ROUTE_MLAYER_PROVIDER = "/internal/ui/providers";
  public static final String ROUTE_MLAYER_GEOQUERY = "/internal/ui/geoquery";
  public static final String ROUTE_MLAYER_DATASET = "/internal/ui/dataset";
  public static final String ROUTE_MLAYER_POPULAR_DATASETS = "/internal/ui/popularDatasets";
  public static final String SUMMARY_TOTAL_COUNT_SIZE_API = "/internal/ui/summary";
  public static final String COUNT_SIZE_API = "/internal/ui/realtimedataset";

  /** Geometries. */
  public static final ArrayList<String> GEOMETRIES =
      new ArrayList<String>(Arrays.asList(BBOX, POLYGON, LINESTRING, POINT));

  public static final String OPERATION = "operation";
  public static final String ATTRIBUTE_FILTER = "attribute-filter";

  /** Errors. */
  public static final String INVALID_SYNTAX = "invalidSyntax";

  public static final String INVALID_VALUE = "invalidValue";
  public static final String BAD_REQUEST = "Bad Request";
  public static final String LOCATION_NOT_FOUND = "location not found";

  /** Query Pattern. */
  public static final String PATTERN_TEXT = "^[\\*]{0,1}[A-Za-z ]+[\\*]{0,1}";

  public static final String PATTERN_ARRAY = "^\\[.*\\]$";

  public static final String REL_KEY = "rel";
  public static final ArrayList<String> ITEMS_KEY =
      new ArrayList<String>(
          Arrays.asList(RESOURCE, RESOURCE_GRP, RESOURCE_SVR, PROVIDER, COS, TYPE, ALL));
}
