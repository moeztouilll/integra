'use client';

import Image from 'next/image';
import { ShoppingCart } from 'lucide-react';

interface Product {
  id: number;
  title: string;
  price: number;
  image: string;
  category: string;
  gradient: string;
  description: string;
}

interface ProductCardProps {
  product: Product;
  onAddToCart: (product: Product) => void;
}

export default function ProductCard({ product, onAddToCart }: ProductCardProps) {
  return (
    <div className="group relative rounded-2xl overflow-hidden bg-card border border-border hover:border-primary transition-all duration-300 hover:shadow-lg hover:shadow-primary/20">
      {/* Image Container */}
      <div className="relative h-64 w-full overflow-hidden bg-gradient-to-br from-card to-border">
        <img
          src={product.image}
          alt={product.title}
          className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-110"
        />
        {/* Gradient Overlay */}
        <div className={`absolute inset-0 bg-gradient-to-t ${product.gradient} opacity-0 mix-blend-multiply transition-opacity duration-300 group-hover:opacity-30`} />

        {/* Category Badge */}
        <div className="absolute top-4 right-4 rounded-full bg-primary/80 backdrop-blur-md px-3 py-1 text-xs font-semibold text-primary-foreground">
          {product.category}
        </div>
      </div>

      {/* Content */}
      <div className="p-6">
        <h3 className="mb-2 text-xl font-bold text-card-foreground line-clamp-1">
          {product.title}
        </h3>

        <p className="mb-4 text-sm text-muted-foreground line-clamp-2">
          {product.description}
        </p>

        {/* Price and Button */}
        <div className="flex items-center justify-between">
          <span className="text-2xl font-bold bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">
            ${product.price}
          </span>

          <button
            onClick={() => onAddToCart(product)}
            className="flex items-center justify-center gap-2 rounded-lg bg-primary hover:bg-primary/80 px-4 py-2 font-semibold text-primary-foreground transition-all hover:shadow-lg hover:shadow-primary/30 active:scale-95"
          >
            <ShoppingCart className="h-5 w-5" />
            <span className="hidden sm:inline">Add</span>
          </button>
        </div>
      </div>
    </div>
  );
}
