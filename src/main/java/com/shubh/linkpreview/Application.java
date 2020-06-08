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
    private final static ObjectMapper mapper= new ObjectMapper();

    Application(Servlet servlet){
        this.servlet=servlet;
    }

    @Bean
    public ServletRegistrationBean<Servlet> registerServlet(){
        return new ServletRegistrationBean <>(servlet,"/");
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(){
        return args -> {

            Page page =  new Page();
            page.setOriginalUrl("http://www.thrillophilia.com/blog/india-best-travel-bloggers/");
            page.setTitle("INDIA’S BEST TRAVEL BLOGGERS: WORLD SERIES 2016");
            page.setDescription("Seeing new places, meeting different people, trying some tasty dishes will definitely make you feel like living in an alternate world. We at Thrillophilia are coming up with a series of top bloggers from around the globe. Let’s first read about India’s top bloggers and learn about the fascinating destinations where they have travelled. Also learn about the various travel hacks they have got to share with you.");
            page.setImage("http://www.thrillophilia.com/blog/wp-content/uploads/2016/11/Top-54-Indian-Travel-Bloggers-2016-6.jpg");

            logger.info(mapper.writeValueAsString(page));
        };
    }
}
