import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ChatSessionResponse,
  CreateMessageRequest,
  MessageResponse,
} from '../models/chat.models';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

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

  closeChat(chatSessionId: number): Observable<ChatSessionResponse> {
    return this.http.put<ChatSessionResponse>(`${this.base}/chat-sessions/${chatSessionId}/close`, {});
  }
}
