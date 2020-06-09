package com.shubh.linkpreview;

import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

// This test might hang your computer!
class ApplicationTest {

    public final RedissonClient redissonClient;
    public final AtomicLong counter;
    public final int LEN=8;

    public ApplicationTest(){
        Config config= new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        redissonClient= Redisson.create(config);
        counter=new AtomicLong(0);
    }


    public String generateURL(){
        long num = counter.incrementAndGet();
//        saveCounter();

        String binary = Long.toBinaryString((1L<<47)|num).substring(1);     // 62^8 > 2^47 => 2^47 numbers can be generated without duplicates!
//        logger.info("Key Generated: "+binary+" of len "+binary.length());      // 10^5 billion urls!
        binary = new StringBuilder(binary).reverse().toString();
        num =  Long.parseLong(binary,2);
//        logger.info("Using num "+num);

        StringBuilder url= new StringBuilder();
        for(int i=0;i<LEN;i++){
            long SZ = 62;
            String INDICES = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            url.append(INDICES.charAt((int) (num%SZ)));
            num=num/SZ;
        }

        return url.reverse().toString();
    }

    @Test
    public void uniqueKeyGenerationTest() throws InterruptedException {
        System.out.println("Running this test will clear all the redis data! Or terminate within 8 secs!");
        Thread.sleep(10000);
        redissonClient.getKeys().flushall();

        int limit=1000000;  // 1e6
        for(int i=0;i<limit;i++){
            if(i%10000==0) System.out.println("No dups found in "+i+" Keys");
            String url = generateURL();
            RBucket <Integer> rBucket = redissonClient.getBucket(url);
            if(rBucket.isExists()){
                throw  new IllegalStateException("Duplicate found at "+i+"!");
            }
            rBucket.set(i);
        }

    }


}