-- Test script for Neon PostgreSQL connection
-- Run this in Neon's SQL Editor to verify your database setup

-- Test 1: Check if tables exist
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;

-- Test 2: Check users table structure
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'users' 
ORDER BY ordinal_position;

-- Test 3: Check if default data was inserted
SELECT COUNT(*) as user_count FROM users;
SELECT COUNT(*) as category_count FROM category;
SELECT COUNT(*) as listing_count FROM listing;

-- Test 4: Test a simple query
SELECT u.username, u.email, u.role, COUNT(l.id) as listing_count
FROM users u
LEFT JOIN listing l ON u.id = l.user_id
GROUP BY u.id, u.username, u.email, u.role;

-- Test 5: Check categories and their listings
SELECT c.name as category_name, COUNT(l.id) as listing_count
FROM category c
LEFT JOIN listing l ON c.id = l.category_id
GROUP BY c.id, c.name
ORDER BY listing_count DESC;

-- Test 6: Verify foreign key constraints
SELECT 
    tc.table_name, 
    kcu.column_name, 
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name 
FROM 
    information_schema.table_constraints AS tc 
    JOIN information_schema.key_column_usage AS kcu
      ON tc.constraint_name = kcu.constraint_name
      AND tc.table_schema = kcu.table_schema
    JOIN information_schema.constraint_column_usage AS ccu
      ON ccu.constraint_name = tc.constraint_name
      AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY' 
ORDER BY tc.table_name, kcu.column_name;

-- Test 7: Check if indexes exist (performance)
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY tablename, indexname; 