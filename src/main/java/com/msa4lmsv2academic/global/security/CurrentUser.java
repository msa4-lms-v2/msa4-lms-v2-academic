package com.msa4lmsv2academic.global.security;

public record CurrentUser(Long id, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
