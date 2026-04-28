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

import java.util.List;
import java.util.Optional;

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
                .toList();
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

    public MessageResponse createMessage(CreateMessageRequest request) {
        ChatSession chatSession = chatSessionRepository.findById(request.getChatSessionId())
                .orElseThrow(() -> new RuntimeException("ChatSession no encontrada"));

        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender no encontrado"));

        Message message = Message.builder()
                .chatSession(chatSession)
                .sender(sender)
                .content(request.getContent())
                .tipo(request.getTipo() != null ? request.getTipo() : Message.TipoMensaje.TEXTO)
                .build();

        Message savedMessage = messageRepository.save(message);

        Long collectorId = chatSession.getLoanRequest().getArtwork().getCollector().getId();
        Long foundationId = chatSession.getLoanRequest().getFoundation().getId();

        if (!sender.getId().equals(collectorId) && !sender.getId().equals(foundationId)) {
            throw new RuntimeException("Este usuario no pertenece al chat");
        }

        if (chatSession.getEstado() == ChatSession.EstadoChat.CERRADO) {
            throw new RuntimeException("No se pueden enviar mensajes a un chat cerrado");
        }

        return mapToResponse(savedMessage);
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }

    public Optional<Message> getMessageById(Long id) {
        return messageRepository.findById(id);
    }
}