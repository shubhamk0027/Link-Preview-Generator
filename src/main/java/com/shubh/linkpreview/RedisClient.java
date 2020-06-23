package com.shubh.linkpreview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RedisClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisClient.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final RedissonClient redissonClient;

    private final AtomicLong counter;
    private final int LEN;
    private final int TTLDays;

    public RedisClient(
            @Value("${LEN}") int LEN,
            @Value("${TTL}") int days) {

        this.LEN = LEN;
        this.TTLDays = days;

        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        redissonClient = Redisson.create(config);

        // Load/Store the counter from the redis
        counter = new AtomicLong(loadCounter());
    }


    private long loadCounter() {
        // will not collide with other keys
        RBucket <Long> counterBucket = redissonClient.getBucket("COUNTER_VALUE");
        if(!counterBucket.isExists()) {
            logger.info("No initial counter value found. Starting it from 0");
            return 0;
        }
        logger.info("Loading counter value from " + counterBucket.get());
        return counterBucket.get();
    }

    private void saveCounter() {
        RBucket <Long> counterBucket = redissonClient.getBucket("COUNTER_VALUE");
        counterBucket.set(counter.longValue());
    }


    private String generateURL() {

        long num = counter.incrementAndGet();
        saveCounter();

        String binary = Long.toBinaryString((1L << 47) | num).substring(1);         // 62^8 > 2^47 => 2^47 numbers can be generated without duplicates!
        // logger.info("Key Generated: "+binary+" of len "+binary.length());      // 10^5 billion urls!
        binary = new StringBuilder(binary).reverse().toString();
        num = Long.parseLong(binary, 2);

        StringBuilder url = new StringBuilder();
        for(int i = 0; i < LEN; i++) {
            long SZ = 62;
            String INDICES = "Za0Yb1Xc2Wd3Ve4Uf5Tg6Sh7Ri8Qj9PkOlNmMnLoKpJqIrHsGtFuEvDwCxByAz";
            url.append(INDICES.charAt((int) (num % SZ)));
            num = num / SZ;
        }

        return url.toString();
    }


    public Meta getFromShortenUrl(String shortenUrl) throws JsonProcessingException {

        RBucket <String> shortUrlToPage = redissonClient.getBucket(shortenUrl);
        if(!shortUrlToPage.isExists()) {
            throw new IllegalArgumentException("This Page does not exists!");
        }

        String pageString = shortUrlToPage.get();
        Meta meta = mapper.readValue(pageString, Meta.class);

        String originalUrl = meta.getOriginalUrl();
        RBucket <String> originalUrlToPage = redissonClient.getBucket(originalUrl);

        // update the TTL
        originalUrlToPage.set(pageString, TTLDays, TimeUnit.DAYS);
        shortUrlToPage.set(pageString, TTLDays, TimeUnit.DAYS);

        return meta;
    }


    /**
     * Will return a new url or update the TTL days of the original URL along with new content
     *
     * @param meta the topic,url.. rest details
     * @return the shortenURL
     */
    // can be improved by storing shortenUrl inside another HashMap!
    public String getFromMetaDetails(Meta meta) throws JsonProcessingException {

        RBucket <String> orgUrlToPage = redissonClient.getBucket(meta.getOriginalUrl());

        if(orgUrlToPage.isExists()) {

            String pageString = orgUrlToPage.get();
            Meta readMeta = mapper.readValue(pageString, Meta.class);

            String shortenUrl = readMeta.getShortenUrl();
            meta.setShortenUrl(shortenUrl);
            // use current/updated meta details for updating the content
            pageString = mapper.writeValueAsString(meta);

            RBucket <String> shortUrlToPage = redissonClient.getBucket(shortenUrl);
            orgUrlToPage.set(pageString, TTLDays, TimeUnit.DAYS);
            shortUrlToPage.set(pageString, TTLDays, TimeUnit.DAYS);

            return shortenUrl;
        }

        String shortenUrl = generateURL();
        logger.info("Key Generated: " + shortenUrl);
        meta.setShortenUrl(shortenUrl);

        String pageString;
        try {
            pageString = mapper.writeValueAsString(meta);
        }catch(JsonProcessingException e) {
            logger.error("Error in serializing the Page details", e);
            throw e;
        }

        RBucket <String> shortUrlToPage = redissonClient.getBucket(shortenUrl);
        shortUrlToPage.set(pageString, TTLDays, TimeUnit.DAYS);
        orgUrlToPage.set(pageString, TTLDays, TimeUnit.DAYS);

        logger.info("Generated " + shortenUrl + " For " + pageString);

        return shortenUrl;
    }

}

// https://stackoverflow.com/questions/30087921/redis-best-way-to-store-a-large-map-dictionary/30094048#:~:text=Prefer%20HSET%20besides%20of%20KEYS,if%20memory%20is%20main%20target.
// Java collections in Redisson are handled the same way as objects. This allows you to perform lock-free, thread-safe, and atomic operations on the Java collections in Redisson.
// https://dzone.com/articles/introducing-redisson-live-object-object-hash-mappi
// https://www.alibabacloud.com/blog/interview-with-the-creator-of-redisson-building-an-open-source-enterprise-redis-client_593854
// https://instagram-engineering.com/storing-hundreds-of-millions-of-simple-key-value-pairs-in-redis-1091ae80f74c