package com.shubh.linkpreview;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    private final Servlet servlet;
    private final static Logger logger = LoggerFactory.getLogger(Application.class);

    Application(Servlet servlet) {
        this.servlet = servlet;
    }

    @Bean
    public ServletRegistrationBean <Servlet> registerServlet() {
        return new ServletRegistrationBean <>(servlet, "/");
    }


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        logger.info("Link Generator Running...");
    }
}
