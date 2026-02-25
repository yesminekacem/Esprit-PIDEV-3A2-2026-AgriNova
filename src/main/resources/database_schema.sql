DROP DATABASE IF EXISTS agrirent_db;
CREATE DATABASE agrirent_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE agrirent_db;


CREATE TABLE inventory (
    inventory_id INT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(255) NOT NULL,
    item_type ENUM('EQUIPMENT', 'TOOL', 'CONSUMABLE', 'STORAGE') NOT NULL,
    description TEXT,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price DOUBLE NOT NULL DEFAULT 0 CHECK (unit_price >= 0),
    purchase_date DATE,
    condition_status ENUM('EXCELLENT', 'GOOD', 'FAIR', 'POOR') NOT NULL DEFAULT 'GOOD',
    is_rentable BOOLEAN NOT NULL DEFAULT FALSE,
    rental_price_per_day DOUBLE DEFAULT 0,
    rental_status ENUM('AVAILABLE', 'RENTED_OUT', 'IN_USE', 'MAINTENANCE', 'RETIRED') NOT NULL DEFAULT 'AVAILABLE',
    last_maintenance_date DATE,
    next_maintenance_date DATE,
    total_usage_hours INT DEFAULT 0,
    owner_name VARCHAR(255),
    owner_contact VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    image_path VARCHAR(500),
    INDEX idx_item_type (item_type),
    INDEX idx_rental_status (rental_status),
    INDEX idx_is_rentable (is_rentable),
    INDEX idx_item_name (item_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE rental (
    rental_id INT AUTO_INCREMENT PRIMARY KEY,
    inventory_id INT NOT NULL,
    owner_name VARCHAR(255) NOT NULL,
    renter_name VARCHAR(255) NOT NULL,
    renter_contact VARCHAR(100) NOT NULL,
    renter_address VARCHAR(500),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    actual_return_date DATE,
    daily_rate DOUBLE NOT NULL DEFAULT 0,
    total_days INT DEFAULT 0,
    total_cost DOUBLE DEFAULT 0,
    security_deposit DOUBLE DEFAULT 0,
    late_fee DOUBLE DEFAULT 0,
    requires_delivery BOOLEAN DEFAULT FALSE,
    delivery_fee DOUBLE DEFAULT 0,
    delivery_address VARCHAR(500),
    rental_status ENUM('PENDING', 'APPROVED', 'ACTIVE', 'RETURNED', 'COMPLETED', 'CANCELLED', 'DISPUTED') NOT NULL DEFAULT 'PENDING',
    pickup_condition VARCHAR(255),
    return_condition VARCHAR(255),
    pickup_photos TEXT,
    return_photos TEXT,
    damage_notes TEXT,
    owner_rating INT CHECK (owner_rating IS NULL OR (owner_rating >= 1 AND owner_rating <= 5)),
    renter_rating INT CHECK (renter_rating IS NULL OR (renter_rating >= 1 AND renter_rating <= 5)),
    owner_review TEXT,
    renter_review TEXT,
    payment_status ENUM('PENDING', 'DEPOSIT_PAID', 'FULLY_PAID', 'REFUNDED', 'DISPUTED') NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rental_inventory FOREIGN KEY (inventory_id) REFERENCES inventory(inventory_id) ON DELETE RESTRICT,
    INDEX idx_rental_status (rental_status),
    INDEX idx_start_date (start_date),
    INDEX idx_end_date (end_date),
    INDEX idx_renter_name (renter_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE rental_history (
    history_id INT AUTO_INCREMENT PRIMARY KEY,
    rental_id INT NOT NULL,
    action_type ENUM('CREATED', 'APPROVED', 'ACTIVATED', 'RETURNED', 'COMPLETED', 'CANCELLED', 'UPDATED', 'DISPUTED') NOT NULL,
    action_description TEXT,
    performed_by VARCHAR(255),
    action_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_rental FOREIGN KEY (rental_id) REFERENCES rental(rental_id) ON DELETE CASCADE,
    INDEX idx_rental_id (rental_id),
    INDEX idx_action_timestamp (action_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


INSERT INTO inventory (item_name, item_type, description, quantity, unit_price, purchase_date, condition_status, is_rentable, rental_price_per_day, rental_status, last_maintenance_date, next_maintenance_date, total_usage_hours, owner_name, owner_contact) VALUES
('John Deere 5075E Tractor', 'EQUIPMENT', '55 HP utility tractor, ideal for small farms', 1, 35000.00, '2022-03-15', 'EXCELLENT', TRUE, 120.00, 'AVAILABLE', '2024-01-10', '2025-02-20', 450, 'Ahmed Ben Salem', '+216 12 345 678'),
('Seed Drill 3m', 'EQUIPMENT', 'Precision seed drill for cereals', 1, 8500.00, '2021-06-01', 'GOOD', TRUE, 45.00, 'RENTED_OUT', '2024-06-01', '2025-06-01', 120, 'Fatma Mansour', '+216 98 765 432'),
('Heavy Duty Plow', 'TOOL', 'Reversible moldboard plow', 2, 1200.00, '2020-11-20', 'GOOD', TRUE, 25.00, 'AVAILABLE', '2024-08-15', '2025-08-15', 80, 'Mohamed Khelifi', '+216 22 111 222'),
('Wheat Seeds - Premium', 'CONSUMABLE', 'Certified wheat seeds 50kg bags', 100, 45.00, '2024-09-01', 'EXCELLENT', FALSE, NULL, 'AVAILABLE', NULL, NULL, 0, 'Coop Agricole Tunis', '+216 70 123 456'),
('NPK Fertilizer 500kg', 'CONSUMABLE', 'NPK 15-15-15 compound fertilizer', 20, 280.00, '2024-08-15', 'EXCELLENT', FALSE, NULL, 'AVAILABLE', NULL, NULL, 0, 'Coop Agricole Tunis', '+216 70 123 456'),
('Grain Storage Silo 10T', 'STORAGE', 'Metal silo for grain storage', 1, 15000.00, '2023-02-10', 'EXCELLENT', TRUE, 30.00, 'AVAILABLE', NULL, '2025-03-01', 0, 'Ali Trabelsi', '+216 55 444 333'),
('Irrigation Pump 2HP', 'EQUIPMENT', 'Diesel irrigation pump', 1, 1800.00, '2022-05-12', 'FAIR', TRUE, 15.00, 'MAINTENANCE', '2024-10-01', '2025-02-15', 320, 'Samia Jridi', '+216 99 888 777'),
('Harvester Blade Set', 'TOOL', 'Replacement blades for combine', 3, 350.00, '2023-07-01', 'GOOD', TRUE, 20.00, 'AVAILABLE', '2024-09-01', '2025-09-01', 50, 'Mohamed Khelifi', '+216 22 111 222');

INSERT INTO rental (inventory_id, owner_name, renter_name, renter_contact, renter_address, start_date, end_date, daily_rate, total_days, total_cost, security_deposit, requires_delivery, delivery_fee, rental_status, payment_status) VALUES
(2, 'Fatma Mansour', 'Hassan Dridi', '+216 11 222 333', 'Sousse, Route de Kairouan', '2025-02-01', '2025-02-15', 45.00, 14, 630.00, 315.00, FALSE, 0, 'ACTIVE', 'FULLY_PAID'),
(1, 'Ahmed Ben Salem', 'Karim Bouslama', '+216 44 555 666', 'Nabeul, Zone Industrielle', '2025-01-20', '2025-01-25', 120.00, 5, 600.00, 300.00, TRUE, 50.00, 'COMPLETED', 'FULLY_PAID'),
(3, 'Mohamed Khelifi', 'Leila Amara', '+216 77 888 999', 'Béja, Douar El Houch', '2025-02-10', '2025-02-12', 25.00, 2, 50.00, 25.00, FALSE, 0, 'PENDING', 'PENDING');


INSERT INTO rental_history (rental_id, action_type, action_description, performed_by) VALUES
(1, 'CREATED', 'Rental request created for Seed Drill', 'Hassan Dridi'),
(1, 'APPROVED', 'Rental approved by owner', 'Fatma Mansour'),
(1, 'ACTIVATED', 'Rental started - equipment picked up', 'System'),
(2, 'CREATED', 'Rental request for tractor', 'Karim Bouslama'),
(2, 'APPROVED', 'Rental approved', 'Ahmed Ben Salem'),
(2, 'COMPLETED', 'Equipment returned in good condition', 'Ahmed Ben Salem'),
(3, 'CREATED', 'Rental request for plow', 'Leila Amara');
