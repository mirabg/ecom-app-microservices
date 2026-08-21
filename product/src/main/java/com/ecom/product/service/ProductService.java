package com.ecom.product.service;


import com.ecom.product.dto.ProductRequest;
import com.ecom.product.dto.ProductResponse;
import com.ecom.product.model.Product;
import com.ecom.product.respository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        Product product = mapProductRequestToProduct(productRequest);
        productRepository.save(product);
        return mapProductToProductResponse(product);
    }

    public List<ProductResponse> fetchAllProducts() {
        List<Product> products = productRepository.findByActiveTrue();
        return products.stream().map(this::mapProductToProductResponse).toList();
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        return productRepository.findById(id)
                .map(existingProduct -> updateProductFromRequest(existingProduct, productRequest))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private ProductResponse updateProductFromRequest(Product product, ProductRequest productRequest) {
        product.setCategory(productRequest.getCategory());
        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setImageUrl(productRequest.getImageUrl());
        product.setPrice(productRequest.getPrice());
        product.setStockQuantity(productRequest.getStockQuantity());

        productRepository.save(product);
        return mapProductToProductResponse(product);
    }

    private Product mapProductRequestToProduct(ProductRequest productRequest) {
        Product product = new Product();
        product.setCategory(productRequest.getCategory());
        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setImageUrl(productRequest.getImageUrl());
        product.setPrice(productRequest.getPrice());
        product.setStockQuantity(productRequest.getStockQuantity());

        return product;
    }

    private ProductResponse mapProductToProductResponse(Product product) {
        ProductResponse productResponse = new ProductResponse();
        productResponse.setName(product.getName());
        productResponse.setDescription(product.getDescription());
        productResponse.setImageUrl(product.getImageUrl());
        productResponse.setPrice(product.getPrice());
        productResponse.setStockQuantity(product.getStockQuantity());
        productResponse.setCategory(product.getCategory());
        productResponse.setId(product.getId());
        productResponse.setActive(product.getActive());
        return productResponse;
    }

    @Transactional
    public boolean deleteProduct(Long id) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setActive(false);
                    productRepository.save(product);
                    return true;
                }).orElse(false);
    }

    public List<ProductResponse> searchProducts(String keyword) {
        return productRepository.searchProducts(keyword).stream().map(this::mapProductToProductResponse).toList();
    }

    public ProductResponse fetchProductById(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .map(this::mapProductToProductResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }
}
