package com.example.bankcards.service;

import com.example.bankcards.TestDataFactory;
import com.example.bankcards.dto.user.UserResponseDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.IllegalOperationException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.impl.AdminUserServiceImpl;
import com.example.bankcards.util.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AdminUserServiceImpl service;

    @Test
    void getAllUsers_shouldReturnPagedModel() {
        User user = TestDataFactory.regularUser();
        UserResponseDto dto = TestDataFactory.userDto("testuser");

        when(userRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(userMapper.toDto(user)).thenReturn(dto);

        PagedModel<UserResponseDto> result =
                service.getAllUsers(PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals("testuser", result.getContent().get(0).username());
    }

    @Test
    void getUserByName_shouldReturnUser() {
        User user = TestDataFactory.regularUser();
        UserResponseDto dto = TestDataFactory.userDto("testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dto);

        UserResponseDto result = service.getUserByName("testuser");

        assertEquals("testuser", result.username());
        assertEquals(2L, result.id());
    }

    @Test
    void getUserByName_shouldThrowException() {
        when(userRepository.findByUsername("missing"))
                .thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.getUserByName("missing"));
    }

    @Test
    void deleteUser_shouldThrow_whenAdmin() {
        User admin = TestDataFactory.adminUser();

        when(userRepository.findWithRelationsByUsername("admin"))
                .thenReturn(Optional.of(admin));

        assertThrows(IllegalOperationException.class,
                () -> service.deleteUserByName("admin"));
    }

    @Test
    void deleteUser_shouldThrow_whenHasMoney() {
        User user = TestDataFactory.regularUser();
        Card cardWithMoney = TestDataFactory.activeCard(1L, user, BigDecimal.valueOf(100));
        user.setCards(List.of(cardWithMoney));

        when(userRepository.findWithRelationsByUsername("testuser"))
                .thenReturn(Optional.of(user));

        assertThrows(IllegalOperationException.class,
                () -> service.deleteUserByName("testuser"));
    }

    @Test
    void deleteUser_shouldSucceed_whenNoMoney() {
        User user = TestDataFactory.regularUser();
        Card emptyCard = TestDataFactory.activeCard(1L, user, BigDecimal.ZERO);
        user.setCards(List.of(emptyCard));

        when(userRepository.findWithRelationsByUsername("testuser"))
                .thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> service.deleteUserByName("testuser"));
        verify(userRepository).delete(user);
    }
}