-- Demo seed (US-16): applied only with the demo profile via
-- spring.flyway.locations. Deterministic ids vehicle-1..25.
INSERT INTO vehicles
    (id, plate, make, model, year, tank_capacity_l, odometer_km,
     type, status, fuel_type)
VALUES
('99b5202f-f181-3828-9461-3571f0f67f94', '2137KLM', 'Toyota', 'Corolla', 2019, 51, 12470, 'CAR', 'AVAILABLE', 'GASOLINE'),
('78265825-828f-3015-94d0-572bb1607f91', '2174PRT', 'Volkswagen', 'Golf', 2020, 52, 24940, 'CAR', 'AVAILABLE', 'GASOLINE'),
('9b976d57-1507-3b3d-b4cc-6d3739199e63', '2211BCD', 'Tesla', 'Model 3', 2021, 53, 37410, 'CAR', 'AVAILABLE', 'GASOLINE'),
('e5408722-0573-37e3-bf5f-2a20377b924d', '2248FGH', 'Renault', 'Clio', 2022, 54, 49880, 'CAR', 'AVAILABLE', 'GASOLINE'),
('09a31dd6-1bc9-338e-a3de-2c28a1388ecd', '2285JKL', 'Peugeot', '208', 2023, 50, 62350, 'CAR', 'AVAILABLE', 'GASOLINE'),
('f739b7d4-5873-3352-a277-3186b3e83881', '2322MNP', 'Hyundai', 'i30', 2024, 51, 74820, 'CAR', 'AVAILABLE', 'GASOLINE'),
('b4f9433f-8e31-357f-89c4-bbee5cec1a98', '2359QRS', 'Kia', 'Ceed', 2025, 52, 87290, 'CAR', 'AVAILABLE', 'GASOLINE'),
('d7638ab7-914f-348d-a954-b5ad4b5ee486', '2396TVW', 'Ford', 'Focus', 2018, 53, 99760, 'CAR', 'AVAILABLE', 'GASOLINE'),
('1bc4d6e6-3da1-3fac-b9c4-c6a5d53b8df3', '2433XYZ', 'Seat', 'Leon', 2019, 54, 112230, 'CAR', 'AVAILABLE', 'HYBRID'),
('223413b0-01a7-3ee9-95de-8ae057cfd678', '2470DFG', 'Ford', 'Transit', 2020, 80, 124700, 'VAN', 'AVAILABLE', 'DIESEL'),
('30b0cb02-9e34-3c0e-b8fe-01c423b29278', '2507HJK', 'Mercedes-Benz', 'Sprinter', 2021, 81, 137170, 'VAN', 'AVAILABLE', 'DIESEL'),
('dfd2853c-c3de-350f-9819-28e19b78107b', '2544LMP', 'Volkswagen', 'Crafter', 2022, 82, 149640, 'VAN', 'AVAILABLE', 'DIESEL'),
('f4d0da8a-30b7-3012-a8b2-a337d6af0dea', '2581NQR', 'Renault', 'Master', 2023, 83, 162110, 'VAN', 'AVAILABLE', 'DIESEL'),
('e9e49b3c-3322-3e06-bc65-a7854a37bb14', '2618STV', 'Peugeot', 'Boxer', 2024, 84, 174580, 'VAN', 'AVAILABLE', 'DIESEL'),
('cfcb6628-7d49-3eb9-85b9-fa1899b4d5bd', '2655WXZ', 'Citroen', 'Jumper', 2025, 80, 2050, 'VAN', 'IN_USE', 'DIESEL'),
('bb54b559-0459-3379-96d4-f65f9c81db1f', '2692BDF', 'Fiat', 'Ducato', 2018, 81, 14520, 'VAN', 'IN_USE', 'DIESEL'),
('8af07b18-e6de-3ab9-a159-45a44d6c5c39', '2729GJL', 'Iveco', 'Daily', 2019, 82, 26990, 'VAN', 'IN_USE', 'DIESEL'),
('84b6df26-b972-30d6-90bb-9c01edc1e6a3', '2766MQT', 'Toyota', 'Proace', 2020, 83, 39460, 'VAN', 'IN_USE', 'DIESEL'),
('479eaee3-c452-329c-a850-47b5800bf9ed', '2803RWZ', 'Opel', 'Vivaro', 2021, 84, 51930, 'VAN', 'IN_USE', 'DIESEL'),
('36ac0309-80fc-30d2-9c53-f90753337cc5', '2840CDF', 'Volvo', 'FH', 2022, 400, 64400, 'TRUCK', 'IN_USE', 'DIESEL'),
('0459b5f6-9eca-3da6-9197-00d7a30e7eb4', '2877HKM', 'Scania', 'R', 2023, 401, 76870, 'TRUCK', 'IN_USE', 'DIESEL'),
('4e836847-5dac-34e0-9120-2bf2f7a3046b', '2914PSV', 'MAN', 'TGX', 2024, 402, 89340, 'TRUCK', 'IN_USE', 'DIESEL'),
('b3538e14-cae5-3f32-945e-dc03aa6bc6d3', '2951TWY', 'Mercedes-Benz', 'Actros', 2025, 403, 101810, 'TRUCK', 'IN_MAINTENANCE', 'DIESEL'),
('4d6e5521-b33f-3919-8409-f66f844b678f', '2988BCM', 'Yamaha', 'MT-07', 2018, 19, 114280, 'MOTORCYCLE', 'IN_MAINTENANCE', 'GASOLINE'),
('1cce6e0a-f957-309b-9e14-2ad402214330', '3025FPR', 'Honda', 'CB500F', 2019, 15, 126750, 'MOTORCYCLE', 'OUT_OF_SERVICE', 'GASOLINE');
