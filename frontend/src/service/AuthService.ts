import api from './api'
import {AxiosResponse} from 'axios'
import {Home} from '../models/dto/Auth'

export function getHome(): Promise<AxiosResponse<Home>> {
    return api.get<Home>('/home')
}
