import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Product } from '../../../core/models/product.model';
import { Product as ProductService } from '../../../core/services/product';
import { MediaService } from '../../../core/services/media';

export interface DialogData {
  product?: Product;
  mode: 'create' | 'edit';
}

@Component({
  selector: 'app-product-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './product-form-dialog.html',
  styleUrl: './product-form-dialog.scss'
})
export class ProductFormDialog implements OnInit {
  productForm: FormGroup;
  loading = false;
  errorMessage = '';
  selectedFiles: File[] = [];
  imagePreviews: string[] = [];
  existingImages: any[] = []; // Images déjà sauvegardées
  imagesToDelete: string[] = []; // IDs des images à supprimer
  uploadProgress = 0;
  totalImages = 0;

  categories = [
    'Smartphones',
    'Laptops',
    'Tablets',
    'Accessories',
    'Headphones',
    'Cameras',
    'Gaming',
    'Wearables',
    'Other'
  ];

  constructor(
    private readonly fb: FormBuilder,
    private readonly productService: ProductService,
    private readonly mediaService: MediaService,
    public readonly dialogRef: MatDialogRef<ProductFormDialog>,
    @Inject(MAT_DIALOG_DATA) public data: DialogData
  ) {
    this.productForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      price: ['', [Validators.required, Validators.min(0.01)]],
      stock: ['', [Validators.required, Validators.min(0), Validators.pattern(/^\d+$/)]],
      category: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    if (this.data.mode === 'edit' && this.data.product) {
      this.productForm.patchValue({
        name: this.data.product.name,
        description: this.data.product.description,
        price: this.data.product.price,
        stock: this.data.product.stock,
        category: this.data.product.category
      });

      // Charger les images existantes
      this.loadExistingImages(this.data.product.id);
    }
  }

  loadExistingImages(productId: string): void {
    this.mediaService.getMediaByProduct(productId).subscribe({
      next: (media) => {
        this.existingImages = media.map(m => ({
          id: m.id,
          url: this.mediaService.getImageUrl(m.url)
        }));
      },
      error: (error) => {
        console.error('Erreur chargement images:', error);
      }
    });
  }

  removeExistingImage(imageId: string, index: number): void {
    this.imagesToDelete.push(imageId);
    this.existingImages.splice(index, 1);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      const files = Array.from(input.files);
      
      // Validate files
      for (const file of files) {
        // Check file type
        if (!file.type.startsWith('image/')) {
          this.errorMessage = `Le fichier ${file.name} n'est pas une image`;
          return;
        }
        
        // Check file size (max 2MB)
        if (file.size > 2 * 1024 * 1024) {
          this.errorMessage = `Le fichier ${file.name} est trop grand (max 2MB)`;
          return;
        }
      }

      // Add files to selection
      this.selectedFiles.push(...files);
      
      // Generate previews
      files.forEach(file => {
        const reader = new FileReader();
        reader.onload = (e: any) => {
          this.imagePreviews.push(e.target.result);
        };
        reader.readAsDataURL(file);
      });

      this.errorMessage = '';
    }
  }

  removeImage(index: number): void {
    this.selectedFiles.splice(index, 1);
    this.imagePreviews.splice(index, 1);
  }

  async onSubmit(): Promise<void> {
    if (this.productForm.invalid) {
      Object.keys(this.productForm.controls).forEach(key => {
        this.productForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    try {
      const productData = this.productForm.value;

      let productId: string;

      if (this.data.mode === 'edit' && this.data.product) {
        // Update existing product
        await this.productService.updateProduct(this.data.product.id, productData).toPromise();
        productId = this.data.product.id;

        // Supprimer les images marquées pour suppression
        if (this.imagesToDelete.length > 0) {
          for (const imageId of this.imagesToDelete) {
            await this.mediaService.deleteMedia(imageId).toPromise();
          }
        }
      } else {
        // Create new product
        const newProduct = await this.productService.createProduct(productData).toPromise();
        if (!newProduct) {
          throw new Error('Failed to create product');
        }
        productId = newProduct.id;
      }

      // Upload new images if any
      if (this.selectedFiles.length > 0) {
        this.totalImages = this.selectedFiles.length;
        this.uploadProgress = 0;

        for (let i = 0; i < this.selectedFiles.length; i++) {
          await this.mediaService.uploadMedia(this.selectedFiles[i], productId).toPromise();
          this.uploadProgress = i + 1;
        }
      }

      // Success
      this.dialogRef.close({ success: true, productId });

    } catch (error: any) {
      console.error('Error saving product:', error);
      this.errorMessage = error.error?.message || 'Une erreur est survenue lors de l\'enregistrement';
      this.loading = false;
    }
  }

  onCancel(): void {
    this.dialogRef.close({ success: false });
  }

  getErrorMessage(fieldName: string): string {
    const control = this.productForm.get(fieldName);
    if (control?.hasError('required')) {
      return 'Ce champ est requis';
    }
    if (control?.hasError('minlength')) {
      const minLength = control.errors?.['minlength'].requiredLength;
      return `Minimum ${minLength} caractères`;
    }
    if (control?.hasError('min')) {
      const min = control.errors?.['min'].min;
      return `La valeur doit être supérieure à ${min}`;
    }
    if (control?.hasError('pattern')) {
      return 'Veuillez entrer un nombre entier';
    }
    return '';
  }
}
