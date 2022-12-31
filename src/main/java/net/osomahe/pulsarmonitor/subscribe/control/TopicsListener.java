package net.osomahe.pulsarmonitor.subscribe.control;

import com.jayway.jsonpath.JsonPath;
import net.osomahe.pulsarmonitor.schema.boundary.SchemaValidationFacade;
import net.osomahe.pulsarmonitor.schema.entity.SchemaRecord;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageListener;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.jboss.logging.Logger;
import org.json.JSONObject;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.regex.Pattern;


@ApplicationScoped
public class TopicsListener implements MessageListener<byte[]> {

    private static final Pattern TOPIC_GROUPING_PATTERN = Pattern.compile("\\-partition-\\d+");

    private static final String UNKNOWN = "unknown";

    @Inject
    Logger log;

    @Inject
    SchemaValidationFacade facadeSchema;

    @Inject
    MetricRegistry metricRegistry;

    @ConfigProperty(name = "monitor.user-breakdown-jsonpath")
    Optional<String> oUserBreakdownJsonPath;

    @ConfigProperty(name = "monitor.group-partitioned")
    Boolean groupPartitioned;

    @ConfigProperty(name = "monitor.message-encoding")
    String messageEncoding;

    Charset messageCharset;

    @PostConstruct
    void init() {
        messageCharset = Charset.forName(messageEncoding);
    }

    @Override
    public void received(Consumer<byte[]> consumer, Message<byte[]> message) {
        consumer.acknowledgeAsync(message);
        var messageTopic = getTopicName(message);
        var messageValue = new String(message.getValue(), messageCharset);

        var oJsonObject = facadeSchema.createJsonObject(messageValue);
        var oSchema = oJsonObject.flatMap(facadeSchema::findSchemaRecord);
        var oUserBreakdown = findJsonPathBreakdown(messageValue);

        metricRegistry.counter(Metadata.builder()
                                .withName("pulsarMessage")
                                .withDescription("Displays number of consumed pulsar messages")
                                .build(),
                        getTags(messageTopic, oJsonObject, oSchema, oUserBreakdown))
                .inc();
    }

    private String getTopicName(Message<byte[]> message) {
        if (message == null || message.getTopicName() == null) {
            return null;
        }
        var topicName = message.getTopicName();
        if (Boolean.TRUE.equals(groupPartitioned)) {
            return TOPIC_GROUPING_PATTERN.matcher(topicName).replaceAll("");
        }
        return topicName;
    }

    private Tag[] getTags(String topic, Optional<JSONObject> oJsonObject, Optional<SchemaRecord> oSchema, Optional<String> oUserBreakdown) {
        var tags = new Tag[4];
        tags[0] = new Tag("topic", topic);
        tags[1] = new Tag("contentType", oJsonObject.map(jo -> "json").orElse(UNKNOWN));
        tags[2] = new Tag("jsonSchema", oSchema.map(schema -> schema.name).orElse(UNKNOWN));
        tags[3] = new Tag("jsonPathBreakdown", oUserBreakdown.orElse(UNKNOWN));

        log.debugf("tags: %s", tags);
        return tags;
    }

    private Optional<String> findJsonPathBreakdown(String json) {
        if (oUserBreakdownJsonPath.isPresent()) {
            try {
                return Optional.ofNullable(JsonPath.parse(json).read(oUserBreakdownJsonPath.get(), String.class));
            } catch (Exception e) {
                log.warnf(e, "Cannot read jsonPath: %s in json: %s", oUserBreakdownJsonPath.get(), oneLiner(json));
            }
        }
        return Optional.empty();
    }

    private String oneLiner(String multiLine) {
        if (multiLine == null) {
            return null;
        }
        return multiLine
                .replaceAll("\n", " ")
                .replaceAll("  ", " ")
                .replaceAll("  ", " ")
                .replaceAll("  ", " ")
                .replaceAll("  ", " ");
    }
}
