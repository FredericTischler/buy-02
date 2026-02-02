import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { MediaService } from './media';

describe('MediaService', () => {
  let service: MediaService;
  let httpMock: HttpTestingController;
  const apiBase = 'http://localhost:8083/api/media';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(MediaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should upload media with form data', () => {
    const file = new File(['fake'], 'image.png', { type: 'image/png' });
    service.uploadMedia(file, 'product-1').subscribe();

    const req = httpMock.expectOne(`${apiBase}/upload`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    expect(req.request.body.get('productId')).toBe('product-1');
    req.flush({ id: 'media-1' });
  });

  it('should list media by product', () => {
    service.getMediaByProduct('product-1').subscribe();

    const req = httpMock.expectOne(`${apiBase}/product/product-1`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should delete media', () => {
    service.deleteMedia('media-1').subscribe();

    const req = httpMock.expectOne(`${apiBase}/media-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });

  it('should build full image url', () => {
    expect(service.getImageUrl('/api/media/file/image.png')).toBe('http://localhost:8083/api/media/file/image.png');
  });
});
