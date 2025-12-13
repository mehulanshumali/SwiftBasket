package com.SwiftBasket.product_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.SwiftBasket.product_service.document.ProductDocument;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {
    Page<ProductDocument> findByNameOrDescription(String name, String description, Pageable pageable);
    Page<ProductDocument> findByCategoryName(String categoryName, Pageable pageable);
    
    @Query("{\"bool\": {\"should\": [" +
           "{\"match\": {\"name\": {\"query\": \"?0\", \"fuzziness\": \"AUTO\"}}}, " +
           "{\"match\": {\"description\": {\"query\": \"?0\", \"fuzziness\": \"AUTO\"}}}" +
           "]}}")
    
    Page<ProductDocument> searchByKeyword(String keyword, Pageable pageable);

}
