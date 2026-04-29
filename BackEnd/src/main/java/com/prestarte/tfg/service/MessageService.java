package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.CreateMessageRequest;
import com.prestarte.tfg.model.dto.MessageResponse;
import com.prestarte.tfg.model.entity.ChatSession;
import com.prestarte.tfg.model.entity.Message;
import com.prestarte.tfg.model.entity.User;
import com.prestarte.tfg.repository.ChatSessionRepository;
import com.prestarte.tfg.repository.MessageRepository;
import com.prestarte.tfg.repository.UserRepository;
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

    public List<MessageResponse> getMessagesByChatSessionId(Long chatSessionId) {
        return messageRepository.findByChatSessionIdOrderBySentAtAsc(chatSessionId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse createMessage(CreateMessageRequest request) {
        // 1. Validaciones previas
        ChatSession chatSession = chatSessionRepository.findById(request.getChatSessionId())
                .orElseThrow(() -> new RuntimeException("ChatSession no encontrada"));

        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new RuntimeException("Remitente no encontrado"));

        if (chatSession.getEstado() == ChatSession.EstadoChat.CERRADO) {
            throw new RuntimeException("No se pueden enviar mensajes a un chat cerrado");
        }

        // 2. Validar que el usuario participa en este préstamo
        Long collectorId = chatSession.getLoanRequest().getArtwork().getCollector().getId();
        Long foundationId = chatSession.getLoanRequest().getFoundation().getId();

        if (!sender.getId().equals(collectorId) && !sender.getId().equals(foundationId)) {
            throw new RuntimeException("Este usuario no tiene permiso para participar en este chat");
        }

        // 3. Crear y guardar
        Message message = Message.builder()
                .chatSession(chatSession)
                .sender(sender)
                .content(request.getContent())
                .tipo(request.getTipo() != null ? request.getTipo() : Message.TipoMensaje.TEXTO)
                .build();

        return mapToResponse(messageRepository.save(message));
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