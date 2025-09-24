-- Comprehensive Development Data Script (NOT a Flyway migration)
-- This script should be run manually for development purposes only

-- Update users: use city_id for known cities, custom_city for others, and set planType
INSERT INTO "users" (username, email, password, role, first_name, last_name, bio, city_id, custom_city, contact_number, profile_image_url, plan_type) VALUES
('test', 'test@test.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', NULL, NULL, NULL, (SELECT id FROM city WHERE name = 'Cape Town'), NULL, NULL, 'profiles/10/profile.jpg', 'FREE'),
('john_doe', 'john@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'John', 'Doe', 'Tech enthusiast and software developer. Love collecting vintage electronics and gaming gear.', (SELECT id FROM city WHERE name = 'Johannesburg'), NULL, '+27-10-555-0101', 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?q=80&w=880&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'VERIFIED'),
('sarah_smith', 'sarah@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Sarah', 'Smith', 'Fashion designer with 10+ years experience. Specializing in vintage and designer pieces.', (SELECT id FROM city WHERE name = 'Cape Town'), NULL, '+27-21-555-0102', 'https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'VERIFIED'),
('mike_wilson', 'mike@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Mike', 'Wilson', 'Home improvement expert and furniture restorer. Quality craftsmanship guaranteed.', (SELECT id FROM city WHERE name = 'Bloemfontein'), NULL, '+27-51-555-0103', 'https://images.unsplash.com/photo-1568602471122-7832951cc4c5?q=80&w=1470&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'FREE'),
('emma_davis', 'emma@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Emma', 'Davis', 'Book lover and rare book collector. PhD in Literature with extensive collection.', (SELECT id FROM city WHERE name = 'Port Elizabeth'), NULL, '+27-41-555-0104', 'https://images.unsplash.com/photo-1479936343636-73cdc5aae0c3?q=80&w=880&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'FREE'),
('alex_chen', 'alex@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Alex', 'Chen', 'Sports equipment dealer and outdoor enthusiast. Certified in sports equipment maintenance.', (SELECT id FROM city WHERE name = 'Durban'), NULL, '+27-31-555-0105', 'https://images.unsplash.com/photo-1560250097-0b93528c311a?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'FREE'),
('lisa_johnson', 'lisa@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Lisa', 'Johnson', 'Art collector and gallery owner. Specializing in contemporary and modern art.', (SELECT id FROM city WHERE name = 'Cape Town'), NULL, '+27-21-555-0106', 'https://images.unsplash.com/photo-1515138692129-197a2c608cfd?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'FREE'),
('david_brown', 'david@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'David', 'Brown', 'Musician and instrument dealer. Professional guitarist with 15 years experience.', (SELECT id FROM city WHERE name = 'Pretoria'), NULL, '+27-12-555-0107', 'https://images.unsplash.com/photo-1504257432389-52343af06ae3?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'FREE'),
('maria_garcia', 'maria@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Maria', 'Garcia', 'Jewelry designer and gemologist. Certified appraiser with GIA credentials.', (SELECT id FROM city WHERE name = 'Kimberley'), NULL, '+27-53-555-0108', 'https://images.unsplash.com/photo-1508185140592-283327020902?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 'FREE')
ON CONFLICT (email) DO NOTHING;

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
WHERE u.email IN ('test@test.com', 'john@example.com', 'sarah@example.com', 'mike@example.com', 'emma@example.com', 'alex@example.com', 'lisa@example.com', 'david@example.com', 'maria@example.com')
ON CONFLICT (user_id) DO NOTHING;

-- Insert Other Users' Listings
INSERT INTO listing (user_id, title, description, category_id, price, condition, city_id, custom_city, created_at, expires_at, sold)
VALUES
    ((SELECT id FROM users WHERE email = 'john@example.com'), 'MacBook Air M2', 'Lightweight and powerful MacBook Air M2, 13-inch display, perfect for students and professionals.', '11111111-1111-1111-1111-111111111113', 1099.00, 'LIKE_NEW', (SELECT id FROM city WHERE name = 'Johannesburg'), NULL, NOW() - INTERVAL '3 days', NOW() + INTERVAL '27 days', false),
    ((SELECT id FROM users WHERE email = 'sarah@example.com'), 'Vintage Chanel Bag', 'Authentic vintage Chanel shoulder bag in excellent condition, timeless style with classic design.', '22222222-2222-2222-2222-222222225516', 2500.00, 'EXCELLENT', (SELECT id FROM city WHERE name = 'Cape Town'), NULL, NOW() - INTERVAL '7 days', NOW() + INTERVAL '23 days', false),
    ((SELECT id FROM users WHERE email = 'mike@example.com'), 'Vintage Dining Table Set', 'Elegant wooden dining table set with 6 chairs, vintage design, well-maintained and sturdy.', '33333333-3333-3333-3333-333333337001', 800.00, 'EXCELLENT', (SELECT id FROM city WHERE name = 'Bloemfontein'), NULL, NOW() - INTERVAL '6 days', NOW() + INTERVAL '24 days', false),
    ((SELECT id FROM users WHERE email = 'alex@example.com'), 'Mountain Bike', 'Durable mountain bike with front suspension, 21-speed gears, and strong frame for off-road riding.', '44444444-4444-4444-4444-444444448002', 350.00, 'GOOD', (SELECT id FROM city WHERE name = 'Durban'), NULL, NOW() - INTERVAL '3 days', NOW() + INTERVAL '27 days', false),
    ((SELECT id FROM users WHERE email = 'emma@example.com'), 'Rare Book Collection', 'Collection of rare and collectible books, well-preserved and perfect for collectors or enthusiasts.', '55555555-5555-5555-5555-555555555555', 500.00, 'EXCELLENT', (SELECT id FROM city WHERE name = 'Port Elizabeth'), NULL, NOW() - INTERVAL '4 days', NOW() + INTERVAL '26 days', false),
    ((SELECT id FROM users WHERE email = 'lisa@example.com'), 'Contemporary Art Painting', 'Original contemporary artwork on canvas, vibrant colors and unique design for modern interiors.', '99999999-9999-9999-9999-999999999201', 1200.00, 'EXCELLENT', (SELECT id FROM city WHERE name = 'Cape Town'), NULL, NOW() - INTERVAL '5 days', NOW() + INTERVAL '25 days', false),
    ((SELECT id FROM users WHERE email = 'david@example.com'), 'Fender Stratocaster Guitar', 'Classic Fender Stratocaster electric guitar in excellent condition, iconic tone and playability.', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 1800.00, 'EXCELLENT', (SELECT id FROM city WHERE name = 'Pretoria'), NULL, NOW() - INTERVAL '8 days', NOW() + INTERVAL '22 days', false),
    ((SELECT id FROM users WHERE email = 'maria@example.com'), '5 x Bananas', 'Fresh ripe bananas, sold as a bunch of 5, great for snacking or smoothies.', 'ffffffff-ffff-ffff-ffff-fffffffffff3', 8.00, 'EXCELLENT', (SELECT id FROM city WHERE name = 'Kimberley'), NULL, NOW() - INTERVAL '2 days', NOW() + INTERVAL '28 days', false)
ON CONFLICT (user_id, title) DO NOTHING;

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
INSERT INTO subscription (user_id, business_id, stripe_subscription_id, stripe_customer_id, plan_type, status, amount, currency, billing_cycle, current_period_start, current_period_end)
SELECT
    admin.id,
    b.id,
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
JOIN business b ON b.owner_id = admin.id
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
WHERE l.title = 'iPhone 13 Pro - Mint Condition' AND buyer.id != seller.id AND l.id NOT IN ('20000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000002','20000000-0000-0000-0000-000000000003','20000000-0000-0000-0000-000000000011')
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
WHERE l.title = 'Gaming Laptop - RTX 3060' AND buyer.id != seller.id AND l.id NOT IN ('20000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000002','20000000-0000-0000-0000-000000000003','20000000-0000-0000-0000-000000000011')
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
WHERE l.title = 'Vintage Wolf Art Print' AND buyer.id != seller.id AND l.id NOT IN ('20000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000002','20000000-0000-0000-0000-000000000003','20000000-0000-0000-0000-000000000011');

-- Insert Transactions for ResellerJoe's business listings
INSERT INTO "transaction" (buyer_id, seller_id, listing_id, sale_price, sale_date, status, payment_method, notes)
SELECT
    buyer.id,
    l.created_by,
    l.id,
    l.price,
    l.created_at + INTERVAL '3 days',
    'COMPLETED',
    'CREDIT_CARD',
    'Business listing transaction completed successfully'
FROM listing l
JOIN business b ON l.business_id = b.id
JOIN "users" buyer ON buyer.email = 'john@example.com'
WHERE l.title = 'Wireless Earbuds Pro' AND b.owner_id = l.created_by AND buyer.id != l.created_by AND l.id NOT IN ('20000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000002','20000000-0000-0000-0000-000000000003','20000000-0000-0000-0000-000000000011')
UNION ALL
SELECT
    buyer.id,
    l.created_by,
    l.id,
    l.price,
    l.created_at + INTERVAL '2 days',
    'COMPLETED',
    'CREDIT_CARD',
    'Business listing transaction completed successfully'
FROM listing l
JOIN business b ON l.business_id = b.id
JOIN "users" buyer ON buyer.email = 'emma@example.com'
WHERE l.title = 'Smart LED Desk Lamp' AND b.owner_id = l.created_by AND buyer.id != l.created_by AND l.id NOT IN ('20000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000002','20000000-0000-0000-0000-000000000003','20000000-0000-0000-0000-000000000011');

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

-- Insert users with hardcoded UUIDs for review references
INSERT INTO "users" (id, username, email, password, role, first_name, last_name, plan_type)
VALUES
('00000000-0000-0000-0000-000000000001', 'reviewer1', 'reviewer1@example.com', '$2a$10$dummyhash', 'HAS_ACCOUNT', 'Reviewer', 'One', 'FREE'),
('00000000-0000-0000-0000-000000000003', 'reviewer3', 'reviewer3@example.com', '$2a$10$dummyhash', 'HAS_ACCOUNT', 'Reviewer', 'Three', 'FREE'),
('00000000-0000-0000-0000-000000000004', 'reviewer4', 'reviewer4@example.com', '$2a$10$dummyhash', 'HAS_ACCOUNT', 'Reviewer', 'Four', 'FREE'),
('00000000-0000-0000-0000-000000000006', 'reviewer6', 'reviewer6@example.com', '$2a$10$dummyhash', 'HAS_ACCOUNT', 'Reviewer', 'Six', 'FREE'),
('00000000-0000-0000-0000-000000000011', 'reviewer11', 'reviewer11@example.com', '$2a$10$dummyhash', 'HAS_ACCOUNT', 'Reviewer', 'Eleven', 'FREE')
ON CONFLICT (id) DO NOTHING;




INSERT INTO listing_image (listing_id, image)
SELECT
    l.id,
    li.image_path
FROM listing l
         JOIN (
    VALUES
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

-- Sample notifications for dev users
INSERT INTO notification (id, user_id, type, message, data, created_at, read, action_required)
VALUES
  ('40000000-0000-0000-0000-000000000001', (SELECT id FROM users WHERE email = 'admin@admin.com'), 'SYSTEM', 'Welcome to the Admin Pro Store!', NULL, NOW() - INTERVAL '1 day', false, false),
  ('40000000-0000-0000-0000-000000000003', (SELECT id FROM users WHERE email = 'reseller@marketplace.com'), 'INVITATION', 'You have been invited to join Joe''s Reseller Store as a MANAGER.', '{"businessId":"..."}', NOW() - INTERVAL '3 hours', false, true)
ON CONFLICT (id) DO NOTHING;
