package com.ecommerce.app.address;

import com.ecommerce.app.user.AppUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/delivery")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF')")
    public ResponseEntity<List<Address>> getUserAddress(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(addressService.getUserAddress(user));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AddressResponse> addUserDeliveryInfo(@AuthenticationPrincipal AppUser user,
                                                 @Valid @RequestBody AddressDTO addressDTO) {
        return ResponseEntity.ok(addressService.saveAddress(user, addressDTO));
    }

    @PutMapping("/{addressId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> updateAddress(@AuthenticationPrincipal AppUser user,
                                           @Valid @RequestBody AddressDTO addressDTO, @PathVariable Long addressId) {
        addressService.updateAddress(user, addressDTO, addressId);
        return ResponseEntity.ok("Address successfully updated");
    }

    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> deleteUserAddress(@AuthenticationPrincipal AppUser user, @PathVariable Long addressId) {
        addressService.deleteAddress(user, addressId);
        return ResponseEntity.ok("Address successfully deleted");
    }
}
