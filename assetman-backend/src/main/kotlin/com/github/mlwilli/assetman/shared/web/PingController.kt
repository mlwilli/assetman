package com.github.mlwilli.assetman.shared.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class PingController {

    @GetMapping("/api/ping")
    fun ping() = mapOf("status" to "ok")
}
