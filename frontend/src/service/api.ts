import axios from 'axios';

const getBackendBaseUrl = () => {
    const {protocol, hostname} = window.location;
    if (hostname === 'localhost') return `http://localhost:8080/api`
    return `${protocol}//${hostname}/api`;
};

const api = axios.create({
    baseURL: getBackendBaseUrl(),
    timeout: 500000,
});


export default api;