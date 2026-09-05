package com.innowise.userservice.service;

import com.innowise.userservice.model.dto.mapper.CardMapperImpl;
import com.innowise.userservice.model.dto.mapper.UserMapperImpl;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.repository.CardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.cache.CacheEvictor;
import com.innowise.userservice.util.TestConstant;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {UserMapperImpl.class, CardMapperImpl.class})
public abstract class BaseServiceTest {
    @MockitoBean
    protected UserRepository userRepository;

    @MockitoBean
    protected CardRepository cardRepository;

    @MockitoBean
    protected CacheEvictor cacheEvictor;

    protected static User buildTestUser() {
        return User.builder()
                .id(TestConstant.ID)
                .name(TestConstant.USER_NAME)
                .surname(TestConstant.USER_NAME)
                .birthDate(TestConstant.LOCAL_DATE_YESTERDAY)
                .email(TestConstant.USER_EMAIL)
                .build();
    }
}