package com.foonbot.aqi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.foonbot.aqi.dtos.Message;

@RestController
public class HelloworldApplication {

    @GetMapping("/hello")
    public String sayHello(){
        return "Hello World";
    }
    
    @GetMapping("/helloJson")
    public Message sayHelloJson(){
        return new Message("Hello World");
    }

    @PostMapping("/helloJson")
    public Message sayHelloJson(@RequestBody Message message){
        return message;
    }
}
