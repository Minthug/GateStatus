// src/App.tsx
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import FigureList from './pages/FigureList';
import FigureDetail from './pages/FigureDetail';
import FigureCreate from './pages/FigureCreate';

function App() {
    return (
        <Router>
            <Layout>
                <Routes>
                    <Route path="/" element={<FigureList />} />
                    <Route path="/figures/:id" element={<FigureDetail />} />
                    <Route path="/figures/create" element={<FigureCreate />} />
                </Routes>
            </Layout>
        </Router>
    );
}

export default App;