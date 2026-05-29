import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

/**
 * Entry point for the Solace Publisher UI application.
 * Initializes the React root and renders the main App component.
 */
createRoot(document.getElementById('root')!).render(<App />);
