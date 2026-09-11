package com.msa4lmsv2academic.domain.infochange.entity;

import com.msa4lmsv2academic.domain.user.entity.User;

public record ProfileSnapshot(
        String name,
        String phoneNumber,
        String email,
        String address,
        String profileImageKey
) {
    public static ProfileSnapshot from(User user) {
        return new ProfileSnapshot(
                user.getName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getAddress(),
                user.getProfileImageKey()
        );
    }
}
