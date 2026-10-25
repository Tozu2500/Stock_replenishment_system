package com.example.stockreplenishment;

import com.example.stockreplenishment.model.*;
import com.example.stockreplenishment.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Authentication (login) and authorization (USER may only read, ADMIN may change data) in the web UI and API.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private AppUserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;

    @Test
    void loginChecksThePassword() throws Exception {
        userRepository.save(new AppUser("anna", passwordEncoder.encode("secret1"), Role.USER));
        mockMvc.perform(get("/products")).andExpect(redirectedUrl("/login"));
        mockMvc.perform(formLogin("/login").user("anna").password("secret1"))
                .andExpect(authenticated().withUsername("anna").withRoles("USER"));
        mockMvc.perform(formLogin("/login").user("anna").password("wrong"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCanViewPagesButNotChangeData() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/products")).andExpect(status().isOk());
        mockMvc.perform(get("/products/new")).andExpect(status().isForbidden());
        mockMvc.perform(post("/products").with(csrf()).param("name", "Hacked")).andExpect(status().isForbidden());
        mockMvc.perform(post("/categories/1/delete").with(csrf())).andExpect(status().isForbidden());
    }

    /** A rejected action goes back to the previous page (Referer header), but never to another site. */
    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectedActionRedirectsOnlyInsideTheApplication() throws Exception {
        Category category = categoryRepository.save(new Category("Electronics"));
        category.getProducts().add(productRepository.save(new Product("Keyboard", "KB-1", BigDecimal.TEN, 1, 5, category)));
        String delete = "/categories/" + category.getId() + "/delete";
        mockMvc.perform(get("/products/new")).andExpect(status().isOk()); // admin may open forms

        mockMvc.perform(post(delete).with(csrf()).header("Referer", "http://localhost/categories"))
                .andExpect(redirectedUrl("/categories")).andExpect(flash().attributeExists("errorMessage"));
        mockMvc.perform(post(delete).with(csrf()).header("Referer", "http://localhost//evil.example/login"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void apiUsesHttpBasicAndOnlyAdminCanWrite() throws Exception {
        userRepository.save(new AppUser("anna", passwordEncoder.encode("secret1"), Role.USER));
        userRepository.save(new AppUser("boss", passwordEncoder.encode("secret2"), Role.ADMIN));
        mockMvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/products").with(httpBasic("anna", "secret1"))).andExpect(status().isOk());
        mockMvc.perform(delete("/api/products/1").with(httpBasic("anna", "secret1"))).andExpect(status().isForbidden());
        // The admin passes security and reaches validation: 400 instead of 401 or 403
        mockMvc.perform(post("/api/products").with(httpBasic("boss", "secret2"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
    }
}
