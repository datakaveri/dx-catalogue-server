package iudx.catalogue.server.validator;

public class Constants {

  /** General definations. */
  public static final String ID_KEYWORD = "id.keyword";

  public static final String VALUE = "value";
  public static final String CAT_DOC = "cat/_doc";

  public static final String ITEM_STATUS = "itemStatus";
  public static final String ACTIVE = "ACTIVE";
  public static final String ITEM_CREATED_AT = "itemCreatedAt";
  public static final String DATA_UPLOAD_STATUS = "dataUploadStatus";
  public static final String MEDIA_URL = "mediaURL";
  public static final String LAST_UPDATED = "lastUpdated";
  public static final String CONTEXT = "@context";

  /** Error messages. */
  public static final String NON_EXISTING_LINK_MSG = "No such cat item exists: ";

  public static final String VALIDATION_FAILURE_MSG = "Validation failed";
  public static final String INVALID_SCHEMA_MSG = "Invalid Schema";

  public static final String ID_MATCH_SUB_QUERY =
      "{\"query\":{\"bool\":{\"should\":[{"
          + "\"bool\":{\"must\":[{"
          + "\"match\":{\"id.keyword\":\"$1\"}}]}},";
  public static final String ITEM_EXISTS_QUERY =
      ID_MATCH_SUB_QUERY.concat(
          "{\"bool\":{\"must\":[{\"match\":{\"type.keyword\":\"$2\"}},"
              + "{\"match\":{\"$3.keyword\":\"$4\"}}]}}]}},\"_source\":[\"type\"]}");

  public static final String ORGANIZATION_ID_MATCH_SUB_QUERY =
      "{\"query\":{\"bool\":{\"must\":[{"
          + "\"bool\":{\"must\":[{"
          + "\"match\":{\"organizationId.keyword\":\"$1\"}}]}},";
  public static final String AI_SANDBOX_ITEM_EXISTS_QUERY =
      ORGANIZATION_ID_MATCH_SUB_QUERY.concat(
          "{\"bool\":{\"must\":[{\"match\":{\"type.keyword\":\"$2\"}},"
              + "{\"match\":{\"$3.keyword\":\"$4\"}}]}}]}},\"_source\":[\"type\"]}");

  public static final String PROVIDER_ITEM_EXISTS_QUERY =
      ID_MATCH_SUB_QUERY.concat(
          "{\"bool\":{\"must\":[{\"match\":{\"ownerUserId.keyword\":\"$2\"}},"
              + "{\"match\":{\"resourceServerRegURL.keyword\":\"$3\"}}]}}]}},"
              + "\"_source\":[\"type\"]}");

  public static final String RESOURCE_ITEM_EXISTS_QUERY =
      "{\"query\":{\"bool\":{\"should\":[{"
          + "\"match\":{\"id.keyword\":\"$1\"}},{"
          + "\"match\":{\"id.keyword\":\"$2\"}},{"
          + "\"match\":{\"id.keyword\":\"$3\"}},{"
          + "\"bool\":{\"must\":[{"
          + "\"match\":{\"type.keyword\":\"iudx:Resource\"}},{"
          + "\"match\":{\"name.keyword\":\"$4\"}}, {"
          + "\"match\":{\"resourceGroup.keyword\":\"$3\"}}]}}]}},"
          + "\"_source\":[\"type\"]}";
  public static final String OWNER_ITEM_EXISTS_QUERY =
      "{\"query\":{\"bool\":{\"must\":[{"
          + "\"match\":{\"type\":\"iudx:Owner\"}},"
          + "{\"match\":{\"name.keyword\":\"$1\"}}]}}}";
  public static final String ITEM_WITH_NAME_EXISTS_QUERY =
      "{\"query\":{\"bool\":{\"must\":[{"
          + "\"match\":{\"type\":\"$1\"}},"
          + "{\"match\":{\"name.keyword\":\"$2\"}}]}}}";
  static final String FILTER_PATH = "?filter_path=took,hits.total.value,hits.hits._source";
}
