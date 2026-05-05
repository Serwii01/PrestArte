package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.CreateMessageRequest;
import com.prestarte.tfg.model.dto.MessageResponse;
import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository; // <--- Necesario para validar al transportista

    public List<MessageResponse> getMessagesByChatSessionId(Long chatSessionId) {
        return messageRepository.findByChatSessionIdOrderBySentAtAsc(chatSessionId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse createMessage(CreateMessageRequest request) {
        ChatSession chatSession = chatSessionRepository.findById(request.getChatSessionId())
                .orElseThrow(() -> new RuntimeException("ChatSession no encontrada"));

        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new RuntimeException("Remitente no encontrado"));

        if (chatSession.getEstado() == ChatSession.EstadoChat.CERRADO) {
            throw new RuntimeException("No se pueden enviar mensajes a un chat cerrado");
        }

        // --- VALIDACIÓN TRIPLE ACTUALIZADA ---
        validateParticipant(chatSession, sender);

        Message message = Message.builder()
                .chatSession(chatSession)
                .sender(sender)
                .content(request.getContent())
                .tipo(request.getTipo() != null ? request.getTipo() : Message.TipoMensaje.TEXTO)
                .build();

        return mapToResponse(messageRepository.save(message));
    }

    /**
     * Lógica de validación para permitir 3 actores
     */
    private void validateParticipant(ChatSession chatSession, User sender) {
        LoanRequest loan = chatSession.getLoanRequest();
        Long senderId = sender.getId();

        // 1. ¿Es el Coleccionista?
        if (senderId.equals(loan.getArtwork().getCollector().getId())) return;

        // 2. ¿Es la Fundación?
        if (senderId.equals(loan.getFoundation().getId())) return;

        // 3. ¿Es el Transportista asignado?
        // Buscamos si existe un envío para este préstamo y si la empresa coincide
        boolean isAuthorizedTransport = shipmentRepository.findByLoanRequestId(loan.getId())
                .map(shipment -> shipment.getTransportCompany().getId().equals(senderId))
                .orElse(false);

        if (isAuthorizedTransport) return;

        // Si no es ninguno de los tres...
        throw new RuntimeException("Este usuario no tiene permiso para participar en este chat");
    }

    public List<MessageResponse> getAllMessages() {
        return messageRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public MessageResponse getMessageDtoById(Long id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mensaje no encontrado"));
        return mapToResponse(message);
    }

    private MessageResponse mapToResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .chatSessionId(message.getChatSession().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .tipo(message.getTipo())
                .build();
    }
}