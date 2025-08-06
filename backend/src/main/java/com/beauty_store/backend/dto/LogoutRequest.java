package com.beauty_store.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LogoutRequest {
    @NotNull(message = "ID người dùng không được để trống")
    private Long id;
}