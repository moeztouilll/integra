# Product Management Setup Guide

## Quick Start

### 1. Database Setup

Run the SQL migration script to create the product tables:

```bash
mysql -u root -p 3a8 < sql/product_integration.sql
```

This creates:
- `product` table for storing product information
- `sale` table for tracking sales transactions

### 2. Verify Installation

Check that the tables were created:

```sql
USE 3a8;
SHOW TABLES LIKE 'product';
SHOW TABLES LIKE 'sale';
```

### 3. Access Product Management

1. Log in as an **admin** user
2. Navigate to the Admin Dashboard
3. Click the **Products** button in the sidebar (shopping cart icon)
4. You'll see the product management interface

## Features Available

### Admin Dashboard - Products Tab

The products tab in the admin dashboard provides:

- **Product Table**: View all products with columns for:
  - ID
  - Name
  - Category
  - Price & Currency
  - Status (draft/published/archived)
  - Stock level
  - Views count
  - Sales count

- **Search**: Real-time search by name, category, or ID

- **Actions**:
  - ➕ Add Product
  - ✏️ Edit Product
  -🗑️ Delete Product

### Product Statistics

The Statistics tab now includes product metrics:
- Total Products
- Published Products
- Product Views
- Total Sales

## Role-Based Access

### Admin Users Only
- Create new products
- Edit existing products
- Delete products
- View all product statistics
- Manage inventory and pricing

### Security
All product management operations verify admin role:
```java
if (!isAdmin(currentUser)) {
    throw new SecurityException("Only administrators can create products");
}
```

## Product Fields

When creating/editing a product:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| Name | Text | Yes | Product name |
| Description | Text Area | No | Detailed description |
| Price | Number | Yes | Product price |
| Currency | Dropdown | Yes | TND, USD, EUR, GBP |
| Category | Combo | No | Product category (editable) |
| Status | Dropdown | Yes | draft, published, archived |
| Stock | Number | No | Available quantity |
| Discount (%) | Number | No | Discount percentage (remise) |
| Digital Product | Checkbox | No | Is this a digital product? |
| Image URL | Text | No | Path to product image |

## Product Status

- **draft**: Product is being prepared, not visible to public
- **published**: Product is live and visible to customers
- **archived**: Product is no longer available but kept for records

## API Usage

### ProductService Methods

```java
ProductService productService = new ProductService();
User adminUser = getCurrentAdminUser();

// Create a product
Product product = new Product();
product.setName("Laptop HP");
product.setPrice(600.00);
product.setCurrency("TND");
product.setStatus("published");
product.setCategory("Informatique");
product.setStock(10);
product.setEntrepreneurId(adminUser.getId());

int productId = productService.create(product, adminUser);

// Get all products
List<Product> allProducts = productService.getAllProducts();

// Get published products only
List<Product> published = productService.getPublishedProducts();

// Search products
List<Product> results = productService.searchProducts("laptop");

// Update product
product.setPrice(550.00);
productService.update(product, adminUser);

// Delete product
productService.delete(productId, adminUser);

// Get statistics
ProductService.ProductStats stats = productService.getProductStats();
System.out.println("Total: " + stats.getTotal());
System.out.println("Published: " + stats.getPublished());
System.out.println("Views: " + stats.getTotalViews());
System.out.println("Sales: " + stats.getTotalSales());
```

## Integration with Existing System

### Database Connection
Uses the existing `MyConnection` utility:
```java
Connection conn = MyConnection.getInstance();
```

### User Authentication
Integrates with existing user system:
- Uses `User` entity with role field
- Checks for "admin" role
- Links products to users via `entrepreneur_id`

### Admin Dashboard
- New navigation button added
- New products page integrated
- Statistics updated to include product metrics

## Sample Data

To load sample products (optional), uncomment the INSERT statements in `sql/product_integration.sql`:

```sql
INSERT INTO product (name, description, price, currency, is_digital, download_url, entrepreneur_id, status, stock, remise, category) VALUES
('Samsung Television', 'Experience vibrant clarity...', 1400.00, 'TND', 1, NULL, (SELECT id FROM users WHERE role='admin' LIMIT 1), 'published', 5, 20, 'Informatique'),
('Peugeot 208', 'Experience the perfect blend...', 20000.00, 'TND', 1, NULL, (SELECT id FROM users WHERE role='admin' LIMIT 1), 'published', 4, 0, 'Voiture');
```

## Troubleshooting

### "Only administrators can create products"
- Verify you're logged in as admin
- Check user role in database: `SELECT role FROM users WHERE id = 'your-user-id';`
- Ensure role is exactly 'admin' (case-insensitive)

### Products not showing in table
- Check database connection
- Verify product table exists
- Check console for SQL errors
- Ensure products exist: `SELECT COUNT(*) FROM product;`

### Cannot access Products tab
- Verify you're in Admin Dashboard
- Check that AdminDashboard.fxml includes products page
- Ensure navigation button is visible

## File Structure

```
src/main/java/edu/connexion3a8/
├── entities/
│   └── Product.java                    # Product entity
├── services/
│   └── ProductService.java             # Product business logic
└── controllers/collaboration/
    └── AdminController.java            # Admin dashboard with products

src/main/resources/collaboration/
└── AdminDashboard.fxml                 # UI with products tab

sql/
└── product_integration.sql             # Database schema

docs/
├── PRODUCT_INTEGRATION.md              # Detailed documentation
└── PRODUCT_SETUP_GUIDE.md              # This file
```

## Next Steps

1. ✅ Database tables created
2. ✅ Product entity and service implemented
3. ✅ Admin dashboard integration complete
4. ✅ Role-based access control enforced
5. ⏭️ Test product CRUD operations
6. ⏭️ Add sample products
7. ⏭️ Implement public product catalog (future)
8. ⏭️ Add shopping cart functionality (future)
9. ⏭️ Integrate payment processing (future)

## Support

For detailed information, see:
- `docs/PRODUCT_INTEGRATION.md` - Complete integration documentation
- `sql/product_integration.sql` - Database schema and comments
- Source code comments in Product.java and ProductService.java

## Credits

Based on Moez's gestion product module, adapted for the Investi platform with role-based access control and integration with the existing user authentication system.
