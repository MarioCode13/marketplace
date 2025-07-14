-- Comprehensive Development Data Script (NOT a Flyway migration)
-- This script should be run manually for development purposes only

-- Insert Original Users (for backward compatibility)
INSERT INTO "users" (username, email, password, role, first_name, last_name, bio, location, contact_number, profile_image_url) VALUES
('admin', 'admin@admin.com', '$2a$10$r1d0EfJx3L7OSW9ofStBPueFKHXQtyrUVhwf09h4pLOEOSMKGJmPm', 'SUBSCRIBED', 'Admin', 'User', 'System administrator and marketplace enthusiast. I love testing new features and helping users.', 'New York, NY', '+1-555-0100', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?q=80&w=688&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D')
ON CONFLICT (email) DO NOTHING;

INSERT INTO "users" (username, email, password, role, profile_image_url) VALUES
('test', 'test@test.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'profiles/10/profile.jpg')
ON CONFLICT (email) DO NOTHING;

-- Insert Additional Test Users with rich profiles
INSERT INTO "users" (username, email, password, role, first_name, last_name, bio, location, contact_number, profile_image_url) VALUES
('john_doe', 'john@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'John', 'Doe', 'Tech enthusiast and software developer. Love collecting vintage electronics and gaming gear.', 'San Francisco, CA', '+1-555-0101', 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?q=80&w=880&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
('sarah_smith', 'sarah@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Sarah', 'Smith', 'Fashion designer with 10+ years experience. Specializing in vintage and designer pieces.', 'Los Angeles, CA', '+1-555-0102', 'https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
('mike_wilson', 'mike@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Mike', 'Wilson', 'Home improvement expert and furniture restorer. Quality craftsmanship guaranteed.', 'Chicago, IL', '+1-555-0103', 'https://images.unsplash.com/photo-1568602471122-7832951cc4c5?q=80&w=1470&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
('emma_davis', 'emma@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Emma', 'Davis', 'Book lover and rare book collector. PhD in Literature with extensive collection.', 'Boston, MA', '+1-555-0104', 'https://images.unsplash.com/photo-1479936343636-73cdc5aae0c3?q=80&w=880&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
('alex_chen', 'alex@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Alex', 'Chen', 'Sports equipment dealer and outdoor enthusiast. Certified in sports equipment maintenance.', 'Seattle, WA', '+1-555-0105', 'https://images.unsplash.com/photo-1560250097-0b93528c311a?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
('lisa_johnson', 'lisa@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Lisa', 'Johnson', 'Art collector and gallery owner. Specializing in contemporary and modern art.', 'Miami, FL', '+1-555-0106', 'https://images.unsplash.com/photo-1515138692129-197a2c608cfd?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
('david_brown', 'david@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'David', 'Brown', 'Musician and instrument dealer. Professional guitarist with 15 years experience.', 'Nashville, TN', '+1-555-0107', 'https://images.unsplash.com/photo-1504257432389-52343af06ae3?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D'),
('maria_garcia', 'maria@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Maria', 'Garcia', 'Jewelry designer and gemologist. Certified appraiser with GIA credentials.', 'Phoenix, AZ', '+1-555-0108', 'https://images.unsplash.com/photo-1508185140592-283327020902?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D')
ON CONFLICT (email) DO NOTHING;

-- Insert Profile Completion records for all users
INSERT INTO profile_completion (user_id, has_profile_photo, has_bio, has_contact_number, has_location, has_verified_email, has_verified_phone, has_id_verification, has_address_verification, completion_percentage) 
SELECT id, false, true, true, true, true, true, true, true, 87.50
FROM "users"
WHERE email IN ('admin@admin.com', 'test@test.com', 'john@example.com', 'sarah@example.com', 'mike@example.com', 'emma@example.com', 'alex@example.com', 'lisa@example.com', 'david@example.com', 'maria@example.com')
ON CONFLICT (user_id) DO NOTHING;

-- Insert Trust Ratings with varied scores
INSERT INTO trust_rating (user_id, overall_score, document_score, profile_score, review_score, transaction_score, total_reviews, positive_reviews, total_transactions, successful_transactions) 
SELECT 
    u.id,
    CASE 
        WHEN u.email = 'admin@admin.com' THEN 98.00
        WHEN u.email = 'sarah@example.com' THEN 96.00
        WHEN u.email = 'emma@example.com' THEN 94.00
        WHEN u.email = 'john@example.com' THEN 92.00
        WHEN u.email = 'mike@example.com' THEN 90.00
        WHEN u.email = 'alex@example.com' THEN 88.00
        WHEN u.email = 'lisa@example.com' THEN 95.00
        WHEN u.email = 'david@example.com' THEN 93.00
        WHEN u.email = 'maria@example.com' THEN 97.00
        ELSE 85.00
    END as overall_score,
    100.00 as document_score,
    87.50 as profile_score,
    CASE 
        WHEN u.email = 'admin@admin.com' THEN 95.00
        WHEN u.email = 'sarah@example.com' THEN 96.00
        WHEN u.email = 'emma@example.com' THEN 94.00
        WHEN u.email = 'john@example.com' THEN 92.00
        WHEN u.email = 'mike@example.com' THEN 90.00
        WHEN u.email = 'alex@example.com' THEN 88.00
        WHEN u.email = 'lisa@example.com' THEN 95.00
        WHEN u.email = 'david@example.com' THEN 93.00
        WHEN u.email = 'maria@example.com' THEN 97.00
        ELSE 85.00
    END as review_score,
    CASE 
        WHEN u.email = 'admin@admin.com' THEN 98.00
        WHEN u.email = 'sarah@example.com' THEN 96.00
        WHEN u.email = 'emma@example.com' THEN 94.00
        WHEN u.email = 'john@example.com' THEN 92.00
        WHEN u.email = 'mike@example.com' THEN 90.00
        WHEN u.email = 'alex@example.com' THEN 88.00
        WHEN u.email = 'lisa@example.com' THEN 95.00
        WHEN u.email = 'david@example.com' THEN 93.00
        WHEN u.email = 'maria@example.com' THEN 97.00
        ELSE 85.00
    END as transaction_score,
    CASE 
        WHEN u.email = 'admin@admin.com' THEN 25
        WHEN u.email = 'sarah@example.com' THEN 18
        WHEN u.email = 'emma@example.com' THEN 15
        WHEN u.email = 'john@example.com' THEN 12
        WHEN u.email = 'mike@example.com' THEN 10
        WHEN u.email = 'alex@example.com' THEN 8
        WHEN u.email = 'lisa@example.com' THEN 20
        WHEN u.email = 'david@example.com' THEN 14
        WHEN u.email = 'maria@example.com' THEN 22
        ELSE 5
    END as total_reviews,
    CASE 
        WHEN u.email = 'admin@admin.com' THEN 24
        WHEN u.email = 'sarah@example.com' THEN 17
        WHEN u.email = 'emma@example.com' THEN 14
        WHEN u.email = 'john@example.com' THEN 11
        WHEN u.email = 'mike@example.com' THEN 9
        WHEN u.email = 'alex@example.com' THEN 7
        WHEN u.email = 'lisa@example.com' THEN 19
        WHEN u.email = 'david@example.com' THEN 13
        WHEN u.email = 'maria@example.com' THEN 21
        ELSE 4
    END as positive_reviews,
    CASE 
        WHEN u.email = 'admin@admin.com' THEN 35
        WHEN u.email = 'sarah@example.com' THEN 28
        WHEN u.email = 'emma@example.com' THEN 22
        WHEN u.email = 'john@example.com' THEN 18
        WHEN u.email = 'mike@example.com' THEN 15
        WHEN u.email = 'alex@example.com' THEN 12
        WHEN u.email = 'lisa@example.com' THEN 30
        WHEN u.email = 'david@example.com' THEN 25
        WHEN u.email = 'maria@example.com' THEN 32
        ELSE 8
    END as total_transactions,
    CASE 
        WHEN u.email = 'admin@admin.com' THEN 34
        WHEN u.email = 'sarah@example.com' THEN 27
        WHEN u.email = 'emma@example.com' THEN 21
        WHEN u.email = 'john@example.com' THEN 17
        WHEN u.email = 'mike@example.com' THEN 14
        WHEN u.email = 'alex@example.com' THEN 11
        WHEN u.email = 'lisa@example.com' THEN 29
        WHEN u.email = 'david@example.com' THEN 24
        WHEN u.email = 'maria@example.com' THEN 31
        ELSE 7
    END as successful_transactions
FROM "users" u
WHERE u.email IN ('admin@admin.com', 'test@test.com', 'john@example.com', 'sarah@example.com', 'mike@example.com', 'emma@example.com', 'alex@example.com', 'lisa@example.com', 'david@example.com', 'maria@example.com')
ON CONFLICT (user_id) DO NOTHING;

-- Insert Admin's Listings with real B2 photos
INSERT INTO listing (user_id, title, description, category_id, price, condition, location, created_at, expires_at, sold) 
SELECT 
    u.id,
    l.title,
    l.description,
    c.id,
    l.price,
    l.condition,
    l.location,
    l.created_at,
    l.expires_at,
    l.sold
FROM (
    VALUES 
    ('admin@admin.com', 'iPhone 13 Pro - Mint Condition', 'Excellent condition iPhone 13 Pro, 256GB, Sierra Blue. Includes original box, charger, and all accessories. Barely used, perfect for anyone looking for a premium phone.', 'Electronics', 799.99, 'EXCELLENT', 'New York, NY', NOW() - INTERVAL '30 days', NOW() + INTERVAL '30 days', false),
    ('admin@admin.com', 'Gaming Laptop - RTX 3060', 'Powerful gaming laptop with RTX 3060, 16GB RAM, 512GB SSD. Perfect for gaming and work. Runs all modern games at high settings.', 'Electronics', 1299.00, 'GOOD', 'New York, NY', NOW() - INTERVAL '25 days', NOW() + INTERVAL '35 days', false),
    ('admin@admin.com', 'Vintage Wolf Art Print', 'Beautiful black and white wolf art print. High quality, perfect for home or office decoration. Framed and ready to hang.', 'Art & Collectibles', 150.00, 'EXCELLENT', 'New York, NY', NOW() - INTERVAL '20 days', NOW() + INTERVAL '40 days', false),
    ('admin@admin.com', 'Professional Camera Lens', 'High-quality camera lens, perfect for professional photography. Includes carrying case and lens caps.', 'Electronics', 450.00, 'LIKE_NEW', 'New York, NY', NOW() - INTERVAL '15 days', NOW() + INTERVAL '45 days', false),
    ('admin@admin.com', 'iPhone 13 Pro Max - Space Gray', 'Like new iPhone 13 Pro Max, 512GB, Space Gray. Complete with box and all original accessories. Perfect condition.', 'Electronics', 999.99, 'LIKE_NEW', 'New York, NY', NOW() - INTERVAL '10 days', NOW() + INTERVAL '50 days', false)
) AS l(user_email, title, description, category_name, price, condition, location, created_at, expires_at, sold)
JOIN "users" u ON u.email = l.user_email
JOIN category c ON c.name = l.category_name
ON CONFLICT (user_id, title) DO NOTHING;

-- Insert Other Users' Listings
INSERT INTO listing (user_id, title, description, category_id, price, condition, location, created_at, expires_at, sold)
SELECT
    u.id,
    l.title,
    l.description,
    c.id,
    l.price,
    l.condition,
    l.location,
    l.created_at,
    l.expires_at,
    l.sold
FROM (
         VALUES
             ('john@example.com', 'MacBook Air M2', 'Like new MacBook Air with M2 chip, 8GB RAM, 256GB SSD. Perfect for work and study. Includes original box and charger.', 'Electronics', 1099.00, 'LIKE_NEW', 'San Francisco, CA', NOW() - INTERVAL '3 days', NOW() + INTERVAL '27 days', false),
             ('sarah@example.com', 'Vintage Chanel Bag', 'Authentic vintage Chanel bag from 1990s. Classic black leather with gold hardware. Excellent condition with original dust bag.', 'Fashion & Accessories', 2500.00, 'EXCELLENT', 'Los Angeles, CA', NOW() - INTERVAL '7 days', NOW() + INTERVAL '23 days', false),
             ('mike@example.com', 'Vintage Dining Table Set', 'Solid oak dining table with 6 chairs. Beautiful craftsmanship from the 1960s. Perfect for family gatherings.', 'Home & Garden', 800.00, 'EXCELLENT', 'Chicago, IL', NOW() - INTERVAL '6 days', NOW() + INTERVAL '24 days', false),
             ('alex@example.com', 'Mountain Bike', 'Trek mountain bike with 21-speed gears. Great for trails and city riding. Recently serviced and ready to ride.', 'Sports & Outdoors', 350.00, 'GOOD', 'Seattle, WA', NOW() - INTERVAL '3 days', NOW() + INTERVAL '27 days', false),
             ('emma@example.com', 'Rare Book Collection', 'Collection of 10 rare first edition books from the 1800s. Includes works by Dickens, Twain, and other classics.', 'Books & Media', 500.00, 'EXCELLENT', 'Boston, MA', NOW() - INTERVAL '4 days', NOW() + INTERVAL '26 days', false),
             ('lisa@example.com', 'Contemporary Art Painting', 'Original acrylic painting by local artist. Abstract composition with vibrant colors. 24x36 inches, ready to hang.', 'Art & Collectibles', 1200.00, 'EXCELLENT', 'Miami, FL', NOW() - INTERVAL '5 days', NOW() + INTERVAL '25 days', false),
             ('david@example.com', 'Fender Stratocaster Guitar', 'Vintage Fender Stratocaster from 1985. Excellent condition with original pickups. Perfect for collectors and players.', 'Musical Instruments', 1800.00, 'EXCELLENT', 'Nashville, TN', NOW() - INTERVAL '8 days', NOW() + INTERVAL '22 days', false),
             ('maria@example.com', '5 x Bananas', '5 x premium bananas for sale', 'Food & Beverages', 8.00, 'EXCELLENT', 'Phoenix, AZ', NOW() - INTERVAL '2 days', NOW() + INTERVAL '28 days', false)
     ) AS l(user_email, title, description, category_name, price, condition, location, created_at, expires_at, sold)
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