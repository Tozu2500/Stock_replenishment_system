package com.example.stockreplenishment.dto;

import com.example.stockreplenishment.model.Supplier;
import jakarta.validation.constraints.*;

public record SupplierRequest(@NotBlank @Size(max = 100) String name,
                              @NotBlank @Email @Size(max = 100) String email,
                              @NotNull @Min(0) @Max(365) Integer leadTimeDays,
                              Boolean active) {

    public SupplierRequest {
        active = active == null || active; // a supplier is active unless the request says otherwise
    }

    public static SupplierRequest from(Supplier supplier) {
        return new SupplierRequest(supplier.getName(), supplier.getEmail(), supplier.getLeadTimeDays(),
                supplier.isActive());
    }
}
