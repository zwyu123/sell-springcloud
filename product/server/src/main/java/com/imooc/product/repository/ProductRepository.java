package com.imooc.product.repository;

import com.imooc.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByProductIdIn(List<String> productIdList);
}
