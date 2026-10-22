/** API shapes, kept in one place per domain contract (see contracts/openapi). */
export interface Customer {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
}

export interface Product {
  id: string;
  sku: string;
  name: string;
  description: string;
  price: number;
  currency: string;
  active: boolean;
}

export interface ProductPage {
  content: Product[];
  page: number;
  size: number;
  totalElements: number;
}

export interface CartLine {
  productId: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Cart {
  id: string;
  customerId: string;
  currency: string;
  total: number;
  items: CartLine[];
}

export interface ApiProblem {
  status: number;
  detail?: string;
  code?: string;
}
