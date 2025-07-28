import axios from 'axios';

const getBackendBaseUrl = () => {
    const {protocol, hostname} = window.location;
    return `${protocol}//${hostname}:8080/api`;
};

const api = axios.create({
    baseURL: getBackendBaseUrl(),
    timeout: 500000,
});


export default api;