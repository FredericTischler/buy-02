import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';

import { ProductFormDialog, DialogData } from './product-form-dialog';
import { Product as ProductService } from '../../../core/services/product';
import { MediaService } from '../../../core/services/media';
import { Product } from '../../../core/models/product.model';

describe('ProductFormDialog', () => {
  let component: ProductFormDialog;
  let fixture: ComponentFixture<ProductFormDialog>;
  let productService: jasmine.SpyObj<ProductService>;
  let mediaService: jasmine.SpyObj<MediaService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ProductFormDialog>>;

  const mockProduct: Product = {
    id: 'p1',
    name: 'Test Product',
    description: 'Test Description for product',
    price: 99.99,
    stock: 10,
    category: 'Electronics',
    sellerId: 's1',
    sellerName: 'Test Seller'
  };

  const createDialogData = (mode: 'create' | 'edit', product?: Product): DialogData => ({
    mode,
    product
  });

  const setupTestBed = async (dialogData: DialogData) => {
    productService = jasmine.createSpyObj<ProductService>('ProductService', [
      'createProduct',
      'updateProduct'
    ]);
    productService.createProduct.and.returnValue(of(mockProduct));
    productService.updateProduct.and.returnValue(of(mockProduct));

    mediaService = jasmine.createSpyObj<MediaService>('MediaService', [
      'getMediaByProduct',
      'getImageUrl',
      'uploadMedia',
      'deleteMedia'
    ]);
    mediaService.getMediaByProduct.and.returnValue(of([]));
    mediaService.getImageUrl.and.callFake((url: string) => `https://localhost:8083${url}`);
    mediaService.uploadMedia.and.returnValue(of({ id: 'm1', url: '/img.jpg' } as any));
    mediaService.deleteMedia.and.returnValue(of(undefined));

    dialogRef = jasmine.createSpyObj<MatDialogRef<ProductFormDialog>>('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [
        ProductFormDialog,
        NoopAnimationsModule,
        ReactiveFormsModule
      ],
      providers: [
        { provide: ProductService, useValue: productService },
        { provide: MediaService, useValue: mediaService },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: dialogData }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductFormDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  describe('Create Mode', () => {
    beforeEach(async () => {
      await setupTestBed(createDialogData('create'));
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize empty form in create mode', () => {
      expect(component.productForm.get('name')?.value).toBe('');
      expect(component.productForm.get('description')?.value).toBe('');
      expect(component.productForm.get('price')?.value).toBe('');
      expect(component.productForm.get('stock')?.value).toBe('');
      expect(component.productForm.get('category')?.value).toBe('');
    });

    it('should have required validators', () => {
      expect(component.productForm.valid).toBeFalse();

      component.productForm.get('name')?.setValue('');
      expect(component.productForm.get('name')?.hasError('required')).toBeTrue();
    });

    it('should validate minimum length for name', () => {
      component.productForm.get('name')?.setValue('ab');
      expect(component.productForm.get('name')?.hasError('minlength')).toBeTrue();

      component.productForm.get('name')?.setValue('abc');
      expect(component.productForm.get('name')?.hasError('minlength')).toBeFalse();
    });

    it('should validate minimum length for description', () => {
      component.productForm.get('description')?.setValue('short');
      expect(component.productForm.get('description')?.hasError('minlength')).toBeTrue();

      component.productForm.get('description')?.setValue('long enough description');
      expect(component.productForm.get('description')?.hasError('minlength')).toBeFalse();
    });

    it('should validate minimum price', () => {
      component.productForm.get('price')?.setValue(0);
      expect(component.productForm.get('price')?.hasError('min')).toBeTrue();

      component.productForm.get('price')?.setValue(0.01);
      expect(component.productForm.get('price')?.hasError('min')).toBeFalse();
    });

    it('should validate stock as integer', () => {
      component.productForm.get('stock')?.setValue('abc');
      expect(component.productForm.get('stock')?.hasError('pattern')).toBeTrue();

      component.productForm.get('stock')?.setValue('10');
      expect(component.productForm.get('stock')?.hasError('pattern')).toBeFalse();
    });

    it('should create product successfully', fakeAsync(() => {
      component.productForm.patchValue({
        name: 'New Product',
        description: 'New product description',
        price: 49.99,
        stock: 20,
        category: 'Electronics'
      });

      component.onSubmit();
      tick();

      expect(productService.createProduct).toHaveBeenCalledWith(jasmine.objectContaining({
        name: 'New Product',
        description: 'New product description',
        price: 49.99,
        stock: 20,
        category: 'Electronics'
      }));
      expect(dialogRef.close).toHaveBeenCalledWith(jasmine.objectContaining({ success: true }));
    }));

    it('should not submit invalid form', () => {
      component.productForm.get('name')?.setValue('');

      component.onSubmit();

      expect(productService.createProduct).not.toHaveBeenCalled();
    });

    it('should handle create product error', fakeAsync(() => {
      productService.createProduct.and.returnValue(throwError(() => ({ error: { message: 'Creation failed' } })));

      component.productForm.patchValue({
        name: 'New Product',
        description: 'New product description',
        price: 49.99,
        stock: 20,
        category: 'Electronics'
      });

      component.onSubmit();
      tick();

      expect(component.errorMessage).toContain('Creation failed');
      expect(component.loading).toBeFalse();
    }));
  });

  describe('Edit Mode', () => {
    beforeEach(async () => {
      await setupTestBed(createDialogData('edit', mockProduct));
    });

    it('should populate form with product data in edit mode', () => {
      expect(component.productForm.get('name')?.value).toBe('Test Product');
      expect(component.productForm.get('description')?.value).toBe('Test Description for product');
      expect(component.productForm.get('price')?.value).toBe(99.99);
      expect(component.productForm.get('stock')?.value).toBe(10);
      expect(component.productForm.get('category')?.value).toBe('Electronics');
    });

    it('should load existing images in edit mode', () => {
      expect(mediaService.getMediaByProduct).toHaveBeenCalledWith('p1');
    });

    it('should update product successfully', fakeAsync(() => {
      component.productForm.patchValue({
        name: 'Updated Product'
      });

      component.onSubmit();
      tick();

      expect(productService.updateProduct).toHaveBeenCalledWith('p1', jasmine.any(Object));
      expect(dialogRef.close).toHaveBeenCalledWith(jasmine.objectContaining({ success: true }));
    }));

    it('should delete marked images on update', fakeAsync(() => {
      component.existingImages = [{ id: 'img1', url: '/img1.jpg' }];
      component.imagesToDelete = ['img1'];

      component.onSubmit();
      tick();

      expect(mediaService.deleteMedia).toHaveBeenCalledWith('img1');
    }));
  });

  describe('File Handling', () => {
    beforeEach(async () => {
      await setupTestBed(createDialogData('create'));
    });

    it('should add valid image files', () => {
      const file = new File([''], 'test.jpg', { type: 'image/jpeg' });
      Object.defineProperty(file, 'size', { value: 1024 * 1024 });

      const event = { target: { files: [file] } } as unknown as Event;

      component.onFileSelected(event);

      expect(component.selectedFiles.length).toBe(1);
    });

    it('should reject non-image files', () => {
      const file = new File([''], 'test.pdf', { type: 'application/pdf' });

      const event = { target: { files: [file] } } as unknown as Event;

      component.onFileSelected(event);

      expect(component.errorMessage).toContain('n\'est pas une image');
      expect(component.selectedFiles.length).toBe(0);
    });

    it('should reject files larger than 2MB', () => {
      const file = new File([''], 'test.jpg', { type: 'image/jpeg' });
      Object.defineProperty(file, 'size', { value: 3 * 1024 * 1024 });

      const event = { target: { files: [file] } } as unknown as Event;

      component.onFileSelected(event);

      expect(component.errorMessage).toContain('trop grand');
    });

    it('should remove selected image', () => {
      component.selectedFiles = [new File([''], 'test.jpg', { type: 'image/jpeg' })];
      component.imagePreviews = ['data:image/jpeg;base64,...'];

      component.removeImage(0);

      expect(component.selectedFiles.length).toBe(0);
      expect(component.imagePreviews.length).toBe(0);
    });

    it('should mark existing image for deletion', () => {
      component.existingImages = [
        { id: 'img1', url: '/img1.jpg' },
        { id: 'img2', url: '/img2.jpg' }
      ];

      component.removeExistingImage('img1', 0);

      expect(component.imagesToDelete).toContain('img1');
      expect(component.existingImages.length).toBe(1);
    });

    it('should upload new images on submit', fakeAsync(() => {
      const file = new File([''], 'test.jpg', { type: 'image/jpeg' });
      component.selectedFiles = [file];

      component.productForm.patchValue({
        name: 'New Product',
        description: 'New product description',
        price: 49.99,
        stock: 20,
        category: 'Electronics'
      });

      component.onSubmit();
      tick();

      expect(mediaService.uploadMedia).toHaveBeenCalledWith(file, 'p1');
    }));
  });

  describe('Error Messages', () => {
    beforeEach(async () => {
      await setupTestBed(createDialogData('create'));
    });

    it('should return required error message', () => {
      component.productForm.get('name')?.setValue('');
      component.productForm.get('name')?.markAsTouched();

      expect(component.getErrorMessage('name')).toBe('Ce champ est requis');
    });

    it('should return minlength error message', () => {
      component.productForm.get('name')?.setValue('ab');
      component.productForm.get('name')?.markAsTouched();

      expect(component.getErrorMessage('name')).toContain('Minimum');
      expect(component.getErrorMessage('name')).toContain('caractères');
    });

    it('should return min error message', () => {
      component.productForm.get('price')?.setValue(0);
      component.productForm.get('price')?.markAsTouched();

      expect(component.getErrorMessage('price')).toContain('supérieure');
    });

    it('should return pattern error message', () => {
      component.productForm.get('stock')?.setValue('abc');
      component.productForm.get('stock')?.markAsTouched();

      expect(component.getErrorMessage('stock')).toContain('nombre entier');
    });
  });

  describe('Dialog Actions', () => {
    beforeEach(async () => {
      await setupTestBed(createDialogData('create'));
    });

    it('should close dialog on cancel', () => {
      component.onCancel();

      expect(dialogRef.close).toHaveBeenCalledWith({ success: false });
    });

    it('should have available categories', () => {
      expect(component.categories.length).toBeGreaterThan(0);
      expect(component.categories).toContain('Smartphones');
      expect(component.categories).toContain('Laptops');
    });
  });
});