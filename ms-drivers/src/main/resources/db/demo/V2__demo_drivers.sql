-- Demo seed (US-16): applied only with the demo profile via
-- spring.flyway.locations. Deterministic ids driver-1..20.
INSERT INTO drivers
    (id, full_name, email, phone, license_number, license_category,
     license_expires_at, status)
VALUES
('a6efc71c-384f-3cb0-8f2c-6d0d82e754fe', 'Lucia Fernandez', 'lucia.fernandez1@example.com', '+34 600 100 001', 'DL-DEMO-0001', 'B', CURRENT_DATE + 375, 'ACTIVE'),
('144f496a-9827-33ce-b2d3-744ceac9ca8f', 'Mateo Garcia', 'mateo.garcia2@example.com', '+34 600 100 002', 'DL-DEMO-0002', 'C', CURRENT_DATE + 385, 'ACTIVE'),
('b68130b4-021d-3595-8143-756111db88c1', 'Sofia Lopez', 'sofia.lopez3@example.com', '+34 600 100 003', 'DL-DEMO-0003', 'B', CURRENT_DATE + 395, 'ACTIVE'),
('86b56f1c-f937-3e77-8cb0-fad046e46f99', 'Hugo Martinez', 'hugo.martinez4@example.com', '+34 600 100 004', 'DL-DEMO-0004', 'A', CURRENT_DATE + 405, 'ACTIVE'),
('6028e910-cc14-3d83-8d1f-801df6fdbe77', 'Martina Sanchez', 'martina.sanchez5@example.com', '+34 600 100 005', 'DL-DEMO-0005', 'B', CURRENT_DATE + 415, 'ACTIVE'),
('cd421fbc-23d7-3a98-9a82-2a77dbe0822b', 'Leo Perez', 'leo.perez6@example.com', '+34 600 100 006', 'DL-DEMO-0006', 'C', CURRENT_DATE + 425, 'ACTIVE'),
('bd65fca3-fd19-3c82-8200-22eedb3cc4a6', 'Daniela Gomez', 'daniela.gomez7@example.com', '+34 600 100 007', 'DL-DEMO-0007', 'B', CURRENT_DATE + 435, 'ON_LEAVE'),
('bc53b168-7011-324d-bb48-a87b9ec03281', 'Alejandro Ruiz', 'alejandro.ruiz8@example.com', '+34 600 100 008', 'DL-DEMO-0008', 'B', CURRENT_DATE + 445, 'ACTIVE'),
('ccae5611-473f-38f5-bc76-671465a218d3', 'Valeria Diaz', 'valeria.diaz9@example.com', '+34 600 100 009', 'DL-DEMO-0009', 'C', CURRENT_DATE + 455, 'ACTIVE'),
('cfba82bd-3dcc-3a3a-addc-21bd82a74185', 'Pablo Moreno', 'pablo.moreno10@example.com', '+34 600 100 010', 'DL-DEMO-0010', 'B', CURRENT_DATE + 465, 'ACTIVE'),
('92b175d2-bdc9-3802-9890-9c05557f0f73', 'Carmen Alvarez', 'carmen.alvarez11@example.com', '+34 600 100 011', 'DL-DEMO-0011', 'A', CURRENT_DATE + 475, 'ACTIVE'),
('f8684010-5ad2-33b3-8ed9-c9c6eddc94a5', 'Diego Romero', 'diego.romero12@example.com', '+34 600 100 012', 'DL-DEMO-0012', 'B', CURRENT_DATE + 485, 'ACTIVE'),
('c323494f-f14c-3ea4-b87d-19840efacfef', 'Elena Navarro', 'elena.navarro13@example.com', '+34 600 100 013', 'DL-DEMO-0013', 'C', CURRENT_DATE + 495, 'ACTIVE'),
('000ffb62-7292-34b1-8186-b90bcacea3bd', 'Javier Torres', 'javier.torres14@example.com', '+34 600 100 014', 'DL-DEMO-0014', 'B', CURRENT_DATE + 505, 'ON_LEAVE'),
('888a7bc2-a9b1-3090-b09f-c517970ec810', 'Paula Dominguez', 'paula.dominguez15@example.com', '+34 600 100 015', 'DL-DEMO-0015', 'B', CURRENT_DATE + 515, 'ACTIVE'),
('ad6b55ea-aec5-3e8c-9d52-4e376f1bf314', 'Andres Vazquez', 'andres.vazquez16@example.com', '+34 600 100 016', 'DL-DEMO-0016', 'C', CURRENT_DATE + 525, 'ACTIVE'),
('333ade4d-0283-35d3-a079-9eb47d6b37d1', 'Nora Serrano', 'nora.serrano17@example.com', '+34 600 100 017', 'DL-DEMO-0017', 'B', CURRENT_DATE - 5, 'ACTIVE'),
('f108b462-1aac-3822-8174-79638d7ccbd0', 'Adrian Blanco', 'adrian.blanco18@example.com', '+34 600 100 018', 'DL-DEMO-0018', 'A', CURRENT_DATE + 12, 'ACTIVE'),
('210cadbd-e610-3c41-abf3-0b6b31ef626c', 'Clara Molina', 'clara.molina19@example.com', '+34 600 100 019', 'DL-DEMO-0019', 'B', CURRENT_DATE + 22, 'ACTIVE'),
('56fe200f-3886-35ff-9ec1-efbc07d27e2e', 'Ivan Ortega', 'ivan.ortega20@example.com', '+34 600 100 020', 'DL-DEMO-0020', 'C', CURRENT_DATE + 29, 'SUSPENDED');
