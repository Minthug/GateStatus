// src/types/figure.ts
export interface Figure {
    id: number;
    name: string;
    englishName: string;
    birth: string;
    place: string;
    profileUrl: string;
    figureType: string;
    education: string[];
    careers: Career[];
    sites: string[];
    activities: string[];
    updateSource: string;
    tags: Tag[];
}

export interface Career {
    period: string;
    description: string;
}

export interface Tag {
    id: number;
    name: string;
}