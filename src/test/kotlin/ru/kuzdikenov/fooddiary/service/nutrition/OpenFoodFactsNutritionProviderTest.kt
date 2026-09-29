package ru.kuzdikenov.fooddiary.service.nutrition

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.anything
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import ru.kuzdikenov.fooddiary.config.properties.OpenFoodFactsProperties
import ru.kuzdikenov.fooddiary.exception.ExternalNutritionLookupException
import ru.kuzdikenov.api.dto.NutritionLookupResponse

class OpenFoodFactsNutritionProviderTest {

    private val restClientBuilder = RestClient.builder()
    private val server: MockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val provider = OpenFoodFactsNutritionProvider(
        restClientBuilder,
        OpenFoodFactsProperties("https://world.openfoodfacts.org")
    )

    @Test
    fun `parses nutriments from open food facts response`() {
        server.expect(anything())
            .andExpect(method(HttpMethod.GET))
            .andExpect(requestTo(org.hamcrest.Matchers.containsString("/cgi/search.pl")))
            .andRespond(
                withSuccess(
                    """
                    {
                      "products": [
                        {
                          "product_name": "Bananas, raw",
                          "nutriments": {
                            "energy-kcal_100g": 89.0,
                            "proteins_100g": 1.1,
                            "fat_100g": 0.33,
                            "carbohydrates_100g": 22.8
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val result = provider.lookup("banana")

        assertNotNull(result)
        assertEquals("Bananas, raw", result!!.name)
        assertEquals(89.0, result.caloriesPer100g)
        assertEquals(1.1, result.proteinsPer100g)
        assertEquals(0.33, result.fatsPer100g)
        assertEquals(22.8, result.carbohydratesPer100g)
        assertEquals(NutritionLookupResponse.Source.EXTERNAL_API, result.source)
        server.verify()
    }

    @Test
    fun `returns null when response has no products`() {
        server.expect(anything())
            .andRespond(withSuccess("""{"products": []}""", MediaType.APPLICATION_JSON))

        assertNull(provider.lookup("unknown-product"))
        server.verify()
    }

    @Test
    fun `throws external exception on http error`() {
        server.expect(anything()).andRespond(withServerError())

        assertThrows(ExternalNutritionLookupException::class.java) {
            provider.lookup("banana")
        }
        server.verify()
    }

    @Test
    fun `returns null when product misses nutriments`() {
        server.expect(anything())
            .andRespond(withSuccess("""{"products": [{"product_name": "Bananas"}]}""", MediaType.APPLICATION_JSON))

        assertNull(provider.lookup("banana"))
        server.verify()
    }
}
