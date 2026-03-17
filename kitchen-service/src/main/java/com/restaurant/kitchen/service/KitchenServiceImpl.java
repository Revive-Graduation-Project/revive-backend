package com.restaurant.kitchen.service;

import com.restaurant.kitchen.dto.KitchenTicketDTO;
import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.entity.KitchenTicket;
import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;
import com.restaurant.kitchen.enums.TicketStatus;
import com.restaurant.kitchen.exception.ChefNotFoundException;
import com.restaurant.kitchen.exception.TicketNotFoundException;
import com.restaurant.kitchen.mapper.KitchenTicketMapper;
import com.restaurant.kitchen.messaging.MessageHandler;
import com.restaurant.kitchen.repository.ChefProfileRepository;
import com.restaurant.kitchen.repository.KitchenTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KitchenServiceImpl implements KitchenService {

    private final MessageHandler messageHandler;
    private final ChefProfileRepository chefProfileRepository;
    private final KitchenTicketRepository kitchenTicketRepository;
    private final KitchenTicketMapper ticketMapper;

    private ChefProfile findChef(Long id) {

        return chefProfileRepository.findById(id)
                .orElseThrow(() -> new ChefNotFoundException(id));

    }

    @Override
    public void updateDisplayName(Long id, String displayName) {

        ChefProfile retrievedChef = findChef(id);
        retrievedChef.setDisplayName(displayName.trim());
        chefProfileRepository.save(retrievedChef);
    }

    @Override
    public void updateStation(Long id, Station station) {

        ChefProfile retrievedChef = findChef(id);
        retrievedChef.setStation(station);
        chefProfileRepository.save(retrievedChef);

    }

    @Override
    public void updateChefStatus(Long id, ChefStatus status) {

        ChefProfile retrievedChef = findChef(id);
        retrievedChef.setStatus(status);
        chefProfileRepository.save(retrievedChef);
    }

    @Transactional //both of updating db and publishing event must success
    @Override
    public void updateTicketStatus(Long id, TicketStatus status) {

        KitchenTicket retrievedTicket = kitchenTicketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        retrievedTicket.setStatus(status);
        kitchenTicketRepository.save(retrievedTicket);

        if (status.equals(TicketStatus.READY))
            messageHandler.publishTicketReadyEvent(id, retrievedTicket);
    }

    @Override
    public List<KitchenTicketDTO> getActiveTickets() {
        List<KitchenTicket> activeTickets = kitchenTicketRepository.findByStatusNot(TicketStatus.READY);

        if (activeTickets.isEmpty())
            throw new TicketNotFoundException(null);

        return ticketMapper.toDTOList(activeTickets);
    }
}
