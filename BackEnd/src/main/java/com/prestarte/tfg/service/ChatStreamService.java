package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.entity.ChatSession;
import com.prestarte.tfg.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Chat en tiempo real con Server-Sent Events.
 *
 * El stream solo avisa de que hay un mensaje nuevo (su id): el cliente lo
 * pide después por REST. Así cada participante recibe el mensaje con sus
 * propias reglas de visibilidad (por ejemplo, el anonimato del
 * coleccionista) sin duplicar esa lógica aquí.
 *
 * Los suscriptores se guardan en memoria: vale para una sola instancia
 * del backend. Con varias, habría que repartir los avisos (Redis pub/sub…).
 */
@Service
@RequiredArgsConstructor
public class ChatStreamService {

    private static final Logger log = LoggerFactory.getLogger(ChatStreamService.class);

    /** Al caducar, el navegador se reconecta solo. */
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final ChatSessionRepository chatSessionRepository;
    private final ChatSessionService chatSessionService;

    private final Map<Long, Set<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    /** Abre un stream para un participante del chat. */
    @Transactional(readOnly = true)
    public SseEmitter subscribe(Long chatSessionId) {
        ChatSession chat = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Sesión de chat", chatSessionId));
        chatSessionService.requireParticipant(chat.getLoanRequest());

        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        Set<SseEmitter> set = subscribers.computeIfAbsent(chatSessionId, id -> new CopyOnWriteArraySet<>());
        set.add(emitter);
        Runnable remove = () -> set.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        send(set, emitter, SseEmitter.event().name("ready").data("ok"));
        return emitter;
    }

    /**
     * Avisa a los suscriptores de un mensaje nuevo. Si hay transacción,
     * espera a que se confirme: si no, el cliente pediría un mensaje que
     * aún no se ve en la base de datos.
     */
    public void publish(Long chatSessionId, Long messageId) {
        Runnable push = () -> {
            Set<SseEmitter> set = subscribers.get(chatSessionId);
            if (set == null) return;
            for (SseEmitter emitter : set) {
                send(set, emitter, SseEmitter.event().name("message").data(Map.of("messageId", messageId)));
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    push.run();
                }
            });
        } else {
            push.run();
        }
    }

    /** Comentario periódico para que proxies y navegadores no corten la conexión por inactividad. */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        subscribers.forEach((chatId, set) -> {
            for (SseEmitter emitter : set) {
                send(set, emitter, SseEmitter.event().comment("ping"));
            }
        });
        subscribers.values().removeIf(Set::isEmpty);
    }

    private static void send(Set<SseEmitter> set, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            // Cliente desconectado: se descarta sin ruido.
            set.remove(emitter);
            log.debug("Stream de chat cerrado: {}", e.getMessage());
        }
    }
}
