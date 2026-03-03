# Product Management Integration

## Overview

This document describes the integration of Moez's product management module into the main Investi application. The integration provides a complete e-commerce/marketplace functionality with role-based access control.

## Features

### Core Functionality
- **Product CRUD Operations**: Create, Read, Update, Delete products
- **Role-Based Access Control**: Only administrators can manage products
- **Product Catalog**: Browse and search published products
- **Product Statistics**: Track views, sales, and inventory
- **Multi-Currency Support**: TND, USD, EUR, GBP
- **Digital Products**: Support for both physical and digital products
- **Discount Management**: Percentage-based discounts (remise)
- **Image Management**: Upload and manage product images

### Database Schema

#### Product Table
```sql
CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'TND',
    is_digital TINYINT(1) DEFAULT 0,
    download_url VARCHAR(500),
    entrepreneur_id CHAR(36),
    status ENUM('draft','published','archived') DEFAULT 'draft',
    views_count INT DEFAULT 0,
    sales_count INT DEFAULT 0,
    stock INT DEFAULT NULL,
    remise INT DEFAULT NULL,
    category VARCHAR(255) DEFAULT 'Uncategorized',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (entrepreneur_id) REFERENCES users(id)
);
```

#### Sale Table
```sql
CREATE TABLE sale (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reference VARCHAR(100) NOT NULL UNIQUE,
    customer_id CHAR(36) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'TND',
    status VARCHAR(50),
    payment_method VARCHAR(100),
    payment_status ENUM('unpaid','paid','refunded') DEFAULT 'unpaid',
    transaction_id VARCHAR(255),
    shipping_address TEXT,
    billing_address TEXT,
    notes TEXT,
    product_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
);
```

## Architecture

### Entity Layer
- **Location**: `src/main/java/edu/connexion3a8/entities/Product.java`
- **Purpose**: Product data model with all attributes and helper methods
- **Key Methods**:
  - `getFinalPrice()`: Calculate price after discount
  - `getCategoryName()`: Get category with fallback
  - `getTitle()`, `getImage()`: Convenience accessors

### Service Layer
- **Location**: `src/main/java/edu/connexion3a8/services/ProductService.java`
- **Purpose**: Business logic and database operations
- **Security**: All write operations require admin role verification
- **Key Methods**:
  - `create(Product, User)`: Create new product (admin only)
  - `update(Product, User)`: Update existing product (admin only)
  - `delete(long, User)`: Delete product (admin only)
  - `getAllProducts()`: Get all products (public)
  - `getPublishedProducts()`: Get published products only (public)
  - `searchProducts(String)`: Search by name, category, or description
  - `incrementViewsCount(long)`: Track product views
  - `incrementSalesCount(long)`: Track product sales
  - `getProductStats()`: Get aggregate statistics

### Controller Layer
- **Location**: `src/main/java/edu/connexion3a8/controllers/collaboration/AdminController.java`
- **Integration**: Product management embedded in admin dashboard
- **Features**:
  - Product table with search functionality
  - Add/Edit/Delete operations
  - Product statistics in dashboard
  - Role-based access enforcement

### View Layer
- **Location**: `src/main/resources/collaboration/AdminDashboard.fxml`
- **Features**:
  - Products navigation tab
  - Product table with sortable columns
  - Search bar for filtering
  - Action buttons for CRUD operations
  - Status-based row styling

## Role-Based Access Control

### Admin Users
- **Full Access**: Create, read, update, delete all products
- **Dashboard Access**: View product statistics and analytics
- **Inventory Management**: Manage stock levels and pricing

### Non-Admin Users
- **Read-Only Access**: View published products only
- **No Management**: Cannot create, edit, or delete products
- **Public Catalog**: Browse and search published products

### Security Implementation
```java
private boolean isAdmin(User user) {
    return user != null && "admin".equalsIgnoreCase(user.getRole());
}

public int create(Product product, User currentUser) throws SQLException {
    if (!isAdmin(currentUser)) {
        throw new SecurityException("Only administrators can create products");
    }
    // ... create logic
}
```

## Integration Points

### 1. Admin Dashboard
- New "Products" navigation button with shopping cart icon
- Dedicated products page with full CRUD interface
- Integrated into existing admin navigation system

### 2. Statistics Dashboard
- Product count (total, published, draft)
- View and sales metrics
- Integrated into platform statistics

### 3. Database
- Product and sale tables added to main database
- Foreign key relationships with users table
- Proper indexing for performance

## Usage Guide

### For Administrators

#### Adding a Product
1. Navigate to Admin Dashboard
2. Click "Products" in the sidebar
3. Click "➕ Add Product" button
4. Fill in product details:
   - Name (required)
   - Description
   - Price (required)
   - Currency
   - Category
   - Stock level
   - Discount percentage
   - Status (draft/published/archived)
   - Digital product checkbox
   - Image upload
5. Click "Save"

#### Editing a Product
1. Select product from table
2. Click "✏️ Edit Product"
3. Modify details
4. Click "Update Product"

#### Deleting a Product
1. Select product from table
2. Click "🗑️ Delete Product"
3. Confirm deletion

#### Searching Products
- Use search bar to filter by name, category, or ID
- Real-time filtering as you type

### For Developers

#### Creating a Product Programmatically
```java
ProductService productService = new ProductService();
User adminUser = getCurrentAdminUser();

Product product = new Product(
    "Product Name",
    "Product Description",
    99.99,
    "TND",
    false, // isDigital
    "/path/to/image.jpg",
    adminUser.getId(),
    "Electronics",
    "published",
    10, // stock
    15  // 15% discount
);

int productId = productService.create(product, adminUser);
```

#### Querying Products
```java
// Get all products
List<Product> allProducts = productService.getAllProducts();

// Get published products only
List<Product> published = productService.getPublishedProducts();

// Search products
List<Product> results = productService.searchProducts("laptop");

// Get product by ID
Product product = productService.getProductById(1L);

// Get statistics
ProductService.ProductStats stats = productService.getProductStats();
System.out.println("Total products: " + stats.getTotal());
System.out.println("Published: " + stats.getPublished());
```

## Database Migration

### Step 1: Run SQL Script
```bash
mysql -u root -p 3a8 < sql/product_integration.sql
```

### Step 2: Verify Tables
```sql
USE 3a8;
SHOW TABLES LIKE 'product';
SHOW TABLES LIKE 'sale';
DESCRIBE product;
DESCRIBE sale;
```

### Step 3: (Optional) Load Sample Data
Uncomment the INSERT statements in `sql/product_integration.sql` and re-run.

## Configuration

### Database Connection
The product service uses the existing `MyConnection` utility:
```java
Connection conn = MyConnection.getInstance();
```

Ensure your `.env` file has correct database credentials:
```properties
DB_URL=jdbc:mysql://localhost:3306/3a8
DB_USER=root
DB_PASSWORD=your_password
```

### Image Upload Directory
Products support image uploads. The default directory is:
```
uploads/products/
```

Ensure this directory exists and has write permissions.

## Testing

### Manual Testing Checklist
- [ ] Admin can create products
- [ ] Admin can edit products
- [ ] Admin can delete products
- [ ] Non-admin users cannot manage products
- [ ] Search functionality works
- [ ] Product statistics display correctly
- [ ] Image upload works
- [ ] Currency selection works
- [ ] Discount calculation is correct
- [ ] Stock tracking works

### Test Data
Sample products are provided in the SQL file. Uncomment to load:
- Samsung Television (Electronics)
- Peugeot 208 (Automobile)
- HP Laptop (Electronics)

## Troubleshooting

### Products Not Showing
1. Check database connection
2. Verify product table exists
3. Check user role (must be admin)
4. Review console for SQL errors

### Access Denied Errors
1. Verify current user is admin
2. Check role in users table
3. Ensure proper authentication

### Image Upload Fails
1. Check uploads/products directory exists
2. Verify write permissions
3. Check file size limits

## Future Enhancements

### Planned Features
- [ ] Product reviews and ratings
- [ ] Inventory alerts for low stock
- [ ] Bulk product import/export
- [ ] Product variants (size, color, etc.)
- [ ] Advanced analytics dashboard
- [ ] Integration with payment gateways
- [ ] Order management system
- [ ] Customer purchase history

### API Endpoints (Future)
- REST API for product catalog
- Mobile app integration
- Third-party marketplace integration

## Credits

- **Original Module**: Moez's gestion product system
- **Integration**: Adapted for Investi platform
- **Database Schema**: Based on pi2 database structure
- **Role-Based Access**: Integrated with existing user authentication

## Support

For issues or questions:
1. Check this documentation
2. Review console logs for errors
3. Verify database schema matches specification
4. Ensure proper role-based access configuration

## Version History

- **v1.0** (2026-03-03): Initial integration
  - Product CRUD operations
  - Admin dashboard integration
  - Role-based access control
  - Statistics dashboard
  - Search functionality
