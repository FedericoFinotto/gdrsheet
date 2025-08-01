import api from './api';

export const getPersonaggioById = (id) => {
    return api.get(`/personaggi/${id}`);
};

export const getModificatoriPersonaggioById = (id) => {
    return api.get(`/personaggi/modificatori/${id}`);
};

export const getAllPersonaggioItemsDTOByIdPersonaggio = (id) => {
    return api.get(`/personaggi/items/${id}`);
};

export const getItem = (id) => {
    return api.get(`/item/${id}`);
};
