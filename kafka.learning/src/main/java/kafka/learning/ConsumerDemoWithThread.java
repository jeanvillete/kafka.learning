package kafka.learning;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import static java.util.Collections.singleton;

public class ConsumerDemoWithThread {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerDemoWithThread.class);

    public static void main(String[] args) {
        String bootstrapServers = "localhost:9092";
        String groupId = "my-fourth-application";
        String topic = "first_topic";

        LOGGER.info(
                "Creating and starting new Thread ConsumerRunnable with, " +
                        "Topic [{}], " +
                        "BootstrapServer [{}] and " +
                        "GroupId [{}]",
                topic,
                bootstrapServers,
                groupId
        );

        CountDownLatch latch = new CountDownLatch(1);

        // create the consumer runnable
        ConsumerRunnable myConsumerRunnable = new ConsumerRunnable(
                topic,
                bootstrapServers,
                groupId,
                latch
        );
        new Thread(myConsumerRunnable).start();

        // add shutdown hook
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    LOGGER.info("Caught shutdown hook");
                    myConsumerRunnable.shutdown();
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        LOGGER.error("InterruptedException has been raised; ", e);
                    }
                    LOGGER.info("Application has exited.");
                }, "Thread-Shutting-Down-Hook")
        );

        try {
            latch.await();
        } catch (InterruptedException e) {
            LOGGER.error("Application got interrupted; ", e);
        } finally {
            LOGGER.info("Application is closing...");
        }
    }

}

class ConsumerRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerRunnable.class);

    private CountDownLatch latch;
    private String topic;
    private String bootstrapServers;
    private String groupId;
    private KafkaConsumer<String, String> consumer;

    public ConsumerRunnable(String topic, String bootstrapServers, String groupId, CountDownLatch latch) {
        this.topic = topic;
        this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
        this.latch = latch;

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // earliest="very beginning of the topic", latest="only new messages onwards", none="throw an error"

        // create consumer
        this.consumer = new KafkaConsumer<>(properties);

        // subscribe consumer to our topic(s)
        this.consumer.subscribe(singleton(topic));
    }

    @Override
    public void run() {
        try {
            ConsumerRecords<String, String> consumedRecords;
            while ((consumedRecords = consumer.poll(Duration.ofMillis(500L))) != null) {
                consumedRecords.iterator().forEachRemaining(consumerRecord ->
                        LOGGER.info(
                                "Message received from Topic: {}, with Key: {}, Partition: {}, Offset: {}, Value: {}",
                                topic,
                                consumerRecord.key(),
                                consumerRecord.partition(),
                                consumerRecord.offset(),
                                consumerRecord.value()
                        )
                );
            }
        } catch (WakeupException e) {
            LOGGER.error("Received shutdown signal!", e);
        } finally {
            this.consumer.close();

            // tell our main() code/Thread we're done with the consumer
            this.latch.countDown();
        }
    }

    public void shutdown() {
        // the wakeup() method below is a special method to interrupt consumer.poll()
        // it will throw the exception WakeUpException
        this.consumer.wakeup();
    }
}
