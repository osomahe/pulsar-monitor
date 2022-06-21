# Pulsar monitor

This application monitors JSON messages in Apache Pulsar. It subscribes to given topics, validates messages and provides validation statistics via Prometheus metrics endpoint.

Prometheus metrics endpoint path: `/q/metrics`

Application provides 3 layers of validation:

* **json** - if message is json (jsonValid/jsonUnrecognized)
* **schema** - if message is valid base on provided json schemas (schemaValid/schemaUnrecognized)
* **userBreakdown** - if message could be breakdown by user-defined json path (userBreakdownValid/userBreakdownUnrecognized)

Tags:
* topic - which topic received the message
* schema - which json schema validated topic
* userBreakdown - value received from json via configured json path (default `type`) 

## Installation

For simple start up you can use Docker image [https://github.com/osomahe/pulsar-monitor/pkgs/container/pulsar-monitor](https://github.com/osomahe/pulsar-monitor/pkgs/container/pulsar-monitor).

Release notes can be found at [releases.md](releases.md).

Environment variables:

* **MONITOR_JSON_SCHEMA_DIR** - path to directory where json schema files are stored e.g. `/opt/json-schemas`
* **MONITOR_TOPICS_PATTERNS** - define which topic patters should application monitor e.g. `persistent://public/default/customer,persistent://public/default/catalog`
* **MONITOR_USER_BREAKDOWN_JSONPATH** - default "type" define json path for user's metrics breakdown
* MONITOR_GROUP_PARTITIONED - default "true" defines whether partition topics should be grouped into single one
* MONITOR_MESSAGE_ENCODING - default "utf-8" defines what encoding should be used to decode loaded message
* QUARKUS_LOG_CATEGORY__NET_OSOMAHE_PULSARMONITOR__LEVEL - default "INFO" for detailed debugging you can set this to `DEBUG`
* PULSAR_SERVICE_URL - default "pulsar://localhost:6650" url to connect to Apache Pulsar instance
* PULSAR_TLS_TRUST_CERT - not set by default, used for transport encryption using tLS certificate e.g. `/pulsar/certs/ca.cert.pem`
* PULSAR_TLS_CERT_FILE - not set by default, path for client certificate for TLS authorization `/pulsar/certs/pulsar-source-app.cert.pem`
* PULSAR_TLS_KEY_FILE - not set by default, path for client key to certificate for TLS authorization `/pulsar/certs/pulsar-source-app.key-pk8.pem`
* PULSAR_CLIENT_NAME - default "pulsar-monitor" name used subscription name for message consuming
* PULSAR_HEALTH_TOPIC - default "non-persistent://public/default/health-check" topic used for health checking of readiness probe

Examples:
```bash
docker run -d --name pulsar-monitor -p 8080:8080 -e PULSAR_SERVICE_URL="pulsar://pulsarhostname:6650"  -e MONITOR_JSON_SCHEMA_DIR="/opt/json-schemas" -e MONITOR_TOPICS_PATTERNS="persistent://public/default/customer,persistent://public/default/catalog" ghcr.io/osomahe/pulsar-monitor
```

## Metrics examples


**jsonUnrecognized**

```
# HELP application_jsonUnrecognized_total Displays number of consumed invalid json messages
# TYPE application_jsonUnrecognized_total counter
application_jsonUnrecognized_total{topic="persistent://public/default/customer"} 12.0
```


**jsonValid**

```
# HELP application_jsonValid_total Displays number of consumed valid json messages
# TYPE application_jsonValid_total counter
application_jsonValid_total{topic="persistent://public/default/customer"} 5.0
application_jsonValid_total{schema="catalog-item-created",topic="persistent://public/default/catalog",userBreakdown="catalog-item-created"} 14.0
application_jsonValid_total{schema="customer-order-created",topic="persistent://public/default/customer",userBreakdown="customer-order-created"} 32.0
application_jsonValid_total{schema="customer-order-paid",topic="persistent://public/default/customer",userBreakdown="customer-order-paid"} 28.0
```


**schemaUnrecognized**

```
# HELP application_schemaUnrecognized_total How many consumed message was application not able to find json schema for
# TYPE application_schemaUnrecognized_total counter
application_schemaUnrecognized_total{topic="persistent://public/default/customer"} 17.0
```


**schemaValid**

```
# HELP application_schemaValid_total How many consumed message was application able to find json schema for
# TYPE application_schemaValid_total counter
application_schemaValid_total{schema="catalog-item-created",topic="persistent://public/default/catalog",userBreakdown="catalog-item-created"} 14.0
application_schemaValid_total{schema="customer-order-created",topic="persistent://public/default/customer",userBreakdown="customer-order-created"} 32.0
application_schemaValid_total{schema="customer-order-paid",topic="persistent://public/default/customer",userBreakdown="customer-order-paid"} 28.0
```


**userBreakdownUnrecognized**

```
# HELP application_userBreakdownUnrecognized_total How many consumed message was application not able to breakdown by user defined json path
# TYPE application_userBreakdownUnrecognized_total counter
application_userBreakdownUnrecognized_total{topic="persistent://public/default/customer"} 17.0
```


**userBreakdownValid**

```
# HELP application_userBreakdownValid_total How many consumed message was application not able to breakdown by user defined json path
# TYPE application_userBreakdownValid_total counter
application_userBreakdownValid_total{schema="catalog-item-created",topic="persistent://public/default/catalog",userBreakdown="catalog-item-created"} 14.0
application_userBreakdownValid_total{schema="customer-order-created",topic="persistent://public/default/customer",userBreakdown="customer-order-created"} 32.0
application_userBreakdownValid_total{schema="customer-order-paid",topic="persistent://public/default/customer",userBreakdown="customer-order-paid"} 28.0
```

### Health checks

* Liveness probe - `/q/health/live`
* Readiness probe - `/q/health/ready`

## Maintainers

This project was developed with support of companies [HP Tronic](http://www.hptronic.cz/) and [Osomahe](https://www.osomahe.com/).
