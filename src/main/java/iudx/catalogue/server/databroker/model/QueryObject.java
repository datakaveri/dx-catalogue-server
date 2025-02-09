package iudx.catalogue.server.databroker.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true, publicConverter = false)
public class QueryObject {
  private String primaryKey;
  private String origin;
  private String databaseTableName;
  private String aud;
  private JsonObject cons;
  private Integer exp;
  private Integer iat;
  private String iid;
  private String iss;
  private String role;
  private String sub;
  private String iudxID;
  private String api;
  private String httpMethod;
  private Long epochTime;
  private String userid;

  public QueryObject() {
    // Default constructor
  }

  public QueryObject(JsonObject json) {
    QueryObjectConverter.fromJson(json, this);
  }

  public QueryObject(String primaryKey, String origin, JsonObject request) {
    this.primaryKey = primaryKey;
    this.origin = origin;
    // Use setters to initialize fields from request
    if (request != null) {
      setAud(request.getString("aud"));
      setCons(request.getJsonObject("cons"));
      setExp(request.getInteger("exp"));
      setIat(request.getInteger("iat"));
      setIid(request.getString("iid"));
      setIss(request.getString("iss"));
      setRole(request.getString("role"));
      setSub(request.getString("sub"));
      setIudxID(request.getString("iudxID"));
      setApi(request.getString("api"));
      setHttpMethod(request.getString("httpMethod"));
      setEpochTime(request.getLong("epochTime"));
      setUserid(request.getString("userid"));
      setDatabaseTableName(request.getString("databaseTableName"));
    }
  }

  public String getPrimaryKey() {
    return primaryKey;
  }

  public void setPrimaryKey(String primaryKey) {
    this.primaryKey = primaryKey != null ? primaryKey : "";
  }

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin != null ? origin : "";
  }

  public String getDatabaseTableName() {
    return databaseTableName;
  }

  public void setDatabaseTableName(String databaseTableName) {
    this.databaseTableName = databaseTableName != null ? databaseTableName : "";
  }

  public String getAud() {
    return aud;
  }

  public void setAud(String aud) {
    this.aud = aud != null ? aud : "";
  }

  public JsonObject getCons() {
    return cons;
  }

  public void setCons(JsonObject cons) {
    this.cons = cons != null ? cons : new JsonObject();
  }

  public Integer getExp() {
    return exp;
  }

  public void setExp(Integer exp) {
    this.exp = exp != null ? exp : 0;
  }

  public Integer getIat() {
    return iat;
  }

  public void setIat(Integer iat) {
    this.iat = iat != null ? iat : 0;
  }

  public String getIid() {
    return iid;
  }

  public void setIid(String iid) {
    this.iid = iid != null ? iid : "";
  }

  public String getIss() {
    return iss;
  }

  public void setIss(String iss) {
    this.iss = iss != null ? iss : "";
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role != null ? role : "";
  }

  public String getSub() {
    return sub;
  }

  public void setSub(String sub) {
    this.sub = sub != null ? sub : "";
  }

  public String getIudxID() {
    return iudxID;
  }

  public void setIudxID(String iudxID) {
    this.iudxID = iudxID != null ? iudxID : "";
  }

  public String getApi() {
    return api;
  }

  public void setApi(String api) {
    this.api = api != null ? api : "";
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod != null ? httpMethod : "";
  }

  public Long getEpochTime() {
    return epochTime;
  }

  public void setEpochTime(Long epochTime) {
    this.epochTime = epochTime != null ? epochTime : 0L;
  }

  public String getUserid() {
    return userid;
  }

  public void setUserid(String userid) {
    this.userid = userid != null ? userid : "";
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    QueryObjectConverter.toJson(this, json);
    return json;
  }
}
