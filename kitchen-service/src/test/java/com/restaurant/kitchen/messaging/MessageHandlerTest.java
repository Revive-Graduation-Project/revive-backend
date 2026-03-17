package com.restaurant.kitchen.messaging;

import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.events.ProfileCreationFailedEvent;
import com.restaurant.kitchen.events.UserCreatedEvent;
import com.restaurant.kitchen.mapper.ChefProfileMapper;
import com.restaurant.kitchen.repository.ChefProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageHandlerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ChefProfileRepository chefProfileRepository;

    @Mock
    private ChefProfileMapper mapper;

    @InjectMocks
    private MessageHandler messageHandler; // ← real class

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(messageHandler, "exchange", "test.exchange");
    }

    @Test
    void shouldIgnoreNonChefUser() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setRole("CUSTOMER");

        messageHandler.createChefProfile(event, "corr-001", "saga-001");

        verifyNoInteractions(chefProfileRepository);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void shouldRepublishCreatedEventWhenChefAlreadyExists() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setRole("CHEF");
        event.setAuthUserId(1L);

        ChefProfile existingChef = new ChefProfile();
        when(chefProfileRepository.findByAuthUserId(1L))
                .thenReturn(Optional.of(existingChef));

        messageHandler.createChefProfile(event, "corr-001", "saga-001");

        verify(chefProfileRepository, never()).save(any());
        verify(rabbitTemplate).convertAndSend(
                eq("test.exchange"),
                eq("chef-profile.created"),
                any(),
                any(MessagePostProcessor.class));
    }

    @Test
    void shouldSaveAndPublishCreatedEventWhenChefDoesNotExist() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setRole("CHEF");
        event.setAuthUserId(1L);

        ChefProfile mappedChef = new ChefProfile();
        ChefProfile savedChef = new ChefProfile();

        when(chefProfileRepository.findByAuthUserId(1L)).thenReturn(Optional.empty());
        when(mapper.toEntity(event)).thenReturn(mappedChef);
        when(chefProfileRepository.save(mappedChef)).thenReturn(savedChef);

        messageHandler.createChefProfile(event, "corr-001", "saga-001");

        verify(chefProfileRepository).save(mappedChef);
        verify(rabbitTemplate).convertAndSend(
                eq("test.exchange"),
                eq("chef-profile.created"),
                any(),
                any(MessagePostProcessor.class));
    }

    @Test
    void shouldPublishFailedEventWhenSaveThrowsException() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setRole("CHEF");
        event.setAuthUserId(1L);

        when(chefProfileRepository.findByAuthUserId(1L)).thenReturn(Optional.empty());
        when(mapper.toEntity(event)).thenReturn(new ChefProfile());
        when(chefProfileRepository.save(any())).thenThrow(new DataIntegrityViolationException("DB error"));

        ProfileCreationFailedEvent failedEvent = new ProfileCreationFailedEvent();
        when(mapper.toProfileCreationFailedEvent(event)).thenReturn(failedEvent);

        messageHandler.createChefProfile(event, "corr-001", "saga-001");

        verify(rabbitTemplate).convertAndSend(
                eq("test.exchange"),
                eq("chef-profile.failed"),
                any(),
                any(MessagePostProcessor.class));
    }
}