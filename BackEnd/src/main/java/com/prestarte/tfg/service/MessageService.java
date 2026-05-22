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

/**
 * Servicio que gestiona los mensajes intercambiados dentro de una
 * sesión de chat.
 *
 * Cubre la lectura del histórico, el envío de mensajes de texto y el
 * envío con archivo adjunto. El emisor se toma siempre del usuario
 * autenticado, no del cuerpo de la petición, para evitar
 * suplantaciones.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;
    private final DBFileRepository dbFileRepository;
    private final CurrentUser currentUser;
    private final ChatSessionService chatSessionService;

    /** Devuelve el histórico de una sesión, accesible solo a sus participantes. */
    public List<MessageResponse> getMessagesByChatSessionId(Long chatSessionId) {
        ChatSession chat = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Sesión de chat", chatSessionId));
        chatSessionService.requireParticipant(chat.getLoanRequest());

        return messageRepository.findByChatSessionIdOrderBySentAtAsc(chatSessionId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Crea un mensaje de texto en la sesión indicada. El emisor se
     * resuelve a partir del JWT de la petición. Se rechazan los
     * mensajes vacíos y los enviados a un chat ya cerrado.
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

        User sender = userRepository.findById(currentUser.currentId())
                .orElseThrow(() -> new AccessDeniedException("Sesión no reconocida"));

        chatSessionService.requireParticipant(chatSession.getLoanRequest());

        Message message = Message.builder()
                .chatSession(chatSession)
                .sender(sender)
                .content(request.getContent().trim())
                .tipo(request.getTipo() != null ? request.getTipo() : Message.TipoMensaje.TEXTO)
                .build();

        return mapToResponse(messageRepository.save(message));
    }

    /** Devuelve todos los mensajes del sistema. Reservado a auditoría. */
    public List<MessageResponse> getAllMessages() {
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Solo administradores pueden listar todos los mensajes");
        }
        return messageRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Devuelve un mensaje concreto comprobando que el usuario pueda verlo. */
    public MessageResponse getMessageDtoById(Long id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Mensaje", id));
        chatSessionService.requireParticipant(message.getChatSession().getLoanRequest());
        return mapToResponse(message);
    }

    /**
     * Crea un mensaje con archivo adjunto. El tipo del mensaje se
     * deduce del MIME del archivo: si es una imagen se marca como
     * {@code IMAGEN}; en cualquier otro caso, como {@code DOCUMENTO}.
     */
    @Transactional
    public MessageResponse createMessageWithFile(Long chatSessionId, String content,
                                                 org.springframework.web.multipart.MultipartFile file)
            throws java.io.IOException {
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Sesión de chat", chatSessionId));

        if (chatSession.getEstado() == ChatSession.EstadoChat.CERRADO) {
            throw new IllegalStateException("No se pueden enviar mensajes a un chat cerrado");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Adjunto vacío o no proporcionado");
        }

        chatSessionService.requireParticipant(chatSession.getLoanRequest());

        User sender = userRepository.findById(currentUser.currentId())
                .orElseThrow(() -> new AccessDeniedException("Sesión no reconocida"));

        DBFile dbFile = DBFile.builder()
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .data(file.getBytes())
                .fileSize(file.getSize())
                .build();
        dbFileRepository.save(dbFile);

        boolean isImage = file.getContentType() != null && file.getContentType().startsWith("image/");
        Message.TipoMensaje tipo = isImage ? Message.TipoMensaje.IMAGEN : Message.TipoMensaje.DOCUMENTO;

        Message msg = Message.builder()
                .chatSession(chatSession)
                .sender(sender)
                .content(content != null ? content.trim() : "")
                .tipo(tipo)
                .attachment(dbFile)
                .build();

        return mapToResponse(messageRepository.save(msg));
    }

    /** Compone el DTO de respuesta a partir de la entidad de mensaje. */
    private MessageResponse mapToResponse(Message message) {
        var att = message.getAttachment();
        return MessageResponse.builder()
                .id(message.getId())
                .chatSessionId(message.getChatSession().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .tipo(message.getTipo())
                .attachmentId(att != null ? att.getId() : null)
                .attachmentFileName(att != null ? att.getFileName() : null)
                .attachmentFileType(att != null ? att.getFileType() : null)
                .build();
    }
}
