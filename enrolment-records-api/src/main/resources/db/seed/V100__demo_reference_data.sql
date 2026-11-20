-- Sample reference data, loaded only with the "demo" profile.
INSERT INTO academic_years (label, start_date, end_date, applications_open) VALUES
    ('2026/2027', '2026-09-14', '2027-07-23', TRUE),    -- id 1 : open
    ('2025/2026', '2025-09-15', '2026-07-24', FALSE);   -- id 2 : closed

INSERT INTO grade_levels (code, name, min_age, max_age, requires_previous_report) VALUES
    ('PRI1', 'Primary 1',          5,  7, FALSE),       -- id 1
    ('PRI4', 'Primary 4',          8, 10, TRUE),        -- id 2
    ('JSS1', 'Junior Secondary 1', 10, 13, TRUE);       -- id 3

-- PRI1 has only 2 seats so the demo can show the capacity rule and the waitlist.
INSERT INTO class_offerings (academic_year_id, grade_level_id, capacity)
SELECT y.id, g.id, c.seats
FROM (VALUES ('2026/2027', 'PRI1', 2),
             ('2026/2027', 'PRI4', 30),
             ('2026/2027', 'JSS1', 40),
             ('2025/2026', 'PRI1', 30)) AS c (year_label, grade_code, seats)
JOIN academic_years y ON y.label = c.year_label
JOIN grade_levels   g ON g.code  = c.grade_code;
