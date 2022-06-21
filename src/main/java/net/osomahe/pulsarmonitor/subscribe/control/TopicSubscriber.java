package net.osomahe.pulsarmonitor.subscribe.control;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;


@Startup
@ApplicationScoped
public class TopicSubscriber {

    @Inject
    Logger log;

    @ConfigProperty(name = "pulsar.client-name")
    String subscriberName;


    @ConfigProperty(name = "monitor.topics-patterns")
    String[] topicsPatters;

    @Inject
    PulsarClient pulsarClient;

    @Inject
    TopicsListener topicsListener;

    List<Consumer<byte[]>> consumers;

    void startup(@Observes StartupEvent event) {
        consumers = Arrays.stream(topicsPatters).flatMap(this::createConsumer).toList();
    }

    private Stream<Consumer<byte[]>> createConsumer(String topicsPattern) {
        try {
            return Stream.of(pulsarClient.newConsumer()
                    .topicsPattern(topicsPattern)
                    .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                    .subscriptionName(subscriberName)
                    .messageListener(topicsListener)
                    .subscribe());
        } catch (PulsarClientException e) {
            throw new IllegalStateException("Cannot subscribe to topicsPattern %s".formatted(topicsPattern), e);
        }
    }

    void shutdown(@Observes ShutdownEvent event) {
        consumers.stream().forEach(Consumer::closeAsync);
    }
}
