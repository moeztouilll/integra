'use client';

import { Trash2, ShoppingBag } from 'lucide-react';

interface Product {
  id: number;
  title: string;
  price: number;
  image: string;
  category: string;
  gradient: string;
  description: string;
}

interface CartItem {
  product: Product;
  quantity: number;
}

interface CartProps {
  items: CartItem[];
  onRemove: (productId: number) => void;
}

export default function Cart({ items, onRemove }: CartProps) {
  const total = items.reduce((sum, item) => sum + item.product.price * item.quantity, 0);

  if (items.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16">
        <ShoppingBag className="mb-4 h-16 w-16 text-muted-foreground" />
        <h2 className="mb-2 text-2xl font-bold text-foreground">Your cart is empty</h2>
        <p className="text-muted-foreground">Add products to get started</p>
      </div>
    );
  }

  return (
    <div className="max-w-4xl">
      <h1 className="mb-8 text-4xl font-bold text-foreground">Shopping Cart</h1>

      {/* Cart Items */}
      <div className="mb-8 space-y-4">
        {items.map((item) => (
          <div
            key={item.product.id}
            className="flex gap-4 rounded-2xl border border-border bg-card p-6 hover:border-primary transition-all"
          >
            {/* Product Image */}
            <div className="relative h-24 w-24 flex-shrink-0 rounded-lg overflow-hidden">
              <img
                src={item.product.image}
                alt={item.product.title}
                className="h-full w-full object-cover"
              />
            </div>

            {/* Product Details */}
            <div className="flex-1">
              <h3 className="font-bold text-card-foreground">{item.product.title}</h3>
              <p className="text-sm text-muted-foreground mb-2">{item.product.category}</p>
              <p className="text-lg font-semibold text-primary">
                ${(item.product.price * item.quantity).toFixed(2)}
              </p>
            </div>

            {/* Quantity */}
            <div className="flex items-center gap-4">
              <div className="text-center">
                <p className="text-sm text-muted-foreground">Qty</p>
                <p className="text-2xl font-bold text-foreground">{item.quantity}</p>
              </div>

              {/* Remove Button */}
              <button
                onClick={() => onRemove(item.product.id)}
                className="flex h-10 w-10 items-center justify-center rounded-lg border border-border hover:border-destructive hover:bg-destructive/10 transition-all"
              >
                <Trash2 className="h-5 w-5 text-destructive" />
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Summary */}
      <div className="rounded-2xl border border-border bg-gradient-to-br from-primary/10 to-accent/10 p-6">
        <div className="mb-6 space-y-3">
          <div className="flex justify-between text-foreground">
            <span>Subtotal</span>
            <span>${total.toFixed(2)}</span>
          </div>
          <div className="flex justify-between text-foreground">
            <span>Shipping</span>
            <span className="text-green-400">Free</span>
          </div>
          <div className="border-t border-border pt-3">
            <div className="flex justify-between">
              <span className="font-bold text-foreground">Total</span>
              <span className="text-2xl font-bold bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">
                ${total.toFixed(2)}
              </span>
            </div>
          </div>
        </div>

        <button className="w-full rounded-lg bg-primary hover:bg-primary/80 px-6 py-3 font-bold text-primary-foreground transition-all hover:shadow-lg hover:shadow-primary/30 active:scale-95">
          Proceed to Checkout
        </button>
      </div>
    </div>
  );
}
