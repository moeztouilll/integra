'use client';

import { useState } from 'react';
import Link from 'next/link';
import Sidebar from '@/components/sidebar';
import ProductCard from '@/components/product-card';
import Cart from '@/components/cart';

interface Product {
  id: number;
  title: string;
  price: number;
  image: string;
  category: string;
  gradient: string;
  description: string;
}

const products: Product[] = [
  {
    id: 1,
    title: 'Wireless Headphones',
    price: 199.99,
    image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&h=500&fit=crop',
    category: 'Audio',
    gradient: 'from-purple-600 via-pink-500 to-red-500',
    description: 'Premium noise-cancelling headphones with 30-hour battery',
  },
  {
    id: 2,
    title: 'Smart Watch Pro',
    price: 349.99,
    image: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500&h=500&fit=crop',
    category: 'Wearables',
    gradient: 'from-blue-600 via-cyan-400 to-teal-400',
    description: 'Advanced fitness tracking and health monitoring',
  },
  {
    id: 3,
    title: 'Gaming Laptop',
    price: 1299.99,
    image: 'https://images.unsplash.com/photo-1588872657840-790ff3ec6d51?w=500&h=500&fit=crop',
    category: 'Computers',
    gradient: 'from-violet-600 via-purple-500 to-pink-500',
    description: 'High-performance laptop for gaming and creativity',
  },
  {
    id: 4,
    title: 'Mechanical Keyboard',
    price: 149.99,
    image: 'https://images.unsplash.com/photo-1587829191301-7b86b5049bbf?w=500&h=500&fit=crop',
    category: 'Peripherals',
    gradient: 'from-orange-600 via-red-500 to-pink-500',
    description: 'RGB mechanical keyboard with custom switches',
  },
  {
    id: 5,
    title: '4K Webcam',
    price: 249.99,
    image: 'https://images.unsplash.com/photo-1598933679122-f127c3eb5e06?w=500&h=500&fit=crop',
    category: 'Accessories',
    gradient: 'from-green-600 via-emerald-500 to-teal-500',
    description: 'Professional 4K webcam for streaming and calls',
  },
  {
    id: 6,
    title: 'Portable Speaker',
    price: 99.99,
    image: 'https://images.unsplash.com/photo-1589003077984-894e133814c9?w=500&h=500&fit=crop',
    category: 'Audio',
    gradient: 'from-pink-600 via-rose-500 to-red-500',
    description: 'Compact speaker with 360-degree sound',
  },
];

export default function Home() {
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [cartItems, setCartItems] = useState<Array<{ product: Product; quantity: number }>>([]);
  const [showCart, setShowCart] = useState(false);

  const categories = ['All', 'Audio', 'Wearables', 'Computers', 'Peripherals', 'Accessories'];
  const filteredProducts =
    selectedCategory === 'All'
      ? products
      : products.filter((p) => p.category === selectedCategory);

  const handleAddToCart = (product: Product) => {
    setCartItems((prev) => {
      const existing = prev.find((item) => item.product.id === product.id);
      if (existing) {
        return prev.map((item) =>
          item.product.id === product.id
            ? { ...item, quantity: item.quantity + 1 }
            : item,
        );
      }
      return [...prev, { product, quantity: 1 }];
    });
  };

  return (
    <div className="min-h-screen bg-background">
      <Sidebar
        activeTab="products"
        cartCount={cartItems.reduce((sum, item) => sum + item.quantity, 0)}
        onCartClick={() => setShowCart(!showCart)}
      />

      <main className="ml-24 overflow-auto">
        <div className="p-8">
          {!showCart ? (
            <>
              {/* Header */}
              <div className="mb-8">
                <h1 className="mb-2 text-4xl font-bold text-foreground">
                  Explore Premium
                </h1>
                <p className="text-muted-foreground">
                  Discover the latest tech products with cutting-edge technology
                </p>
              </div>

              {/* Featured Banner */}
              <div className="mb-8 rounded-2xl bg-gradient-to-br from-purple-600 via-pink-500 to-red-500 p-8 text-white">
                <h2 className="mb-2 text-3xl font-bold">New Collection Available</h2>
                <p>Experience premium quality with the latest innovation in tech</p>
              </div>

              {/* Category Filter */}
              <div className="mb-8 flex flex-wrap gap-3">
                {categories.map((category) => (
                  <button
                    key={category}
                    onClick={() => setSelectedCategory(category)}
                    className={`rounded-full px-6 py-2 font-medium transition-all ${
                      selectedCategory === category
                        ? 'bg-primary text-primary-foreground'
                        : 'border border-border bg-card text-foreground hover:border-primary'
                    }`}
                  >
                    {category}
                  </button>
                ))}
              </div>

              {/* Products Grid */}
              <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
                {filteredProducts.map((product) => (
                  <ProductCard
                    key={product.id}
                    product={product}
                    onAddToCart={handleAddToCart}
                  />
                ))}
              </div>
            </>
          ) : (
            <Cart items={cartItems} onRemove={(id) => setCartItems(cartItems.filter(item => item.product.id !== id))} />
          )}
        </div>
      </main>
    </div>
  );
}
