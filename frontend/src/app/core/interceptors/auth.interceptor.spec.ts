import { HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { of } from 'rxjs';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  beforeEach(() => localStorage.clear());

  it('should forward original request when token missing', (done) => {
    const request = new HttpRequest('GET', '/test');
    const next: HttpHandlerFn = (req) => {
      expect(req).toBe(request);
      return of({} as HttpEvent<unknown>);
    };

    authInterceptor(request, next).subscribe(() => done());
  });

  it('should attach authorization header when token exists', (done) => {
    localStorage.setItem('auth_token', 'jwt');
    const request = new HttpRequest('GET', '/test');
    const next: HttpHandlerFn = (req) => {
      expect(req.headers.get('Authorization')).toBe('Bearer jwt');
      return of({} as HttpEvent<unknown>);
    };

    authInterceptor(request, next).subscribe(() => done());
  });
});
