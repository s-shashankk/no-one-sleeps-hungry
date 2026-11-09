import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { AuthProvider } from './auth'
import { TextsProvider } from './texts'
import './styles.css'
import './polish.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <TextsProvider>
          <App />
        </TextsProvider>
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>,
)
