package com.example.stockreplenishment.service;

import com.example.stockreplenishment.dto.ProductRequest;
import com.example.stockreplenishment.exception.*;
import com.example.stockreplenishment.model.*;
import com.example.stockreplenishment.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests with Mockito: the repositories are replaced by mocks, so only ProductService itself is tested. */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock private ProductRepository productRepository;
    @Mock private CategoryService categoryService;
    @Mock private SupplierRepository supplierRepository;
    @Mock private PurchaseOrderRepository orderRepository;
    @InjectMocks private ProductService productService;

    private final Category category = new Category("Electronics");
    private final ProductRequest request = new ProductRequest("Keyboard", "kb-1", BigDecimal.TEN, 10, 50, true, 1L, Set.of());

    @Test
    void createsProductWithUpperCaseSku() {
        when(categoryService.findById(1L)).thenReturn(category);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product product = productService.save(null, request);

        assertThat(product.getSku()).isEqualTo("KB-1");
        assertThat(product.getCategory()).isSameAs(category);
    }

    @Test
    void movingProductUpdatesBothCategories() {
        Category office = new Category("Office Supplies");
        Product product = new Product("Keyboard", "KB-1", BigDecimal.TEN, 1, 5, category);
        assertThat(category.getProducts()).containsExactly(product);

        product.setCategory(office);

        assertThat(category.getProducts()).isEmpty();
        assertThat(office.getProducts()).containsExactly(product);
    }

    @Test
    void rejectsDuplicateSku() {
        Product existing = new Product("Old keyboard", "KB-1", BigDecimal.TEN, 1, 5, category);
        ReflectionTestUtils.setField(existing, "id", 7L);
        when(productRepository.findBySku("KB-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> productService.save(null, request)).hasMessage("SKU KB-1 is already used by Old keyboard");
        verify(productRepository, never()).save(any());
    }

    @Test
    void refusesToDeleteProductThatIsInStock() {
        Product product = new Product("Keyboard", "KB-1", BigDecimal.TEN, 1, 5, category);
        product.setQuantity(3);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.delete(1L)).isInstanceOf(InvalidOperationException.class);
        verify(productRepository, never()).delete(any());
    }
}
