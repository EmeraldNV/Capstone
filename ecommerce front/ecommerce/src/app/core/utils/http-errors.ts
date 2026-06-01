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
        ? payload.details
            .map((detail) => `${detail.field}: ${detail.message}`)
            .filter((detail) => detail.trim().length > 0)
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
          title: 'Richiesta non valida',
          message: responseMessage?.trim() || 'Controlla i dati inseriti e riprova.',
        };
      case 401:
        return {
          title: 'Credenziali non valide',
          message: 'Email o password non corretti.',
        };
      case 403:
        if (code === 'EMAIL_NOT_VERIFIED') {
          return {
            title: 'Account non verificato',
            message: 'Verifica la tua email prima di accedere.',
          };
        }
        if (code === 'ACCOUNT_DISABLED' || code === 'ACCOUNT_LOCKED') {
          return {
            title: 'Account bloccato',
            message: 'Il tuo account è inattivo o bloccato.',
          };
        }
        return {
          title: 'Accesso negato',
          message: responseMessage?.trim() || 'Non hai i permessi per completare questa operazione.',
        };
      case 404:
        return {
          title: 'Utente non trovato',
          message: 'Nessun account corrisponde all’email inserita.',
        };
      case 409:
        return {
          title: 'Conflitto dati',
          message: responseMessage?.trim() || 'I dati inviati non possono essere salvati.',
        };
      default:
        return {
          title: 'Errore server',
          message: responseMessage?.trim() || 'Si è verificato un errore inatteso.',
        };
    }
  }

  return {
    title: 'Errore',
    message: error instanceof Error && error.message.trim() ? error.message : 'Si è verificato un errore inatteso.',
  };
}
