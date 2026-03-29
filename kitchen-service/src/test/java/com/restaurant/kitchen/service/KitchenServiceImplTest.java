package com.restaurant.kitchen.service;

import com.restaurant.kitchen.dto.KitchenTicketDTO;
import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.entity.KitchenTicket;
import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;
import com.restaurant.kitchen.enums.TicketStatus;
import com.restaurant.kitchen.events.ProfileCreatedEvent;
import com.restaurant.kitchen.events.TicketReadyEvent;
import com.restaurant.kitchen.events.UserCreatedEvent;
import com.restaurant.kitchen.exception.ChefNotFoundException;
import com.restaurant.kitchen.exception.TicketNotFoundException;
import com.restaurant.kitchen.mapper.ChefProfileMapper;
import com.restaurant.kitchen.mapper.KitchenTicketMapper;
import com.restaurant.kitchen.messaging.MessagePublisher;
import com.restaurant.kitchen.repository.ChefProfileRepository;
import com.restaurant.kitchen.repository.KitchenTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KitchenServiceImplTest {

    @Mock
    private MessagePublisher publisher;

    @Mock
    private ChefProfileRepository chefProfileRepository;

    @Mock
    private KitchenTicketRepository kitchenTicketRepository;

    @Mock
    private KitchenTicketMapper ticketMapper;

    @Mock
    private ChefProfileMapper chefMapper;

    @InjectMocks
    private KitchenServiceImpl kitchenService;

    // Dummy Data Objects
    private UserCreatedEvent chefUserEvent;
    private UserCreatedEvent nonChefUserEvent;
    private ChefProfile chefProfile;
    private ProfileCreatedEvent profileCreatedEvent;
    private KitchenTicket kitchenTicket;
    private TicketReadyEvent ticketReadyEvent;
    private KitchenTicketDTO kitchenTicketDTO;

    // Realistic Constant Values
    private static final String SAGA_ID = "saga-483d-9f89-c44a";
    private static final String CORRELATION_ID = "corr-1122-3344-5566";
    private static final Long CHEF_ID = 205L;
    private static final Long TICKET_ID = 9081L;
    private static final Long AUTH_USER_ID = 10042L;

    @BeforeEach
    void setUp() {
        // Arrange reusable, specific dummy data
        chefUserEvent = mock(UserCreatedEvent.class);
        lenient().when(chefUserEvent.getAuthUserId()).thenReturn(AUTH_USER_ID);
        lenient().when(chefUserEvent.getRole()).thenReturn("CHEF");

        nonChefUserEvent = mock(UserCreatedEvent.class);
        lenient().when(nonChefUserEvent.getAuthUserId()).thenReturn(8851L);
        lenient().when(nonChefUserEvent.getRole()).thenReturn("WAITER");

        chefProfile = ChefProfile.builder()
                .id(CHEF_ID)
                .authUserId(AUTH_USER_ID)
                .displayName("Gordon Ramsay")
                .station(Station.GRILL)
                .status(ChefStatus.ACTIVE)
                .build();

        profileCreatedEvent = mock(ProfileCreatedEvent.class);

        kitchenTicket = KitchenTicket.builder()
                .id(TICKET_ID)
                .status(TicketStatus.PENDING)
                .build();

        ticketReadyEvent = mock(TicketReadyEvent.class);

        kitchenTicketDTO = mock(KitchenTicketDTO.class);
    }

    // ------------------------------------------------------------------------
    // Tests for createChefProfile
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Should gracefully ignore user creation when user role is not CHEF")
    void createChefProfile_WhenUserIsNotChef_ShouldReturnWithoutSavingAndPublishing() {
        // Arrange
        // (nonChefUserEvent prepared in setUp)

        // Act
        kitchenService.createChefProfile(nonChefUserEvent, CORRELATION_ID, SAGA_ID);

        // Assert
        verify(chefProfileRepository, never()).findByAuthUserId(any());
        verify(chefProfileRepository, never()).save(any());
        verify(publisher, never()).publishChefCreated(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should republish ProfileCreatedEvent without saving database when chef already exists")
    void createChefProfile_WhenChefAlreadyExists_ShouldRepublishEvent() {
        // Arrange
        when(chefProfileRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.of(chefProfile));
        when(chefMapper.toProfileCreatedEvent(chefProfile)).thenReturn(profileCreatedEvent);

        // Act
        kitchenService.createChefProfile(chefUserEvent, CORRELATION_ID, SAGA_ID);

        // Assert
        verify(chefProfileRepository, times(1)).findByAuthUserId(AUTH_USER_ID);
        verify(chefProfileRepository, never()).save(any());
        verify(chefMapper, times(1)).toProfileCreatedEvent(chefProfile);
        verify(publisher, times(1)).publishChefCreated(profileCreatedEvent, SAGA_ID, CORRELATION_ID);
    }

    @Test
    @DisplayName("Should successfully save chef to database and publish event when chef is entirely new")
    void createChefProfile_WhenChefIsNew_ShouldSaveAndPublishEvent() {
        // Arrange
        when(chefProfileRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.empty());
        when(chefMapper.toEntity(chefUserEvent)).thenReturn(chefProfile);
        when(chefProfileRepository.save(chefProfile)).thenReturn(chefProfile);
        when(chefMapper.toProfileCreatedEvent(chefProfile)).thenReturn(profileCreatedEvent);

        // Act
        kitchenService.createChefProfile(chefUserEvent, CORRELATION_ID, SAGA_ID);

        // Assert
        verify(chefProfileRepository, times(1)).findByAuthUserId(AUTH_USER_ID);
        verify(chefMapper, times(1)).toEntity(chefUserEvent);
        verify(chefProfileRepository, times(1)).save(chefProfile);
        verify(chefMapper, times(1)).toProfileCreatedEvent(chefProfile);
        verify(publisher, times(1)).publishChefCreated(profileCreatedEvent, SAGA_ID, CORRELATION_ID);
    }

    @Test
    @DisplayName("Should throw custom RuntimeException when saving down to DB fails")
    void createChefProfile_WhenDatabaseSaveFails_ShouldThrowRuntimeException() {
        // Arrange
        when(chefProfileRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.empty());
        when(chefMapper.toEntity(chefUserEvent)).thenReturn(chefProfile);
        when(chefProfileRepository.save(chefProfile)).thenThrow(new RuntimeException("DB Connection Timeout"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                kitchenService.createChefProfile(chefUserEvent, CORRELATION_ID, SAGA_ID)
        );

        assertTrue(exception.getMessage().contains("Chef profile creation failed for userId: " + AUTH_USER_ID));
        verify(chefProfileRepository, times(1)).save(chefProfile);
        verify(publisher, never()).publishChefCreated(any(), anyString(), anyString());
    }

    // ------------------------------------------------------------------------
    // Tests for updateDisplayName
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Should securely update and trim display name when chef is correctly found")
    void updateDisplayName_WhenChefExists_ShouldUpdateTrimmedNameAndSave() {
        // Arrange
        String untrimmedNewName = "   Chef Marco Pierre White   ";
        when(chefProfileRepository.findById(CHEF_ID)).thenReturn(Optional.of(chefProfile));

        // Act
        kitchenService.updateDisplayName(CHEF_ID, untrimmedNewName);

        // Assert
        assertEquals("Chef Marco Pierre White", chefProfile.getDisplayName());
        verify(chefProfileRepository, times(1)).findById(CHEF_ID);
        verify(chefProfileRepository, times(1)).save(chefProfile);
    }

    @Test
    @DisplayName("Should throw ChefNotFoundException during name update if chef does not exist")
    void updateDisplayName_WhenChefDoesNotExist_ShouldThrowException() {
        // Arrange
        when(chefProfileRepository.findById(CHEF_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ChefNotFoundException.class, () -> 
                kitchenService.updateDisplayName(CHEF_ID, "Chef Marco")
        );

        verify(chefProfileRepository, times(1)).findById(CHEF_ID);
        verify(chefProfileRepository, never()).save(any());
    }

    // ------------------------------------------------------------------------
    // Tests for updateStation
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Should accurately update chef station when valid chef is found")
    void updateStation_WhenChefExists_ShouldUpdateStationAndSave() {
        // Arrange
        when(chefProfileRepository.findById(CHEF_ID)).thenReturn(Optional.of(chefProfile));

        // Act
        kitchenService.updateStation(CHEF_ID, Station.FRY);

        // Assert
        assertEquals(Station.FRY, chefProfile.getStation());
        verify(chefProfileRepository, times(1)).findById(CHEF_ID);
        verify(chefProfileRepository, times(1)).save(chefProfile);
    }

    @Test
    @DisplayName("Should throw ChefNotFoundException during station update if chef does not exist")
    void updateStation_WhenChefDoesNotExist_ShouldThrowException() {
        // Arrange
        when(chefProfileRepository.findById(CHEF_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ChefNotFoundException.class, () -> 
                kitchenService.updateStation(CHEF_ID, Station.FRY)
        );

        verify(chefProfileRepository, times(1)).findById(CHEF_ID);
        verify(chefProfileRepository, never()).save(any());
    }

    // ------------------------------------------------------------------------
    // Tests for updateChefStatus
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Should update current chef status successfully when valid chef is found")
    void updateChefStatus_WhenChefExists_ShouldUpdateStatusAndSave() {
        // Arrange
        when(chefProfileRepository.findById(CHEF_ID)).thenReturn(Optional.of(chefProfile));

        // Act
        kitchenService.updateChefStatus(CHEF_ID, ChefStatus.INACTIVE);

        // Assert
        assertEquals(ChefStatus.INACTIVE, chefProfile.getStatus());
        verify(chefProfileRepository, times(1)).findById(CHEF_ID);
        verify(chefProfileRepository, times(1)).save(chefProfile);
    }

    @Test
    @DisplayName("Should throw ChefNotFoundException during status update if chef does not exist")
    void updateChefStatus_WhenChefDoesNotExist_ShouldThrowException() {
        // Arrange
        when(chefProfileRepository.findById(CHEF_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ChefNotFoundException.class, () -> 
                kitchenService.updateChefStatus(CHEF_ID, ChefStatus.INACTIVE)
        );

        verify(chefProfileRepository, times(1)).findById(CHEF_ID);
        verify(chefProfileRepository, never()).save(any());
    }

    // ------------------------------------------------------------------------
    // Tests for updateTicketStatus
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Should only update DB without triggering event when new ticket status is NOT READY")
    void updateTicketStatus_WhenStatusIsNotReady_ShouldUpdateAndSaveWithoutPublishing() {
        // Arrange
        when(kitchenTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(kitchenTicket));
        TicketStatus newStatus = TicketStatus.COOKING;

        // Act
        kitchenService.updateTicketStatus(TICKET_ID, newStatus);

        // Assert
        assertEquals(TicketStatus.COOKING, kitchenTicket.getStatus());
        verify(kitchenTicketRepository, times(1)).findById(TICKET_ID);
        verify(kitchenTicketRepository, times(1)).save(kitchenTicket);
        verify(publisher, never()).publishTicketReady(anyLong(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should update DB AND trigger TicketReadyEvent when ticket status maps exactly to READY")
    void updateTicketStatus_WhenStatusIsReady_ShouldUpdateSaveAndPublishEvent() {
        // Arrange
        when(kitchenTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(kitchenTicket));
        when(ticketMapper.toTicketReadyEvent(kitchenTicket)).thenReturn(ticketReadyEvent);

        // Act
        kitchenService.updateTicketStatus(TICKET_ID, TicketStatus.READY);

        // Assert
        assertEquals(TicketStatus.READY, kitchenTicket.getStatus());
        verify(kitchenTicketRepository, times(1)).findById(TICKET_ID);
        verify(kitchenTicketRepository, times(1)).save(kitchenTicket);
        verify(ticketMapper, times(1)).toTicketReadyEvent(kitchenTicket);
        
        // Use anyString() because correlationId and sagaId are UUID.randomUUID()
        verify(publisher, times(1)).publishTicketReady(eq(TICKET_ID), eq(ticketReadyEvent), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw TicketNotFoundException during ticket update if no existing ticket matched ID")
    void updateTicketStatus_WhenTicketDoesNotExist_ShouldThrowException() {
        // Arrange
        when(kitchenTicketRepository.findById(TICKET_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TicketNotFoundException.class, () -> 
                kitchenService.updateTicketStatus(TICKET_ID, TicketStatus.COOKING)
        );

        verify(kitchenTicketRepository, times(1)).findById(TICKET_ID);
        verify(kitchenTicketRepository, never()).save(any());
        verify(publisher, never()).publishTicketReady(anyLong(), any(), anyString(), anyString());
    }

    // ------------------------------------------------------------------------
    // Tests for getActiveTickets
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Should correctly return a list of mapped DTOs if repository finds non-ready active tickets")
    void getActiveTickets_WhenActiveTicketsExist_ShouldReturnMappedDtoList() {
        // Arrange
        List<KitchenTicket> returnedTickets = List.of(kitchenTicket);
        List<KitchenTicketDTO> mappedDTOs = List.of(kitchenTicketDTO);
        
        when(kitchenTicketRepository.findByStatusNot(TicketStatus.READY)).thenReturn(returnedTickets);
        when(ticketMapper.toDTOList(returnedTickets)).thenReturn(mappedDTOs);

        // Act
        List<KitchenTicketDTO> result = kitchenService.getActiveTickets();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(kitchenTicketRepository, times(1)).findByStatusNot(TicketStatus.READY);
        verify(ticketMapper, times(1)).toDTOList(returnedTickets);
    }

    @Test
    @DisplayName("Should throw TicketNotFoundException with null when no active unready tickets are found")
    void getActiveTickets_WhenNoActiveTicketsExist_ShouldThrowException() {
        // Arrange
        when(kitchenTicketRepository.findByStatusNot(TicketStatus.READY)).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(TicketNotFoundException.class, () -> 
                kitchenService.getActiveTickets()
        );

        verify(kitchenTicketRepository, times(1)).findByStatusNot(TicketStatus.READY);
        verify(ticketMapper, never()).toDTOList(any());
    }
}