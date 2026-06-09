package com.example.bankcards.controller;

import com.example.bankcards.TestDataFactory;
import com.example.bankcards.dto.user.UserResponseDto;
import com.example.bankcards.exception.IllegalOperationException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    private final org.springframework.security.core.userdetails.User adminUser =
            new org.springframework.security.core.userdetails.User(
                    "admin", "password",
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    @Test
    void getUsers_shouldReturnPagedModel() throws Exception {
        UserResponseDto dto = TestDataFactory.userDto("testuser");
        PagedModel<UserResponseDto> paged = new PagedModel<>(
                new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        when(adminUserService.getAllUsers(any())).thenReturn(paged);

        mockMvc.perform(get("/api/admin/users")
                        .with(user(adminUser))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("testuser"));
    }

    @Test
    void getUser_shouldReturnUser() throws Exception {
        var dto = TestDataFactory.userDto("Daniel");

        when(adminUserService.getUserByName("Daniel")).thenReturn(dto);

        mockMvc.perform(get("/api/admin/users/Daniel")
                        .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Daniel"))
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void getUser_shouldReturn404() throws Exception {
        when(adminUserService.getUserByName("missing"))
                .thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(get("/api/admin/users/missing")
                        .with(user(adminUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_shouldReturn204() throws Exception {
        doNothing().when(adminUserService).deleteUserByName("testuser");

        mockMvc.perform(delete("/api/admin/users/testuser")
                        .with(user(adminUser)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_shouldReturn422_whenIllegalOperation() throws Exception {
        doThrow(new IllegalOperationException("Cannot delete admin"))
                .when(adminUserService).deleteUserByName("admin");

        mockMvc.perform(delete("/api/admin/users/admin")
                        .with(user(adminUser)))
                .andExpect(status().isUnprocessableEntity());
    }
}