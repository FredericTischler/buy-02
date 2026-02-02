import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Media } from '../models/media.model';
import { resolveApiBase } from '../utils/api-host';

@Injectable({
  providedIn: 'root',
})
export class MediaService {
  private readonly apiBase = resolveApiBase(8083);
  private readonly API_URL = `${this.apiBase}/api/media`;

  constructor(private readonly http: HttpClient) { }

  /**
   * Uploader une image pour un produit
   */
  uploadMedia(file: File, productId: string): Observable<Media> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('productId', productId);
    
    return this.http.post<Media>(`${this.API_URL}/upload`, formData);
  }

  /**
   * Récupérer toutes les images d'un produit
   */
  getMediaByProduct(productId: string): Observable<Media[]> {
    return this.http.get<Media[]>(`${this.API_URL}/product/${productId}`);
  }

  /**
   * Supprimer une image
   */
  deleteMedia(mediaId: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${mediaId}`);
  }

  /**
   * Construire l'URL complète d'une image
   */
  getImageUrl(url: string): string {
    return `${this.apiBase}${url}`;
  }
}
