import api from './api';

export const getPersonaggioById = (id) => {
    return api.get(`/personaggi/${id}`);
};
