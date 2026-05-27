package com.mobilebackend.ungdunglapkehoachdulich.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for admin role updates.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRoleReq {
    private String role;
}