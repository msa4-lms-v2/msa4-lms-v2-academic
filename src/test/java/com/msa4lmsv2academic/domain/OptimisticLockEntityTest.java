package com.msa4lmsv2academic.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;

class OptimisticLockEntityTest {

    @Test
    void professorHasVersionField() throws NoSuchFieldException {
        assertThat(Professor.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }
}
