package com.SwiftBasket.product_service.service.impl;

import org.springframework.stereotype.Service;

import com.SwiftBasket.product_service.entity.Product;
import com.SwiftBasket.product_service.mapper.ProductMapper;
import com.SwiftBasket.product_service.repository.ProductRepository;
import com.SwiftBasket.product_service.repository.ProductSearchRepository;
import com.SwiftBasket.product_service.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductSearchRepository productSearchRepository;
    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductRepository productRepository,
                              ProductSearchRepository productSearchRepository,
                              ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productSearchRepository = productSearchRepository;
        this.productMapper = productMapper;
    }

}
