# Backend Location Hierarchy Fix

## Problem
The attached clinics feature in the login screen is not working because the backend API is not returning `location_id` and `parent_location` fields in the Site response.

## Diagnosis
- The app logs showed that all 128 sites had `locationId=null` and `parentLocationId=null`
- The `Site` entity in the Android app has correct JSON mappings defined:
  ```kotlin
  @Json(name = "location_id")
  val locationId: Int? = null,
  @Json(name = "parent_location")
  val parentLocationId: Int? = null,
  ```
- The mock backend correctly provides these fields (see `app/src/mockBackend/assets/mock/locations.json`)

## Solution Requirements
The backend endpoint `/openmrs/ws/rest/v1/biometric/location` must return Sites with the following structure:

### JSON Response Format
```json
{
  "results": [
    {
      "uuid": "8d6c993e-c2cc-11de-8d13-0010c6dffd0f",
      "name": "CFL Clinic",
      "country": "Uganda",
      "cluster": "Central",
      "countryCode": "UG",
      "siteCode": "CFL",
      "location_id": 1,
      "parent_location": null
    },
    {
      "uuid": "cf724edd-7307-4010-bd90-2242bf9e0fc2",
      "name": "A",
      "country": "Uganda",
      "cluster": "Central",
      "countryCode": "UG",
      "siteCode": "A",
      "location_id": 2,
      "parent_location": 1
    },
    {
      "uuid": "126b85a7-43e3-4833-b7c5-0443fff0b976",
      "name": "A1",
      "country": "Uganda",
      "cluster": "Central",
      "countryCode": "UG",
      "siteCode": "A1",
      "location_id": 3,
      "parent_location": 1
    }
  ]
}
```

### Field Explanations
- **uuid**: Unique identifier for the location
- **name**: Display name of the location/clinic
- **country**: Country name
- **cluster**: Cluster/region information
- **countryCode**: ISO country code
- **siteCode**: Short code for the site
- **location_id**: (NEW) Unique integer ID for this location - used to identify parent locations
- **parent_location**: (NEW) The `location_id` of this location's parent location (null if this is a top-level location)

### Parent-Child Relationships
The hierarchy should follow this pattern:
- Top-level locations have `parent_location: null`
- Child locations have `parent_location` set to their parent's `location_id`

Example hierarchy:
```
CFL Clinic (location_id=1, parent_location=null)
├── A (location_id=2, parent_location=1)
├── A1 (location_id=3, parent_location=1)
├── A2 (location_id=4, parent_location=1)
└── A3 (location_id=5, parent_location=1)
```

## How to Verify
After backend changes:
1. Clear app cache and reinstall the app
2. Login with the same credentials
3. Check the Logcat output for the site loading - should no longer see `locationId=null` and `parentLocationId=null`
4. The attached clinics dropdown should now populate with child clinics based on the selected parent location

## Files Affected
- Backend API endpoint: `/openmrs/ws/rest/v1/biometric/location`
- Android App: `app/src/main/java/com/jnj/vaccinetracker/login/LoginViewModel.kt` (filterAttachedClinicsByParent function)

## Related Files
- Mock data reference: `app/src/mockBackend/assets/mock/locations.json`
- Site entity definition: `app/src/main/java/com/jnj/vaccinetracker/common/domain/entities/Sites.kt`

