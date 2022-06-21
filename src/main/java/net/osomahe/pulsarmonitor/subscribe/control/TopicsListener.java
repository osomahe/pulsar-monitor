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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Pattern;


@ApplicationScoped
public class TopicsListener implements MessageListener<byte[]> {

    private static final Pattern TOPIC_GROUPING_PATTERN = Pattern.compile("\\-partition-\\d+");

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

        var tags = getTags(messageTopic, oSchema, oUserBreakdown);

        if (oJsonObject.isEmpty()) {
            metricRegistry.counter(Metadata.builder()
                    .withName("jsonUnrecognized")
                    .withDescription("Displays number of consumed invalid json messages")
                    .build(), tags).inc();
        } else {
            metricRegistry.counter(Metadata.builder()
                    .withName("jsonValid")
                    .withDescription("Displays number of consumed valid json messages")
                    .build(), tags).inc();
        }
        if (oSchema.isEmpty()) {
            metricRegistry.counter(Metadata.builder()
                    .withName("schemaUnrecognized")
                    .withDescription("How many consumed message was application not able to find json schema for")
                    .build(), tags).inc();
        } else {
            metricRegistry.counter(Metadata.builder()
                    .withName("schemaValid")
                    .withDescription("How many consumed message was application able to find json schema for")
                    .build(), tags).inc();
        }
        if (oUserBreakdownJsonPath.isPresent()) {
            if (oUserBreakdown.isEmpty()) {
                metricRegistry.counter(Metadata.builder().withName("userBreakdownUnrecognized")
                        .withDescription("How many consumed message was application not able to breakdown by user defined json path")
                        .build(), tags).inc();
            } else {
                metricRegistry.counter(Metadata.builder().withName("userBreakdownValid")
                        .withDescription("How many consumed message was application not able to breakdown by user defined json path")
                        .build(), tags).inc();
            }
        }
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

    private Tag[] getTags(String topic, Optional<SchemaRecord> oSchema, Optional<String> oUserBreakdown) {
        var tags = new ArrayList<Tag>();
        tags.add(new Tag("topic", topic));
        if (oSchema.isPresent()) {
            tags.add(new Tag("schema", oSchema.get().name));
        }
        if (oUserBreakdown.isPresent()) {
            tags.add(new Tag("userBreakdown", oUserBreakdown.get()));
        }

        log.debugf("tags: %s", String.join(",", tags.stream().map(Tag::toString).toList()));
        return tags.toArray(new Tag[tags.size()]);
    }

    private Optional<String> findJsonPathBreakdown(String json) {
        if (oUserBreakdownJsonPath.isPresent()) {
            try {
                return Optional.ofNullable(JsonPath.parse(json).read(oUserBreakdownJsonPath.get(), String.class));
            } catch (Exception e) {
                log.debugf(e, "Cannot read jsonPath: %s in json: %s", oUserBreakdownJsonPath.get(), oneLiner(json));
            }
        }
        return Optional.empty();
    }

    private String oneLiner(String multiLine) {
        if (multiLine == null) {
            return null;
        }
        return multiLine.replaceAll("\n", " ");
    }
}
