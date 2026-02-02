import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { fakeAsync, tick } from '@angular/core/testing';

import { Register } from './register';
import { Auth } from '../../../core/services/auth';

describe('Register', () => {
  let component: Register;
  let fixture: ComponentFixture<Register>;
  let authSpy: jasmine.SpyObj<Auth>;
  let router: Router;
  const originalFileReader = globalThis.FileReader;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj<Auth>('Auth', ['register', 'login', 'uploadAvatar']);
    await TestBed.configureTestingModule({
      imports: [
        Register,
        HttpClientTestingModule,
        RouterTestingModule.withRoutes([]),
        NoopAnimationsModule,
      ],
      providers: [{ provide: Auth, useValue: authSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(Register);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  afterAll(() => {
    globalThis.FileReader = originalFileReader;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should reject avatar that is too large', () => {
    const file = new File(['a'], 'avatar.png', { type: 'image/png' });
    Object.defineProperty(file, 'size', { value: 6 * 1024 * 1024 });
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [file] });

    component.onAvatarSelected({ target: input } as unknown as Event);

    expect(component.errorMessage).toContain('5MB');
    expect(component.selectedAvatar).toBeNull();
  });

  it('should reject non image avatars', () => {
    const file = new File(['a'], 'doc.txt', { type: 'text/plain' });
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [file] });

    component.onAvatarSelected({ target: input } as unknown as Event);

    expect(component.errorMessage).toContain('image');
  });

  it('should accept valid avatars and generate preview', () => {
    class MockFileReader {
      result: string | ArrayBuffer | null = null;
      onload: (() => void) | null = null;
      readAsDataURL(): void {
        this.result = 'data:image/png;base64,preview';
        this.onload?.();
      }
    }
    globalThis.FileReader = MockFileReader as unknown as typeof FileReader;

    const file = new File(['image'], 'avatar.png', { type: 'image/png' });
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [file] });

    component.onAvatarSelected({ target: input } as unknown as Event);

    expect(component.selectedAvatar).toBe(file);
    expect(component.avatarPreview).toBe('data:image/png;base64,preview');
  });

  it('should remove avatar state', () => {
    component.selectedAvatar = new File(['a'], 'avatar.png', { type: 'image/png' });
    component.avatarPreview = 'data:image/png;base64,preview';

    component.removeAvatar();

    expect(component.selectedAvatar).toBeNull();
    expect(component.avatarPreview).toBeNull();
  });

  it('should not submit invalid form', () => {
    component.onSubmit();
    expect(authSpy.register).not.toHaveBeenCalled();
  });

  it('should register and redirect without avatar', fakeAsync(() => {
    component.registerForm.setValue({
      name: 'Alice',
      email: 'alice@mail.com',
      password: 'password',
      role: 'CLIENT',
    });
    authSpy.register.and.returnValue(of({}));

    component.onSubmit();
    tick(2000);

    expect(authSpy.register).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    expect(component.successMessage).toContain('Compte créé');
  }));

  it('should handle registration errors', () => {
    component.registerForm.setValue({
      name: 'Alice',
      email: 'alice@mail.com',
      password: 'password',
      role: 'CLIENT',
    });
    authSpy.register.and.returnValue(throwError(() => ({ error: { error: 'Email utilisé' } })));

    component.onSubmit();

    expect(component.errorMessage).toBe('Email utilisé');
    expect(component.loading).toBeFalse();
  });

  it('should login and upload avatar after registration', fakeAsync(() => {
    component.registerForm.setValue({
      name: 'Alice',
      email: 'alice@mail.com',
      password: 'password',
      role: 'CLIENT',
    });
    const file = new File(['img'], 'avatar.png', { type: 'image/png' });
    component.selectedAvatar = file;

    authSpy.register.and.returnValue(of({}));
    authSpy.login.and.returnValue(of({
      token: 'jwt',
      userId: '1',
      email: 'alice@mail.com',
      name: 'Alice',
      role: 'CLIENT',
      avatar: '',
      type: 'Bearer',
    }));
    authSpy.uploadAvatar.and.returnValue(of({}));

    component.onSubmit();
    tick(2000);

    expect(authSpy.login).toHaveBeenCalled();
    expect(authSpy.uploadAvatar).toHaveBeenCalledWith(file, 'jwt');
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  }));

  it('should redirect even if login fails after registration with avatar', fakeAsync(() => {
    component.registerForm.setValue({
      name: 'Alice',
      email: 'alice@mail.com',
      password: 'password',
      role: 'CLIENT',
    });
    const file = new File(['img'], 'avatar.png', { type: 'image/png' });
    component.selectedAvatar = file;

    authSpy.register.and.returnValue(of({}));
    authSpy.login.and.returnValue(throwError(() => new Error('Login failed')));

    component.onSubmit();
    tick(2000);

    expect(authSpy.login).toHaveBeenCalled();
    expect(authSpy.uploadAvatar).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    expect(component.successMessage).toContain('Compte créé');
  }));

  it('should redirect even if avatar upload fails', fakeAsync(() => {
    component.registerForm.setValue({
      name: 'Alice',
      email: 'alice@mail.com',
      password: 'password',
      role: 'CLIENT',
    });
    const file = new File(['img'], 'avatar.png', { type: 'image/png' });
    component.selectedAvatar = file;

    authSpy.register.and.returnValue(of({}));
    authSpy.login.and.returnValue(of({
      token: 'jwt',
      userId: '1',
      email: 'alice@mail.com',
      name: 'Alice',
      role: 'CLIENT',
      avatar: '',
      type: 'Bearer',
    }));
    authSpy.uploadAvatar.and.returnValue(throwError(() => new Error('Upload failed')));

    component.onSubmit();
    tick(2000);

    expect(authSpy.uploadAvatar).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    expect(component.successMessage).toContain('Compte créé');
  }));

  it('should handle empty file input', () => {
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [] });

    component.onAvatarSelected({ target: input } as unknown as Event);

    expect(component.selectedAvatar).toBeNull();
  });
});
