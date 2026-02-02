import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { WishlistService } from './wishlist';

describe('WishlistService', () => {
  let service: WishlistService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [WishlistService]
    });

    service = TestBed.inject(WishlistService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('loadWishlist', () => {
    it('should load wishlist and update subject', () => {
      const mockWishlist = ['p1', 'p2', 'p3'];

      service.loadWishlist();

      const req = httpMock.expectOne(req => req.url.includes('/api/users/wishlist'));
      expect(req.request.method).toBe('GET');
      req.flush(mockWishlist);

      service.wishlist$.subscribe(wishlist => {
        expect(wishlist).toEqual(mockWishlist);
      });
    });

    it('should handle error and set empty wishlist', () => {
      service.loadWishlist();

      const req = httpMock.expectOne(req => req.url.includes('/api/users/wishlist'));
      req.error(new ErrorEvent('Network error'));

      service.wishlist$.subscribe(wishlist => {
        expect(wishlist).toEqual([]);
      });
    });
  });

  describe('getWishlist', () => {
    it('should get wishlist and update subject', (done) => {
      const mockWishlist = ['p1', 'p2'];

      service.getWishlist().subscribe(wishlist => {
        expect(wishlist).toEqual(mockWishlist);
        done();
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/users/wishlist'));
      expect(req.request.method).toBe('GET');
      req.flush(mockWishlist);
    });
  });

  describe('getWishlistIds', () => {
    it('should get wishlist ids without updating subject', (done) => {
      const mockWishlist = ['p1', 'p2'];

      service.getWishlistIds().subscribe(wishlist => {
        expect(wishlist).toEqual(mockWishlist);
        done();
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/users/wishlist'));
      expect(req.request.method).toBe('GET');
      req.flush(mockWishlist);
    });
  });

  describe('addToWishlist', () => {
    it('should add product to wishlist', (done) => {
      const response = { message: 'Added', wishlist: ['p1', 'p2'] };

      service.addToWishlist('p2').subscribe(result => {
        expect(result).toEqual(response);
        done();
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/users/wishlist/p2'));
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(response);
    });

    it('should update subject after adding', (done) => {
      const response = { message: 'Added', wishlist: ['p1', 'p2'] };

      service.addToWishlist('p2').subscribe(() => {
        service.wishlist$.subscribe(wishlist => {
          expect(wishlist).toEqual(['p1', 'p2']);
          done();
        });
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/users/wishlist/p2'));
      req.flush(response);
    });
  });

  describe('removeFromWishlist', () => {
    it('should remove product from wishlist', (done) => {
      const response = { message: 'Removed', wishlist: ['p1'] };

      service.removeFromWishlist('p2').subscribe(result => {
        expect(result).toEqual(response);
        done();
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/users/wishlist/p2'));
      expect(req.request.method).toBe('DELETE');
      req.flush(response);
    });

    it('should update subject after removing', (done) => {
      const response = { message: 'Removed', wishlist: ['p1'] };

      service.removeFromWishlist('p2').subscribe(() => {
        service.wishlist$.subscribe(wishlist => {
          expect(wishlist).toEqual(['p1']);
          done();
        });
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/users/wishlist/p2'));
      req.flush(response);
    });
  });

  describe('isInWishlist', () => {
    it('should check if product is in wishlist', (done) => {
      const response = { inWishlist: true };

      service.isInWishlist('p1').subscribe(result => {
        expect(result).toEqual(response);
        done();
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/users/wishlist/check/p1'));
      expect(req.request.method).toBe('GET');
      req.flush(response);
    });
  });

  describe('isInWishlistSync', () => {
    it('should check local wishlist state', () => {
      // Set up initial state
      const response = { message: 'Added', wishlist: ['p1', 'p2'] };
      service.addToWishlist('p2').subscribe();
      const req = httpMock.expectOne(req => req.url.includes('/api/users/wishlist/p2'));
      req.flush(response);

      expect(service.isInWishlistSync('p1')).toBeTrue();
      expect(service.isInWishlistSync('p2')).toBeTrue();
      expect(service.isInWishlistSync('p3')).toBeFalse();
    });
  });

  describe('toggleWishlist', () => {
    it('should add product if not in wishlist', (done) => {
      const addResponse = { message: 'Added', wishlist: ['p1'] };

      service.toggleWishlist('p1').subscribe(result => {
        expect(result.message).toBe('Added');
        done();
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/users/wishlist/p1') && req.method === 'POST');
      req.flush(addResponse);
    });

    it('should remove product if already in wishlist', (done) => {
      // First add the product
      const addResponse = { message: 'Added', wishlist: ['p1'] };
      service.addToWishlist('p1').subscribe();
      const addReq = httpMock.expectOne(req => req.url.includes('/api/users/wishlist/p1') && req.method === 'POST');
      addReq.flush(addResponse);

      // Then toggle (should remove)
      const removeResponse = { message: 'Removed', wishlist: [] };
      service.toggleWishlist('p1').subscribe(result => {
        expect(result.message).toBe('Removed');
        done();
      });

      const removeReq = httpMock.expectOne(req => req.url.includes('/api/users/wishlist/p1') && req.method === 'DELETE');
      removeReq.flush(removeResponse);
    });
  });

  describe('clearWishlist', () => {
    it('should clear entire wishlist', (done) => {
      const response = { message: 'Wishlist cleared' };

      service.clearWishlist().subscribe(result => {
        expect(result).toEqual(response);
        done();
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/users/wishlist') && req.method === 'DELETE');
      req.flush(response);
    });

    it('should reset subject after clearing', (done) => {
      const response = { message: 'Wishlist cleared' };

      service.clearWishlist().subscribe(() => {
        service.wishlist$.subscribe(wishlist => {
          expect(wishlist).toEqual([]);
          done();
        });
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/users/wishlist') && req.method === 'DELETE');
      req.flush(response);
    });
  });

  describe('getWishlistCount', () => {
    it('should return correct count', () => {
      // Set up initial state
      const response = { message: 'Added', wishlist: ['p1', 'p2', 'p3'] };
      service.addToWishlist('p3').subscribe();
      const req = httpMock.expectOne(req => req.url.includes('/api/users/wishlist/p3'));
      req.flush(response);

      expect(service.getWishlistCount()).toBe(3);
    });

    it('should return 0 for empty wishlist', () => {
      expect(service.getWishlistCount()).toBe(0);
    });
  });

  describe('resetWishlist', () => {
    it('should reset wishlist to empty array', (done) => {
      // First add some items
      const response = { message: 'Added', wishlist: ['p1', 'p2'] };
      service.addToWishlist('p2').subscribe();
      const req = httpMock.expectOne(req => req.url.includes('/api/users/wishlist/p2'));
      req.flush(response);

      // Then reset
      service.resetWishlist();

      service.wishlist$.subscribe(wishlist => {
        expect(wishlist).toEqual([]);
        done();
      });
    });
  });
});