package com.example.stockreplenishment.service;

import com.example.stockreplenishment.model.*;
import org.junit.jupiter.api.Test;

import static com.example.stockreplenishment.service.ReplenishmentService.recommendedQuantity;
import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit tests of the business rules: no Spring context and no database are needed. */
class ReplenishmentRulesTest {
    @Test
    void stockStatusDependsOnMinimumLevel() {
        assertThat(StockStatus.of(0, 10)).isEqualTo(StockStatus.OUT_OF_STOCK);
        assertThat(StockStatus.of(10, 10)).isEqualTo(StockStatus.LOW_STOCK);
        assertThat(StockStatus.of(11, 10)).isEqualTo(StockStatus.IN_STOCK);
    }

    @Test
    void recommendsTopUpToTargetOnlyAtMinimumLevel() {
        assertThat(recommendedQuantity(5, 0, 10, 50)).isEqualTo(45);
        assertThat(recommendedQuantity(21, 0, 20, 50)).isZero();
        assertThat(recommendedQuantity(2, 3, 10, 50)).isEqualTo(45); // 3 units are already on order
        assertThat(recommendedQuantity(4, 11, 5, 15)).isZero();
    }

    @Test
    void salesAlwaysReduceStockAndAdjustmentsKeepTheirSign() {
        assertThat(MovementType.SALE.toChange(3)).isEqualTo(-3);
        assertThat(MovementType.RETURN.toChange(-3)).isEqualTo(3);
        assertThat(MovementType.ADJUSTMENT.toChange(-2)).isEqualTo(-2);
    }

    @Test
    void purchaseOrderFollowsStatusWorkflow() {
        assertThat(OrderStatus.PENDING.getNextStatuses()).containsExactly(OrderStatus.ORDERED, OrderStatus.CANCELLED);
        assertThat(OrderStatus.ORDERED.getNextStatuses()).containsExactly(OrderStatus.RECEIVED, OrderStatus.CANCELLED);
        assertThat(OrderStatus.RECEIVED.getNextStatuses()).isEmpty();
    }
}
