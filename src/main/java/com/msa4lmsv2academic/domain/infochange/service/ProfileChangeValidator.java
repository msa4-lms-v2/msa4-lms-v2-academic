package com.msa4lmsv2academic.domain.infochange.service;

import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.repository.UserRepository;
import com.msa4lmsv2academic.global.error.DuplicateProfileEmailException;
import com.msa4lmsv2academic.global.error.InvalidInfoChangeRequestException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class ProfileChangeValidator {

    private final UserRepository userRepository;

    public ProfileChangeValues resolve(
            User user,
            String rawName,
            String rawPhoneNumber,
            String rawEmail,
            String rawAddress,
            MultipartFile profileImage
    ) {
        String name = changedValue(rawName, user.getName(), "newName", false);
        String phoneNumber = changedValue(rawPhoneNumber, user.getPhoneNumber(), "newPhoneNumber", false);
        String email = changedValue(rawEmail, user.getEmail(), "newEmail", true);
        String address = changedValue(rawAddress, user.getAddress(), "newAddress", false);
        boolean profileImageChanged = profileImage != null && !profileImage.isEmpty();

        if (email != null) {
            validateEmailAvailable(user, email);
        }
        ProfileChangeValues values = new ProfileChangeValues(
                name, phoneNumber, email, address, profileImageChanged
        );
        if (values.changedFields().isEmpty()) {
            throw new InvalidInfoChangeRequestException("현재 프로필과 다른 변경 항목을 하나 이상 입력해야 합니다.");
        }
        return values;
    }

    public void validateEmailAvailable(User user, String email) {
        if (email != null && userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
            throw new DuplicateProfileEmailException();
        }
    }

    private String changedValue(String rawValue, String currentValue, String fieldName, boolean ignoreCase) {
        if (rawValue == null) {
            return null;
        }
        if (!StringUtils.hasText(rawValue)) {
            throw new InvalidInfoChangeRequestException(fieldName + "은 공백일 수 없습니다.");
        }
        String normalized = rawValue.trim();
        boolean same = ignoreCase
                ? currentValue != null && normalized.equalsIgnoreCase(currentValue)
                : Objects.equals(normalized, currentValue);
        return same ? null : normalized;
    }
}
