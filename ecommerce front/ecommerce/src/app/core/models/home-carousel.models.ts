export interface HomeCarouselImageResponse {
  id: number;
  imageUrl: string;
  altText?: string | null;
  sortOrder: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}
