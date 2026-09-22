INSERT INTO teams (name) VALUES ('Engineering');
INSERT INTO employees (name, email, team_id, role) VALUES ('Alice', 'alice@example.com', 1, 'EMPLOYEE');
INSERT INTO employees (name, email, team_id, role) VALUES ('Bob', 'bob@example.com', 1, 'EMPLOYEE');

INSERT INTO floors (name, max_capacity) VALUES ('Level 1', 100);
INSERT INTO zones (name, floor_id) VALUES ('Zone A', 1);

INSERT INTO desks (desk_type, x_coordinate, y_coordinate, zone_id, assigned_employee_id) VALUES ('HOT', 10.0, 10.0, 1, NULL);
INSERT INTO desks (desk_type, x_coordinate, y_coordinate, zone_id, assigned_employee_id) VALUES ('FIXED', 20.0, 20.0, 1, 1); -- Assigned to Alice
