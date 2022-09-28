#!/bin/bash

nohup mvn clean compile exec:java@catalogue-server & 
sleep 40
mvn clean test -Dtest=ServerVerticleDeboardTest
mv target/jacoco.exec target/ServerVerticleDeboardTest.exec
mvn test -Dtest=ApiServerVerticlePreprareTest
mv target/jacoco.exec target/ApiServerVerticlePreprareTest.exec
mvn test -Dtest=ApiServerVerticleTest
mv target/jacoco.exec target/ApiServerVerticleTest.exec
mvn test -Dtest=ServerVerticleDeboardTest
mvn test -Dtest=DatabaseServiceTest
mv target/jacoco.exec target/DatabaseServiceTest.exec
<<<<<<< HEAD
mvn test -Dtest=ConstraintsValidationTest,AuthenticationServiceTest,ElasticClientTest,QueryDecoderTest,SummarizerTest,ValidatorServiceTest,RatingServiceTest,DatabrokerServiceTest,AuthenticationServiceImplTest,NLPSearchServiceImplTest,ValidatorServiceImplTest,ExceptionHandlerTest,QueryMapperTest,RespBuilderTest,QueryBuilderTest,AuthorizationRequestTest,JwtDataTest
mv target/jacoco.exec target/jacoco2.exec
=======
mvn test -Dtest=ConstraintsValidationTest,AuthenticationServiceTest,ElasticClientTest,QueryDecoderTest,SummarizerTest,ValidatorServiceTest,RatingServiceTest,DatabrokerServiceTest
>>>>>>> including code cov from apiServer tests
