-- Comprehensive Development Data Script (NOT a Flyway migration)
-- This script should be run manually for development purposes only

-- Update users: use city_id for known cities, custom_city for others, and set planType
INSERT INTO "users" (username, email, password, role, first_name, last_name, bio, city_id, custom_city, contact_number, profile_image_url, plan_type) VALUES
('admin', 'admin@admin.com', '$2a$10$r1d0EfJx3L7OSW9ofStBPueFKHXQtyrUVhwf09h4pLOEOSMKGJmPm', 'SUBSCRIBED', 'Admin', 'User', 'System administrator and marketplace enthusiast. I love testing new features and helping users.', 1, NULL, '+27-10-555-0100', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?q=80&w=688&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'PRO_STORE'),
('test', 'test@test.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', NULL, NULL, NULL, 3, NULL, NULL, 'profiles/10/profile.jpg', 'FREE'),
('john_doe', 'john@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'John', 'Doe', 'Tech enthusiast and software developer. Love collecting vintage electronics and gaming gear.', 1, NULL, '+27-10-555-0101', 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?q=80&w=880&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'VERIFIED'),
('sarah_smith', 'sarah@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Sarah', 'Smith', 'Fashion designer with 10+ years experience. Specializing in vintage and designer pieces.', 3, NULL, '+27-21-555-0102', 'https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'RESELLER'),
('mike_wilson', 'mike@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Mike', 'Wilson', 'Home improvement expert and furniture restorer. Quality craftsmanship guaranteed.', 6, NULL, '+27-51-555-0103', 'https://images.unsplash.com/photo-1568602471122-7832951cc4c5?q=80&w=1470&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'FREE'),
('emma_davis', 'emma@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Emma', 'Davis', 'Book lover and rare book collector. PhD in Literature with extensive collection.', 5, NULL, '+27-41-555-0104', 'https://images.unsplash.com/photo-1479936343636-73cdc5aae0c3?q=80&w=880&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'FREE'),
('alex_chen', 'alex@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Alex', 'Chen', 'Sports equipment dealer and outdoor enthusiast. Certified in sports equipment maintenance.', 4, NULL, '+27-31-555-0105', 'https://images.unsplash.com/photo-1560250097-0b93528c311a?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'FREE'),
('lisa_johnson', 'lisa@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Lisa', 'Johnson', 'Art collector and gallery owner. Specializing in contemporary and modern art.', 3, NULL, '+27-21-555-0106', 'https://images.unsplash.com/photo-1515138692129-197a2c608cfd?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'FREE'),
('david_brown', 'david@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'David', 'Brown', 'Musician and instrument dealer. Professional guitarist with 15 years experience.', 2, NULL, '+27-12-555-0107', 'https://images.unsplash.com/photo-1504257432389-52343af06ae3?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'FREE'),
('maria_garcia', 'maria@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Maria', 'Garcia', 'Jewelry designer and gemologist. Certified appraiser with GIA credentials.', 10, NULL, '+27-53-555-0108', 'https://images.unsplash.com/photo-1508185140592-283327020902?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'FREE')
ON CONFLICT (email) DO NOTHING;

-- Add reseller user
INSERT INTO "users" (username, email, password, role, plan_type, first_name, last_name, bio, city_id, contact_number, created_at)
VALUES (
  'resellerjoe',
  'reseller@marketplace.com',
  '$2a$10$7QJ8QwQwQwQwQwQwQwQwQeQwQwQwQwQwQwQwQwQwQwQwQwQwQwQw', -- bcrypt for 'password'
  'HAS_ACCOUNT',
  'RESELLER',
  'Joe',
  'Reseller',
  'We sell the best gadgets and accessories!',
  (SELECT id FROM city WHERE name = 'Cape Town'),
  '+27111222333',
  NOW()
)
ON CONFLICT (email) DO NOTHING;

-- Add store branding for reseller
INSERT INTO store_branding (user_id, slug, logo_url, banner_url, theme_color, primary_color, secondary_color, light_or_dark, about, store_name)
SELECT id, 'reseller-joe',
  'https://images.unsplash.com/photo-1519125323398-675f0ddb6308?q=80&w=100',
  'https://images.unsplash.com/photo-1465101046530-73398c7f28ca?q=80&w=800',
  '#e53e3e',
  '#e53e3e',
  '#ffffff',
  'light',
  'Welcome to Joe''s Reseller Store! Find top gadgets and more.',
  'Joe''s Reseller Store'
FROM "users" WHERE email = 'reseller@marketplace.com'
ON CONFLICT (user_id) DO NOTHING;

-- Store branding for admin (pro store)
INSERT INTO store_branding (user_id, slug, logo_url, banner_url, theme_color, primary_color, secondary_color, light_or_dark, about, store_name)
SELECT id, 'admin-pro', 'https://images.unsplash.com/photo-1614851099518-055a1000e6d5?q=80&w=1470&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?q=80&w=1470', '#6470ff', '#ff131a', '#ffffff', 'light', 'Welcome to the Admin Pro Store! We offer the best tech and collectibles.', 'Admin Pro Store'
FROM "users" WHERE email = 'admin@admin.com'
ON CONFLICT (user_id) DO NOTHING;

-- Insert Profile Completion records for all users
INSERT INTO profile_completion (user_id, has_profile_photo, has_bio, has_contact_number, has_location, has_id_document, has_drivers_license, has_proof_of_address, completion_percentage) 
SELECT id, true, true, true, true, true, true, true, 100.00
FROM "users"
WHERE email IN ('admin@admin.com', 'test@test.com', 'john@example.com', 'sarah@example.com', 'mike@example.com', 'emma@example.com', 'alex@example.com', 'lisa@example.com', 'david@example.com', 'maria@example.com')
ON CONFLICT (user_id) DO NOTHING;

-- Insert Trust Ratings with visually accurate demo scores
INSERT INTO trust_rating (user_id, overall_score, verification_score, profile_score, review_score, transaction_score, total_reviews, positive_reviews, total_transactions, successful_transactions) 
SELECT 
    u.id,
    98.00 as overall_score,
    100.00 as verification_score,
    100.00 as profile_score,
    95.00 as review_score,
    90.00 as transaction_score,
    25 as total_reviews,
    24 as positive_reviews,
    35 as total_transactions,
    34 as successful_transactions
FROM "users" u
WHERE u.email IN ('admin@admin.com', 'test@test.com', 'john@example.com', 'sarah@example.com', 'mike@example.com', 'emma@example.com', 'alex@example.com', 'lisa@example.com', 'david@example.com', 'maria@example.com')
ON CONFLICT (user_id) DO NOTHING;

-- Insert Admin's Listings with real B2 photos
INSERT INTO listing (user_id, title, description, category_id, price, condition, city_id, custom_city, created_at, expires_at, sold) 
SELECT 
    u.id,
    l.title,
    l.description,
    c.id,
    l.price,
    l.condition,
    l.city_id,
    l.custom_city,
    l.created_at,
    l.expires_at,
    l.sold
FROM (
    VALUES 
    ('admin@admin.com', 'iPhone 13 Pro - Mint Condition', 'Excellent condition iPhone 13 Pro, 256GB, Sierra Blue. Includes original box, charger, and all accessories. Barely used, perfect for anyone looking for a premium phone.', 'Electronics', 799.99, 'EXCELLENT', 1, NULL, NOW() - INTERVAL '30 days', NOW() + INTERVAL '30 days', false),
    ('admin@admin.com', 'Gaming Laptop - RTX 3060', 'Powerful gaming laptop with RTX 3060, 16GB RAM, 512GB SSD. Perfect for gaming and work. Runs all modern games at high settings.', 'Electronics', 1299.00, 'GOOD', 1, NULL, NOW() - INTERVAL '25 days', NOW() + INTERVAL '35 days', false),
    ('admin@admin.com', 'Vintage Wolf Art Print', 'Beautiful black and white wolf art print. High quality, perfect for home or office decoration. Framed and ready to hang.', 'Art & Collectibles', 150.00, 'EXCELLENT', 1, NULL, NOW() - INTERVAL '20 days', NOW() + INTERVAL '40 days', false),
    ('admin@admin.com', 'Professional Camera Lens', 'High-quality camera lens, perfect for professional photography. Includes carrying case and lens caps.', 'Electronics', 450.00, 'LIKE_NEW', 1, NULL, NOW() - INTERVAL '15 days', NOW() + INTERVAL '45 days', false),
    ('admin@admin.com', 'iPhone 13 Pro Max - Space Gray', 'Like new iPhone 13 Pro Max, 512GB, Space Gray. Complete with box and all original accessories. Perfect condition.', 'Electronics', 999.99, 'LIKE_NEW', 1, NULL, NOW() - INTERVAL '10 days', NOW() + INTERVAL '50 days', false)
) AS l(user_email, title, description, category_name, price, condition, city_id, custom_city, created_at, expires_at, sold)
JOIN "users" u ON u.email = l.user_email
JOIN category c ON c.name = l.category_name
ON CONFLICT (user_id, title) DO NOTHING;

-- Insert Other Users' Listings
INSERT INTO listing (user_id, title, description, category_id, price, condition, city_id, custom_city, created_at, expires_at, sold)
SELECT
    u.id,
    l.title,
    l.description,
    c.id,
    l.price,
    l.condition,
    l.city_id,
    l.custom_city,
    l.created_at,
    l.expires_at,
    l.sold
FROM (
         VALUES
             ('john@example.com', 'MacBook Air M2', 'Like new MacBook Air with M2 chip, 8GB RAM, 256GB SSD. Perfect for work and study. Includes original box and charger.', 'Electronics', 1099.00, 'LIKE_NEW', 1, NULL, NOW() - INTERVAL '3 days', NOW() + INTERVAL '27 days', false),
             ('sarah@example.com', 'Vintage Chanel Bag', 'Authentic vintage Chanel bag from 1990s. Classic black leather with gold hardware. Excellent condition with original dust bag.', 'Fashion & Accessories', 2500.00, 'EXCELLENT', 3, NULL, NOW() - INTERVAL '7 days', NOW() + INTERVAL '23 days', false),
             ('mike@example.com', 'Vintage Dining Table Set', 'Solid oak dining table with 6 chairs. Beautiful craftsmanship from the 1960s. Perfect for family gatherings.', 'Home & Garden', 800.00, 'EXCELLENT', 6, NULL, NOW() - INTERVAL '6 days', NOW() + INTERVAL '24 days', false),
             ('alex@example.com', 'Mountain Bike', 'Trek mountain bike with 21-speed gears. Great for trails and city riding. Recently serviced and ready to ride.', 'Sports & Outdoors', 350.00, 'GOOD', 4, NULL, NOW() - INTERVAL '3 days', NOW() + INTERVAL '27 days', false),
             ('emma@example.com', 'Rare Book Collection', 'Collection of 10 rare first edition books from the 1800s. Includes works by Dickens, Twain, and other classics.', 'Books & Media', 500.00, 'EXCELLENT', 5, NULL, NOW() - INTERVAL '4 days', NOW() + INTERVAL '26 days', false),
             ('lisa@example.com', 'Contemporary Art Painting', 'Original acrylic painting by local artist. Abstract composition with vibrant colors. 24x36 inches, ready to hang.', 'Art & Collectibles', 1200.00, 'EXCELLENT', 3, NULL, NOW() - INTERVAL '5 days', NOW() + INTERVAL '25 days', false),
             ('david@example.com', 'Fender Stratocaster Guitar', 'Vintage Fender Stratocaster from 1985. Excellent condition with original pickups. Perfect for collectors and players.', 'Musical Instruments', 1800.00, 'EXCELLENT', 2, NULL, NOW() - INTERVAL '8 days', NOW() + INTERVAL '22 days', false),
             ('maria@example.com', '5 x Bananas', '5 x premium bananas for sale', 'Food & Beverages', 8.00, 'EXCELLENT', 10, NULL, NOW() - INTERVAL '2 days', NOW() + INTERVAL '28 days', false)
     ) AS l(user_email, title, description, category_name, price, condition, city_id, custom_city, created_at, expires_at, sold)
         JOIN "users" u ON u.email = l.user_email
         JOIN category c ON c.name = l.category_name
ON CONFLICT (user_id, title) DO NOTHING;

-- Insert Listing Images with existing B2 paths
INSERT INTO listing_image (listing_id, image)
SELECT 
    l.id,
    li.image_path
FROM listing l
JOIN (
    VALUES 
    ('iPhone 13 Pro - Mint Condition', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?q=80&w=1470&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
    ('iPhone 13 Pro - Mint Condition', 'https://images.unsplash.com/photo-1616410011236-7a42121dd981?q=80&w=1632&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
    ('Gaming Laptop - RTX 3060', 'https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?q=80&w=764&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
    ('Gaming Laptop - RTX 3060', 'https://plus.unsplash.com/premium_photo-1670274609267-202ec99f8620?q=80&w=736&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
    ('Vintage Wolf Art Print', 'https://images.unsplash.com/photo-1518443855757-dfadac7101ae?q=80&w=1470&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
    ('Professional Camera Lens', 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?q=80&w=764&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
    ('iPhone 13 Pro Max - Space Gray', 'https://images.unsplash.com/photo-1616348436168-de43ad0db179?q=80&w=781&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
    ('MacBook Air M2', 'https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?q=80&w=1470&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
    ('MacBook Air M2', 'https://images.unsplash.com/photo-1541807084-5c52b6b3adef?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
    ('Vintage Chanel Bag', 'https://images.unsplash.com/photo-1586413595198-1840407316e9?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
    ('Vintage Dining Table Set', 'https://images.unsplash.com/photo-1739999063943-8eb0ed3df5f1?q=80&w=740&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
    ('Mountain Bike', 'https://images.unsplash.com/photo-1534150034764-046bf225d3fa?q=80&w=1476&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
    ('Rare Book Collection', 'https://images.unsplash.com/photo-1535905557558-afc4877a26fc?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
    ('Contemporary Art Painting', 'https://images.unsplash.com/photo-1515405295579-ba7b45403062?w=600&auto=format&fit=crop&q=60&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8NHx8cGFpbnRpbmd8ZW58MHx8MHx8fDA%3D'),
    ('Fender Stratocaster Guitar', 'https://images.unsplash.com/photo-1520166012956-add9ba0835cb?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
    ('5 x Bananas', 'https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?q=80&w=880&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D')
) AS li(listing_title, image_path) ON l.title = li.listing_title;

-- ResellerJoe's Listings
INSERT INTO listing (user_id, title, description, category_id, price, condition, city_id, custom_city, created_at, expires_at, sold)
SELECT
    u.id,
    'Wireless Earbuds Pro',
    'Brand new wireless earbuds with noise cancellation, 24h battery, and fast charging. Includes case and all accessories.',
    c.id,
    89.99,
    'NEW',
    city.id,
    NULL,
    NOW() - INTERVAL '2 days',
    NOW() + INTERVAL '28 days',
    false
FROM "users" u
JOIN category c ON c.name = 'Electronics'
JOIN city ON city.name = 'Cape Town'
WHERE u.email = 'reseller@marketplace.com';

INSERT INTO listing (user_id, title, description, category_id, price, condition, city_id, custom_city, created_at, expires_at, sold)
SELECT
    u.id,
    'Smart LED Desk Lamp',
    'Modern LED desk lamp with adjustable brightness, color temperature, and USB charging port. Perfect for home or office.',
    c.id,
    39.99,
    'NEW',
    city.id,
    NULL,
    NOW() - INTERVAL '1 days',
    NOW() + INTERVAL '29 days',
    false
FROM "users" u
JOIN category c ON c.name = 'Home & Garden'
JOIN city ON city.name = 'Cape Town'
WHERE u.email = 'reseller@marketplace.com';

-- Images for ResellerJoe's Listings
INSERT INTO listing_image (listing_id, image)
SELECT l.id, 'https://images.unsplash.com/photo-1722439667098-f32094e3b1d4?q=80&w=735&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'
FROM listing l
JOIN "users" u ON l.user_id = u.id
WHERE l.title = 'Wireless Earbuds Pro' AND u.email = 'reseller@marketplace.com';

INSERT INTO listing_image (listing_id, image)
SELECT l.id, 'https://plus.unsplash.com/premium_photo-1672166939372-5b16118eee45?q=80&w=627&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'
FROM listing l
JOIN "users" u ON l.user_id = u.id
WHERE l.title = 'Smart LED Desk Lamp' AND u.email = 'reseller@marketplace.com';

-- Insert verification documents for admin (verified)
INSERT INTO verification_document (user_id, document_type, document_url, status, verified_at, verified_by) 
SELECT 
    admin.id,
    vd.document_type,
    vd.document_url,
    vd.status,
    vd.verified_at,
    admin.id as verified_by
FROM (
    VALUES 
    ('ID_CARD', 'documents/id_photos/admin_passport.jpg', 'APPROVED', NOW() - INTERVAL '60 days'),
    ('DRIVERS_LICENSE', 'documents/drivers_license/admin_dl.jpg', 'APPROVED', NOW() - INTERVAL '60 days'),
    ('PROOF_OF_ADDRESS', 'documents/address/admin_utility_bill.pdf', 'APPROVED', NOW() - INTERVAL '60 days')
) AS vd(document_type, document_url, status, verified_at)
JOIN "users" admin ON admin.email = 'admin@admin.com';

-- Insert subscription for admin
INSERT INTO subscription (user_id, stripe_subscription_id, stripe_customer_id, plan_type, status, amount, currency, billing_cycle, current_period_start, current_period_end)
SELECT 
    admin.id,
    'sub_admin_001',
    'cus_admin_001',
    'PREMIUM',
    'ACTIVE',
    29.99,
    'USD',
    'MONTHLY',
    NOW() - INTERVAL '15 days',
    NOW() + INTERVAL '15 days'
FROM "users" admin
WHERE admin.email = 'admin@admin.com'
ON CONFLICT (stripe_subscription_id) DO NOTHING;

-- Insert Transactions (Completed Sales) - One per listing to avoid unique constraint violation
INSERT INTO "transaction" (buyer_id, seller_id, listing_id, sale_price, sale_date, status, payment_method, notes)
SELECT 
    buyer.id,
    seller.id,
    l.id,
    l.price,
    l.created_at + INTERVAL '5 days',
    'COMPLETED',
    'CREDIT_CARD',
    'Transaction completed successfully'
FROM listing l
JOIN "users" seller ON l.user_id = seller.id
JOIN "users" buyer ON buyer.email = 'john@example.com'
WHERE l.title = 'iPhone 13 Pro - Mint Condition' AND buyer.id != seller.id
UNION ALL
SELECT 
    buyer.id,
    seller.id,
    l.id,
    l.price,
    l.created_at + INTERVAL '5 days',
    'COMPLETED',
    'CREDIT_CARD',
    'Transaction completed successfully'
FROM listing l
JOIN "users" seller ON l.user_id = seller.id
JOIN "users" buyer ON buyer.email = 'sarah@example.com'
WHERE l.title = 'Gaming Laptop - RTX 3060' AND buyer.id != seller.id
UNION ALL
SELECT 
    buyer.id,
    seller.id,
    l.id,
    l.price,
    l.created_at + INTERVAL '5 days',
    'COMPLETED',
    'CREDIT_CARD',
    'Transaction completed successfully'
FROM listing l
JOIN "users" seller ON l.user_id = seller.id
JOIN "users" buyer ON buyer.email = 'emma@example.com'
WHERE l.title = 'Vintage Wolf Art Print' AND buyer.id != seller.id;

-- Insert More Transactions (Various Statuses) - Using different listings
INSERT INTO "transaction" (buyer_id, seller_id, listing_id, sale_price, sale_date, status, payment_method, notes)
SELECT 
    buyer.id,
    seller.id,
    l.id,
    l.price,
    NOW() - INTERVAL '1 day',
    'PENDING',
    'PAYPAL',
    'Payment pending'
FROM listing l
JOIN "users" seller ON l.user_id = seller.id
JOIN "users" buyer ON buyer.email IN ('mike@example.com', 'lisa@example.com')
WHERE l.title IN ('Professional Camera Lens', 'Contemporary Art Painting')
AND buyer.id != seller.id;

-- Insert Additional Transactions with different statuses for variety
INSERT INTO "transaction" (buyer_id, seller_id, listing_id, sale_price, sale_date, status, payment_method, notes)
SELECT 
    buyer.id,
    seller.id,
    l.id,
    l.price,
    NOW() - INTERVAL '3 days',
    'CANCELLED',
    'CREDIT_CARD',
    'Buyer cancelled transaction'
FROM listing l
JOIN "users" seller ON l.user_id = seller.id
JOIN "users" buyer ON buyer.email IN ('david@example.com', 'maria@example.com')
WHERE l.title IN ('MacBook Air M2', 'Vintage Chanel Bag')
AND buyer.id != seller.id;

-- Insert Reviews for Transactions
INSERT INTO review (reviewer_id, reviewed_user_id, transaction_id, rating, comment, is_positive)
SELECT 
    t.buyer_id,
    t.seller_id,
    t.id,
    CASE 
        WHEN t.listing_id IN (SELECT id FROM listing WHERE title = 'iPhone 13 Pro - Mint Condition') THEN 5.0
        WHEN t.listing_id IN (SELECT id FROM listing WHERE title = 'Gaming Laptop - RTX 3060') THEN 4.0
        WHEN t.listing_id IN (SELECT id FROM listing WHERE title = 'Vintage Wolf Art Print') THEN 5.0
        ELSE 4.0
    END,
    CASE 
        WHEN t.listing_id IN (SELECT id FROM listing WHERE title = 'iPhone 13 Pro - Mint Condition') THEN 'Excellent condition, exactly as described. Fast shipping and great communication!'
        WHEN t.listing_id IN (SELECT id FROM listing WHERE title = 'Gaming Laptop - RTX 3060') THEN 'Great laptop, runs all my games perfectly. Minor scratches but overall very good condition.'
        WHEN t.listing_id IN (SELECT id FROM listing WHERE title = 'Vintage Wolf Art Print') THEN 'Beautiful artwork, perfect for my office. Seller was very professional and packaging was excellent.'
        ELSE 'Good transaction, item as described. Would buy from this seller again.'
    END,
    true
FROM "transaction" t
WHERE t.status = 'COMPLETED';

-- Insert Additional Reviews (for variety) - One per transaction to avoid unique constraint violation
INSERT INTO review (reviewer_id, reviewed_user_id, transaction_id, rating, comment, is_positive)
SELECT 
    t.buyer_id,
    t.seller_id,
    t.id,
    CASE WHEN FLOOR(RANDOM() * 2) = 0 THEN 4.0 ELSE 5.0 END, -- Random rating between 4-5
    CASE 
        WHEN FLOOR(RANDOM() * 2) = 0 THEN 'Great seller, highly recommended!'
        ELSE 'Excellent communication and fast shipping. Item exactly as described.'
    END,
    true
FROM "transaction" t
WHERE t.status = 'COMPLETED'
AND t.id NOT IN (SELECT transaction_id FROM review)
LIMIT 3; 

-- Admin purchases (admin@admin.com as buyer)
INSERT INTO "transaction" (buyer_id, seller_id, listing_id, sale_price, sale_date, status, payment_method, notes)
SELECT 
    admin.id,
    seller.id,
    l.id,
    l.price,
    NOW() - INTERVAL '2 days',
    'COMPLETED',
    'CREDIT_CARD',
    'Admin purchase for dev data'
FROM listing l
JOIN "users" seller ON l.user_id = seller.id
JOIN "users" admin ON admin.email = 'admin@admin.com'
WHERE l.title IN ('MacBook Air M2', 'Vintage Chanel Bag', 'Mountain Bike')
  AND seller.email != 'admin@admin.com'; 