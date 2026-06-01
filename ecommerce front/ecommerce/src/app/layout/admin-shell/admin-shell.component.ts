import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SessionService } from '../../core/services/session.service';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './admin-shell.component.html',
  styleUrl: './admin-shell.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminShellComponent {
  protected readonly session = inject(SessionService);

  readonly navigation = [
    { label: 'Dashboard', path: '/admin/dashboard' },
    { label: 'Utenti', path: '/admin/users' },
    { label: 'Prodotti', path: '/admin/products' },
  ];

  readonly storefrontLinks = [
    { label: 'Home sito', path: '/' },
    { label: 'Shop', path: '/shop' },
    { label: 'Carrello', path: '/cart' },
    { label: 'Profilo', path: '/account/profile' },
  ];
}
