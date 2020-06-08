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
import java.util.concurrent.atomic.AtomicInteger;

// Need to create a new link of image as well
@Service
public class RedisClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisClient.class);
    private final ObjectMapper mapper= new ObjectMapper();
    private RedissonClient redissonClient;

    private final AtomicInteger counter;
    private final int LEN;
    private final int TTLDays;

    RedisClient(
            @Value("${MAXTRIES}") int MAXTRIES,
            @Value("${LEN}") int LEN,
            @Value("${TTL}") int days){

        this.LEN=LEN;
        this.TTLDays=days;

        Config config= new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        for(int i=0;i<MAXTRIES;i++){
            try{
                redissonClient= Redisson.create(config);
                logger.info("Successfully Connected to Redis");
                break;
            }catch(Exception e){
                logger.info(e.getMessage());
            }
        }

        // get and store these in redis!
        counter = new AtomicInteger(0);
    }


    private String generateURL(){
        int num = counter.incrementAndGet();
        StringBuilder url= new StringBuilder();
        for(int i=0;i<LEN;i++){
            int SZ = 62;
            String INDICES = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            url.append(INDICES.charAt(num% SZ));
            num=num/ SZ;
        }
        return url.toString();
    }


    public Page getFromShortenUrl(String shortenUrl) throws JsonProcessingException {

        RBucket<String> shortUrlToPage = redissonClient.getBucket(shortenUrl);
        if(!shortUrlToPage.isExists()) {
            throw new IllegalArgumentException("This Page does not exists!");
        }

        Page page = mapper.readValue(shortUrlToPage.get(),Page.class);
        String originalUrl = page.getOriginalUrl();
        RBucket<String> originalUrlToPage = redissonClient.getBucket(originalUrl);

        originalUrlToPage.set(shortUrlToPage.get(),TTLDays,TimeUnit.DAYS);
        shortUrlToPage.set(shortUrlToPage.get(),TTLDays,TimeUnit.DAYS);

        return page;
    }



    /**
     * Will return a new unused url with setting TLL to TLLDays
     * @param originalUrl the url to be shortended
     * @param page the topic,url.. rest details
     * @return the shortenURL
     */
    // can be improved by storing shortenUrl inside another HashMap!
    public String getFromOriginalUrl(String originalUrl, Page page) throws JsonProcessingException {

        RBucket<String> orgUrlToPage = redissonClient.getBucket(originalUrl);
        // Update the TTL days if value exists and return the URL
        if(orgUrlToPage.isExists()) {
            String pageString = orgUrlToPage.get();
            Page readPage = mapper.readValue(pageString,Page.class);
            String shortenUrl =  readPage.getShortenUrl();
            RBucket<String> shortUrlToPage = redissonClient.getBucket(shortenUrl);
            orgUrlToPage.set(pageString,TTLDays,TimeUnit.DAYS);
            shortUrlToPage.set(pageString,TTLDays,TimeUnit.DAYS);
            return shortenUrl;
        }

        String shortenUrl = generateURL(); // generate while we get an empty bucket
        while(redissonClient.getBucket(shortenUrl).isExists()) shortenUrl=generateURL();
        page.setShortenUrl(shortenUrl);

        String pageString;
        try{
            pageString = mapper.writeValueAsString(page);
        }catch(JsonProcessingException e){
            logger.error("Error in serializing the Page details",e);
            throw e;
        }

        RBucket<String> shortUrlToPage = redissonClient.getBucket(shortenUrl);
        shortUrlToPage.set(pageString,TTLDays,TimeUnit.DAYS);
        orgUrlToPage.set(pageString,TTLDays, TimeUnit.DAYS);

        logger.info("Generated "+shortenUrl+" For "+pageString);
        return shortenUrl;
    }

}


// https://stackoverflow.com/questions/30087921/redis-best-way-to-store-a-large-map-dictionary/30094048#:~:text=Prefer%20HSET%20besides%20of%20KEYS,if%20memory%20is%20main%20target.
// Java collections in Redisson are handled the same way as objects. This allows you to perform lock-free, thread-safe, and atomic operations on the Java collections in Redisson.
// https://dzone.com/articles/introducing-redisson-live-object-object-hash-mappi
// https://www.alibabacloud.com/blog/interview-with-the-creator-of-redisson-building-an-open-source-enterprise-redis-client_593854
// https://instagram-engineering.com/storing-hundreds-of-millions-of-simple-key-value-pairs-in-redis-1091ae80f74c