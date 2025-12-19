package com.github.mlwilli.assetman

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
    exclude = [UserDetailsServiceAutoConfiguration::class]
)
class AssetmanApplication

fun main(args: Array<String>) {
    runApplication<AssetmanApplication>(*args)
}
