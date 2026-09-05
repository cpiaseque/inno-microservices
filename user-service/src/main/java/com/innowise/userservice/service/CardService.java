package com.innowise.userservice.service;

import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.model.dto.CardResponseDto;
import com.innowise.userservice.model.entity.Card;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.util.CardFieldsGenerator;

import java.util.List;

/**
 * Service for managing cards in the system.
 * Handles business logic for card operations including automatic number generation,
 * holder name auto formatting, and expiration date calculation.
 *
 * @see Card
 * @see CardFieldsGenerator
 * @see CardResponseDto
 */
public interface CardService {
    /**
     * Retrieves all cards from the system.
     *
     * @return list of all cards, empty list if no cards exist in the database
     */
    List<CardResponseDto> getAllCards();

    /**
     * Retrieves a card by unique identifier.
     *
     * @param cardId the unique identifier of the card to retrieve
     * @return the card data
     * @throws ResourceNotFoundException if the card with given ID does not exist
     */
    CardResponseDto getCardById(Long cardId);

    /**
     * Retrieves multiple cards by their identifiers.
     * Returns only existing cards, non-existent IDs are ignored.
     *
     * @param cardsIds the list of card identifiers to retrieve
     * @return list of found cards, empty list if no cards found by IDs
     */
    List<CardResponseDto> getCardsByIds(List<Long> cardsIds);

    /**
     * Creates a new card for a user with automatically generated data.
     * Card number is uniquely generated, expiration date is set to 3 years from now,
     * and holder name is formatted from user's name and surname.
     *
     * @param userId the unique identifier of the user who owns the card
     * @return the created card data with generated fields
     * @throws ResourceNotFoundException if the user with given ID does not exist
     * @see CardFieldsGenerator#generateCardNumber()
     * @see CardFieldsGenerator#formatCardHolderName(User)
     */
    CardResponseDto createCard(Long userId);

    /**
     * Deletes a card by unique identifier.
     *
     * @param cardId the unique identifier of the card to delete
     * @throws ResourceNotFoundException if the card with given ID does not exist
     */
    void deleteCard(Long cardId);

    /**
     * Verifies if a user is the owner of a specific card.
     *
     * @param cardId the unique identifier of the card to check
     * @param userId the unique identifier of the user to verify ownership
     * @return true if the user owns the card, false otherwise
     */
    boolean isCardOwner(Long cardId, Long userId);
}