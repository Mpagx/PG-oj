export type PasswordResetRequest = {
  email?: string;
  code?: string;
  newPassword?: string;
  checkPassword?: string;
};
