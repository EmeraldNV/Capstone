import { Injectable } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { PaymentStatusUpdate } from '../models/payment.models';

@Injectable({
  providedIn: 'root',
})
export class PaymentWebSocketService {
  private readonly updatesSubject = new Subject<PaymentStatusUpdate>();
  private readonly connectionSubject = new BehaviorSubject<boolean>(false);
  private socket: WebSocket | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private reconnectAttempts = 0;
  private manualClose = false;
  private activeSessionId = '';

  readonly updates$ = this.updatesSubject.asObservable();
  readonly connected$ = this.connectionSubject.asObservable();

  connect(sessionId: string): void {
    if (!sessionId || typeof window === 'undefined') {
      return;
    }

    if (this.activeSessionId === sessionId && this.socket && this.socket.readyState === WebSocket.OPEN) {
      return;
    }

    this.disconnect();
    this.activeSessionId = sessionId;
    this.manualClose = false;
    this.reconnectAttempts = 0;
    this.openSocket();
  }

  disconnect(): void {
    this.manualClose = true;
    this.activeSessionId = '';
    this.clearReconnectTimer();

    if (this.socket) {
      try {
        this.socket.close();
      } catch {
        // ignore close errors
      }
      this.socket = null;
    }

    this.connectionSubject.next(false);
  }

  private openSocket(): void {
    if (!this.activeSessionId || typeof window === 'undefined') {
      return;
    }

    const url = new URL('/ws/payments', window.location.origin);
    url.protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    url.searchParams.set('sessionId', this.activeSessionId);

    this.socket = new WebSocket(url.toString());
    this.socket.onopen = () => {
      this.reconnectAttempts = 0;
      this.connectionSubject.next(true);
    };
    this.socket.onmessage = (event) => {
      try {
        const update = JSON.parse(event.data as string) as PaymentStatusUpdate;
        this.updatesSubject.next(update);
      } catch {
        // ignore malformed messages
      }
    };
    this.socket.onerror = () => {
      this.connectionSubject.next(false);
    };
    this.socket.onclose = () => {
      this.connectionSubject.next(false);
      this.socket = null;
      if (!this.manualClose && this.activeSessionId) {
        this.scheduleReconnect();
      }
    };
  }

  private scheduleReconnect(): void {
    this.clearReconnectTimer();
    if (this.reconnectAttempts >= 6) {
      return;
    }

    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 8000);
    this.reconnectAttempts += 1;
    this.reconnectTimer = window.setTimeout(() => {
      if (!this.manualClose && this.activeSessionId) {
        this.openSocket();
      }
    }, delay);
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }
}
