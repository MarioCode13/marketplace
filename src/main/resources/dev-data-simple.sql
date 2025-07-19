-- Simple Development Data Script (NOT a Flyway migration)
-- Run this script manually for development purposes

-- Insert Admin User
INSERT INTO "users" (username, email, password, role, first_name, last_name, bio, location, contact_number, profile_image_url) VALUES
('admin', 'admin@admin.com', '$2a$10$r1d0EfJx3L7OSW9ofStBPueFKHXQtyrUVhwf09h4pLOEOSMKGJmPm', 'SUBSCRIBED', 'Admin', 'User', 'System administrator and marketplace enthusiast. I love testing new features and helping users.', 'New York, NY', '+1-555-0100', NULL)
ON CONFLICT (email) DO NOTHING;

-- Insert Test User
INSERT INTO "users" (username, email, password, role) VALUES
('test', 'test@test.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT')
ON CONFLICT (email) DO NOTHING;

-- Insert Additional Test Users
INSERT INTO "users" (username, email, password, role, first_name, last_name, bio, location, contact_number) VALUES
('john_doe', 'john@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'John', 'Doe', 'Tech enthusiast', 'San Francisco, CA', '+1-555-0101'),
('sarah_smith', 'sarah@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Sarah', 'Smith', 'Fashion designer', 'Los Angeles, CA', '+1-555-0102'),
('mike_wilson', 'mike@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Mike', 'Wilson', 'Home improvement expert', 'Chicago, IL', '+1-555-0103'),
('emma_davis', 'emma@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Emma', 'Davis', 'Book lover', 'Boston, MA', '+1-555-0104'),
('alex_chen', 'alex@example.com', '$2a$10$79.l8wpc.yZ6CnXz6a18H.dM3Jb47V8HIfd44Xm57GvS1O8o2jMAC', 'HAS_ACCOUNT', 'Alex', 'Chen', 'Sports equipment dealer', 'Seattle, WA', '+1-555-0105')
ON CONFLICT (email) DO NOTHING;

-- Insert Profile Completion for Admin
INSERT INTO profile_completion (user_id, has_profile_photo, has_bio, has_contact_number, has_location, has_id_document, has_drivers_license, has_proof_of_address, completion_percentage) 
SELECT id, true, true, true, true, true, true, true, 100.00
FROM "users"
WHERE email = 'admin@admin.com'
ON CONFLICT (user_id) DO NOTHING;

-- Insert Profile Completion for Test Users
INSERT INTO profile_completion (user_id, has_profile_photo, has_bio, has_contact_number, has_location, has_id_document, has_drivers_license, has_proof_of_address, completion_percentage) 
SELECT id, true, true, true, true, true, true, true, 100.00
FROM "users"
WHERE email IN ('test@test.com', 'john@example.com', 'sarah@example.com', 'mike@example.com', 'emma@example.com', 'alex@example.com')
ON CONFLICT (user_id) DO NOTHING;

-- Insert Trust Rating for Admin
INSERT INTO trust_rating (user_id, overall_score, document_score, profile_score, review_score, transaction_score, total_reviews, positive_reviews, total_transactions, successful_transactions) 
SELECT id, 98.00, 100.00, 100.00, 95.00, 90.00, 25, 24, 35, 34
FROM "users"
WHERE email = 'admin@admin.com';

-- Insert Trust Ratings for Other Users
INSERT INTO trust_rating (user_id, overall_score, document_score, profile_score, review_score, transaction_score, total_reviews, positive_reviews, total_transactions, successful_transactions) 
SELECT id, 90.00, 100.00, 100.00, 85.00, 80.00, 10, 8, 15, 13
FROM "users"
WHERE email IN ('test@test.com', 'john@example.com', 'sarah@example.com', 'mike@example.com', 'emma@example.com', 'alex@example.com');

-- Insert Admin's iPhone 13 Pro Listing
INSERT INTO listing (user_id, title, description, category_id, price, condition, location, created_at, expires_at, sold) 
SELECT u.id, 'iPhone 13 Pro - Mint Condition', 'Excellent condition iPhone 13 Pro, 256GB, Sierra Blue. Includes original box, charger, and all accessories. Barely used, perfect for anyone looking for a premium phone.', c.id, 799.99, 'EXCELLENT', 'New York, NY', NOW() - INTERVAL '30 days', NOW() + INTERVAL '30 days', false
FROM "users" u, category c
WHERE u.email = 'admin@admin.com' AND c.name = 'Electronics'
ON CONFLICT (user_id, title) DO NOTHING;

-- Insert Admin's Gaming Laptop Listing
INSERT INTO listing (user_id, title, description, category_id, price, condition, location, created_at, expires_at, sold) 
SELECT u.id, 'Gaming Laptop - RTX 3060', 'Powerful gaming laptop with RTX 3060, 16GB RAM, 512GB SSD. Perfect for gaming and work. Runs all modern games at high settings.', c.id, 1299.00, 'GOOD', 'New York, NY', NOW() - INTERVAL '25 days', NOW() + INTERVAL '35 days', false
FROM "users" u, category c
WHERE u.email = 'admin@admin.com' AND c.name = 'Electronics'
ON CONFLICT (user_id, title) DO NOTHING;

-- Insert Admin's Wolf Art Print Listing
INSERT INTO listing (user_id, title, description, category_id, price, condition, location, created_at, expires_at, sold) 
SELECT u.id, 'Vintage Wolf Art Print', 'Beautiful black and white wolf art print. High quality, perfect for home or office decoration. Framed and ready to hang.', c.id, 150.00, 'EXCELLENT', 'New York, NY', NOW() - INTERVAL '20 days', NOW() + INTERVAL '40 days', false
FROM "users" u, category c
WHERE u.email = 'admin@admin.com' AND c.name = 'Art & Collectibles'
ON CONFLICT (user_id, title) DO NOTHING;

-- Insert Admin's Camera Lens Listing
INSERT INTO listing (user_id, title, description, category_id, price, condition, location, created_at, expires_at, sold) 
SELECT u.id, 'Professional Camera Lens', 'High-quality camera lens, perfect for professional photography. Includes carrying case and lens caps.', c.id, 450.00, 'LIKE_NEW', 'New York, NY', NOW() - INTERVAL '15 days', NOW() + INTERVAL '45 days', false
FROM "users" u, category c
WHERE u.email = 'admin@admin.com' AND c.name = 'Electronics'
ON CONFLICT (user_id, title) DO NOTHING;

-- Insert Admin's iPhone 13 Pro Max Listing
INSERT INTO listing (user_id, title, description, category_id, price, condition, location, created_at, expires_at, sold) 
SELECT u.id, 'iPhone 13 Pro Max - Space Gray', 'Like new iPhone 13 Pro Max, 512GB, Space Gray. Complete with box and all original accessories. Perfect condition.', c.id, 999.99, 'LIKE_NEW', 'New York, NY', NOW() - INTERVAL '10 days', NOW() + INTERVAL '50 days', false
FROM "users" u, category c
WHERE u.email = 'admin@admin.com' AND c.name = 'Electronics'
ON CONFLICT (user_id, title) DO NOTHING;

-- Insert Listing Image for Gaming Laptop (using the existing B2 reference)
INSERT INTO listing_image (listing_id, image)
SELECT l.id, 'listings/0ccddbe2-0c88-47ee-b231-54150d5b4234_3kcgKLkfPRbskpsYzW239d.jpg'
FROM listing l, "users" u
WHERE l.title = 'Gaming Laptop - RTX 3060' AND l.user_id = u.id AND u.email = 'admin@admin.com';

-- Insert verification documents for admin (removing non-existent document references)
INSERT INTO verification_document (user_id, document_type, document_url, status, verified_at, verified_by) 
SELECT u.id, 'ID_CARD', 'documents/id_photos/admin_passport.jpg', 'APPROVED', NOW() - INTERVAL '60 days', u.id
FROM "users" u
WHERE u.email = 'admin@admin.com';

INSERT INTO verification_document (user_id, document_type, document_url, status, verified_at, verified_by) 
SELECT u.id, 'DRIVERS_LICENSE', 'documents/drivers_license/admin_dl.jpg', 'APPROVED', NOW() - INTERVAL '60 days', u.id
FROM "users" u
WHERE u.email = 'admin@admin.com';

INSERT INTO verification_document (user_id, document_type, document_url, status, verified_at, verified_by) 
SELECT u.id, 'PROOF_OF_ADDRESS', 'documents/address/admin_utility_bill.pdf', 'APPROVED', NOW() - INTERVAL '60 days', u.id
FROM "users" u
WHERE u.email = 'admin@admin.com';

-- Insert subscription for admin
INSERT INTO subscription (user_id, stripe_subscription_id, stripe_customer_id, plan_type, status, amount, currency, billing_cycle, current_period_start, current_period_end)
SELECT u.id, 'sub_admin_001', 'cus_admin_001', 'PREMIUM', 'ACTIVE', 29.99, 'USD', 'MONTHLY', NOW() - INTERVAL '15 days', NOW() + INTERVAL '15 days'
FROM "users" u
WHERE u.email = 'admin@admin.com'
ON CONFLICT (stripe_subscription_id) DO NOTHING; 