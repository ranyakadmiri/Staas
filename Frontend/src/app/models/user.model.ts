export enum UserStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED'
}

export interface User {
  id: number;
  email: string;
  username?: string;
  status: UserStatus;
  createdAt?: Date;
}
