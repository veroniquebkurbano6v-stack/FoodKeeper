export interface TPage<T = any> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface Result<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface FoodAddDTO {
  name: string
  categoryId: number
  quantity: number
  unit: string
  purchaseDate: string
  expiryDays: number
  storageZone: string
  price?: string
  totalPrice?: string
}

export interface FoodUpdateDTO {
  id: number
  name?: string
  quantity?: number
  expiryDate?: string
  storageZone?: string
}

export interface QuickAddDTO {
  favoriteFoodId: number
  quantity: number
  purchaseDate?: string
  expiryDays?: number
  price?: string
  totalPrice?: string
}

export interface FavoriteFoodAddDTO {
  name: string
  categoryId: number
  unit: string
  storageZone: string
  defaultExpiryDays?: number
  defaultPrice?: string
}

export interface FoodDetailVO {
  id: number
  name: string
  categoryName: string
  quantity: number
  unit: string
  purchaseDate: string
  expiryDate: string
  remainingDays: number
  expiryStatus: string
  storageZone: string
  totalPrice: string
}

export interface FoodListVO {
  id: number
  name: string
  categoryId: number
  quantity: number
  unit: string
  expiryDate: string
  remainingDays: number
  expiryStatus: string
  storageZone: string
}

export interface FavoriteFoodListVO {
  id: number
  name: string
  categoryName: string
  categoryId: number
  unit: string
  storageZone: string
  storageZoneName: string
  defaultExpiryDays?: number
  defaultPrice?: string
  useCount: number
}