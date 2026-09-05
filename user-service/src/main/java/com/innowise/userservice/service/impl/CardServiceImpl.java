package com.innowise.userservice.service.impl;

import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.model.dto.CardResponseDto;
import com.innowise.userservice.model.dto.mapper.CardMapper;
import com.innowise.userservice.model.entity.Card;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.repository.CardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.CardService;
import com.innowise.userservice.service.cache.CacheEvictor;
import com.innowise.userservice.util.CardFieldsGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service("cardService")
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;
    private final CacheEvictor cacheEvictor;

    @Override
    @Transactional(readOnly = true)
    public List<CardResponseDto> getAllCards() {
        List<Card> retrievedCards = cardRepository.findAll();

        return cardMapper.toResponseDtoList(retrievedCards);
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponseDto getCardById(Long cardId) {
        Card retrievedCard = cardRepository.findCardById(cardId)
                .orElseThrow(() -> ResourceNotFoundException.cardNotFound(cardId));

        return cardMapper.toResponseDto(retrievedCard);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardResponseDto> getCardsByIds(List<Long> cardsIds) {
        List<Card> retrievedCards = cardRepository.findCardsByIdIn(cardsIds);

        return cardMapper.toResponseDtoList(retrievedCards);
    }

    @Override
    @Transactional
    public CardResponseDto createCard(Long userId) {
        User userToAddCard = userRepository.findUserById(userId)
                .orElseThrow(() -> ResourceNotFoundException.userNotFound(userId));

        String newCardNumber;
        do {
            newCardNumber = CardFieldsGenerator.generateCardNumber();
        } while (cardRepository.existsByNumber(newCardNumber));

        Card newCard = Card.builder()
                .user(userToAddCard)
                .number(newCardNumber)
                .holder(CardFieldsGenerator.formatCardHolderName(userToAddCard))
                .expirationDate(LocalDate.now().plusYears(CardFieldsGenerator.CARD_VALIDITY_YEARS))
                .build();

        Card createdCard = cardRepository.save(newCard);

        cacheEvictor.evictUser(userToAddCard.getId(), userToAddCard.getEmail());

        return cardMapper.toResponseDto(createdCard);
    }

    @Override
    @Transactional
    public void deleteCard(Long cardId) {
        Card cardToDelete = cardRepository.findCardById(cardId)
                .orElseThrow(() -> ResourceNotFoundException.cardNotFound(cardId));

        cacheEvictor.evictUser(cardToDelete.getUser().getId(), cardToDelete.getUser().getEmail());

        cardRepository.deleteCardById(cardId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCardOwner(Long cardId, Long userId) {
        Optional<Card> cardInDb = cardRepository.findCardById(cardId);
        Optional<User> userInDb = userRepository.findUserById(userId);

        return cardInDb.isPresent() && userInDb.isPresent()
                && cardInDb.get().getUser().getId().equals(userInDb.get().getId());
    }
}