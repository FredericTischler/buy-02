import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { Login } from './login';
import { Auth } from '../../../core/services/auth';

describe('Login', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let authSpy: jasmine.SpyObj<Auth>;
  let router: Router;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj<Auth>('Auth', ['login']);
    await TestBed.configureTestingModule({
      imports: [
        Login,
        HttpClientTestingModule,
        RouterTestingModule.withRoutes([]),
        NoopAnimationsModule,
      ],
      providers: [{ provide: Auth, useValue: authSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not submit when form invalid', () => {
    component.onSubmit();
    expect(authSpy.login).not.toHaveBeenCalled();
  });

  it('should route sellers to dashboard on success', () => {
    component.loginForm.setValue({ email: 'seller@mail.com', password: 'secret' });
    authSpy.login.and.returnValue(of({
      token: 'jwt',
      userId: '1',
      email: 'seller@mail.com',
      name: 'Seller',
      role: 'SELLER',
      avatar: '',
      type: 'Bearer',
    }));

    component.onSubmit();

    expect(router.navigate).toHaveBeenCalledWith(['/seller/dashboard']);
    expect(component.loading).toBeFalse();
  });

  it('should show error when authentication fails', () => {
    component.loginForm.setValue({ email: 'client@mail.com', password: 'secret' });
    authSpy.login.and.returnValue(throwError(() => ({ error: { error: 'Invalid credentials' } })));

    component.onSubmit();

    expect(component.errorMessage).toBe('Invalid credentials');
    expect(component.loading).toBeFalse();
  });

  it('should route clients to catalog', () => {
    component.loginForm.setValue({ email: 'client@mail.com', password: 'secret' });
    authSpy.login.and.returnValue(of({
      token: 'jwt',
      userId: '1',
      email: 'client@mail.com',
      name: 'Client',
      role: 'CLIENT',
      avatar: '',
      type: 'Bearer',
    }));

    component.onSubmit();

    expect(router.navigate).toHaveBeenCalledWith(['/products']);
  });
});
