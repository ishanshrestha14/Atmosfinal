package com.ecommerce.app.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JWTServiceTest {

    private final JWTService jwtService = new JWTService();

    private final UserDetails alice = User.withUsername("alice").password("x").roles("CUSTOMER").build();
    private final UserDetails bob = User.withUsername("bob").password("x").roles("CUSTOMER").build();

    @BeforeEach
    void configure() {
        ReflectionTestUtils.setField(jwtService, "secretKey", "ZGV2LW9ubHktc2VjcmV0LWNoYW5nZS1tZS1pbi1wcm9kdWN0aW9uLTEyMzQ1Njc4OTA=");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 120_000L);
    }

    @Test
    void tokenIsValidForTheUserItWasIssuedTo() {
        String token = jwtService.generateToken(alice);

        assertThat(jwtService.isTokenValid(token, alice)).isTrue();
    }

    @Test
    void tokenIsNotValidForADifferentUser() {
        String token = jwtService.generateToken(alice);

        assertThat(jwtService.isTokenValid(token, bob)).isFalse();
    }
}
