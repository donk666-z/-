package com.campus.delivery.dto;

import com.campus.delivery.entity.Address;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddressSaveResult {
    private Address address;
    private boolean geocodeAttempted;
    private boolean geocodeResolved;
}
