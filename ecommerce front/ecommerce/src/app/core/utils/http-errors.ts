import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorResponse } from '../models/api.models';

export interface ErrorSummary {
  message: string;
  details: string[];
}

export function summarizeHttpError(error: unknown, fallbackMessage: string): ErrorSummary {
  if (error instanceof HttpErrorResponse) {
    const payload = error.error as Partial<ApiErrorResponse> | string | null | undefined;
    if (typeof payload === 'string' && payload.trim()) {
      return { message: payload, details: [] };
    }

    if (payload && typeof payload === 'object') {
      const details = Array.isArray(payload.details)
        ? payload.details.map((detail) => `${detail.field}: ${detail.message}`).filter((detail) => detail.trim().length > 0)
        : [];

      return {
        message: payload.message?.trim() ? payload.message : fallbackMessage,
        details,
      };
    }

    return {
      message: error.message?.trim() ? error.message : fallbackMessage,
      details: [],
    };
  }

  if (error instanceof Error) {
    return {
      message: error.message.trim() ? error.message : fallbackMessage,
      details: [],
    };
  }

  return {
    message: fallbackMessage,
    details: [],
  };
}

export function mapAuthErrorMessage(error: unknown): { title: string; message: string } {
  if (error instanceof HttpErrorResponse) {
    const payload = error.error as Partial<ApiErrorResponse> | string | null | undefined;
    const code = typeof payload === 'object' && payload ? payload.error?.toUpperCase() : '';
    const responseMessage = typeof payload === 'object' && payload ? payload.message : '';

    switch (error.status) {
      case 400:
        return {
          title: 'Invalid request',
          message: responseMessage?.trim() || 'Check the submitted data and try again.',
        };
      case 401:
        return {
          title: 'Invalid credentials',
          message: 'Incorrect email or password.',
        };
      case 403:
        if (code === 'EMAIL_NOT_VERIFIED') {
          return {
            title: 'Account not verified',
            message: 'Verify your email before signing in.',
          };
        }
        if (code === 'ACCOUNT_DISABLED' || code === 'ACCOUNT_LOCKED') {
          return {
            title: 'Account locked',
            message: 'Your account is inactive or locked.',
          };
        }
        return {
          title: 'Access denied',
          message: responseMessage?.trim() || 'You do not have permission to complete this action.',
        };
      case 404:
        return {
          title: 'User not found',
          message: 'No account matches the submitted email address.',
        };
      case 409:
        return {
          title: 'Data conflict',
          message: responseMessage?.trim() || 'The submitted data could not be saved.',
        };
      default:
        return {
          title: 'Server error',
          message: responseMessage?.trim() || 'An unexpected error occurred.',
        };
    }
  }

  return {
    title: 'Error',
    message: error instanceof Error && error.message.trim() ? error.message : 'An unexpected error occurred.',
  };
}