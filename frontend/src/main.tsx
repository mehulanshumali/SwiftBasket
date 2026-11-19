import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import { createRouter, RouterProvider } from '@tanstack/react-router'

import { Toaster } from '@/components/ui/sonner'
import { AuthProvider } from '@/context/AuthContext'
import { CartProvider } from '@/context/CartContext'
import { routeTree } from '@/routeTree.gen'

import './index.css'

const router = createRouter({ routeTree })

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <CartProvider>
        <RouterProvider router={router} />
      </CartProvider>
    </AuthProvider>
    <Toaster position="bottom-center" richColors />
  </StrictMode>,
)
