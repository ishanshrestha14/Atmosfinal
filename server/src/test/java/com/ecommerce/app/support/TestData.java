package com.ecommerce.app.support;

import com.ecommerce.app.address.Address;
import com.ecommerce.app.category.Category;
import com.ecommerce.app.category.CategoryRepository;
import com.ecommerce.app.inventory.Inventory;
import com.ecommerce.app.inventory.InventoryRepository;
import com.ecommerce.app.order.WebOrder;
import com.ecommerce.app.order.WebOrderContent;
import com.ecommerce.app.order.repos.AddressRepository;
import com.ecommerce.app.order.repos.WebOrderContentRepository;
import com.ecommerce.app.order.repos.WebOrderRepository;
import com.ecommerce.app.product.Product;
import com.ecommerce.app.product.ProductRepository;
import com.ecommerce.app.user.AppUser;
import com.ecommerce.app.user.AppUserRepository;
import com.ecommerce.app.user.utils.Role;
import com.ecommerce.app.user.utils.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Creates persisted fixtures through the real repositories. Every call uses unique names,
 * so tests never collide with each other or with seed data.
 */
@TestComponent
@RequiredArgsConstructor
public class TestData {

    public static final String PASSWORD = "Passw0rd!";

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final AddressRepository addressRepository;
    private final WebOrderRepository webOrderRepository;
    private final WebOrderContentRepository webOrderContentRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUser customer() {
        return user("CUSTOMER");
    }

    public AppUser staff() {
        return user("STAFF");
    }

    public Product productWithStock(int price, int quantity) {
        String suffix = unique();
        Category category = categoryRepository.save(Category.builder().name("category-" + suffix).build());
        Product product = productRepository.save(Product.builder()
                .name("product-" + suffix)
                .filePath("/img/" + suffix + ".png")
                .shortDescription("short")
                .longDescription("long")
                .brand("brand")
                .price(price)
                .category(category)
                .build());
        Inventory inventory = inventoryRepository.save(Inventory.builder()
                .product(product)
                .quantity(quantity)
                .build());
        product.setInventory(inventory);
        return product;
    }

    public Address addressOf(AppUser user) {
        return addressRepository.save(Address.builder()
                .addressLine1("1 Test Street")
                .city("Testville")
                .country("Testland")
                .appUser(user)
                .build());
    }

    public WebOrder orderFor(AppUser user, Product product, int quantity) {
        WebOrder order = webOrderRepository.save(WebOrder.builder()
                .appUser(user)
                .address(addressOf(user))
                .createdDate(LocalDateTime.now().toString())
                .build());
        webOrderContentRepository.save(WebOrderContent.builder()
                .product(product)
                .quantity(quantity)
                .webOrder(order)
                .build());
        return order;
    }

    public boolean orderExists(WebOrder order) {
        return webOrderRepository.existsById(order.getId());
    }

    public Address reload(Address address) {
        return addressRepository.findById(address.getId()).orElse(null);
    }

    public boolean productExists(Product product) {
        return productRepository.existsById(product.getId());
    }

    public int stockOf(Product product) {
        return inventoryRepository.findByProduct(product).orElseThrow().getQuantity();
    }

    private AppUser user(String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(roleName)
                        .createdDate(LocalDateTime.now().toString())
                        .build()));
        String suffix = unique();
        return appUserRepository.save(AppUser.builder()
                .username("user-" + suffix)
                .email(suffix + "@test.local")
                .password(passwordEncoder.encode(PASSWORD))
                .enabled(true)
                .roles(List.of(role))
                .createdDate(LocalDateTime.now().toString())
                .build());
    }

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
