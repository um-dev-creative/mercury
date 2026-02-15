package com.prx.mercury.jpa.entity;

import com.prx.mercury.jpa.sql.entity.UserEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {

    @Test
    void getId() {
        UserEntity userEntity = new UserEntity();
        assertNull(userEntity.getId());
    }

    @Test
    void setId() {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(null);
        assertNull(userEntity.getId());
    }

}
