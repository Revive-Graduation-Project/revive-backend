package com.restaurant.kitchen.controller;

import com.restaurant.kitchen.service.KitchenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/kitchen")
public class KitchenController {

    private final KitchenService service;

//    @PatchMapping("/")
//    public String activeTickets() {
//        try {
//            service.getActiveTickets();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
}
