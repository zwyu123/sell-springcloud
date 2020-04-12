package com.imooc.product.service;

import com.imooc.product.common.DecreaseStockInput;
import com.imooc.product.common.ProductOutput;
import com.imooc.product.entity.Product;
import com.imooc.product.enums.ResultEnum;
import com.imooc.product.exception.ProductException;
import com.imooc.product.repository.ProductRepository;
import com.imooc.product.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    public List<ProductVO> list() {
        List<Product> productList = productRepository.findAll();
        List<ProductVO> productVOList = productList.stream().map(r ->
                ProductVO.builder().productId(r.getProductId())
                        .productName(r.getProductName())
                        .productPrice(r.getProductPrice())
                        .build())
                .collect(Collectors.toList());
        return productVOList;
    }

    public List<ProductOutput> findList(List<String> productIdList) {
        List<Product> productList =  productRepository.findByProductIdIn(productIdList);
        List<ProductOutput> productOutputs = productList.stream().map(r ->
                ProductOutput.builder().productId(r.getProductId())
                        .productName(r.getProductName())
                        .productPrice(r.getProductPrice())
                        .productStock(r.getProductStock())
                        .build())
                .collect(Collectors.toList());
        return productOutputs;
    }

    @Transactional
    public void decreaseStock(List<DecreaseStockInput> inputs) {
        for (DecreaseStockInput input : inputs) {
            Optional<Product> productOptional = productRepository.findById(input.getProductId());
            //判断商品是否存在
            if (!productOptional.isPresent()) {
                throw new ProductException(ResultEnum.PRODUCT_NOT_EXIST);
            }
            Product product = productOptional.get();
            //库存是否足够
            Integer result = product.getProductStock() - input.getProductQuantity();
            if (result < 0) {
                throw new ProductException(ResultEnum.PRODUCT_STOCK_ERROR);
            }
            product.setProductStock(result);
            productRepository.save(product);
        }
    }
}
