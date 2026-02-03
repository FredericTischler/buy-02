import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Product as ProductModel, Page } from '../models/product.model';
import { HttpParams } from '@angular/common/http';
import { resolveApiBase } from '../utils/api-host';

@Injectable({
  providedIn: 'root',
})
export class Product {
  private readonly apiBase = resolveApiBase(8082);
  private readonly API_URL = `${this.apiBase}/api/products`;

  constructor(private readonly http: HttpClient) { }

  /**
   * Récupérer tous les produits
   */
  getAllProducts(): Observable<ProductModel[]> {
    return this.http.get<ProductModel[]>(this.API_URL);
  }

  /**
   * Récupérer un produit par ID
   */
  getProductById(id: string): Observable<ProductModel> {
    return this.http.get<ProductModel>(`${this.API_URL}/${id}`);
  }

  /**
   * Rechercher des produits par mot-clé
   */
  searchProducts(keyword: string): Observable<ProductModel[]> {
    return this.http.get<ProductModel[]>(`${this.API_URL}/search?keyword=${keyword}`);
  }

  /**
   * Récupérer les produits par catégorie
   */
  getProductsByCategory(category: string): Observable<ProductModel[]> {
    return this.http.get<ProductModel[]>(`${this.API_URL}/category/${category}`);
  }

  /**
   * Créer un nouveau produit (SELLER uniquement)
   */
  createProduct(product: Partial<ProductModel>): Observable<ProductModel> {
    return this.http.post<ProductModel>(this.API_URL, product);
  }

  /**
   * Mettre à jour un produit (SELLER uniquement)
   */
  updateProduct(id: string, product: Partial<ProductModel>): Observable<ProductModel> {
    return this.http.put<ProductModel>(`${this.API_URL}/${id}`, product);
  }

  /**
   * Supprimer un produit (SELLER uniquement)
   */
  deleteProduct(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  /**
   * Récupérer les produits du vendeur connecté
   */
  getMyProducts(): Observable<ProductModel[]> {
    return this.http.get<ProductModel[]>(`${this.API_URL}/seller/my-products`);
  }

  /**
   * Recherche avancée avec filtres et pagination
   */
  filterProducts(params: {
    keyword?: string;
    category?: string;
    minPrice?: number;
    maxPrice?: number;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }): Observable<Page<ProductModel>> {
    let httpParams = new HttpParams();
    if (params.keyword) httpParams = httpParams.set('keyword', params.keyword);
    if (params.category) httpParams = httpParams.set('category', params.category);
    if (params.minPrice != null) httpParams = httpParams.set('minPrice', params.minPrice.toString());
    if (params.maxPrice != null) httpParams = httpParams.set('maxPrice', params.maxPrice.toString());
    httpParams = httpParams.set('page', (params.page ?? 0).toString());
    httpParams = httpParams.set('size', (params.size ?? 12).toString());
    httpParams = httpParams.set('sortBy', params.sortBy ?? 'createdAt');
    httpParams = httpParams.set('sortDir', params.sortDir ?? 'desc');
    return this.http.get<Page<ProductModel>>(`${this.API_URL}/filter`, { params: httpParams });
  }
}
