import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { Product } from './product';
import { Product as ProductModel } from '../models/product.model';

describe('Product service', () => {
  let service: Product;
  let httpMock: HttpTestingController;
  const apiBase = 'http://localhost:8082/api/products';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(Product);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch all products', () => {
    const mock: ProductModel[] = [{ id: '1', name: 'Phone' } as ProductModel];

    service.getAllProducts().subscribe(products => {
      expect(products).toEqual(mock);
    });

    const req = httpMock.expectOne(apiBase);
    expect(req.request.method).toBe('GET');
    req.flush(mock);
  });

  it('should fetch product by id', () => {
    service.getProductById('123').subscribe(product => {
      expect(product.id).toBe('123');
    });

    const req = httpMock.expectOne(`${apiBase}/123`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: '123' });
  });

  it('should search products', () => {
    service.searchProducts('phone').subscribe();
    const req = httpMock.expectOne(`${apiBase}/search?keyword=phone`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should fetch products by category', () => {
    service.getProductsByCategory('tech').subscribe();
    const req = httpMock.expectOne(`${apiBase}/category/tech`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should create product', () => {
    const payload = { name: 'Phone' };
    service.createProduct(payload).subscribe();
    const req = httpMock.expectOne(apiBase);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ id: '1' });
  });

  it('should update product', () => {
    const payload = { name: 'Updated' };
    service.updateProduct('1', payload).subscribe();
    const req = httpMock.expectOne(`${apiBase}/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(payload);
    req.flush({ id: '1' });
  });

  it('should delete product', () => {
    service.deleteProduct('1').subscribe();
    const req = httpMock.expectOne(`${apiBase}/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });

  it('should fetch seller products', () => {
    service.getMyProducts().subscribe();
    const req = httpMock.expectOne(`${apiBase}/seller/my-products`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
