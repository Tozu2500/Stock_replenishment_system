package com.example.stockreplenishment.rest;

import com.example.stockreplenishment.model.Category;
import com.example.stockreplenishment.repository.CategoryRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** REST API tests: CRUD, stock movements, validation (400), missing resources (404) and conflicts (409). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "ADMIN")
class ProductApiTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private CategoryRepository categoryRepository;

    private Long categoryId;

    @BeforeEach
    void setUp() {
        categoryId = categoryRepository.save(new Category("Electronics")).getId();
    }

    @Test
    void createReadUpdateAndDeleteProduct() throws Exception {
        String body = send(post("/api/products"), json("Keyboard", "kb-1", 10, 50)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("KB-1"))
                .andExpect(jsonPath("$.categoryName").value("Electronics"))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.id");

        send(put("/api/products/{id}", id), json("Keyboard Pro", "KB-1", 10, 50))
                .andExpect(jsonPath("$.name").value("Keyboard Pro"));
        send(post("/api/products/{id}/movements", id), "{\"type\": \"RECEIPT\", \"quantity\": 4}")
                .andExpect(jsonPath("$.quantity").value(4)).andExpect(jsonPath("$.status").value("LOW_STOCK"));
        mockMvc.perform(delete("/api/products/{id}", id)).andExpect(status().isConflict()); // still in stock
        send(post("/api/products/{id}/movements", id), "{\"type\": \"SALE\", \"quantity\": 4}");
        mockMvc.perform(delete("/api/products/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/products/{id}", id)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product with id " + id + " was not found"));
    }

    @Test
    void invalidProductIsRejectedWithFieldErrors() throws Exception {
        send(post("/api/products"), "{\"name\": \"\", \"sku\": \"KB 1\", \"unitPrice\": -1, \"minimumStock\": 10, \"targetStock\": 5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.sku").value("may only contain letters, digits and dashes"))
                .andExpect(jsonPath("$.fieldErrors.unitPrice").exists())
                .andExpect(jsonPath("$.fieldErrors.stockLevelsValid").exists())
                .andExpect(jsonPath("$.fieldErrors.categoryId").value("choose a category"));
    }

    @Test
    void duplicateSkuIsConflict() throws Exception {
        send(post("/api/products"), json("Keyboard", "KB-1", 10, 50)).andExpect(status().isCreated());
        send(post("/api/products"), json("Copy", "kb-1", 1, 2)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("SKU KB-1 is already used by Keyboard"));
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String json) throws Exception {
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String json(String name, String sku, int minimum, int target) {
        return """
                {"name": "%s", "sku": "%s", "unitPrice": 49.90, "minimumStock": %d, "targetStock": %d, "categoryId": %d}
                """.formatted(name, sku, minimum, target, categoryId);
    }
}
