package com.example.bankcards.controller;

import com.example.bankcards.TestDataFactory;
import com.example.bankcards.dto.card.CardResponseDto;
import com.example.bankcards.dto.card.CardTransferRequestDto;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.CardAccessDeniedException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.CardUserService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardUserService cardUserService;

    @MockitoBean
    private CardMaskingService maskingService;

    private final CustomUserDetails testUser = new CustomUserDetails(
            2L, "testuser", "password",
            List.of(new SimpleGrantedAuthority("ROLE_USER")));

    @Test
    void getMyCards_shouldReturn200() throws Exception {
        CardResponseDto dto = TestDataFactory.cardDto(
                1L, "****1000", 2L, "testuser", CardStatus.ACTIVE, BigDecimal.valueOf(1000));
        PagedModel<CardResponseDto> paged = new PagedModel<>(
                new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        when(cardUserService.getMyCards(eq(2L), eq(CardStatus.ACTIVE), any()))
                .thenReturn(paged);

        mockMvc.perform(get("/api/users/cards")
                        .with(user(testUser))
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cardNumber").value("****1000"));
    }

    @Test
    void getCardBalance_shouldReturn200() throws Exception {
        when(cardUserService.getCardBalance(2L, "1234567890123456"))
                .thenReturn(BigDecimal.valueOf(500));
        when(maskingService.mask("1234567890123456")).thenReturn("****3456");

        mockMvc.perform(get("/api/users/cards/{cardNumber}/balance", "1234567890123456")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(content().string("500"));
    }

    @Test
    void getCardBalance_shouldReturn404WhenCardNotFound() throws Exception {
        when(cardUserService.getCardBalance(2L, "0000000000000000"))
                .thenThrow(new CardNotFoundException("Card not found"));
        when(maskingService.mask("0000000000000000")).thenReturn("****0000");

        mockMvc.perform(get("/api/users/cards/{cardNumber}/balance", "0000000000000000")
                        .with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCardBalance_shouldReturn403WhenNotOwner() throws Exception {
        when(cardUserService.getCardBalance(2L, "1234567890123456"))
                .thenThrow(new CardAccessDeniedException("Card does not belong to user"));
        when(maskingService.mask("1234567890123456")).thenReturn("****3456");

        mockMvc.perform(get("/api/users/cards/{cardNumber}/balance", "1234567890123456")
                        .with(user(testUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void transferBetweenMyAccount_shouldReturn200() throws Exception {
        CardTransferRequestDto request = new CardTransferRequestDto(
                "1111111111111111", "2222222222222222", BigDecimal.valueOf(200));

        doNothing().when(cardUserService).transferBetweenAccount(eq(2L), any(CardTransferRequestDto.class));
        when(maskingService.mask("1111111111111111")).thenReturn("****1111");
        when(maskingService.mask("2222222222222222")).thenReturn("****2222");

        mockMvc.perform(post("/api/users/cards/transfer")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void transferBetweenMyAccount_shouldReturn400WhenInsufficientFunds() throws Exception {
        CardTransferRequestDto request = new CardTransferRequestDto(
                "1111111111111111", "2222222222222222", BigDecimal.valueOf(999999));

        doThrow(new InsufficientFundsException("Insufficient funds"))
                .when(cardUserService).transferBetweenAccount(eq(2L), any(CardTransferRequestDto.class));
        when(maskingService.mask("1111111111111111")).thenReturn("****1111");
        when(maskingService.mask("2222222222222222")).thenReturn("****2222");

        mockMvc.perform(post("/api/users/cards/transfer")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void requestBlock_shouldReturn200() throws Exception {
        CardResponseDto dto = TestDataFactory.cardDto(
                1L, "****1111", 2L, "testuser", CardStatus.BLOCKED, BigDecimal.valueOf(500));

        when(cardUserService.requestBlock(2L, "1111111111111111")).thenReturn(dto);
        when(maskingService.mask("1111111111111111")).thenReturn("****1111");

        mockMvc.perform(patch("/api/users/cards/{cardNumber}/block", "1111111111111111")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void requestBlock_shouldReturn404WhenCardNotFound() throws Exception {
        when(cardUserService.requestBlock(2L, "0000000000000000"))
                .thenThrow(new CardNotFoundException("Card not found"));
        when(maskingService.mask("0000000000000000")).thenReturn("****0000");

        mockMvc.perform(patch("/api/users/cards/{cardNumber}/block", "0000000000000000")
                        .with(user(testUser)))
                .andExpect(status().isNotFound());
    }
}