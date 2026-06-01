export interface CartItem {
  productId: number;
  productCode: string;
  slug: string;
  name: string;
  price: number;
  currencyCode: string;
  imageUrl?: string | null;
  shortDescription?: string | null;
  brandName?: string | null;
  categoryName?: string | null;
  categorySlug?: string | null;
  quantity: number;
}
