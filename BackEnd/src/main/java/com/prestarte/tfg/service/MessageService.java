package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.CreateMessageRequest;
import com.prestarte.tfg.model.dto.MessageResponse;
import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.*;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
    private final CurrentUser currentUser;
    private final ChatSessionService chatSessionService;

    /** Mensajes del chat. Solo participantes pueden leerlos. */
    public List<MessageResponse> getMessagesByChatSessionId(Long chatSessionId) {
        ChatSession chat = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Sesión de chat", chatSessionId));
        chatSessionService.requireParticipant(chat.getLoanRequest());

        return messageRepository.findByChatSessionIdOrderBySentAtAsc(chatSessionId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Envía un mensaje al chat. El sender se toma del JWT (no del cliente):
     * eso evita la suplantación enviando un senderId arbitrario.
     */
    @Transactional
    public MessageResponse createMessage(CreateMessageRequest request) {
        ChatSession chatSession = chatSessionRepository.findById(request.getChatSessionId())
                .orElseThrow(() -> ResourceNotFoundException.of(
                        "Sesión de chat", request.getChatSessionId()));

        if (chatSession.getEstado() == ChatSession.EstadoChat.CERRADO) {
            throw new IllegalStateException("No se pueden enviar mensajes a un chat cerrado");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }

        // El sender real es el usuario autenticado, ignoramos request.senderId.
        User sender = userRepository.findById(currentUser.currentId())
                .orElseThrow(() -> new AccessDeniedException("Sesión no reconocida"));

        // Validamos que el sender es participante del préstamo asociado.
        chatSessionService.requireParticipant(chatSession.getLoanRequest());

        Message message = Message.builder()
                .chatSession(chatSession)
                .sender(sender)
                .content(request.getContent().trim())
                .tipo(request.getTipo() != null ? request.getTipo() : Message.TipoMensaje.TEXTO)
                .build();

        return mapToResponse(messageRepository.save(message));
    }

    /** Solo accesible para admin (auditoría). */
    public List<MessageResponse> getAllMessages() {
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Solo administradores pueden listar todos los mensajes");
        }
        return messageRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public MessageResponse getMessageDtoById(Long id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Mensaje", id));
        chatSessionService.requireParticipant(message.getChatSession().getLoanRequest());
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
