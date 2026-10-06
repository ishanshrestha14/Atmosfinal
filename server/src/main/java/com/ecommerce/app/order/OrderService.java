package com.ecommerce.app.order;

import com.ecommerce.app.address.Address;
import com.ecommerce.app.email.OrderItem;
import com.ecommerce.app.handler.exceptions.InsufficientStockException;
import com.ecommerce.app.handler.exceptions.ResourceNotFoundException;
import com.ecommerce.app.inventory.InventoryRepository;
import com.ecommerce.app.logging.LoggingService;
import com.ecommerce.app.product.Product;
import com.ecommerce.app.order.repos.AddressRepository;
import com.ecommerce.app.order.repos.WebOrderContentRepository;
import com.ecommerce.app.order.repos.WebOrderRepository;
import com.ecommerce.app.product.ProductRepository;
import com.ecommerce.app.user.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@LoggingService
@Service
@RequiredArgsConstructor
public class OrderService {

    private final WebOrderRepository webOrderRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final WebOrderContentRepository webOrderContentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<WebOrder> getOrders(AppUser user) {
        Long userId = user.getId();
        return webOrderRepository.findByAppUserId(userId);
    }

    /** Staff can read any order; customers can only read their own. */
    public WebOrder getOrder(AppUser requester, Long orderId) {
        Optional<WebOrder> order = isStaff(requester)
                ? webOrderRepository.findById(orderId)
                : webOrderRepository.findByIdAndAppUser_Id(orderId, requester.getId());
        return order.orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    @Transactional
    public WebOrder addOrder(AppUser appUser, List<WebOrderContentDTO> dto, Long addressId) {
        Address address = addressRepository.findByIdAndAppUser(addressId, appUser)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));

        WebOrder newWebOrder = WebOrder.builder()
                .appUser(appUser)
                .address(address)
                .createdDate(String.valueOf(LocalDateTime.now()))
                .build();

        WebOrder savedOrder = webOrderRepository.save(newWebOrder);

        List<OrderItem> orderItems = new ArrayList<>();

        for (WebOrderContentDTO orderContentDTO : dto) {
            Product product = productRepository.findById(orderContentDTO.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", orderContentDTO.getProductId()));

            if (inventoryRepository.reserveStock(product.getId(), orderContentDTO.getQuantity()) == 0) {
                throw new InsufficientStockException(product.getId());
            }

            WebOrderContent webOrderContent = WebOrderContent.builder()
                    .product(product)
                    .quantity(orderContentDTO.getQuantity())
                    .webOrder(savedOrder)
                    .build();

            savedOrder.getContents().add(webOrderContent);
            webOrderContentRepository.save(webOrderContent);

            var orderItem = OrderItem
                    .builder()
                    .name(product.getName())
                    .image(product.getFilePath())
                    .price(product.getPrice())
                    .quantity(orderContentDTO.getQuantity())
                    .build();

            orderItems.add(orderItem);
        }

        Integer totalPrice = orderItems.stream()
                .mapToInt(item -> item.getPrice() * item.getQuantity())
                .sum();

        eventPublisher.publishEvent(new OrderPlacedEvent(appUser.getEmail(), appUser.getUsername(), orderItems, totalPrice));

        return webOrderRepository.save(savedOrder);
    }

    public void deleteOrder(Long orderId) {
        WebOrder webOrder = webOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        webOrderRepository.delete(webOrder);
    }

    private static boolean isStaff(AppUser user) {
        return user.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_STAFF"));
    }
}
