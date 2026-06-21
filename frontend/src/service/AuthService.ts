import api from './api'
import {AxiosResponse} from 'axios'
import {Home, LoginResponse, UtenteAdmin} from '../models/dto/Auth'

export function getHome(): Promise<AxiosResponse<Home>> {
    return api.get<Home>('/home')
}

export function changePassword(newPassword: string, oldPassword?: string): Promise<AxiosResponse<void>> {
    return api.post<void>('/users/me/password', {newPassword, oldPassword})
}

export function listUsers(): Promise<AxiosResponse<UtenteAdmin[]>> {
    return api.get<UtenteAdmin[]>('/users')
}

export function createUser(username: string, name: string, ruolo: string): Promise<AxiosResponse<UtenteAdmin>> {
    return api.post<UtenteAdmin>('/users', {username, name, ruolo})
}

export function impersonate(id: number): Promise<AxiosResponse<LoginResponse>> {
    return api.post<LoginResponse>(`/users/${id}/impersonate`, {})
}
