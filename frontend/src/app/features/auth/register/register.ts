import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { Auth } from '../../../core/services/auth';

@Component({
  selector: 'app-register',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  registerForm: FormGroup;
  hidePassword = true;
  loading = false;
  errorMessage = '';
  successMessage = '';
  selectedAvatar: File | null = null;
  avatarPreview: string | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: Auth,
    private readonly router: Router
  ) {
    this.registerForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      role: ['CLIENT', Validators.required]
    });
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      
      // Validation taille
      if (file.size > 5 * 1024 * 1024) {
        this.errorMessage = 'L\'image ne doit pas dépasser 5MB';
        return;
      }
      
      // Validation type
      if (!file.type.startsWith('image/')) {
        this.errorMessage = 'Le fichier doit être une image';
        return;
      }
      
      this.selectedAvatar = file;
      this.errorMessage = '';
      
      // Générer la preview
      const reader = new FileReader();
      reader.onload = () => {
        this.avatarPreview = reader.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  removeAvatar(): void {
    this.selectedAvatar = null;
    this.avatarPreview = null;
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.register(this.registerForm.value).subscribe({
      next: (response) => {
        // Si avatar sélectionné, se connecter puis uploader l'avatar
        if (this.selectedAvatar) {
          this.loginAndUploadAvatar();
        } else {
          this.showSuccessAndRedirect();
        }
      },
      error: (error) => {
        console.error('Erreur d\'inscription:', error);
        this.errorMessage = error.error?.error || 'Une erreur est survenue lors de l\'inscription';
        this.loading = false;
      }
    });
  }

  private loginAndUploadAvatar(): void {
    const email = this.registerForm.get('email')?.value;
    const password = this.registerForm.get('password')?.value;
    
    // Se connecter pour obtenir le token
    this.authService.login({ email, password }).subscribe({
      next: (loginResponse) => {
        // Uploader l'avatar avec le token
        this.uploadAvatar(loginResponse.token);
      },
      error: (error) => {
        console.error('Erreur de connexion automatique:', error);
        this.showSuccessAndRedirect();
      }
    });
  }

  private uploadAvatar(token: string): void {
    if (!this.selectedAvatar) return;
    
    this.authService.uploadAvatar(this.selectedAvatar, token).subscribe({
      next: () => {
        this.showSuccessAndRedirect();
      },
      error: (error) => {
        console.error('Erreur upload avatar:', error);
        // Continuer même si l'avatar échoue
        this.showSuccessAndRedirect();
      }
    });
  }

  private showSuccessAndRedirect(): void {
    this.successMessage = 'Compte créé avec succès ! Redirection vers la connexion...';
    setTimeout(() => {
      this.router.navigate(['/login']);
    }, 2000);
    this.loading = false;
  }
}
