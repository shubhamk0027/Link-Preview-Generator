package com.shubh.linkpreview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.Redisson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;


@Service
class Servlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(Servlet.class);
    private final RedisClient redisClient;
    private final ObjectMapper mapper= new ObjectMapper();

    Servlet(RedisClient redisClient){
        this.redisClient=redisClient;
    }

    private String getBody(HttpServletRequest request) throws IOException {
        Scanner s = new Scanner(request.getInputStream(), StandardCharsets.UTF_8).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private String getHtml(Page page){
        return ""+
                "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"utf-8\">\n" +
                "    <title>"+page.getTitle()+"</title>\n" +
                "    <meta property=\"og:title\" content=\""+page.getTitle()+"\" />\n" +
                "    <meta property=\"og:description\" content=\""+page.getDescription()+"\" />\n" +
                "    <meta property=\"og:image\" content=\""+page.getImage()+"\" />\n" +
                "    <meta property=\"twitter:title\" content=\""+page.getTitle()+"\" />\n" +
                "    <meta property=\"twitter:description\" content=\""+page.getDescription()+"\" />\n" +
                "    <meta property=\"twitter:image\" content=\""+page.getImage()+"\" />\n" +
                "    <meta property=\"twitter:card\" content=\"summary_large_image\" />\n" +
                "    <script>\n" +
                "\t\twindow.location = \""+page.getOriginalUrl()+"\";\n" +
                "\t</script>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <p>You will be redirected shortly...</p>\n" +
                "</body>\n" +
                "</html>";
    }

    /**
     * @param req Request for getting the html page associated with the url
     * @param resp Return the html content after generating it if url exists
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.info("GET "+req.getRequestURI());
        try{
            Page page = redisClient.getFromShortenUrl(req.getRequestURI().substring(1));
//            resp.setHeader();
            resp.setHeader("Content-Type","text/html; charset=UTF-8");
            PrintWriter out = resp.getWriter();
            String html =getHtml(page);
            out.write(html);
            logger.info("Returned :"+html);
            out.close();
            resp.setStatus(200);
        }catch(IllegalArgumentException | JsonProcessingException e){
            PrintWriter out = resp.getWriter();
            out.write(e.getMessage());
            logger.info("Page not found");
            e.getStackTrace();
            out.close();
            resp.setStatus(404);
        }
    }

    /**
     * @param req Post req with page details scrapped from the site
     * @param resp shortenUrl, generate it if it does not exists
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = getBody(req);
        logger.info("POST "+req.getRequestURI()+" requested with "+body);
        try{
            if(!req.getRequestURI().equals("/generate")) throw new IllegalArgumentException("This page does not exists!");
            Page page = mapper.readValue(body,Page.class);
            logger.info("Getting request for link: "+page.getOriginalUrl());
            String shortenUrl = redisClient.getFromOriginalUrl(page.getOriginalUrl(),page);
            PrintWriter out = resp.getWriter();
            out.write("{\"shortenUrl\":\""+shortenUrl+"\"}");
            logger.info("Returned :"+"{\"shortenUrl\":\""+shortenUrl+"\"}");
            out.close();
            resp.setStatus(200);
        }catch(IllegalArgumentException | JsonProcessingException e){
            PrintWriter out = resp.getWriter();
            out.write(e.getMessage());
            e.getStackTrace();
            logger.info("Page details not found correct");
            out.close();
            resp.setStatus(404);
        }
    }

}

// As soon as we write the content is sent to the server, so before sending it adjust the content type!
// 404 -> page not found or
// -> browser was able to communicate, but the server could not find what was requested.
// Servlets are multi-threaded inherently but RESTful services are not.
// You are confusing two paradigms here:
// REST is a software architecture “style”;
// Servlet is a server-side technology.