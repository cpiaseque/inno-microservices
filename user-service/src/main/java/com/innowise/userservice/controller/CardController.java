package com.innowise.userservice.controller;

import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.model.dto.CardResponseDto;
import com.innowise.userservice.service.CardService;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing cards in the system.
 * Provides endpoints for CRUD operations on cards with user's role-based authorization.
 * Card numbers are automatically generated and guaranteed to be unique.
 *
 * @see CardService
 * @see CardResponseDto
 */
@Validated
@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {
    private final CardService cardService;

    /**
     * Retrieves a card by unique identifier.
     * Users can only access their own cards unless they have ADMIN role.
     *
     * @param id the unique identifier of the card to retrieve
     * @return the card data
     * @throws ResourceNotFoundException if the card with given ID does not exist
     * @throws AccessDeniedException if user does not have permission to access this card
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @cardService.isCardOwner(#id, authentication.principal)")
    public ResponseEntity<CardResponseDto> getCardById(@PathVariable("id") Long id) {
        CardResponseDto cardResponseDto = cardService.getCardById(id);

        return ResponseEntity.ok(cardResponseDto);
    }

    /**
     * Retrieves specific cards by their IDs. Requires ADMIN role.
     *
     * @param ids list of card IDs to filter by
     * @return list of cards, empty list if no cards found by given IDs
     * @throws AccessDeniedException if user does not have ADMIN role
     */
    @GetMapping(params = "ids")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CardResponseDto>> getCardsByIds(@RequestParam @NotEmpty List<Long> ids) {
        List<CardResponseDto> retrievedCards = cardService.getCardsByIds(ids);

        return ResponseEntity.ok(retrievedCards);
    }

    /**
     * Retrieves all cards in the system. Requires ADMIN role.
     *
     * @return list of all cards, empty list if no cards exist
     * @throws AccessDeniedException if user does not have ADMIN role
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CardResponseDto>> getCards() {
        List<CardResponseDto> retrievedCards = cardService.getAllCards();

        return ResponseEntity.ok(retrievedCards);
    }

    /**
     * Creates a new card for a given user.
     * Automatically generates unique card number and sets expiration date.
     * Holder is generated from user's name and surname.
     *
     * @param userId the authenticated user's ID extracted from JWT token
     * @return the created card data
     * @throws ResourceNotFoundException if the user with given ID does not exist
     * @throws AccessDeniedException if user does not have USER role
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CardResponseDto> createCard(@AuthenticationPrincipal Long userId) {
        CardResponseDto cardResponseDto = cardService.createCard(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(cardResponseDto);
    }

    /**
     * Deletes a card by unique identifier.
     * Users can only delete their own cards unless they have ADMIN role.
     *
     * @param id the unique identifier of the card to delete
     * @return empty response
     * @throws ResourceNotFoundException if the card with given ID does not exist
     * @throws AccessDeniedException if user does not have permission to access this card
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @cardService.isCardOwner(#id, authentication.principal)")
    public ResponseEntity<Void> deleteCard(@PathVariable("id") Long id) {
        cardService.deleteCard(id);

        return ResponseEntity.noContent().build();
    }
}