// src/components/Layout.tsx
import { Box, AppBar, Toolbar, Typography } from '@mui/material';

export default function Layout({ children }: { children: React.ReactNode }) {
    return (
        <Box>
            <AppBar position="static">
                <Toolbar>
                    <Typography variant="h6">Figure Management</Typography>
                </Toolbar>
            </AppBar>
            <Box sx={{ p: 3 }}>
                {children}
            </Box>
        </Box>
    );
}