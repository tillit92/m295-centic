package stegmueller.til.centic.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import stegmueller.til.centic.model.Hello;

@RestController
public class HelloWorldController {

    @GetMapping("/helloworld")
    public String helloWorld() {
        return "Hello World";
    }

    @GetMapping("/hello")
    public String hello(@RequestParam(value="name", defaultValue = " World")  String name) {
        return String.format("Hello %s!", name);
    }

    @GetMapping("/api/hello")
    public ResponseEntity<Hello> name(@RequestParam(value="name", defaultValue = "World")  String name) {
        Hello hello = new Hello(name);
        return new ResponseEntity<>(hello, HttpStatus.OK);
    }
}
