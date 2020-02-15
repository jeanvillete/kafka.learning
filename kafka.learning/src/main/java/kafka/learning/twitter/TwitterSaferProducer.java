package kafka.learning.twitter;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TwitterSaferProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwitterSaferProducer.class);

    private final String apiConsumerKey;
    private final String apiConsumerSecret;
    private final String token;
    private final String secret;

    public TwitterSaferProducer(String apiConsumerKey, String apiConsumerSecret, String token, String secret) {
        this.apiConsumerKey = apiConsumerKey;
        this.apiConsumerSecret = apiConsumerSecret;
        this.token = token;
        this.secret = secret;
    }

    public static void main(String[] args) {
        if (args == null || args.length != 4) {
            throw new IllegalArgumentException(TwitterSaferProducer.class + " must be started providing arguments; apiConsumerKey, apiConsumerSecret, token and secret");
        }

        String apiConsumerKey = args[0];
        String apiConsumerSecret = args[1];
        String token = args[2];
        String secret = args[3];

        LOGGER.debug(
                "Starting " +
                        TwitterSaferProducer.class +
                        " with arguments; apiConsumerKey [{}], apiConsumerSecret [{}], token [{}] and secret [{}]",
                apiConsumerKey,
                apiConsumerSecret,
                token,
                secret
        );

        new TwitterSaferProducer(apiConsumerKey, apiConsumerSecret, token, secret).run();
    }

    public void run() {
        /** Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */
        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);

        // create a twitter account
        Client twitterClient = createTwitterClient(msgQueue);
        twitterClient.connect();

        // create a kafka producer
        KafkaProducer<String, String> kafkaProducer = createKafkaProducer();

        Runtime.getRuntime().addShutdownHook(
                new Thread( () -> {
                    LOGGER.info("Stopping application...");
                    LOGGER.info("Shutting down client from twitter...");
                    twitterClient.stop();
                    LOGGER.info("Shutting down kafka producer...");
                    kafkaProducer.close();
                    LOGGER.info("Shutting done!, bye");
                })
        );

        // loop to send tweets to kafka
        while (!twitterClient.isDone()) {
            String tweet = null;
            try {
                tweet = msgQueue.poll(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.error("InterruptedException raised while receiving tweets; ", e);
                twitterClient.stop();
            }

            Optional.ofNullable(tweet)
                    .ifPresent( _tweet -> {
                                LOGGER.info("Tweet message; [{}]", _tweet);
                                kafkaProducer.send(
                                        new ProducerRecord<>("twitter_tweets", null, _tweet),
                                        (recordMetadata, exception) -> {
                                            if (exception == null) {
                                                LOGGER.info(
                                                        "Received new metadata; Topic: {}, Partition: {}, Offset: {}, Timestamp: {}",
                                                        recordMetadata.topic(),
                                                        recordMetadata.partition(),
                                                        recordMetadata.offset(),
                                                        recordMetadata.timestamp()
                                                );
                                            } else {
                                                LOGGER.error("Error while processing; ", exception);
                                            }
                                        }
                                );
                            }
                    );
        }

        LOGGER.info("End of application....");
    }

    private KafkaProducer<String, String> createKafkaProducer() {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // properties to a safer producer
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        properties.setProperty(ProducerConfig.RETRIES_CONFIG, String.valueOf(Integer.MAX_VALUE));
        properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");

        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);

        return kafkaProducer;
    }

    private Client createTwitterClient(BlockingQueue<String> msgQueue) {

        /** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
        // Optional: set up some followings and track terms
        List<String> terms = Lists.newArrayList("kafka");
        hosebirdEndpoint.trackTerms(terms);

        // These secrets should be read from a config file
        Authentication hosebirdAuth = new OAuth1(apiConsumerKey, apiConsumerSecret, token, secret);

        ClientBuilder builder = new ClientBuilder()
                .name("Hosebird-Client-01")                              // optional: mainly for the logs
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue));

        Client hosebirdClient = builder.build();

        return hosebirdClient;
    }
}
