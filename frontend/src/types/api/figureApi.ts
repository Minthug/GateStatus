// src/api/figureApi.ts
import axios from 'axios';
import { Figure } from '../types/figure';

const API_BASE_URL = 'http://localhost:8080/api/v1';

export const figureApi = {
    getFigures: () => axios.get<Figure[]>(`${API_BASE_URL}/figures`),
    getFigure: (id: number) => axios.get<Figure>(`${API_BASE_URL}/figures/${id}`),
    createFigure: (figure: Omit<Figure, 'id'>) =>
        axios.post<Figure>(`${API_BASE_URL}/figures`, figure),
    updateFigure: (id: number, figure: Partial<Figure>) =>
        axios.put<Figure>(`${API_BASE_URL}/figures/${id}`, figure),
    deleteFigure: (id: number) =>
        axios.delete(`${API_BASE_URL}/figures/${id}`),
};