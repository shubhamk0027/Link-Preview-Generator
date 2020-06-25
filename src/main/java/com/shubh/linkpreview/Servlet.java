package com.shubh.linkpreview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;


@Service
class Servlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(Servlet.class);
    private final LocalRedisClient localRedisClient;
    private final ObjectMapper mapper = new ObjectMapper();


    Servlet(LocalRedisClient localRedisClient) {
        this.localRedisClient = localRedisClient;
    }


    private String getBody(HttpServletRequest request) throws IOException {
        Scanner s = new Scanner(request.getInputStream(), StandardCharsets.UTF_8).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }


    public String getDomainName(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    private String getHtml(Meta meta) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"utf-8\">\n" +
                "    <title>" + meta.getTitle() + "</title>\n" +
                "    <meta property=\"og:title\" content=\"" + meta.getTitle() + "\" />\n" +
                "    <meta property=\"og:image\" content=\"" + meta.getImage() + "\" />\n" +
                "    <meta property=\"og:description\" content=\"" + meta.getDescription() + "\" />\n" +
                "    <meta property=\"og:site_name\" content=\"" + meta.getDomainName() + "\" />\n" +
                "    <meta property=\"og:url\" content=\"" + meta.getOriginalUrl() + "\" />\n" +
                "    <meta property=\"twitter:card\" content=\"summary_large_image\" />\n" +
                "    <script>\n" +
                "       window.location = \"" + meta.getOriginalUrl() + "\";\n" +
                "    </script>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <p>Hold on. You will be redirected shortly...</p>\n" +
                "</body>\n" +
                "</html>";
    }


    /**
     * @param req  Request for getting the html page associated with the url
     * @param resp Return the html content after generating it if url exists
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.info("GET " + req.getRequestURI());

        try {

            if(req.getRequestURI().equals("/favicon.ico")) {
                resp.setStatus(200);
                return;
            }

            Meta meta = localRedisClient.getFromShortenUrl(req.getRequestURI().substring(1));
            resp.setHeader("Content-Type", "text/html; charset=UTF-8");
            PrintWriter out = resp.getWriter();
            String html = getHtml(meta);
            out.write(html);
            out.close();
            resp.setStatus(200);
            logger.info("Returned :" + html);

        }catch(IllegalArgumentException | JsonProcessingException e) {

            e.getStackTrace();
            PrintWriter out = resp.getWriter();
            out.write(e.getMessage());
            out.close();
            resp.setStatus(404);
            logger.info("Page not found");

        }
    }


    /**
     * @param req  Post req with Meta, update the info with new data and return old/new url if it originalUrl does not exists
     * @param resp shortenUrl, generate it if it does not exists
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = getBody(req);
        logger.info("POST " + req.getRequestURI() + " requested with " + body);

        try {

            if(!req.getRequestURI().equals("/generate"))
                throw new IllegalArgumentException("This page does not exists!");
            Meta meta = mapper.readValue(body, Meta.class);
            meta.setDomainName(getDomainName(meta.getOriginalUrl()));
            logger.info("Getting request for link: " + meta.getOriginalUrl());

            String shortenUrl = localRedisClient.getFromMetaDetails(meta);
            PrintWriter out = resp.getWriter();
            out.write("{\"shortenUrl\":\"" + shortenUrl + "\"}");
            out.close();
            resp.setStatus(200);
            logger.info("Returned :" + "{\"shortenUrl\":\"" + shortenUrl + "\"}");

        }catch(IllegalArgumentException | JsonProcessingException | URISyntaxException e) {

            PrintWriter out = resp.getWriter();
            out.write(e.getMessage());
            e.getStackTrace();
            out.close();
            resp.setStatus(404);
            logger.info("Meta tag values found incorrect");

        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(req.getMethod().equals("post") || req.getMethod().equals("POST") ||
                req.getMethod().equals("get") || req.getMethod().equals("GET")) super.service(req, resp);
        else {
            logger.info("This method is not supported");
            PrintWriter out = resp.getWriter();
            out.println("Invalid Request!");
            resp.setStatus(400);
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