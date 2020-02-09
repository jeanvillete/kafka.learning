package kafka.learning;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;

import static java.util.Collections.singleton;

public class ConsumerDemo {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerDemo.class);

    public static void main(String[] args) {
        String bootstrapServers = "localhost:9092";
        String groupId = "my-fourth-application";

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // earliest="very beginning of the topic", latest="only new messages onwards", none="throw an error"

        // create consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // subscribe consumer to our topic(s)
        String topic = "first_topic";
        consumer.subscribe(singleton(topic));

        // poll for new data
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
    }
}
