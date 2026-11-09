// Shapes of the API's answers (kept in step with the backend DTOs by hand).

export interface Me {
  id: number
  fullName: string
  email: string
  phoneNumber: string
  role: Role
}

export type Role = 'ADMIN' | 'VOLUNTEER' | 'HOTEL'

export interface Vendor {
  id: number
  name: string
  contactPerson?: string
  phoneNumber: string
  location: string
  notes?: string
  areaId?: number
  areaName?: string
  logoVersion?: number
  dailyLimit?: number
  mapUrl?: string
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
  newPickups: number
  newJoinRequests: number
  openAreas: number
  cities: number
  volunteers: number
  newMessages: number
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

// ---- areas, hotel pickups, joining, public numbers ----

export interface Area {
  id: number
  city: string
  name: string
  open: boolean
}

export interface PublicArea extends Area {
  hotels: number
}

export interface PublicHotel {
  id: number
  name: string
  location: string
  /** how many more volunteers can book this hotel today */
  spotsLeft: number
  /** the hotel's own map link, if the admin added one */
  mapUrl?: string
}

export interface AreaImpact extends PublicArea {
  pickupMeals: number
}

export interface Impact {
  mealsServed: number
  cities: number
  areasOpen: number
  hotelPartners: number
  pickupsCompleted: number
  areas: AreaImpact[]
}

export type PickupStatus = 'PLEDGED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED'

export const PICKUP_LABEL: Record<PickupStatus, string> = {
  PLEDGED: 'New',
  CONFIRMED: 'Hotel informed',
  COMPLETED: 'Done',
  CANCELLED: 'Cancelled',
}

export interface Pickup {
  id: number
  status: PickupStatus
  vendorId: number
  vendorName: string
  vendorPhone: string
  vendorLocation: string
  areaId: number
  areaName: string
  city: string
  volunteerUserId?: number
  volunteerEmail?: string
  volunteerName: string
  volunteerPhone: string
  deliverTo?: string
  peopleCount: number
  platesServed?: number
  note?: string
  createdAt: string
  closedAt?: string
  closedBy?: 'ADMIN' | 'HOTEL'
}

export type JoinStatus = 'NEW' | 'CONTACTED'

export interface JoinRequest {
  id: number
  status: JoinStatus
  name: string
  phoneNumber: string
  email?: string
  location?: string
  message?: string
  createdAt: string
  contactedAt?: string
}

/** A partner hotel on the home-page slider. logoVersion is missing when the hotel has no logo yet. */
export interface PublicPartner {
  id: number
  name: string
  areaName?: string
  logoVersion?: number
}

export function logoUrl(id: number, version?: number): string {
  return `/api/v1/public/hotels/${id}/logo?v=${version ?? 0}`
}

// ---- volunteer accounts ----

export interface VolunteerRow {
  id: number
  name: string
  email: string
  phone: string
  active: boolean
  joinedAt: string
  lastLoginAt?: string
  pickups: number
  deliveries: number
  platesServed: number
  peopleReported: number
  role: Role
  hotelId?: number
  hotelName?: string
}

export interface MySummary {
  pickups: number
  deliveries: number
  platesServed: number
  peopleReported: number
  inProgress: number
}

/** A volunteer's own pickup (no hotel phone, no admin notes). */
export interface VolunteerPickup {
  id: number
  status: PickupStatus
  hotelName: string
  hotelLocation: string
  hotelMapUrl?: string
  areaName: string
  peopleCount: number
  deliverTo?: string
  platesServed?: number
  createdAt: string
  closedAt?: string
  closedBy?: 'ADMIN' | 'HOTEL'
}

// ---- hotel staff ----

export interface HotelSummary {
  hotelName: string
  hotelLocation: string
  dailyLimit: number
  bookedToday: number
  waiting: number
}

export interface HotelPickup {
  id: number
  status: PickupStatus
  volunteerName: string
  volunteerPhone: string
  peopleCount: number
  deliverTo?: string
  closedBy?: 'ADMIN' | 'HOTEL'
  createdAt: string
  closedAt?: string
}

// ---- feedback and messages to the admin ----

export type FeedbackKind = 'FEEDBACK' | 'MESSAGE'

export interface MyMessage {
  id: number
  kind: FeedbackKind
  rating?: number
  body: string
  status: 'NEW' | 'DONE'
  createdAt: string
}

export interface AdminMessage extends MyMessage {
  senderId: number
  senderName: string
  senderEmail: string
  senderPhone: string
  senderRole: Role
  hotelName?: string
  handledAt?: string
}
