package com.example.bankcards.controller;

import com.example.bankcards.TestDataFactory;
import com.example.bankcards.dto.card.CardCreateRequestDto;
import com.example.bankcards.dto.card.CardResponseDto;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.service.CardAdminService;
import com.example.bankcards.util.CardMaskingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardAdminService cardAdminService;

    @MockitoBean
    private CardMaskingService maskingService;

    private final org.springframework.security.core.userdetails.User adminUser =
            new org.springframework.security.core.userdetails.User(
                    "admin", "password",
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    @Test
    void createCard_shouldReturn201() throws Exception {
        CardCreateRequestDto request = new CardCreateRequestDto("testuser", LocalDate.now().plusYears(3));
        CardResponseDto response = TestDataFactory.cardDto(
                1L, "4000001234567890", 2L, "testuser", CardStatus.ACTIVE, BigDecimal.ZERO);

        when(cardAdminService.createCard(any(CardCreateRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/cards")
                        .with(user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardNumber").value("4000001234567890"))
                .andExpect(jsonPath("$.ownerUsername").value("testuser"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getAllCards_shouldReturn200() throws Exception {
        CardResponseDto dto = TestDataFactory.cardDto(1L, "****1000", 2L, "testuser",
                CardStatus.ACTIVE, BigDecimal.valueOf(1000));
        PagedModel<CardResponseDto> paged = new PagedModel<>(
                new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        when(cardAdminService.getAllCards(eq("testuser"), eq(CardStatus.ACTIVE), any()))
                .thenReturn(paged);

        mockMvc.perform(get("/api/admin/cards")
                        .with(user(adminUser))
                        .param("username", "testuser")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ownerUsername").value("testuser"));
    }

    @Test
    void blockCard_shouldReturn200() throws Exception {
        CardResponseDto dto = TestDataFactory.cardDto(1L, "****1111", 2L, "testuser",
                CardStatus.BLOCKED, BigDecimal.valueOf(500));

        when(cardAdminService.blockCard("1111111111111111")).thenReturn(dto);
        when(maskingService.mask("1111111111111111")).thenReturn("****1111");

        mockMvc.perform(patch("/api/admin/cards/{cardNumber}/block", "1111111111111111")
                        .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void blockCard_shouldReturn404WhenCardNotFound() throws Exception {
        when(cardAdminService.blockCard("0000000000000000"))
                .thenThrow(new CardNotFoundException("Card not found"));
        when(maskingService.mask("0000000000000000")).thenReturn("****0000");

        mockMvc.perform(patch("/api/admin/cards/{cardNumber}/block", "0000000000000000")
                        .with(user(adminUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void activateCard_shouldReturn200() throws Exception {
        CardResponseDto dto = TestDataFactory.cardDto(1L, "****1111", 2L, "testuser",
                CardStatus.ACTIVE, BigDecimal.valueOf(500));

        when(cardAdminService.activateCard("1111111111111111")).thenReturn(dto);
        when(maskingService.mask("1111111111111111")).thenReturn("****1111");

        mockMvc.perform(patch("/api/admin/cards/{cardNumber}/activate", "1111111111111111")
                        .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void deleteCard_shouldReturn204() throws Exception {
        doNothing().when(cardAdminService).deleteCard("1111111111111111");
        when(maskingService.mask("1111111111111111")).thenReturn("****1111");

        mockMvc.perform(delete("/api/admin/cards/{cardNumber}", "1111111111111111")
                        .with(user(adminUser)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCard_shouldReturn404WhenCardNotFound() throws Exception {
        doThrow(new CardNotFoundException("Card not found"))
                .when(cardAdminService).deleteCard("0000000000000000");
        when(maskingService.mask("0000000000000000")).thenReturn("****0000");

        mockMvc.perform(delete("/api/admin/cards/{cardNumber}", "0000000000000000")
                        .with(user(adminUser)))
                .andExpect(status().isNotFound());
    }
}