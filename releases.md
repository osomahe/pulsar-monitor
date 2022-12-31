# Releases

## 1.0.1 (2022-12-31)
* log invalid messages at warning log level

## 1.0.0 (2022-08-26)
* removing metrics:
  * jsonUnrecognized / jsonValid
  * schemaUnrecognized / schemaValid
  * userBreakdownUnrecognized / userBreakdownValid
* adding metric
  * pulsarMessage with everytime filled in tags: topic, contentType, jsonSchema, jsonPathBreakdown 

## 0.2.0 (2022-06-21)
* grouping of partitioned topics
* loading messages as byte array

## 0.1.0 (2022-05-17)
* expose prometheus metrics
* json validation
* json schema validation
* user break down by json path
