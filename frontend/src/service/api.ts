import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api', // adatta il path al tuo backend
    timeout: 10000,
});

export default api;
