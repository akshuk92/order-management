-- Seed data for orders table
-- Note: spring.jpa.hibernate.ddl-auto=update creates the schema first,
-- then this script runs (spring.jpa.defer-datasource-initialization=true)

INSERT INTO orders (customer_name, product_name, quantity, price, status, order_date)
SELECT 'Alice Johnson', 'Wireless Mouse', 2, 19.99, 'PENDING', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM orders WHERE customer_name = 'Alice Johnson' AND product_name = 'Wireless Mouse'
);

INSERT INTO orders (customer_name, product_name, quantity, price, status, order_date)
SELECT 'Bob Martinez', 'Mechanical Keyboard', 1, 89.50, 'CONFIRMED', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM orders WHERE customer_name = 'Bob Martinez' AND product_name = 'Mechanical Keyboard'
);

INSERT INTO orders (customer_name, product_name, quantity, price, status, order_date)
SELECT 'Carla Diaz', '27-inch Monitor', 1, 249.00, 'SHIPPED', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM orders WHERE customer_name = 'Carla Diaz' AND product_name = '27-inch Monitor'
);
