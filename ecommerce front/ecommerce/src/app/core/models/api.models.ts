export interface FieldErrorDetail {
  field: string;
  message: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  details: FieldErrorDetail[];
}

export interface UserResponse {
  id: number;
  email: string;
  status: string;
  emailVerified: boolean;
  roles: string[];
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
}

export interface RegistrationResponse {
  message: string;
  email: string;
  verificationRequired: boolean;
  verificationExpiresAt: string;
}

export interface VerifyEmailResponse {
  message: string;
  verified: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  confirmPassword: string;
}

export interface AddressRequest {
  label: string;
  recipientName: string;
  companyName?: string | null;
  phone?: string | null;
  line1: string;
  line2?: string | null;
  city: string;
  stateRegion?: string | null;
  postalCode: string;
  countryCode: string;
}

export interface AddressResponse {
  label: string;
  recipientName: string;
  companyName?: string | null;
  phone?: string | null;
  line1: string;
  line2?: string | null;
  city: string;
  stateRegion?: string | null;
  postalCode: string;
  countryCode: string;
  defaultAddress: boolean;
}

export interface UserProfileResponse {
  userId: number;
  email: string;
  emailVerified: boolean;
  status: string;
  firstName?: string | null;
  lastName?: string | null;
  companyName?: string | null;
  taxCode?: string | null;
  vatNumber?: string | null;
  birthDate?: string | null;
  phone?: string | null;
  marketingConsent: boolean;
  address?: AddressResponse | null;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  companyName?: string | null;
  taxCode?: string | null;
  vatNumber?: string | null;
  birthDate?: string | null;
  phone?: string | null;
  marketingConsent: boolean;
  address: AddressRequest;
}

export interface AdminCreateUserRequest {
  email: string;
  password: string;
  status?: string;
  emailVerified?: boolean;
  roleCodes: string[];
}

export interface AssignRolesRequest {
  roleCodes: string[];
}

export interface RoleAssignmentResponse {
  userId: number;
  email: string;
  assignedRoles: string[];
  message: string;
}

export interface AuthSession {
  accessToken: string;
  tokenType: string;
  expiresAt: number;
  user: UserResponse;
  roles: string[];
}

export interface AdminFilterOptionResponse {
  value: string;
  label: string;
}

export interface AdminDashboardFilterOptionsResponse {
  categories: AdminFilterOptionResponse[];
  paymentMethods: AdminFilterOptionResponse[];
  orderStatuses: string[];
  paymentStatuses: string[];
}

export interface AdminDashboardFilters {
  from?: string | null;
  to?: string | null;
  categoryId?: number | null;
  paymentMethodCode?: string | null;
  orderStatus?: string | null;
  paymentStatus?: string | null;
}

export interface SalesTrendPointResponse {
  date: string;
  revenue: number;
  orders: number;
}

export interface CategorySalesResponse {
  categoryId?: number | null;
  categoryName: string;
  revenue: number;
  quantity: number;
}

export interface TopProductResponse {
  productId: number;
  productName: string;
  slug: string;
  revenue: number;
  quantity: number;
}

export interface AdminDashboardResponse {
  from: string;
  to: string;
  currencyCode: string;
  totalRevenue: number;
  totalSales: number;
  totalCustomers: number;
  totalTransactions: number;
  averageTicket: number;
  salesTrend: SalesTrendPointResponse[];
  categoryBreakdown: CategorySalesResponse[];
  topProducts: TopProductResponse[];
}

export interface AdminUserResponse {
  id: number;
  email: string;
  status: string;
  emailVerified: boolean;
  roles: string[];
  createdAt: string;
  lastLoginAt?: string | null;
}

export interface AdminUserUpdateRequest {
  email?: string | null;
  status?: string | null;
  emailVerified?: boolean | null;
  roleCodes?: string[] | null;
}

export interface AdminAuditLogResponse {
  id: number;
  actionType: string;
  entityName: string;
  entityId: number;
  actorEmail?: string | null;
  ipAddress?: string | null;
  userAgent?: string | null;
  oldData?: string | null;
  newData?: string | null;
  createdAt: string;
}
