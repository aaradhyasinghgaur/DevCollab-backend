package com.example.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class Application extends SpringBootServletInitializer {

    @Value("${spring.application.name}")
    private String name;

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application){
        return application.sources(Application.class);
    }


    public static void main(String[] args) {
        System.out.println("hello world! this is my first spring boot Application test!");
        SpringApplication.run(Application.class, args);
    }

    @RequestMapping(value = "/")
    public String name(){
        return name;
    }

    //prints output to web server.
//    @GetMapping("/")
//    String home2() {
//        return "Hello World from Tomcat!";
//    }




}


