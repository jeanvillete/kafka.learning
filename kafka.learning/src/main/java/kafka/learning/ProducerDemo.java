package kafka.learning;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerDemo.class);

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);

        for (int i=0; i<1000; i++) {
            ProducerRecord producerRecord = new ProducerRecord<>("first_topic", "message sent from java producer, index; " + i);
            kafkaProducer.send(producerRecord, (recordMetadata, e) -> {
                if (e == null) {
                    LOGGER.info(
                            "Received new metadata; Topic: {}, Partition: {}, Offset: {}, Timestamp: {}",
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
