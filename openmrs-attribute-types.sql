-- OpenMRS Attribute Types Creation Script
-- Run this on your OpenMRS database to create the required person and visit attribute types

-- ============================================
-- PERSON ATTRIBUTE TYPES
-- ============================================

-- 1. LocationId - Stores the OpenMRS location ID where participant was registered
INSERT INTO person_attribute_type (name, description, format, foreign_key, searchable, creator, date_created, changed_by, date_changed, retired, retire_reason, uuid)
VALUES (
    'LocationId',
    'OpenMRS Location ID where participant was registered',
    'java.lang.Integer',
    NULL,
    1,
    1,
    NOW(),
    NULL,
    NULL,
    0,
    NULL,
    UUID()
);

-- 2. ParentLocationId - Stores the parent location ID (for location hierarchy)
INSERT INTO person_attribute_type (name, description, format, foreign_key, searchable, creator, date_created, changed_by, date_changed, retired, retire_reason, uuid)
VALUES (
    'ParentLocationId',
    'Parent Location ID for location hierarchy',
    'java.lang.Integer',
    NULL,
    1,
    1,
    NOW(),
    NULL,
    NULL,
    0,
    NULL,
    UUID()
);

-- 3. ParentLocationUuid - Stores the parent location UUID
INSERT INTO person_attribute_type (name, description, format, foreign_key, searchable, creator, date_created, changed_by, date_changed, retired, retire_reason, uuid)
VALUES (
    'ParentLocationUuid',
    'Parent Location UUID for location hierarchy',
    'java.lang.String',
    NULL,
    1,
    1,
    NOW(),
    NULL,
    NULL,
    0,
    NULL,
    UUID()
);

-- ============================================
-- VISIT ATTRIBUTE TYPES
-- ============================================

-- 4. Visit Location Id - Stores the OpenMRS location ID where visit occurred
INSERT INTO visit_attribute_type (name, description, data_type, creator, date_created, changed_by, date_changed, retired, retire_reason, uuid)
VALUES (
    'Visit Location Id',
    'OpenMRS Location ID where visit occurred',
    'org.openmrs.util.SimpleObject',
    1,
    NOW(),
    NULL,
    NULL,
    0,
    NULL,
    UUID()
);

-- 5. Visit Parent Location Id - Stores the parent location ID for the visit location
INSERT INTO visit_attribute_type (name, description, data_type, creator, date_created, changed_by, date_changed, retired, retire_reason, uuid)
VALUES (
    'Visit Parent Location Id',
    'Parent Location ID for the visit location (for hierarchy)',
    'org.openmrs.util.SimpleObject',
    1,
    NOW(),
    NULL,
    NULL,
    0,
    NULL,
    UUID()
);

-- ============================================
-- VERIFICATION QUERIES
-- ============================================

-- Verify person attribute types were created
SELECT 'PERSON ATTRIBUTE TYPES' as type, person_attribute_type_id, name, format FROM person_attribute_type
WHERE name IN ('LocationId', 'ParentLocationId', 'ParentLocationUuid')
ORDER BY date_created DESC;

-- Verify visit attribute types were created
SELECT 'VISIT ATTRIBUTE TYPES' as type, visit_attribute_type_id, name, data_type FROM visit_attribute_type
WHERE name IN ('Visit Location Id', 'Visit Parent Location Id')
ORDER BY date_created DESC;

