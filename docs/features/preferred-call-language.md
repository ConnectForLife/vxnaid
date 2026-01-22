# Preferred Call Language Feature

## Overview
This feature implements the ability to capture a participant's preferred language for automated call reminders (IVR calls) during registration. This uses the **existing OpenMRS "personLanguage" attribute** instead of creating a new one.

## Implementation Details

### 1. Data Model Changes

#### Constants
**File:** `Constants.kt`
- Uses existing: `ATTRIBUTE_LANGUAGE = "personLanguage"`
- No new attribute needed!

#### Participant Entity
**File:** `Participant.kt`
- Uses existing accessor: `language: String?` - retrieves the value from attributes using `ATTRIBUTE_LANGUAGE`
- No extension function needed - uses existing attribute infrastructure

#### ParticipantManager
**File:** `ParticipantManager.kt`
- Updated to use the selected language from dropdown
- Stores the value in existing `personLanguage` attribute
- No longer hardcoded to "English"

### 2. UI Changes

#### ViewModel
**File:** `RegisterParticipantParticipantDetailsViewModel.kt`
- Added field: `preferredCallLanguage = mutableLiveData<DisplayValue>()` (for UI binding only)
- Added validation: `preferredCallLanguageValidationMessage`
- Updated `doRegistration()` to use selected language value for the existing `language` parameter

#### Flow ViewModel
**File:** `RegisterParticipantFlowViewModel.kt`
- No changes needed - uses existing `language` field in `RegisterDetails`

### 3. How It Works

#### During Registration:
1. User fills in participant details
2. User selects preferred call language from dropdown (labeled "Preferred Call Language")
3. Options available: English, Luganda (hardcoded)
4. On submit, the selected language is saved to the existing `personLanguage` attribute
5. If not selected, defaults to "English"

#### Data Storage:
```kotlin
// In person attributes map - uses EXISTING attribute
personAttributes[Constants.ATTRIBUTE_LANGUAGE] = "English" // or "Luganda"
// Attribute name in OpenMRS: "personLanguage"
```

#### Accessing the Data:
```kotlin
// From any ParticipantBase object
val callLanguage = participant.language
// Returns: "English", "Luganda", etc.
```

## OpenMRS Integration

### Using Existing Person Attribute

**IMPORTANT:** This feature uses the **existing "personLanguage" attribute** that's already part of the standard OpenMRS person model. No new Person Attribute Type needs to be created!

### REST API Integration

#### Setting the Attribute (during registration):
```json
{
  "person": {
    "attributes": [
      {
        "attributeType": "personLanguage",
        "value": "English"
      }
    ]
  }
}
```

#### Retrieving the Attribute:
```json
GET /openmrs/ws/rest/v1/person/{uuid}?v=full

Response:
{
  "attributes": [
    {
      "attributeType": "personLanguage",
      "value": "English"
    }
  ]
}
```

## IVR Callflow Integration

### Recommended Implementation

#### 1. Pre-Call Language Detection
```pseudo
function makeCall(phoneNumber):
    person = findPersonByPhone(phoneNumber)
    language = person.language  // Uses existing personLanguage attribute
    
    if language:
        // Play call in preferred language directly
        playCallflow(language)
    else:
        // Show language selection menu
        showLanguageSelectionMenu()
```

#### 2. Language Selection Menu (Fallback)
```pseudo
function showLanguageSelectionMenu():
    play("Press 1 for English. Nyiga 2 olw'Oluganda.")
    
    selection = getUserInput()
    
    if selection == "1":
        language = "English"
    else if selection == "2":
        language = "Luganda"
    
    // Save selection for future calls
    updatePersonAttribute(personId, "personLanguage", language)
    
    playCallflow(language)
```

### Benefits
1. **Improved UX:** Users hear calls in their preferred language immediately
2. **Reduced Friction:** No need to press buttons for language selection on repeat calls
3. **Data Quality:** Captures language preference during registration
4. **Scalability:** Easy to add more languages in the future

## Database Structure

### Local Storage (VXNAID App)
```kotlin
// Stored in ParticipantEntity attributes field (JSON string)
{
  "Preferred Call Language": "English"
}
```

### OpenMRS Database
```sql
-- person_attribute table
INSERT INTO person_attribute (
  person_id, 
  person_attribute_type_id, 
  value
) VALUES (
  123,  -- person_id
  456,  -- person_attribute_type_id for "Preferred Call Language"
  'English' -- or concept_id if using coded values
);
```

## Future Enhancements

### 1. Multi-Language Support
- Add more language options (Swahili, Ateso, etc.)
- Update configuration to support additional languages

### 2. Smart Language Detection
- Detect language from phone area code
- Use location-based language suggestions

### 3. Language Learning
- Track which language user responds to most
- Auto-update preference based on user behavior

### 4. Validation Rules
- Ensure selected language is supported by IVR system
- Validate against available voice recordings

## Testing

### Manual Test Cases

#### TC1: Registration with Language Selection
1. Start participant registration
2. Fill in required fields
3. Select "English" from Preferred Call Language dropdown
4. Submit registration
5. **Expected:** Language saved in participant attributes

#### TC2: Registration without Language Selection
1. Start participant registration
2. Fill in required fields
3. Leave Preferred Call Language empty
4. Submit registration
5. **Expected:** Registration succeeds, language attribute is null

#### TC3: Update Participant Language
1. Open existing participant
2. Change Preferred Call Language to "Luganda"
3. Save changes
4. **Expected:** Language attribute updated

#### TC4: IVR Call with Saved Language
1. Make call to participant with saved language
2. **Expected:** Call plays in saved language without menu
3. **Expected:** No "Press 1 for English" prompt

#### TC5: IVR Call without Saved Language
1. Make call to participant without saved language
2. **Expected:** Language selection menu appears
3. Select language
4. **Expected:** Preference saved for future calls

## Configuration

### VXNAID Configuration File
Ensure your configuration includes language options:

```json
{
  "personLanguages": [
    {
      "name": "English",
      "code": "en"
    },
    {
      "name": "Luganda",
      "code": "lg"
    }
  ]
}
```

## API Endpoints

### Get Participant with Language
```
GET /api/participant/{uuid}
```

Response:
```json
{
  "participantUuid": "123-456-789",
  "participantId": "ABC123",
  "attributes": {
    "Preferred Call Language": "English"
  }
}
```

### Update Participant Language
```
PUT /api/participant/{uuid}
```

Request:
```json
{
  "attributes": {
    "Preferred Call Language": "Luganda"
  }
}
```

## Error Handling

- Invalid language value → Silently ignore, use fallback menu
- Missing language attribute → Show language selection menu
- OpenMRS sync failure → Store locally, sync when connection restored

## Best Practices

1. **Always provide fallback:** If language preference is missing, show selection menu
2. **Update preference:** Allow users to change language during calls
3. **Validate values:** Only accept configured language values
4. **Log changes:** Track when language preference changes for analytics
5. **Test thoroughly:** Verify both English and Luganda callflows work correctly

## Dependencies

- OpenMRS Person Attribute Type: `Preferred Call Language`
- IVR System with multi-language support
- Voice recordings in all supported languages
- Configuration with language options

## Rollout Checklist

- [ ] Create Person Attribute Type in OpenMRS
- [ ] Deploy VXNAID app with language selection
- [ ] Configure IVR system to use language preference
- [ ] Record voice prompts in all languages
- [ ] Test end-to-end workflow
- [ ] Train operators on new field
- [ ] Monitor language selection data
- [ ] Gather user feedback

## Support

For questions or issues:
1. Check OpenMRS Person Attribute configuration
2. Verify IVR system integration
3. Review call logs for language-specific issues
4. Check configuration file for language options

