import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  constructor(private readonly snackBar: MatSnackBar) {}

  error(message: string): void {
    this.snackBar.open(message, 'Dismiss', {
      duration: 6000,
      panelClass: ['snackbar-error'],
    });
  }

  warn(message: string): void {
    this.snackBar.open(message, 'Dismiss', {
      duration: 5000,
      panelClass: ['snackbar-warn'],
    });
  }

  success(message: string): void {
    this.snackBar.open(message, undefined, {
      duration: 3000,
      panelClass: ['snackbar-success'],
    });
  }
}
