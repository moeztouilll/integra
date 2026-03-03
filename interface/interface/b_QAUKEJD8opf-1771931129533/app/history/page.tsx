'use client';

import Sidebar from '@/components/sidebar';
import { Calendar, Package, Truck, CheckCircle2 } from 'lucide-react';

interface OrderItem {
  id: number;
  title: string;
  price: number;
  quantity: number;
  image: string;
}

interface Order {
  id: string;
  date: string;
  status: 'delivered' | 'shipping' | 'processing';
  total: number;
  items: OrderItem[];
}

const purchaseHistory: Order[] = [
  {
    id: 'ORD-001',
    date: '2024-02-20',
    status: 'delivered',
    total: 349.99,
    items: [
      {
        id: 1,
        title: 'Smart Watch Pro',
        price: 349.99,
        quantity: 1,
        image: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=200&h=200&fit=crop',
      },
    ],
  },
  {
    id: 'ORD-002',
    date: '2024-02-15',
    status: 'delivered',
    total: 449.98,
    items: [
      {
        id: 2,
        title: 'Wireless Headphones',
        price: 199.99,
        quantity: 1,
        image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=200&h=200&fit=crop',
      },
      {
        id: 3,
        title: 'Portable Speaker',
        price: 99.99,
        quantity: 2,
        image: 'https://images.unsplash.com/photo-1589003077984-894e133814c9?w=200&h=200&fit=crop',
      },
    ],
  },
  {
    id: 'ORD-003',
    date: '2024-02-10',
    status: 'shipping',
    total: 1299.99,
    items: [
      {
        id: 4,
        title: 'Gaming Laptop',
        price: 1299.99,
        quantity: 1,
        image: 'https://images.unsplash.com/photo-1588872657840-790ff3ec6d51?w=200&h=200&fit=crop',
      },
    ],
  },
  {
    id: 'ORD-004',
    date: '2024-02-05',
    status: 'processing',
    total: 399.98,
    items: [
      {
        id: 5,
        title: 'Mechanical Keyboard',
        price: 149.99,
        quantity: 1,
        image: 'https://images.unsplash.com/photo-1587829191301-7b86b5049bbf?w=200&h=200&fit=crop',
      },
      {
        id: 6,
        title: '4K Webcam',
        price: 249.99,
        quantity: 1,
        image: 'https://images.unsplash.com/photo-1598933679122-f127c3eb5e06?w=200&h=200&fit=crop',
      },
    ],
  },
];

function StatusBadge({ status }: { status: string }) {
  const statusConfig = {
    delivered: {
      icon: CheckCircle2,
      bg: 'bg-green-500/10',
      text: 'text-green-400',
      label: 'Delivered',
    },
    shipping: {
      icon: Truck,
      bg: 'bg-blue-500/10',
      text: 'text-blue-400',
      label: 'Shipping',
    },
    processing: {
      icon: Package,
      bg: 'bg-yellow-500/10',
      text: 'text-yellow-400',
      label: 'Processing',
    },
  };

  const config = statusConfig[status as keyof typeof statusConfig];
  const Icon = config.icon;

  return (
    <div className={`flex items-center gap-2 rounded-full ${config.bg} px-4 py-2 w-fit`}>
      <Icon className={`h-4 w-4 ${config.text}`} />
      <span className={`text-sm font-semibold ${config.text}`}>{config.label}</span>
    </div>
  );
}

export default function HistoryPage() {
  return (
    <div className="min-h-screen bg-background">
      <Sidebar activeTab="history" cartCount={0} onCartClick={() => {}} />

      <main className="ml-24 overflow-auto">
        <div className="p-8">
          {/* Header */}
          <div className="mb-8">
            <h1 className="mb-2 text-4xl font-bold text-foreground">Purchase History</h1>
            <p className="text-muted-foreground">
              View all your past orders and track deliveries
            </p>
          </div>

          {/* Stats */}
          <div className="mb-8 grid grid-cols-1 gap-4 md:grid-cols-3">
            <div className="rounded-2xl border border-border bg-card p-6">
              <p className="text-sm text-muted-foreground mb-2">Total Orders</p>
              <p className="text-3xl font-bold text-foreground">{purchaseHistory.length}</p>
            </div>
            <div className="rounded-2xl border border-border bg-card p-6">
              <p className="text-sm text-muted-foreground mb-2">Total Spent</p>
              <p className="text-3xl font-bold bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">
                ${purchaseHistory.reduce((sum, order) => sum + order.total, 0).toFixed(2)}
              </p>
            </div>
            <div className="rounded-2xl border border-border bg-card p-6">
              <p className="text-sm text-muted-foreground mb-2">Delivered</p>
              <p className="text-3xl font-bold text-green-400">
                {purchaseHistory.filter((o) => o.status === 'delivered').length}
              </p>
            </div>
          </div>

          {/* Orders List */}
          <div className="space-y-4">
            {purchaseHistory.map((order) => (
              <div
                key={order.id}
                className="rounded-2xl border border-border bg-card p-6 hover:border-primary transition-all hover:shadow-lg hover:shadow-primary/10"
              >
                {/* Order Header */}
                <div className="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
                  <div>
                    <div className="flex items-center gap-2 mb-2">
                      <h3 className="text-xl font-bold text-card-foreground">{order.id}</h3>
                      <StatusBadge status={order.status} />
                    </div>
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <Calendar className="h-4 w-4" />
                      {new Date(order.date).toLocaleDateString('en-US', {
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric',
                      })}
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="text-sm text-muted-foreground">Order Total</p>
                    <p className="text-2xl font-bold bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">
                      ${order.total.toFixed(2)}
                    </p>
                  </div>
                </div>

                {/* Order Items */}
                <div className="mb-4 space-y-3 border-t border-border pt-4">
                  {order.items.map((item) => (
                    <div key={item.id} className="flex gap-4">
                      <img
                        src={item.image}
                        alt={item.title}
                        className="h-16 w-16 rounded-lg object-cover"
                      />
                      <div className="flex-1">
                        <p className="font-semibold text-card-foreground">{item.title}</p>
                        <p className="text-sm text-muted-foreground">
                          Qty: {item.quantity} × ${item.price.toFixed(2)}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="font-bold text-card-foreground">
                          ${(item.price * item.quantity).toFixed(2)}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Action Button */}
                <div className="border-t border-border pt-4">
                  <button className="w-full rounded-lg border border-primary bg-primary/10 hover:bg-primary/20 px-4 py-2 font-semibold text-primary transition-all hover:shadow-lg hover:shadow-primary/20 sm:w-auto">
                    View Details
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </main>
    </div>
  );
}
