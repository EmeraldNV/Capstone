export interface CategoryNavigationResponse {
  id: number;
  categoryCode: string;
  name: string;
  slug: string;
  description?: string | null;
  sortOrder: number;
  parentId?: number | null;
  parentSlug?: string | null;
  children: CategoryNavigationResponse[];
}
