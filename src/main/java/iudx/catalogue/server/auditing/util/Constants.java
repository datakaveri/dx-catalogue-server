package iudx.catalogue.server.auditing.util;

public class Constants {
  public static final String ID = "id";
  /* Errors */
  public static final String SUCCESS = "success";
  public static final String FAILED = "failed";
  public static final String DETAIL = "detail";
  public static final String ERROR_TYPE = "type";
  public static final String TITLE = "title";
  public static final String RESULTS = "results";
  public static final String STATUS = "status";
  public static final String MESSAGE = "message";
  public static final String EMPTY_RESPONSE = "Empty response";

  /* Database */
  public static final String ERROR = "Error";
  public static final String QUERY_KEY = "query";
  public static final String DATA_NOT_FOUND = "Required Data not Found";
  public static final String USERID_NOT_FOUND = "User ID not found";
  public static final String START_TIME = "startTime";
  public static final String END_TIME = "endTime";
  public static final String ENDPOINT = "endPoint";
  public static final String TIME = "time";
  public static final String EPOCH_TIME = "epochTime";
  public static final String INVALID_DATE_TIME = "Date-Time not in correct format.";
  public static final String MISSING_START_TIME = "Start-Time not found.";
  public static final String MISSING_END_TIME = "End-Time not found.";
  public static final String INVALID_TIME = "End-Time cannot be before Start-Time.";
  public static final StringBuilder _METHOD_COLUMN_NAME = new StringBuilder("method)");
  public static final StringBuilder _TIME_COLUMN_NAME = new StringBuilder("time)");
  public static final StringBuilder _USERID_COLUMN_NAME = new StringBuilder("userid)");
  public static final StringBuilder _BODY_COLUMN_NAME = new StringBuilder("body)");
  public static final StringBuilder _ENDPOINT_COLUMN_NAME = new StringBuilder("endpoint)");
  public static final StringBuilder _API_COLUMN_NAME = new StringBuilder("api)");
  public static final StringBuilder _IID_COLUMN_NAME = new StringBuilder("iid)");
  public static final StringBuilder _IUDX_COLUMN_NAME = new StringBuilder("iudxid)");
  public static final StringBuilder _USERROLE_COLUMN_NAME = new StringBuilder("userrole)");

  /* Auditing Service Constants*/
  public static final String USER_ROLE = "user_role";
  public static final String ROLE = "role";

  public static final String USER_ID = "userID";

  public static final String MYACTIVITY_ENABLED = "myactivity_enabled";

  public static final String CONSUMER = "consumer";

  public static final String VIEW = "VIEW";
  public static final String CREATE = "CREATE";
  public static final String UPDATE = "UPDATE";
  public static final String DELETE = "DELETE";
  public static final String IID = "iid";
  public static final String API = "api";
  public static final String METHOD = "http_method";
  public static final String DATABASE_TABLE_NAME = "databaseTableName";
  public static final String IUDX_ID = "iudxID";
  public static final String EXCHANGE_NAME = "auditing";

  public static final String CREATED_AT = "created_at";
  public static final String ASSET_NAME = "asset_name";
  public static final String ASSET_TYPE = "asset_type";
  public static final String OPERATION = "operation";
  public static final String ASSET_ID = "asset_id";
  public static final String ROUTING_KEY = "##";
  public static final String PRIMARY_KEY = "primary_key";
  public static final String ORIGIN = "origin";
  public static final String ORIGIN_SERVER = "origin_server";
  public static final String CAT_SERVER = "Catalogue";
  public static final String WRITE_QUERY =
          "INSERT INTO $0 (id, userRole, userId, iid, api, method, time, iudxID) VALUES"
                  + " ('$1','$2','$3','$4','$5','$6',$7,'$8')";
  public static final String READ_QUERY =
          "SELECT userRole, userId, iid, api, method, time, iudxID from $0 where userId='$1'";
  public static final String START_TIME_QUERY = " and time>=$2";
  public static final String END_TIME_QUERY = " and time<=$3";
  public static final String API_QUERY = " and api='$4'";
  public static final String METHOD_QUERY = " and method='$5'";
}
