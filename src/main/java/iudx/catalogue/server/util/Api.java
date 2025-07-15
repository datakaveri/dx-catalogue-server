package iudx.catalogue.server.util;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.apiserver.util.Constants.ROUTE_MLAYER_POPULAR_DATASETS;

/**
 * This class is used to get complete endpoint by appending configurable base path with the APIs.
 */
public class Api {

  private static volatile Api apiInstance;
  private String dxApiBasePath;
  private StringBuilder routeItems;
  private StringBuilder routUpdateItems;
  private StringBuilder routeOrgAsset;
  private StringBuilder routeDeleteItems;
  private StringBuilder routeInstance;
  private StringBuilder routeRelationship;
  private StringBuilder routeSearch;
  private StringBuilder routeHealthLive;
  private StringBuilder routeSearchMyAssets;
  private StringBuilder routeOwnershipTransfer;
  private StringBuilder routeOwnershipDelete;
  private StringBuilder routeNlpSearch;
  private StringBuilder routeListItems;
  private StringBuilder routeListMulItems;
  private StringBuilder routeGetItems;
  private StringBuilder routeCount;
  private StringBuilder routeRelSearch;
  private StringBuilder routeGeoCoordinates;
  private StringBuilder routeGeoReverse;
  private StringBuilder routeListResourceGroupRel;
  private StringBuilder routeMlayerInstance;
  private StringBuilder routeMlayerDoamin;
  private StringBuilder routeMlayerProvider;
  private StringBuilder routeMlayerGeoquery;
  private StringBuilder routeMlayerDataset;
  private StringBuilder routeMlayerPopularDatasets;
  private StringBuilder routeRating;
  private StringBuilder summaryCountSizeApi;
  private StringBuilder realDatasetApi;
  private StringBuilder stack;

  private Api(String dxApiBasePath) {
    this.dxApiBasePath = dxApiBasePath;
    buildEndpoints();
  }

  /**
   * Returns an instance of the API class using the provided base path.
   *
   * @param dxApiBasePath the base path for the API
   * @return an instance of the API class
   */
  public static Api getInstance(String dxApiBasePath) {
    if (apiInstance == null) {
      synchronized (Api.class) {
        if (apiInstance == null) {
          apiInstance = new Api(dxApiBasePath);
        }
      }
    }
    return apiInstance;
  }

  /** Builds the endpoints for the API by appending the route paths to the base API path. */
  public void buildEndpoints() {
    routeItems = new StringBuilder(dxApiBasePath).append(ROUTE_ITEMS);
    routUpdateItems = new StringBuilder(dxApiBasePath).append(ROUTE_UPDATE_ITEMS);
    routeOrgAsset = new StringBuilder(dxApiBasePath).append(ROUTE_ORG_ASSET);
    routeDeleteItems = new StringBuilder(dxApiBasePath).append(ROUTE_DELETE_ITEMS);
    routeInstance = new StringBuilder(dxApiBasePath).append(ROUTE_INSTANCE);
    routeRelationship = new StringBuilder(dxApiBasePath).append(ROUTE_RELATIONSHIP);
    routeSearch = new StringBuilder(dxApiBasePath).append(ROUTE_SEARCH);
    routeHealthLive = new StringBuilder(dxApiBasePath).append(ROUTE_HEALTH_LIVE);
    routeSearchMyAssets = new StringBuilder(dxApiBasePath).append(ROUTE_SEARCH_MY_ASSETS);
    routeOwnershipTransfer = new StringBuilder(dxApiBasePath).append(OWNERSHIP_TRANSFER_PATH);
    routeOwnershipDelete = new StringBuilder(dxApiBasePath).append(OWNERSHIP_DELETE_PATH);
    routeNlpSearch = new StringBuilder(dxApiBasePath).append(ROUTE_NLP_SEARCH);
    routeListItems = new StringBuilder(dxApiBasePath).append(ROUTE_LIST_ITEMS);
    routeListMulItems = new StringBuilder(dxApiBasePath).append(ROUTE_LIST);
    routeGetItems = new StringBuilder(dxApiBasePath).append(ROUTE_GET_ITEM);
    routeCount = new StringBuilder(dxApiBasePath).append(ROUTE_COUNT);
    routeRelSearch = new StringBuilder(dxApiBasePath).append(ROUTE_REL_SEARCH);
    routeGeoCoordinates = new StringBuilder(dxApiBasePath).append(ROUTE_GEO_COORDINATES);
    routeGeoReverse = new StringBuilder(dxApiBasePath).append(ROUTE_GEO_REVERSE);
    routeListResourceGroupRel =
        new StringBuilder(dxApiBasePath).append(ROUTE_LIST_RESOURCE_GROUP_REL);
    routeMlayerInstance = new StringBuilder(dxApiBasePath).append(ROUTE_MLAYER_INSTANCE);
    routeMlayerDoamin = new StringBuilder(dxApiBasePath).append(ROUTE_MLAYER_DOMAIN);
    routeMlayerProvider = new StringBuilder(dxApiBasePath).append(ROUTE_MLAYER_PROVIDER);
    routeMlayerGeoquery = new StringBuilder(dxApiBasePath).append(ROUTE_MLAYER_GEOQUERY);
    routeMlayerDataset = new StringBuilder(dxApiBasePath).append(ROUTE_MLAYER_DATASET);
    routeMlayerPopularDatasets =
        new StringBuilder(dxApiBasePath).append(ROUTE_MLAYER_POPULAR_DATASETS);
    routeRating = new StringBuilder(ROUTE_RATING);
    summaryCountSizeApi = new StringBuilder(dxApiBasePath).append(SUMMARY_TOTAL_COUNT_SIZE_API);
    realDatasetApi = new StringBuilder(dxApiBasePath).append(COUNT_SIZE_API);
    stack = new StringBuilder(dxApiBasePath).append(ROUTE_STACK);
  }

  public String getRouteItems() {
    return routeItems.toString();
  }

  public String getRoutUpdateItems() {
    return routUpdateItems.toString();
  }
  public String getRouteOrgAsset() {
    return routeOrgAsset.toString();
  }

  public String getRouteDeleteItems() {
    return routeDeleteItems.toString();
  }

  public String getRouteInstance() {
    return routeInstance.toString();
  }

  public String getRouteRelationship() {
    return routeRelationship.toString();
  }

  public String getRouteSearch() {
    return routeSearch.toString();
  }

  public String getRouteHealthLive() {
    return routeHealthLive.toString();
  }

  public String getRouteSearchMyAssets() {
    return routeSearchMyAssets.toString();
  }

  public String getRouteOwnershipTransfer() {
    return routeOwnershipTransfer.toString();
  }
  public String getRouteOwnershipDelete() {
    return routeOwnershipDelete.toString();
  }


  public String getRouteNlpSearch() {
    return routeNlpSearch.toString();
  }

  public String getRouteListItems() {
    return routeListItems.toString();
  }

  public String getRouteListMulItems() {
    return routeListMulItems.toString();
  }

  public String getRouteGetItems() {
    return routeGetItems.toString();
  }

  public String getRouteCount() {
    return routeCount.toString();
  }

  public String getRouteRelSearch() {
    return routeRelSearch.toString();
  }

  public String getRouteGeoCoordinates() {
    return routeGeoCoordinates.toString();
  }

  public String getRouteGeoReverse() {
    return routeGeoReverse.toString();
  }

  public String getRouteListResourceGroupRel() {
    return routeListResourceGroupRel.toString();
  }

  public String getRouteMlayerInstance() {
    return routeMlayerInstance.toString();
  }

  public String getRouteMlayerDomains() {
    return routeMlayerDoamin.toString();
  }

  public String getRouteMlayerProviders() {
    return routeMlayerProvider.toString();
  }

  public String getRouteMlayerGeoQuery() {
    return routeMlayerGeoquery.toString();
  }

  public String getRouteMlayerDataset() {
    return routeMlayerDataset.toString();
  }

  public String getRouteMlayerPopularDatasets() {
    return routeMlayerPopularDatasets.toString();
  }

  public String getRouteRating() {
    return routeRating.toString();
  }

  public String getSummaryCountSizeApi() {
    return summaryCountSizeApi.toString();
  }

  public String getRealDatasetApi() {
    return realDatasetApi.toString();
  }

  public String getStackRestApis() {
    return stack.toString();
  }
}
