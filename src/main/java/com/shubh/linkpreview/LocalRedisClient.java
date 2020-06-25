package com.shubh.linkpreview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

// local redis setup using map
@Service
public class LocalRedisClient {

    private static final Logger logger = LoggerFactory.getLogger(LocalRedisClient.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map <String,String> localRedis;

    private final AtomicLong counter;
    private static final int LEN=8;

    public LocalRedisClient() {
        localRedis = new HashMap <>();
        counter = new AtomicLong(0);
    }

    private String generateURL() {

        long num = counter.incrementAndGet();
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
        String pageString= localRedis.get(shortenUrl);
        if(pageString==null) {
            throw new IllegalArgumentException("This Page does not exists!");
        }
        return mapper.readValue(pageString, Meta.class);
    }


    public String getFromMetaDetails(Meta meta) throws JsonProcessingException {
        String pageString  = localRedis.get(meta.getOriginalUrl());

        if(pageString!=null) {
            Meta readMeta = mapper.readValue(pageString, Meta.class);
            String shortenUrl = readMeta.getShortenUrl();
            meta.setShortenUrl(shortenUrl);
            // use current/updated meta details for updating the content
            pageString = mapper.writeValueAsString(meta);
            localRedis.put(shortenUrl,pageString);
            localRedis.put(readMeta.getOriginalUrl(),pageString);
            return shortenUrl;
        }

        String shortenUrl = generateURL();
        logger.info("Key Generated: " + shortenUrl);
        meta.setShortenUrl(shortenUrl);

        try {
            pageString = mapper.writeValueAsString(meta);
        }catch(JsonProcessingException e) {
            logger.error("Error in serializing the Page details", e);
            throw e;
        }

        localRedis.put(shortenUrl,pageString);
        localRedis.put(meta.getOriginalUrl(),pageString);
        logger.info("Generated " + shortenUrl + " For " + pageString);
        return shortenUrl;
    }

}