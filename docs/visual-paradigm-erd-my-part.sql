-- Visual Paradigm reverse-engineering DDL for the feature scope you are responsible for
-- Scope:
-- - Trip management
-- - Itinerary items (including hotel/restaurant entries added into itinerary)
-- - Flight booking and coach booking
-- - Finance management: expenses, splits, budgets, payments
-- This script is intentionally smaller than the full-system file so the ERD is easier to read.

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS booking_coaches;
DROP TABLE IF EXISTS booking_flights;
DROP TABLE IF EXISTS booking_masters;
DROP TABLE IF EXISTS expense_splits;
DROP TABLE IF EXISTS expenses;
DROP TABLE IF EXISTS budgets;
DROP TABLE IF EXISTS itinerary_items;
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
