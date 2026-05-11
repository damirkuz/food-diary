package ru.kuzdikenov.fooddiary.controller.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class PageController {

    @GetMapping("/")
    fun home(): String {
        return "home"
    }
}
