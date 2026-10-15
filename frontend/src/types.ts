// Shapes of the API's answers (kept in step with the backend DTOs by hand).

export interface Me {
  id: number
  fullName: string
  email: string
  phoneNumber: string
}

export interface Vendor {
  id: number
  name: string
  contactPerson?: string
  phoneNumber: string
  location: string
  notes?: string
  active: boolean
  createdAt: string
}

export interface Coworker {
  id: number
  name: string
  phoneNumber: string
  location: string
  notes?: string
  active: boolean
  createdAt: string
}

export type RequestType = 'NEED_FOOD' | 'SURPLUS_FOOD'
export type RequestStatus = 'PENDING' | 'ACCEPTED' | 'COMPLETED' | 'REJECTED'

export interface FoodRequest {
  id: number
  type: RequestType
  status: RequestStatus
  contactName?: string
  contactPhone?: string
  location: string
  quantity: number
  details?: string
  mealsDelivered?: number
  note?: string
  createdAt: string
  updatedAt: string
  closedAt?: string
}

export interface DashboardSummary {
  totalMealsDelivered: number
  activeVendors: number
  activeCoworkers: number
  pendingRequests: number
}

export const TYPE_LABEL: Record<RequestType, string> = {
  NEED_FOOD: '🤝 Needs food',
  SURPLUS_FOOD: '🍛 Surplus food',
}

export const STATUS_LABEL: Record<RequestStatus, string> = {
  PENDING: 'Pending',
  ACCEPTED: 'Accepted (team to collect)',
  COMPLETED: 'Completed',
  REJECTED: 'Closed (unable)',
}

/** "30 people" / "80 meals" */
export function quantityText(r: Pick<FoodRequest, 'type' | 'quantity'>): string {
  return r.type === 'NEED_FOOD' ? `${r.quantity} people` : `${r.quantity} meals`
}
