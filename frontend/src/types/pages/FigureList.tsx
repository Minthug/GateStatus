// src/pages/FigureList.tsx
import { useEffect, useState } from 'react';
import { Card, Grid, Typography } from '@mui/material';
import { figureApi } from '../api/figureApi';
import { Figure } from '../types/figure';

export default function FigureList() {
    const [figures, setFigures] = useState<Figure[]>([]);

    useEffect(() => {
        const fetchFigures = async () => {
            try {
                const response = await figureApi.getFigures();
                setFigures(response.data);
            } catch (error) {
                console.error('Failed to fetch figures:', error);
            }
        };

        fetchFigures();
    }, []);

    return (
        <Grid container spacing={3}>
            {figures.map((figure) => (
                <Grid item xs={12} sm={6} md={4} key={figure.id}>
                    <Card sx={{ p: 2 }}>
                        <Typography variant="h6">{figure.name}</Typography>
                        <Typography color="text.secondary">
                            {figure.englishName}
                        </Typography>
                        <Typography variant="body2">
                            {figure.place}
                        </Typography>
                    </Card>
                </Grid>
            ))}
        </Grid>
    );
}