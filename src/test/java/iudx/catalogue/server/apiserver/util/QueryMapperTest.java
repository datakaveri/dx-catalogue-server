package iudx.catalogue.server.apiserver.util;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static iudx.catalogue.server.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)

public class QueryMapperTest {


    @Test
    @DisplayName("Test map2Json method ")
    public void testMap2Json(VertxTestContext vertxTestContext) {

        MultiMap queryParameters=MultiMap.caseInsensitiveMultiMap();
        queryParameters.add("dummy key","");
        QueryMapper queryMapper=new QueryMapper();
        assertNull(  queryMapper.map2Json(queryParameters));
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test map2Json method with paramKey value")
    public void testMap2JsonKey(VertxTestContext vertxTestContext) {
        MultiMap queryParameters=MultiMap.caseInsensitiveMultiMap();
        queryParameters.add("dummy key","/////");
        QueryMapper queryMapper=new QueryMapper();
        assertEquals(new JsonObject().put("dummy key","/////"), queryMapper.map2Json(queryParameters));
        vertxTestContext.completeNow();

    }
    @Test
    @DisplayName("Test map2Json method with coordinate value")
    public void testMap2JsonCoordinate(VertxTestContext vertxTestContext) {
        MultiMap queryParameters=MultiMap.caseInsensitiveMultiMap();
        queryParameters.add(COORDINATES,"13");
        QueryMapper queryMapper=new QueryMapper();
        Double value= Double.valueOf(queryParameters.get(COORDINATES));
        assertEquals(new JsonObject().put(COORDINATES,value),queryMapper.map2Json(queryParameters));
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test map2Json method with  Q_Value key")
    public void testMap2JsonQValue(VertxTestContext vertxTestContext) {
        MultiMap queryParameters=MultiMap.caseInsensitiveMultiMap();
        queryParameters.add(Q_VALUE,"!");
        QueryMapper queryMapper=new QueryMapper();
        assertNull( queryMapper.map2Json(queryParameters));
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test map2Json method with [] key")
    public void testMap2JsonWithBracket(VertxTestContext vertxTestContext) {
        MultiMap queryParameters=MultiMap.caseInsensitiveMultiMap();
        queryParameters.add("dummy key","[abcd]");
        QueryMapper queryMapper=new QueryMapper();
        String replacedValue = queryParameters.get("dummy key").replaceAll("[\\w]+[^\\,]*(?:\\.*[\\w])", "\"$0\"");
        assertEquals(new JsonObject().put("dummy key",new JsonArray(replacedValue)), queryMapper.map2Json(queryParameters));
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test map2Json method with exceptAttribute value")
    public void testMap2JsonExceptAttributeValue(VertxTestContext vertxTestContext) {
        MultiMap queryParameters=MultiMap.caseInsensitiveMultiMap();
        queryParameters.add(COORDINATES,"[abcd]");
        QueryMapper queryMapper=new QueryMapper();
        assertNull(queryMapper.map2Json(queryParameters));
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test map2Json method with  Q_Value key and q value")
    public void testMap2JsonQ_Value(VertxTestContext vertxTestContext) {
        MultiMap queryParameters=MultiMap.caseInsensitiveMultiMap();
        queryParameters.add(Q_VALUE,"q");
        QueryMapper queryMapper=new QueryMapper();
        assertEquals(new JsonObject().put(Q_VALUE,"q").put(SEARCH_TYPE,SEARCH_TYPE_TEXT), queryMapper.map2Json(queryParameters));
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test handle method with Q_VALUE GEOMETRY PROPERTY FILTER")
    public void testMap2JsonAttributes(VertxTestContext vertxTestContext) {
        MultiMap queryParameters=MultiMap.caseInsensitiveMultiMap();
        queryParameters.add(Q_VALUE,"q");
        queryParameters.add(GEOMETRY,"q");
        queryParameters.add(PROPERTY,"q");
        queryParameters.add(FILTER,"q");
        QueryMapper queryMapper=new QueryMapper();
        assertEquals(new JsonObject().put(Q_VALUE,"q")
                        .put(GEOMETRY,"q")
                        .put(PROPERTY,"q")
                        .put(FILTER,"q")
                        .put(SEARCH_TYPE,SEARCH_TYPE_GEO+SEARCH_TYPE_TEXT+SEARCH_TYPE_ATTRIBUTE+RESPONSE_FILTER),
                queryMapper.map2Json(queryParameters));
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test validateQueryParam method")
    public void testValidateQueryParam(VertxTestContext vertxTestContext) {
        JsonObject requestBody=new JsonObject();
        JsonArray jsonArray=new JsonArray();
        jsonArray.add("[");
        requestBody.put(SEARCH_TYPE,SEARCH_TYPE_GEO)
                .put(COORDINATES,jsonArray)
                .put(GEOMETRY,LINESTRING);
        assertEquals(new JsonObject().put(STATUS,SUCCESS), QueryMapper.validateQueryParam(requestBody));
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test validateQueryParam method")
    public void testValidateQueryParamInvalid(VertxTestContext vertxTestContext) {
        JsonObject requestBody=new JsonObject();
        JsonArray jsonArray=new JsonArray();
        jsonArray.add("[");
        requestBody.put(SEARCH_TYPE,SEARCH_TYPE_GEO)
                .put(COORDINATES,jsonArray)
                .put(GEOMETRY,"dummy");
        assertEquals(new JsonObject().put(STATUS,FAILED).put(TYPE, TYPE_INVALID_PROPERTY_VALUE).put(DESC,"Invalid coordinate format"), QueryMapper.validateQueryParam(requestBody));
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test validateQueryParam method Validating maxDistance attribute for positive integer")
    public void testValidateQueryParamMaxDistance(VertxTestContext vertxTestContext) {
        JsonObject requestBody=new JsonObject();
        JsonArray jsonArray=new JsonArray();
        jsonArray.add("[");
        requestBody.put(SEARCH_TYPE,SEARCH_TYPE_GEO)
                .put(GEOMETRY,POINT);
        assertEquals( new RespBuilder()
                .withType(TYPE_INVALID_SYNTAX)
                .withTitle(TITLE_INVALID_SYNTAX)
                .getJsonResponse(), QueryMapper.validateQueryParam(requestBody));
        vertxTestContext.completeNow();
    }
//    @Test
//    @DisplayName("Test validateQueryParam method when query param has exceeded the limit")
//    public void testValidateQueryParamExceeded(VertxTestContext vertxTestContext) {
//        JsonObject requestBody=new JsonObject();
//        JsonArray jsonArray=new JsonArray();
//        JsonArray jsonArray2=new JsonArray();
//        jsonArray2.add("dummy").add("abcd").add("abcd").add("abcd").add("abcd");
//        jsonArray.add("dummy");
//        requestBody.put(SEARCH_TYPE,SEARCH_TYPE_ATTRIBUTE)
//                .put(PROPERTY,jsonArray)
//                .put(VALUE,jsonArray2);
//        assertEquals( new JsonObject().put(STATUS,FAILED).put(TYPE, TYPE_INVALID_PROPERTY_VALUE).put(DESC, "The max number of 'value' should be " + VALUE_SIZE), QueryMapper.validateQueryParam(requestBody));
//        vertxTestContext.completeNow();
//    }
    @Test
    @DisplayName("Test validateQueryParam method when Instance has exceeded the limit")
    public void testValidateQueryParamInstance(VertxTestContext vertxTestContext) {
        JsonObject requestBody=new JsonObject();
        JsonArray jsonArray=new JsonArray();
        jsonArray.add("dummy");
        requestBody.put(INSTANCE,"Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the j");
        assertEquals( new JsonObject().put(STATUS,FAILED).put(TYPE, TYPE_INVALID_PROPERTY_VALUE).put(DESC,"The max length of 'instance' should be " + INSTANCE_SIZE), QueryMapper.validateQueryParam(requestBody));
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test validateQueryParam method when Instance has exceeded the limit")
    public void testValidateQueryParamInstanceLimit(VertxTestContext vertxTestContext) {
        JsonObject requestBody=new JsonObject();
        QueryMapper queryMapper=new QueryMapper();
        JsonArray jsonArray=new JsonArray();
        jsonArray.add("7.0000000");
        requestBody.put(SEARCH_TYPE,SEARCH_TYPE_GEO);
        requestBody.put(COORDINATES,jsonArray);
        JsonObject errResponse = new JsonObject().put(STATUS, FAILED).put(TYPE, TYPE_INVALID_PROPERTY_VALUE);
        errResponse.put(DESC, "Invalid coordinate format");
         assertEquals(errResponse,queryMapper.validateQueryParam(requestBody));
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test validateQueryParam method when requestBody contains Geometry")
    public void testValidateQueryParamGeometry(VertxTestContext vertxTestContext) {
        JsonObject requestBody=new JsonObject();
        QueryMapper queryMapper=new QueryMapper();
        requestBody.put(SEARCH_TYPE,SEARCH_TYPE_GEO);
        requestBody.put(GEOMETRY,POINT)
                .put(MAX_DISTANCE,999999);
        JsonObject errResponse = new JsonObject().put(STATUS, FAILED).put(TYPE, TYPE_INVALID_PROPERTY_VALUE);
        errResponse.put(DESC, "The 'maxDistance' should range between 0-10000m");
        assertEquals(errResponse,queryMapper.validateQueryParam(requestBody));
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test validateQueryParam method when searchType contains SEARCH_TYPE_TEXT")
    public void testValidateQueryParamSearch_Type_Test(VertxTestContext vertxTestContext) {
        JsonObject requestBody=new JsonObject();
        QueryMapper queryMapper=new QueryMapper();
        requestBody.put(SEARCH_TYPE,SEARCH_TYPE_TEXT);
        requestBody.put(Q_VALUE,"Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the j");
        JsonObject errResponse = new JsonObject().put(STATUS, FAILED).put(TYPE, TYPE_INVALID_PROPERTY_VALUE);
        errResponse.put(DESC, "The max string(q) size supported is " + STRING_SIZE);
        assertEquals(errResponse,queryMapper.validateQueryParam(requestBody));
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test validateQueryParam method when searchType contains SEARCH_TYPE_TEXT")
    public void testValidateQueryResponse_Filter(VertxTestContext vertxTestContext) {
        JsonObject requestBody=new JsonObject();
        QueryMapper queryMapper=new QueryMapper();
        JsonArray jsonArray=new JsonArray();
        jsonArray.add("value1").add("value2").add("value3").add("value4").add("value5").add("value6").add("value7").add("value8").add("value9").add("value10").add("value11");
        requestBody.put(SEARCH_TYPE,RESPONSE_FILTER);
        requestBody.put(FILTER,jsonArray);
        JsonObject errResponse = new JsonObject().put(STATUS, FAILED).put(TYPE, TYPE_BAD_FILTER);
        errResponse.put(DESC, "The max number of 'filter' should be " + FILTER_VALUE_SIZE);
        assertEquals(errResponse,queryMapper.validateQueryParam(requestBody));
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test validateQueryParam method when requestBody contains LIMIT and OFFSET")
    public void testValidateQueryLimitOffset(VertxTestContext vertxTestContext) {
        JsonObject requestBody=new JsonObject();
        QueryMapper queryMapper=new QueryMapper();
        requestBody.put(LIMIT,10000)
               .put(OFFSET,10000);
        JsonObject errResponse = new JsonObject().put(STATUS, FAILED).put(TYPE, TYPE_INVALID_PROPERTY_VALUE);
        errResponse.put(DESC, "The limit + offset should be between 1 to " + MAX_RESULT_WINDOW);
        assertEquals(errResponse,queryMapper.validateQueryParam(requestBody));
        vertxTestContext.completeNow();
    }
//    @Test
//    @DisplayName("Test validateQueryParam method when searchType contains Search_Type_Attribute")
//    public void testValidateQuerySearch_Type_Attribute(VertxTestContext vertxTestContext) {
//        JsonObject requestBody=new JsonObject();
//        QueryMapper queryMapper=new QueryMapper();
//        JsonArray jsonArray=new JsonArray();
//        JsonArray jsonArray2=new JsonArray();
//        jsonArray2.add("value").add("value").add("value").add("value").add("value");
//        jsonArray.add("value");
//        requestBody.put(SEARCH_TYPE,SEARCH_TYPE_ATTRIBUTE)
//                .put(PROPERTY,jsonArray)
//                .put(VALUE,jsonArray2);
//        JsonObject errResponse = new JsonObject().put(STATUS, FAILED).put(TYPE, TYPE_INVALID_PROPERTY_VALUE);
//        errResponse.put(DESC, "The max number of 'value' should be " + VALUE_SIZE);
//        assertEquals(errResponse,queryMapper.validateQueryParam(requestBody));
//        vertxTestContext.completeNow();
//    }
    @Test
    @DisplayName("Test validateQueryParam method when limit is exceeded")
    public void testValidateQueryParamExceedLimit(VertxTestContext vertxTestContext) {
        JsonObject requestBody=new JsonObject();
        QueryMapper queryMapper=new QueryMapper();
        JsonArray jsonArray=new JsonArray();
        jsonArray.add("value1").add("value2").add("value3").add("value4").add("value5")
                .add("value6").add("value7").add("value8").add("value9").add("value10")
                .add("value11").add("value12").add("value13").add("value14").add("value15")
                .add("value16").add("value17").add("value18").add("value19").add("value20").add("value21");
        requestBody.put(SEARCH_TYPE,SEARCH_TYPE_ATTRIBUTE)
                .put(PROPERTY,jsonArray);
        JsonObject errResponse = new JsonObject().put(STATUS, FAILED).put(TYPE, TYPE_INVALID_PROPERTY_VALUE);
        errResponse.put(DESC, "The max number of 'property' should be " + PROPERTY_SIZE);
        assertEquals(errResponse,queryMapper.validateQueryParam(requestBody));
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test validateQueryParam method when Instance has exceeded the limit")
    public void testValidateQueryParamMaxLimit(VertxTestContext vertxTestContext) {
        JsonObject requestBody=new JsonObject();
        QueryMapper queryMapper=new QueryMapper();
        JsonArray jsonArray=new JsonArray();
        jsonArray.add("7.0000000").add("7.9").add("7.9").add("7.9").add("7.9").add("7.9").add("7.9").add("7.9").add("7.9").add("7.9").add("7.9").add("7.9").add("7.9").add("7.9").add("7.9").add("7.9").add("7.9").add("7.9").add("7.9").add("7.9").add("7.9");
        requestBody.put(SEARCH_TYPE,SEARCH_TYPE_GEO);
        requestBody.put(COORDINATES,jsonArray);
        JsonObject errResponse = new JsonObject().put(STATUS, FAILED).put(TYPE, TYPE_INVALID_PROPERTY_VALUE);
        errResponse.put(DESC, "The max number of 'coordinates' value is " + COORDINATES_SIZE);
        assertEquals(errResponse,queryMapper.validateQueryParam(requestBody));
        vertxTestContext.completeNow();
    }

}
