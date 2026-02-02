import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { authGuard } from './auth.guard';
import { loginGuard } from './login.guard';
import { sellerGuard } from './seller.guard';
import { Auth } from '../services/auth';

describe('Guards', () => {
  let authService: jasmine.SpyObj<Auth>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authService = jasmine.createSpyObj<Auth>('Auth', ['isLoggedIn', 'getCurrentUser', 'isSeller']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: Auth, useValue: authService },
        { provide: Router, useValue: router },
      ],
    });
  });

  it('authGuard should allow authenticated users', () => {
    authService.isLoggedIn.and.returnValue(true);
    const allowed = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
    expect(allowed).toBeTrue();
  });

  it('authGuard should redirect anonymous users', () => {
    authService.isLoggedIn.and.returnValue(false);
    const allowed = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
    expect(allowed).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('loginGuard should redirect logged-in sellers', () => {
    authService.isLoggedIn.and.returnValue(true);
    authService.getCurrentUser.and.returnValue({ role: 'SELLER' } as any);

    const allowed = TestBed.runInInjectionContext(() => loginGuard({} as any, {} as any));
    expect(allowed).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/seller/dashboard']);
  });

  it('loginGuard should redirect customers to catalog', () => {
    authService.isLoggedIn.and.returnValue(true);
    authService.getCurrentUser.and.returnValue({ role: 'CLIENT' } as any);

    const allowed = TestBed.runInInjectionContext(() => loginGuard({} as any, {} as any));
    expect(allowed).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/products']);
  });

  it('loginGuard should allow anonymous visitors', () => {
    authService.isLoggedIn.and.returnValue(false);
    const allowed = TestBed.runInInjectionContext(() => loginGuard({} as any, {} as any));
    expect(allowed).toBeTrue();
  });

  it('sellerGuard should allow logged-in sellers', () => {
    authService.isLoggedIn.and.returnValue(true);
    authService.isSeller.and.returnValue(true);

    const allowed = TestBed.runInInjectionContext(() => sellerGuard({} as any, {} as any));
    expect(allowed).toBeTrue();
  });

  it('sellerGuard should redirect anonymous users', () => {
    authService.isLoggedIn.and.returnValue(false);
    const allowed = TestBed.runInInjectionContext(() => sellerGuard({} as any, {} as any));
    expect(allowed).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('sellerGuard should redirect customers to catalog', () => {
    authService.isLoggedIn.and.returnValue(true);
    authService.isSeller.and.returnValue(false);

    const allowed = TestBed.runInInjectionContext(() => sellerGuard({} as any, {} as any));
    expect(allowed).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/products']);
  });
});
