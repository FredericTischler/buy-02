import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Auth } from '../../../core/services/auth';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
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
      if (file.size > 5 * 1024 * 1024) {
        this.errorMessage = 'L\'image ne doit pas dépasser 5MB';
        return;
      }
      if (!file.type.startsWith('image/')) {
        this.errorMessage = 'Le fichier doit être une image';
        return;
      }
      this.selectedAvatar = file;
      this.errorMessage = '';
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
      next: () => {
        if (this.selectedAvatar) {
          this.loginAndUploadAvatar();
        } else {
          this.showSuccessAndRedirect();
        }
      },
      error: (error) => {
        this.errorMessage = error.error?.error || 'Une erreur est survenue lors de l\'inscription';
        this.loading = false;
      }
    });
  }

  private loginAndUploadAvatar(): void {
    const email = this.registerForm.get('email')?.value;
    const password = this.registerForm.get('password')?.value;
    this.authService.login({ email, password }).subscribe({
      next: (loginResponse) => {
        this.uploadAvatar(loginResponse.token);
      },
      error: () => {
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
      error: () => {
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