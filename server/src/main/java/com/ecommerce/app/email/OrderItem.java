package com.ecommerce.app.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class OrderItem {

    private String image;

    private String name;

    private Integer quantity;

    private BigDecimal price;

    private BigDecimal subTotal;

}