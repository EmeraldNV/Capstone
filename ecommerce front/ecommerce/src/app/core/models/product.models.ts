import { TagliaResponse } from './taglia.models';

export interface ProductImageResponse {
  id: number;
  imageUrl: string;
  altText?: string | null;
  primary: boolean;
  sortOrder: number;
}

export interface ProductSummaryResponse {
  id: number;
  productCode: string;
  slug: string;
  name: string;
  shortDescription?: string | null;
  listPrice: number;
  salePrice?: number | null;
  effectivePrice: number;
  currencyCode: string;
  stockQuantity: number;
  active: boolean;
  thumbnailUrl?: string | null;
  brandName?: string | null;
  categoryName?: string | null;
  categorySlug?: string | null;
  ipfApproved: boolean;
  taglie: TagliaResponse[];
  images: ProductImageResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface ProductDetailResponse {
  id: number;
  productCode: string;
  slug: string;
  name: string;
  shortDescription?: string | null;
  description?: string | null;
  sportType: string;
  gender: string;
  ageGroup: string;
  season?: string | null;
  material?: string | null;
  careInstructions?: string | null;
  taxable: boolean;
  ipfApproved: boolean;
  active: boolean;
  listPrice: number;
  salePrice?: number | null;
  effectivePrice: number;
  currencyCode: string;
  stockQuantity: number;
  brandId?: number | null;
  brandName?: string | null;
  categoryId?: number | null;
  categoryName?: string | null;
  categorySlug?: string | null;
  taglie: TagliaResponse[];
  images: ProductImageResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface ProductUpsertRequest {
  productCode?: string | null;
  slug?: string | null;
  name: string;
  shortDescription?: string | null;
  description?: string | null;
  sportType: string;
  gender: string;
  ageGroup: string;
  season?: string | null;
  material?: string | null;
  careInstructions?: string | null;
  taxable: boolean;
  ipfApproved: boolean;
  active: boolean;
  listPrice: number;
  salePrice?: number | null;
  currencyCode: string;
  stockQuantity: number;
  brandId?: number | null;
  categoryId?: number | null;
  tagliaIds: number[];
}
