package com.msa4lmsv2academic.domain.infochange.service;

import java.util.ArrayList;
import java.util.List;

public record ProfileChangeValues(
        String name,
        String phoneNumber,
        String email,
        String address,
        boolean profileImageChanged
) {
    public List<String> changedFields() {
        List<String> fields = new ArrayList<>();
        if (name != null) fields.add("NAME");
        if (phoneNumber != null) fields.add("PHONE_NUMBER");
        if (email != null) fields.add("EMAIL");
        if (address != null) fields.add("ADDRESS");
        if (profileImageChanged) fields.add("PROFILE_IMAGE");
        return List.copyOf(fields);
    }
}
