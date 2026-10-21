import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, retry } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import {
  ChatSessionResponse,
  CreateMessageRequest,
  MessageResponse,
} from '../models/chat.models';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;
  private readonly auth = inject(AuthService);

  /** Devuelve la sesión de chat de un préstamo (la crea si no existe). */
  getOrCreateForLoan(loanId: number): Observable<ChatSessionResponse> {
    return this.http.get<ChatSessionResponse>(`${this.base}/chat-sessions/loan/${loanId}`);
  }

  /** Historial completo de mensajes del chat (orden cronológico). */
  getMessages(chatSessionId: number): Observable<MessageResponse[]> {
    return this.http.get<MessageResponse[]>(`${this.base}/messages/chat/${chatSessionId}`);
  }

  /** Envía un mensaje. El sender lo resuelve el back desde el JWT. */
  sendMessage(req: CreateMessageRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/messages`, req);
  }

  /** Envía un mensaje con archivo adjunto (foto o documento). */
  sendMessageWithFile(chatSessionId: number, content: string, file: File): Observable<MessageResponse> {
    const form = new FormData();
    form.append('file', file);
    const params = new URLSearchParams({ chatSessionId: String(chatSessionId) });
    if (content) params.set('content', content);
    return this.http.post<MessageResponse>(
      `${this.base}/messages/with-file?${params.toString()}`,
      form,
    );
  }


  closeChat(chatSessionId: number): Observable<ChatSessionResponse> {
    return this.http.put<ChatSessionResponse>(`${this.base}/chat-sessions/${chatSessionId}/close`, {});
  }

  /**
   * Avisos en tiempo real (Server-Sent Events) de un chat. Emite cada vez
   * que hay que recargar los mensajes: al conectar (para recuperar lo que
   * llegara mientras estaba desconectado) y con cada mensaje nuevo. Se
   * reconecta sola si se corta.
   *
   * Usa fetch y no EventSource porque EventSource no permite enviar la
   * cabecera Authorization con el JWT.
   */
  messageStream(chatSessionId: number): Observable<void> {
    const url = `${this.base}/messages/chat/${chatSessionId}/stream`;
    const once = new Observable<void>((subscriber) => {
      const controller = new AbortController();
      const token = this.auth.getToken();
      fetch(url, {
        headers: { Accept: 'text/event-stream', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
        signal: controller.signal,
      })
        .then(async (res) => {
          if (!res.ok || !res.body) throw new Error(`HTTP ${res.status}`);
          const reader = res.body.pipeThrough(new TextDecoderStream()).getReader();
          let buffer = '';
          for (;;) {
            const { value, done } = await reader.read();
            if (done) break;
            buffer += value;
            // Los eventos SSE se separan con una línea en blanco.
            let sep: number;
            while ((sep = buffer.indexOf('\n\n')) >= 0) {
              const block = buffer.slice(0, sep);
              buffer = buffer.slice(sep + 2);
              if (/^event:\s*(ready|message)\s*$/m.test(block)) subscriber.next();
            }
          }
          subscriber.error(new Error('stream cerrado'));
        })
        .catch((err) => {
          if (!controller.signal.aborted) subscriber.error(err);
        });
      return () => controller.abort();
    });
    return once.pipe(retry({ delay: 5000 }));
  }
}
