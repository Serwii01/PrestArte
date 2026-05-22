package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.Message;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO público que representa un mensaje del chat.
 *
 * Incluye los datos del autor, la fecha de envío y, cuando el
 * mensaje lleva un archivo adjunto, la información mínima necesaria
 * para que la interfaz pueda mostrarlo o enlazar a su descarga.
 */
@Data
@Builder
public class MessageResponse {

    private Long id;
    private Long chatSessionId;
    private Long senderId;
    private String senderName;
    private String content;
    private LocalDateTime sentAt;
    private Message.TipoMensaje tipo;

    /** Identificador del archivo adjunto, si el mensaje incluye uno. */
    private String attachmentId;
    private String attachmentFileName;
    private String attachmentFileType;
}
