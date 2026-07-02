import request from '@/utils/request'
import type { FoodAddDTO, FoodUpdateDTO, FoodDetailVO, FoodListVO, Result, QuickAddDTO, FavoriteFoodAddDTO, FavoriteFoodListVO } from '@/types'
import type { Page } from '@/types/page'

export function addFood(data: FoodAddDTO): Promise<Result<number>> {
  return request.post('/food/add', data)
}

export function quickAddFood(data: QuickAddDTO): Promise<Result<number>> {
  return request.post('/food/quick-add', data)
}

export function updateFood(data: FoodUpdateDTO): Promise<Result<boolean>> {
  return request.put('/food/update', data)
}

export function deleteFood(id: number): Promise<Result<boolean>> {
  return request.delete(`/food/delete/${id}`)
}

export function getFoodDetail(id: number): Promise<Result<FoodDetailVO>> {
  return request.get(`/food/detail/${id}`)
}

export function pageFoodList(params: {
  categoryId?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
}): Promise<Result<Page<FoodListVO>>> {
  return request.get('/food/list', { params })
}

export function listExpiringFood(days?: number): Promise<Result<FoodListVO[]>> {
  return request.get('/food/expiring', { params: { days } })
}

export function outboundFood(id: number, quantity: number): Promise<Result<boolean>> {
  return request.post(`/food/outbound/${id}`, null, { params: { quantity } })
}

export function addFavoriteFood(data: FavoriteFoodAddDTO): Promise<Result<number>> {
  return request.post('/favorite/add', data)
}

export function deleteFavoriteFood(id: number): Promise<Result<boolean>> {
  return request.delete(`/favorite/delete/${id}`)
}

export function listFavoriteFoods(): Promise<Result<FavoriteFoodListVO[]>> {
  return request.get('/favorite/list')
}

export function getFavoriteFoodByName(name: string): Promise<Result<FavoriteFoodListVO>> {
  return request.get(`/favorite/search/${encodeURIComponent(name)}`)
}