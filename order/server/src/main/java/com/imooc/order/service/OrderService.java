package com.imooc.order.service;

import com.imooc.order.dto.OrderDTO;
import com.imooc.order.entity.OrderItem;
import com.imooc.order.entity.Order;
import com.imooc.order.enums.OrderStatusEnum;
import com.imooc.order.enums.PayStatusEnum;
import com.imooc.order.repository.OrderItemRepository;
import com.imooc.order.repository.OrderRepository;
import com.imooc.order.utils.KeyUtil;
import com.imooc.product.client.ProductClient;
import com.imooc.product.common.DecreaseStockInput;
import com.imooc.product.common.ProductOutput;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductClient productClient;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * 创建订单
     * @param orderDTO
     * @return
     */
    public OrderDTO create(OrderDTO orderDTO) {
        String orderId = KeyUtil.genUniqueKey();
        //查询商品信息(调用商品服务)
        List<String> productIdList = orderDTO.getOrderItemList().stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toList());

        List<ProductOutput> productList = productClient.listForOrder(productIdList);

        //计算总价
        BigDecimal orderAmout = new BigDecimal(BigInteger.ZERO);
        for (OrderItem orderItem : orderDTO.getOrderItemList()) {
            for (ProductOutput product : productList) {
                if (product.getProductId().equals(orderItem.getProductId())) {
                    //单价*数量
                    orderAmout = product.getProductPrice()
                            .multiply(new BigDecimal(orderItem.getProductQuantity()))
                            .add(orderAmout);
                    BeanUtils.copyProperties(product, orderItem);
                    orderItem.setOrderId(orderId);
                    orderItem.setId(KeyUtil.genUniqueKey());
                    //订单详情入库
                    orderItemRepository.save(orderItem);
                }
            }
        }
        //扣库存
        List<DecreaseStockInput> inputs = orderDTO.getOrderItemList().stream()
                .map(e -> new DecreaseStockInput(e.getProductId(), e.getProductQuantity()))
                .collect(Collectors.toList());
        productClient.decreaseStock(inputs);

        //订单入库
        Order order = new Order();
        orderDTO.setOrderId(orderId);
        BeanUtils.copyProperties(orderDTO, order);
        order.setOrderAmount(orderAmout);
        order.setOrderStatus(OrderStatusEnum.NEW.getCode());
        order.setPayStatus(PayStatusEnum.WAIT.getCode());
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        orderRepository.save(order);
        return orderDTO;
    }
}
