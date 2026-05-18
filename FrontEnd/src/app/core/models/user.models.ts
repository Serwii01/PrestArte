export type Role = 'COLLECTOR' | 'FOUNDATION' | 'TRANSPORT' | 'ADMIN';

export type UserStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface UserResponse {
  id: number;
  email: string;
  name: string;
  phone?: string;
  role: Role;
  status: UserStatus;
  enabled: boolean;
  taxId?: string;

  /** Documento de verificación KYB (DNI, escritura, etc.). */
  verificationFileId?: string | null;
  verificationFileName?: string | null;
  verificationFileType?: string | null;
}
