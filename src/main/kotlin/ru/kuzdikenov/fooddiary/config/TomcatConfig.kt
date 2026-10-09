package ru.kuzdikenov.fooddiary.config

import org.springframework.boot.tomcat.TomcatContextCustomizer
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Относительные редиректы: Location остаётся вида "/login?error",
 * браузер сам подставляет текущий host:port.
 */
@Configuration
class TomcatConfig {

    @Bean
    fun relativeRedirectsCustomizer(): WebServerFactoryCustomizer<TomcatServletWebServerFactory> =
        WebServerFactoryCustomizer { factory ->
            factory.addContextCustomizers(
                TomcatContextCustomizer { context -> context.useRelativeRedirects = true }
            )
        }
}
