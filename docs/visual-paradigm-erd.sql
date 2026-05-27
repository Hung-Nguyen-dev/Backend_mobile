-- Full Visual Paradigm reverse-engineering DDL for the travel app
-- Use this file when you want VP to import the whole schema and auto-generate ERD.
-- You can later hide/disable tables in the diagram if you only need a subset.

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS post_itinerary_details;
DROP TABLE IF EXISTS user_interactions;
DROP TABLE IF EXISTS community_post_interactions;
DROP TABLE IF EXISTS community_follows;
DROP TABLE IF EXISTS community_posts;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS posts;
DROP TABLE IF EXISTS chat_messages;
DROP TABLE IF EXISTS chat_sessions;
DROP TABLE IF EXISTS email_otps;
DROP TABLE IF EXISTS trip_suggestions;
DROP TABLE IF EXISTS ai_itinerary_details;
DROP TABLE IF EXISTS ai_itineraries;
DROP TABLE IF EXISTS itinerary_items;
DROP TABLE IF EXISTS itinerary_details;
DROP TABLE IF EXISTS itineraries;
DROP TABLE IF EXISTS locations;
DROP TABLE IF EXISTS booking_restaurants;
DROP TABLE IF EXISTS booking_hotels;
DROP TABLE IF EXISTS booking_coaches;
DROP TABLE IF EXISTS booking_flights;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS booking_masters;
DROP TABLE IF EXISTS expense_splits;
DROP TABLE IF EXISTS expenses;
DROP TABLE IF EXISTS budgets;
DROP TABLE IF EXISTS trip_members;
DROP TABLE IF EXISTS trips;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    full_name VARCHAR(150) NULL,
    avatar_url VARCHAR(500) NULL,
    role VARCHAR(50) NULL,
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified_at BIGINT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trips (
    id INT AUTO_INCREMENT PRIMARY KEY,
    trip_name VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    status VARCHAR(50) NULL,
    user_id INT NULL,
    CONSTRAINT fk_trips_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trip_members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    member_role INT NULL,
    trip_id INT NULL,
    user_id INT NULL,
    status INT NULL,
    CONSTRAINT fk_trip_members_trip
        FOREIGN KEY (trip_id) REFERENCES trips(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_trip_members_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE itineraries (
    id INT AUTO_INCREMENT PRIMARY KEY,
    day_number INT NULL,
    date DATE NULL,
    trip_id INT NULL,
    CONSTRAINT fk_itineraries_trip
        FOREIGN KEY (trip_id) REFERENCES trips(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE itinerary_details (
    id INT AUTO_INCREMENT PRIMARY KEY,
    visit_time TIME NULL,
    note VARCHAR(1000) NULL,
    itinerary_id INT NULL,
    CONSTRAINT fk_itinerary_details_itinerary
        FOREIGN KEY (itinerary_id) REFERENCES itineraries(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE locations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100) NULL,
    address VARCHAR(500) NULL,
    rating INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_itineraries (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NULL,
    generated_content TEXT NULL,
    created_at DATETIME NULL,
    trip_id INT NULL,
    CONSTRAINT fk_ai_itineraries_trip
        FOREIGN KEY (trip_id) REFERENCES trips(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_itinerary_details (
    id INT AUTO_INCREMENT PRIMARY KEY,
    day_number INT NULL,
    ai_itinerary_id INT NULL,
    location_id INT NULL,
    CONSTRAINT fk_ai_itinerary_details_ai_itinerary
        FOREIGN KEY (ai_itinerary_id) REFERENCES ai_itineraries(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_ai_itinerary_details_location
        FOREIGN KEY (location_id) REFERENCES locations(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trip_suggestions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    trip_id INT NULL,
    location_id INT NULL,
    CONSTRAINT fk_trip_suggestions_trip
        FOREIGN KEY (trip_id) REFERENCES trips(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_trip_suggestions_location
        FOREIGN KEY (location_id) REFERENCES locations(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE itinerary_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    trip_id INT NOT NULL,
    user_id INT NOT NULL,
    kind VARCHAR(50) NULL,
    place_name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NULL,
    lat DOUBLE NULL,
    lon DOUBLE NULL,
    phone VARCHAR(50) NULL,
    website VARCHAR(500) NULL,
    booking_link VARCHAR(500) NULL,
    rating DOUBLE NULL,
    open_now BOOLEAN NULL,
    amenities_json TEXT NULL,
    reviews_json TEXT NULL,
    created_at DATETIME NULL,
    CONSTRAINT fk_itinerary_items_trip
        FOREIGN KEY (trip_id) REFERENCES trips(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_itinerary_items_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE budgets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(100) NULL,
    limit_amount FLOAT NULL,
    trip_id INT NULL,
    CONSTRAINT fk_budgets_trip
        FOREIGN KEY (trip_id) REFERENCES trips(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    amount FLOAT NULL,
    category VARCHAR(100) NULL,
    description VARCHAR(500) NULL,
    trip_id INT NULL,
    user_id INT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_expenses_trip
        FOREIGN KEY (trip_id) REFERENCES trips(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_expenses_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE expense_splits (
    id INT AUTO_INCREMENT PRIMARY KEY,
    owed_amount FLOAT NULL,
    is_settled INT NULL,
    expense_id INT NULL,
    user_id INT NULL,
    CONSTRAINT fk_expense_splits_expense
        FOREIGN KEY (expense_id) REFERENCES expenses(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_expense_splits_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE booking_masters (
    id INT AUTO_INCREMENT PRIMARY KEY,
    total_amount FLOAT NULL,
    payment_status VARCHAR(50) NULL,
    trip_id INT NULL,
    user_id INT NULL,
    CONSTRAINT fk_booking_masters_trip
        FOREIGN KEY (trip_id) REFERENCES trips(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT fk_booking_masters_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE booking_flights (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pnr_code VARCHAR(100) NULL,
    flight_number VARCHAR(100) NULL,
    departure_airport VARCHAR(150) NULL,
    arrival_airport VARCHAR(150) NULL,
    departure_time TIME NULL,
    arrival_time TIME NULL,
    booking_master_id INT NOT NULL,
    CONSTRAINT fk_booking_flights_master
        FOREIGN KEY (booking_master_id) REFERENCES booking_masters(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE booking_coaches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NULL,
    seat VARCHAR(50) NULL,
    pick_up VARCHAR(255) NULL,
    drop_off VARCHAR(255) NULL,
    plate_number VARCHAR(100) NULL,
    departure_date DATE NULL,
    departure_time TIME NULL,
    booking_master_id INT NOT NULL,
    CONSTRAINT fk_booking_coaches_master
        FOREIGN KEY (booking_master_id) REFERENCES booking_masters(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE booking_hotels (
    id INT AUTO_INCREMENT PRIMARY KEY,
    room_type VARCHAR(100) NULL,
    check_in_date DATE NULL,
    check_out_date DATE NULL,
    booking_master_id INT NULL,
    CONSTRAINT fk_booking_hotels_master
        FOREIGN KEY (booking_master_id) REFERENCES booking_masters(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE booking_restaurants (
    id INT AUTO_INCREMENT PRIMARY KEY,
    address VARCHAR(500) NULL,
    reservation_time TIME NULL,
    number_of_guests INT NULL,
    booking_master_id INT NULL,
    CONSTRAINT fk_booking_restaurants_master
        FOREIGN KEY (booking_master_id) REFERENCES booking_masters(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    transaction_no VARCHAR(150) NULL,
    amount FLOAT NULL,
    payment_date DATE NULL,
    booking_master_id INT NULL,
    provider VARCHAR(50) NULL,
    status VARCHAR(50) NULL,
    CONSTRAINT fk_payments_master
        FOREIGN KEY (booking_master_id) REFERENCES booking_masters(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE posts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NULL,
    location VARCHAR(255) NULL,
    latitude DOUBLE NULL,
    longitude DOUBLE NULL,
    budget INT NULL,
    image_url VARCHAR(500) NULL,
    CONSTRAINT fk_posts_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE comments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    content TEXT NULL,
    post_id INT NULL,
    CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT fk_comments_post
        FOREIGN KEY (post_id) REFERENCES posts(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE community_posts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    trip_id INT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NULL,
    image_url TEXT NULL,
    image_urls LONGTEXT NULL,
    location VARCHAR(255) NULL,
    budget INT NULL,
    is_trip_linked INT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_community_posts_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_community_posts_trip
        FOREIGN KEY (trip_id) REFERENCES trips(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE community_follows (
    id INT AUTO_INCREMENT PRIMARY KEY,
    follower_user_id INT NOT NULL,
    followee_user_id INT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_community_follows_follower
        FOREIGN KEY (follower_user_id) REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_community_follows_followee
        FOREIGN KEY (followee_user_id) REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE community_post_interactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_community_post_interactions_post
        FOREIGN KEY (post_id) REFERENCES community_posts(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_community_post_interactions_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_interactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    action_type VARCHAR(100) NULL,
    user_id INT NULL,
    post_id INT NULL,
    CONSTRAINT fk_user_interactions_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT fk_user_interactions_post
        FOREIGN KEY (post_id) REFERENCES posts(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id INT NULL,
    created_at DATETIME NULL,
    user_id INT NULL,
    CONSTRAINT fk_chat_sessions_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender VARCHAR(100) NULL,
    message TEXT NULL,
    created_at DATETIME NULL,
    chat_session_id INT NULL,
    CONSTRAINT fk_chat_messages_session
        FOREIGN KEY (chat_session_id) REFERENCES chat_sessions(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE email_otps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(50) NOT NULL,
    expires_at BIGINT NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL,
    verified_at BIGINT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE posts_itinerary_details (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NULL,
    itinerary_detail_id INT NULL,
    status VARCHAR(20) NULL,
    user_id INT NULL,
    CONSTRAINT fk_posts_itinerary_details_post
        FOREIGN KEY (post_id) REFERENCES posts(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_posts_itinerary_details_itinerary_detail
        FOREIGN KEY (itinerary_detail_id) REFERENCES itinerary_details(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_posts_itinerary_details_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
