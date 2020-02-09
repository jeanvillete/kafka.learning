package kafka.learning;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemoKeys {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerDemoKeys.class);

    public static void main(String[] args) {
        String bootstrapServer = "localhost:9092";

        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);

        for (int i=0; i<10; i++) {
            String topic = "first_topic";
            String value = "message sent from java producer, index; " + i;
            String key = "key " + i;

            ProducerRecord producerRecord = new ProducerRecord<>(
                    topic,
                    key,
                    value
            );

            kafkaProducer.send(producerRecord, (recordMetadata, e) -> {
                if (e == null) {
                    LOGGER.info(
                            "Message with key: {} sent, than it was received new metadata; Topic: {}, Partition: {}, Offset: {}, Timestamp: {}",
                            key,
                            recordMetadata.topic(),
                            recordMetadata.partition(),
                            recordMetadata.offset(),
                            recordMetadata.timestamp()
                    );
                } else {
                    LOGGER.error("Error while processing; ", e);
                }
            });
        }

        kafkaProducer.flush();
        kafkaProducer.close();
    }
}
