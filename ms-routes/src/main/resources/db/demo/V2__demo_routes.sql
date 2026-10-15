-- Demo seed (US-16): applied only with the demo profile via
-- spring.flyway.locations. Deterministic ids route-1..12.
-- vehicle_id/driver_id reuse the deterministic demo ids from
-- ms-vehicles (V2__demo_vehicles.sql) and ms-drivers
-- (V2__demo_drivers.sql) so the demo dataset is coherent:
-- IN_USE vans run the two IN_PROGRESS routes, every other
-- assignment respects status, license category and expiry.
INSERT INTO routes
    (id, vehicle_id, driver_id, origin, destination, planned_start,
     estimated_duration_min, planned_distance_km, status, started_at,
     start_odometer_km, ended_at, end_odometer_km, actual_distance_km,
     notes, created_by, updated_at)
VALUES
-- route-1: COMPLETED Madrid -> Zaragoza (CAR, driver B).
('8ecea952-9d00-3d65-a0de-1934f44e7c7d', '99b5202f-f181-3828-9461-3571f0f67f94', 'a6efc71c-384f-3cb0-8f2c-6d0d82e754fe', 'Madrid', 'Zaragoza', now() - interval '9 days', 300, 350.00, 'COMPLETED', now() - interval '9 days' + interval '10 minutes', 11980, now() - interval '9 days' + interval '5 hours', 12332, 352.00, 'Entrega sin incidencias', NULL, now()),
-- route-2: COMPLETED Madrid -> Valencia (VAN, driver C).
('486b7307-4a26-31a7-92d0-d37139403026', '223413b0-01a7-3ee9-95de-8ae057cfd678', '144f496a-9827-33ce-b2d3-744ceac9ca8f', 'Madrid', 'Valencia', now() - interval '7 days', 360, 468.50, 'COMPLETED', now() - interval '7 days' + interval '10 minutes', 124050, now() - interval '7 days' + interval '6 hours', 124520, 470.00, NULL, NULL, now()),
-- route-3: COMPLETED Madrid -> Sevilla (TRUCK, driver C, long haul).
('6ae652af-6590-3051-bdc0-672a700025c9', '84b6df26-b972-30d6-90bb-9c01edc1e6a3', 'ad6b55ea-aec5-3e8c-9d52-4e376f1bf314', 'Madrid', 'Sevilla', now() - interval '5 days', 660, 1275.50, 'COMPLETED', now() - interval '5 days' + interval '15 minutes', 38100, now() - interval '5 days' + interval '11 hours', 39380, 1280.00, 'Pausa reglamentaria en Cordoba', NULL, now()),
-- route-4: IN_PROGRESS Madrid -> Valladolid (VAN, driver C).
('d3a924ac-06a9-34f6-a0f1-067801c3e9ab', 'bb54b559-0459-3379-96d4-f65f9c81db1f', 'cd421fbc-23d7-3a98-9a82-2a77dbe0822b', 'Madrid', 'Valladolid', now() - interval '2 hours', 150, 193.00, 'IN_PROGRESS', now() - interval '2 hours' + interval '10 minutes', 14455, NULL, NULL, NULL, NULL, NULL, now()),
-- route-5: IN_PROGRESS Barcelona -> Girona (VAN, driver C).
('afaa4dfc-3f4b-3b56-a188-35c521a283cd', '8af07b18-e6de-3ab9-a159-45a44d6c5c39', 'ccae5611-473f-38f5-bc76-671465a218d3', 'Barcelona', 'Girona', now() - interval '1 hour', 90, 102.50, 'IN_PROGRESS', now() - interval '1 hour' + interval '5 minutes', 26920, NULL, NULL, NULL, 'Salida con 10 min de retraso', NULL, now()),
-- route-6: PLANNED Madrid -> Barcelona (CAR, driver B).
('180dd55c-eb41-3308-9a36-780ff1fad097', '78265825-828f-3015-94d0-572bb1607f91', 'b68130b4-021d-3595-8143-756111db88c1', 'Madrid', 'Barcelona', now() + interval '2 days', 360, 620.00, 'PLANNED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
-- route-7: PLANNED Sevilla -> Madrid (CAR, driver B).
('0a5ef36c-96d8-391a-8cef-25c0cb13ca91', '9b976d57-1507-3b3d-b4cc-6d3739199e63', '6028e910-cc14-3d83-8d1f-801df6fdbe77', 'Sevilla', 'Madrid', now() + interval '1 day', 330, 530.00, 'PLANNED', NULL, NULL, NULL, NULL, NULL, 'Recogida en cliente prioritaria', NULL, NULL),
-- route-8: PLANNED Madrid -> Bilbao (VAN, driver C).
('57b032f5-85bf-33a1-9fc4-50e5912ff5f4', '30b0cb02-9e34-3c0e-b8fe-01c423b29278', 'c323494f-f14c-3ea4-b87d-19840efacfef', 'Madrid', 'Bilbao', now() + interval '3 days', 260, 395.00, 'PLANNED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
-- route-9: PLANNED Valencia -> Barcelona (VAN, driver B).
('f0316cf2-05bb-3748-88ff-ed43210a84cd', 'dfd2853c-c3de-350f-9819-28e19b78107b', 'bc53b168-7011-324d-bb48-a87b9ec03281', 'Valencia', 'Barcelona', now() + interval '2 days', 230, 350.00, 'PLANNED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
-- route-10: PLANNED Madrid -> Granada (CAR, driver B).
('0479c5a9-23bc-3668-b6ae-7f8cec5adf79', 'e5408722-0573-37e3-bf5f-2a20377b924d', 'cfba82bd-3dcc-3a3a-addc-21bd82a74185', 'Madrid', 'Granada', now() + interval '4 days', 270, 420.00, 'PLANNED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
-- route-11: PLANNED Zaragoza -> Madrid (CAR, driver B).
('8f1be569-947b-3cd7-9822-7670ee9d27e6', '09a31dd6-1bc9-338e-a3de-2c28a1388ecd', 'f8684010-5ad2-33b3-8ed9-c9c6eddc94a5', 'Zaragoza', 'Madrid', now() + interval '5 days', 300, 315.00, 'PLANNED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
-- route-12: PLANNED Bilbao -> Madrid (VAN, driver B).
('3342f2fb-2bc6-34d6-ac5b-3bb759f9c78b', 'f4d0da8a-30b7-3012-a8b2-a337d6af0dea', '888a7bc2-a9b1-3090-b09f-c517970ec810', 'Bilbao', 'Madrid', now() + interval '6 days', 260, 395.00, 'PLANNED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
