import axios from 'axios';

const getBackendBaseUrl = () => {
    const {protocol, hostname} = window.location;
    if(hostname === 'localhost') return 'http://localhost:8080/api';
    return `${protocol}//${hostname}/api`;
    // return 'https://dnd.federicofin8.it/api'
};

const api = axios.create({
    baseURL: getBackendBaseUrl(),
    timeout: 500000,
});

// Bearer token su ogni richiesta
api.interceptors.request.use(cfg => {
    const token = localStorage.getItem('auth_token');
    if (token) cfg.headers.Authorization = `Bearer ${token}`;
    return cfg;
});

// 401 -> sessione scaduta/non valida: pulisci e torna al login
api.interceptors.response.use(
    res => res,
    err => {
        const status = err?.response?.status;
        const isLoginCall = String(err?.config?.url ?? '').includes('/auth/login');
        if (status === 401 && !isLoginCall) {
            localStorage.removeItem('auth_token');
            localStorage.removeItem('auth_utente');
            if (!window.location.pathname.startsWith('/login')) {
                window.location.href = '/login';
            }
        }
        return Promise.reject(err);
    }
);

export default api;