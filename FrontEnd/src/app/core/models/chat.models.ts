export type ChatStatus = 'ACTIVO' | 'CERRADO';

export type MessageType = 'TEXTO' | 'IMAGEN' | 'DOCUMENTO' | 'ACUERDO';

export interface ChatSessionResponse {
  id: number;
  loanRequestId: number;
  estado: ChatStatus;
  createdAt: string;
  closedAt?: string | null;
}

export interface MessageResponse {
  id: number;
  chatSessionId: number;
  senderId: number;
  senderName: string;
  content: string;
  sentAt: string;
  tipo: MessageType;
  attachmentId?: string | null;
  attachmentFileName?: string | null;
  attachmentFileType?: string | null;
}

export interface CreateMessageRequest {
  chatSessionId: number;
  /** El back ignora este campo: lo resuelve desde el JWT. */
  senderId?: number;
  content: string;
  tipo?: MessageType;
}
