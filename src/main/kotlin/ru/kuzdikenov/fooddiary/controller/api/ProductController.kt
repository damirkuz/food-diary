package ru.kuzdikenov.fooddiary.controller.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import ru.kuzdikenov.api.ProductsApi
import ru.kuzdikenov.api.dto.ProductCreateRequest
import ru.kuzdikenov.api.dto.ProductPageResponse
import ru.kuzdikenov.api.dto.ProductResponse
import ru.kuzdikenov.api.dto.ProductUpdateRequest
import ru.kuzdikenov.fooddiary.mapper.ProductMapper
import ru.kuzdikenov.fooddiary.service.ProductService
import ru.kuzdikenov.fooddiary.service.CurrentUserService
import java.net.URI

@RestController
class ProductController(
    private val productService: ProductService,
    private val currentUserService: CurrentUserService,
    private val productMapper: ProductMapper,
) : ProductsApi {
    override fun createProduct(productCreateRequest: ProductCreateRequest): ResponseEntity<ProductResponse> {
        val ownerId = currentUserService.getCurrentUserId()
        val product = productService.createProduct(productMapper.toCommand(productCreateRequest), ownerId)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .location(URI.create("/api/products/${product.id}"))
            .body(productMapper.toResponse(product))
    }

    override fun deleteProduct(id: Long): ResponseEntity<Unit> {
        val ownerId = currentUserService.getCurrentUserId()
        productService.deleteProduct(id, ownerId)
        return ResponseEntity.noContent().build()
    }

    override fun getCurrentUserProducts(
        search: String?,
        page: Int,
        size: Int,
        sort: String
    ): ResponseEntity<ProductPageResponse> {
        val ownerId = currentUserService.getCurrentUserId()
        val products = productService.getCurrentUserProducts(ownerId, search, page, size, sort)
        return ResponseEntity.ok(productMapper.toPageResponse(products))
    }

    override fun getProductById(id: Long): ResponseEntity<ProductResponse> {
        val ownerId = currentUserService.getCurrentUserId()
        val product = productService.getProductById(id, ownerId)
        return ResponseEntity.ok(productMapper.toResponse(product))
    }

    override fun updateProduct(
        id: Long,
        productUpdateRequest: ProductUpdateRequest
    ): ResponseEntity<ProductResponse> {
        val ownerId = currentUserService.getCurrentUserId()
        val product = productService.updateProduct(id, ownerId, productMapper.toCommand(productUpdateRequest))
        return ResponseEntity.ok(productMapper.toResponse(product))
    }


}
