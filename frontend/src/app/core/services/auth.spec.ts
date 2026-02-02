import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { Auth } from './auth';
import { Cart } from './cart';
import { AuthResponse } from '../models/user.model';

describe('Auth service', () => {
  let service: Auth;
  let httpMock: HttpTestingController;
  let cartSpy: jasmine.SpyObj<Cart>;
  const apiBase = 'http://localhost:8081/api/auth';

  beforeEach(() => {
    cartSpy = jasmine.createSpyObj<Cart>('Cart', ['loadCart', 'clearCartOnLogout']);
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [{ provide: Cart, useValue: cartSpy }],
    });
    localStorage.clear();
    service = TestBed.inject(Auth);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should login and persist token/user', () => {
    const response: AuthResponse = {
      token: 'jwt',
      userId: 'user-1',
      email: 'alice@mail.com',
      name: 'Alice',
      role: 'SELLER',
      avatar: '/avatar.png',
      type: 'Bearer',
    };

    service.login({ email: 'alice@mail.com', password: 'secret' }).subscribe(data => {
      expect(data).toEqual(response);
    });

    const req = httpMock.expectOne(`${apiBase}/login`);
    expect(req.request.method).toBe('POST');
    req.flush(response);

    expect(localStorage.getItem('auth_token')).toBe('jwt');
    expect(JSON.parse(localStorage.getItem('current_user')!).email).toBe('alice@mail.com');
    expect(cartSpy.loadCart).toHaveBeenCalled();
    expect(service.isLoggedIn()).toBeTrue();
    expect(service.isSeller()).toBeTrue();
  });

  it('should logout and clear storage', () => {
    localStorage.setItem('auth_token', 'jwt');
    localStorage.setItem('current_user', JSON.stringify({ id: 'user-1' }));

    service.logout();

    expect(localStorage.getItem('auth_token')).toBeNull();
    expect(localStorage.getItem('current_user')).toBeNull();
    expect(cartSpy.clearCartOnLogout).toHaveBeenCalled();
    expect(service.isLoggedIn()).toBeFalse();
  });

  it('should upload avatar and update cached user', () => {
    localStorage.setItem('current_user', JSON.stringify({ id: 'user-1', avatar: '/old.png' }));
    const file = new File(['img'], 'avatar.png', { type: 'image/png' });

    service.uploadAvatar(file).subscribe();

    const req = httpMock.expectOne('http://localhost:8081/api/users/avatar');
    expect(req.request.method).toBe('POST');
    req.flush({ avatarUrl: '/new.png' });

    expect(JSON.parse(localStorage.getItem('current_user')!).avatar).toBe('/new.png');
  });

  it('should expose current user even when none saved', () => {
    expect(service.getCurrentUser()).toBeNull();
    expect(service.isSeller()).toBeFalse();
  });
});
