package kafka.learning;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class ConsumerDemoAssignSeek {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerDemoAssignSeek.class);

    public static void main(String[] args) {
        String bootstrapServers = "localhost:9092";
        String topic = "first_topic";

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
//        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // earliest="very beginning of the topic", latest="only new messages onwards", none="throw an error"

        // create consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // with assign seek, we don't specify a subscribe to a topic, neither a group id (definition of group id and subscribing to topic have been removed)
        // assign and seek are mostly used to replay data or fetch a specific message

        // assign
        TopicPartition partitionToReadFrom = new TopicPartition(topic, 0);
        consumer.assign(Arrays.asList(partitionToReadFrom));

        // seek
        long offsetToReadFrom = 620L;
        consumer.seek(partitionToReadFrom, offsetToReadFrom);

        int numberOfMessagesToRead = 50;
        Boolean keepOnReading = true;
        Integer numberOfMessagesReadSoFar = 0;

        // poll for new data
        ConsumerRecords<String, String> consumedRecords;
        while (keepOnReading) {
            consumedRecords = consumer.poll(Duration.ofMillis(500L));

            for (ConsumerRecord consumerRecord : consumedRecords) {
                LOGGER.info(
                        "Message received from Topic: {}, with Key: {}, Partition: {}, Offset: {}, Value: {}",
                        topic,
                        consumerRecord.key(),
                        consumerRecord.partition(),
                        consumerRecord.offset(),
                        consumerRecord.value()
                );

                numberOfMessagesReadSoFar += 1;
                if (numberOfMessagesReadSoFar >= numberOfMessagesToRead) {
                    keepOnReading = false; //to exit the while loop
                    break;
                }
            }
        }

        LOGGER.info("Exiting the application");
    }
}
