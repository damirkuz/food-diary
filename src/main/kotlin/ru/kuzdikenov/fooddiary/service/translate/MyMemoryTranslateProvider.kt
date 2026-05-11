package ru.kuzdikenov.fooddiary.service.translate

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body
import ru.kuzdikenov.fooddiary.config.properties.MyMemoryTranslateProperties
import ru.kuzdikenov.fooddiary.exception.ExternalTranslateException

@Component
class MyMemoryTranslateProvider(
    @Qualifier("externalRestClientBuilder")
    restClientBuilder: RestClient.Builder,
    properties: MyMemoryTranslateProperties,
) : TranslationProvider {

    private val restClient = restClientBuilder
        .baseUrl(properties.baseUrl)
        .build()

    override fun translate(text: String): String? {
        val response = try {
            restClient.get()
                .uri { builder ->
                    builder
                        .path("/get")
                        .queryParam("q", text)
                        .queryParam("langpair", "ru|en")
                        .build()
                }
                .retrieve()
                .body<TranslateSearchResponse>()
        } catch (e: RestClientException) {
            throw ExternalTranslateException()
        }

        response?.responseData?.match?.let {
            if (it >= MATCH_PERCENT) {
                return response.responseData.translatedText.takeIf(String::isNotBlank)
            }
        }
        return null
    }

    private data class TranslateSearchResponse(
        val responseData: ResponseData? = null,
    )

    private data class ResponseData(
        val translatedText: String = "",
        val match: Double = 0.0,
    )

    companion object {
        private const val MATCH_PERCENT = 0.9
    }
}
