package com.kuter.identity.dto;

import com.kuter.identity.ShopPermission;
import com.kuter.identity.TenantRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Payload for adding a staff member or cashier to a tenant shop.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Role is required")
    private TenantRole role;

    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    private String phoneNumber;

    private Set<ShopPermission> permissions;
}
