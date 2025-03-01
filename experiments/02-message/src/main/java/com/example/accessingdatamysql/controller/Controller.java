package com.example.accessingdatamysql.controller;

import com.example.accessingdatamysql.dto.MessageWrapper;
import com.example.accessingdatamysql.jpa.Message;
import com.example.accessingdatamysql.service.DatabaseImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/demo")
public class Controller {

    private final DatabaseImpl db;

    @Autowired
    public Controller(DatabaseImpl db) {
        this.db = db;
    }

    @PostMapping("/add")
    public String addNewMessage(@RequestBody MessageWrapper wrapper) {
        Message message = db.saveMessage(wrapper);
        return "Saved";
    }

    @GetMapping("/all")
    public Iterable<Message> getAllMessages() {
        return db.getAllMessages();
    }
}
