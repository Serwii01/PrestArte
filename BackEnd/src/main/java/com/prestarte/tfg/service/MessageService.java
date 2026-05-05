package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
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
    private final ShipmentRepository shipmentRepository;

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

        // Verificamos que el chat no esté cerrado
        if (chatSession.getEstado() == ChatSession.EstadoChat.CERRADO) {
            throw new RuntimeException("No se pueden enviar mensajes a un chat cerrado");
        }

        // Validación de contenido básico
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new RuntimeException("El mensaje no puede estar vacío");
        }

        // VALIDACIÓN TRIPLE ACTUALIZADA
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
     * Lógica de validación para permitir solo a los 3 actores involucrados[cite: 9]
     */
    private void validateParticipant(ChatSession chatSession, User sender) {
        LoanRequest loan = chatSession.getLoanRequest();
        Long senderId = sender.getId();

        // 1. ¿Es el Coleccionista dueño de la obra?[cite: 2, 9]
        if (senderId.equals(loan.getArtwork().getCollector().getId())) return;

        // 2. ¿Es la Fundación solicitante?[cite: 4, 9]
        if (senderId.equals(loan.getFoundation().getId())) return;

        // 3. ¿Es el Transportista asignado a este envío específico?[cite: 5, 7, 9]
        boolean isAuthorizedTransport = shipmentRepository.findByLoanRequestId(loan.getId())
                .map(shipment -> shipment.getTransportCompany().getId().equals(senderId))
                .orElse(false);

        if (isAuthorizedTransport) return;

        // Si llega aquí, el usuario es un "intruso" en esta conversación
        throw new RuntimeException("Acceso denegado: No participas en esta negociación");
    }

    /**
     * Devuelve todos los mensajes del sistema.
     * TODO (bloque 2): este endpoint debe restringirse a ADMIN o eliminarse.
     * Exponer todos los mensajes a cualquier usuario autenticado es una fuga de privacidad.
     */
    public List<MessageResponse> getAllMessages() {
        return messageRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Devuelve un mensaje concreto por id.
     * TODO (bloque 2): validar que el usuario actual es participante del chat al que pertenece.
     */
    public MessageResponse getMessageDtoById(Long id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Mensaje", id));
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