'use client';

import Link from 'next/link';
import {
  ShoppingBag,
  Home,
  History,
  Settings,
  LogOut,
  ShoppingCart,
} from 'lucide-react';

interface SidebarProps {
  activeTab: string;
  cartCount: number;
  onCartClick: () => void;
}

export default function Sidebar({ activeTab, cartCount, onCartClick }: SidebarProps) {
  return (
    <aside className="fixed left-0 top-0 h-screen w-24 bg-gradient-to-b from-sidebar via-sidebar to-sidebar border-r border-sidebar-border flex flex-col items-center justify-between py-6 px-3 z-50">
      {/* Top Section */}
      <div className="flex flex-col items-center gap-8">
        {/* Logo - Enhanced */}
        <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-gradient-to-br from-primary to-accent text-primary-foreground shadow-lg shadow-primary/30 hover:shadow-primary/50 transition-all hover:scale-105">
          <ShoppingBag className="h-7 w-7" />
        </div>

        {/* Divider */}
        <div className="h-px w-12 bg-gradient-to-r from-transparent via-sidebar-border to-transparent"></div>

        {/* Navigation */}
        <nav className="flex flex-col gap-3">
          <Link
            href="/"
            className={`group relative flex h-12 w-12 items-center justify-center rounded-xl font-medium transition-all duration-300 ${
              activeTab === 'products'
                ? 'bg-gradient-to-br from-primary to-accent text-primary-foreground shadow-lg shadow-primary/30'
                : 'text-sidebar-foreground hover:bg-sidebar-accent/20'
            }`}
            title="Products"
          >
            <Home className="h-5 w-5" />
            <span className="absolute left-16 top-3 hidden whitespace-nowrap rounded-lg bg-card px-3 py-1 text-sm font-medium text-foreground shadow-lg group-hover:block">
              Products
            </span>
          </Link>

          <Link
            href="/history"
            className={`group relative flex h-12 w-12 items-center justify-center rounded-xl font-medium transition-all duration-300 ${
              activeTab === 'history'
                ? 'bg-gradient-to-br from-primary to-accent text-primary-foreground shadow-lg shadow-primary/30'
                : 'text-sidebar-foreground hover:bg-sidebar-accent/20'
            }`}
            title="Purchase History"
          >
            <History className="h-5 w-5" />
            <span className="absolute left-16 top-3 hidden whitespace-nowrap rounded-lg bg-card px-3 py-1 text-sm font-medium text-foreground shadow-lg group-hover:block">
              History
            </span>
          </Link>

          <button
            onClick={onCartClick}
            className="group relative flex h-12 w-12 items-center justify-center rounded-xl text-sidebar-foreground hover:bg-sidebar-accent/20 transition-all duration-300"
            title="Shopping Cart"
          >
            <ShoppingCart className="h-5 w-5" />
            {cartCount > 0 && (
              <span className="absolute -top-2 -right-2 flex h-6 w-6 items-center justify-center rounded-full bg-gradient-to-br from-accent to-pink-500 text-xs font-bold text-accent-foreground shadow-lg">
                {cartCount}
              </span>
            )}
            <span className="absolute left-16 top-3 hidden whitespace-nowrap rounded-lg bg-card px-3 py-1 text-sm font-medium text-foreground shadow-lg group-hover:block">
              Cart
            </span>
          </button>
        </nav>
      </div>

      {/* Bottom Section */}
      <div className="flex flex-col gap-3">
        <button
          className="group relative flex h-12 w-12 items-center justify-center rounded-xl text-sidebar-foreground hover:bg-sidebar-accent/20 transition-all duration-300"
          title="Settings"
        >
          <Settings className="h-5 w-5" />
          <span className="absolute left-16 bottom-3 hidden whitespace-nowrap rounded-lg bg-card px-3 py-1 text-sm font-medium text-foreground shadow-lg group-hover:block">
            Settings
          </span>
        </button>

        <button
          className="group relative flex h-12 w-12 items-center justify-center rounded-xl text-sidebar-foreground hover:bg-red-500/20 transition-all duration-300"
          title="Logout"
        >
          <LogOut className="h-5 w-5" />
          <span className="absolute left-16 bottom-1 hidden whitespace-nowrap rounded-lg bg-card px-3 py-1 text-sm font-medium text-foreground shadow-lg group-hover:block">
            Logout
          </span>
        </button>
      </div>
    </aside>
  );
}
