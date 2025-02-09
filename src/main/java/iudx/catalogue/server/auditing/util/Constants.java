package iudx.catalogue.server.auditing.util;

public class Constants {
  public static final String ID = "id";
  /* Errors */
  public static final String SUCCESS = "success";
  public static final String FAILED = "failed";
  public static final String ERROR_TYPE = "type";
  public static final String TITLE = "title";
  public static final String RESULTS = "results";
  public static final String STATUS = "status";

  /* Database */
  public static final String START_TIME = "startTime";
  public static final String END_TIME = "endTime";
  public static final String EPOCH_TIME = "epochTime";

  /* Auditing Service Constants*/
  public static final String USER_ROLE = "userRole";
  public static final String USER_ID = "userID";
  public static final String IID = "iid";
  public static final String API = "api";
  public static final String METHOD = "httpMethod";
  public static final String DATABASE_TABLE_NAME = "databaseTableName";
  public static final String IUDX_ID = "iudxID";
  public static final String EXCHANGE_NAME = "auditing";
  public static final String ROUTING_KEY = "#";
  public static final String PRIMARY_KEY = "primaryKey";
  public static final String ORIGIN = "origin";
  public static final String ORIGIN_SERVER = "cat-server";
}
