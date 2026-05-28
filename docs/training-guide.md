# Vxnaid Application — Operator Training Guide

## What Is Vxnaid?

Vxnaid is an Android tablet application developed by Johnson & Johnson Global Public Health. It helps vaccination campaign operators:

- Identify and register children for vaccination
- Record vaccine doses administered
- Track scheduled, completed, and missed visits
- Generate reports on vaccination activity

The app runs on Android 6.0+ tablets (7 inches or larger) and works offline, syncing data with a backend server when a network connection is available.

---

## User Roles

| Role | What They Do |
|---|---|
| **Sync Admin** | Sets up the app on the tablet (runs the Setup Wizard) |
| **Operator** | Day-to-day use: registering children, recording visits |

> Operators log in through the main login screen. Sync Admins log in through the Setup Wizard only.

---

## App Screens Overview

```
Splash Screen
    └── Setup Wizard (first-time or Sync Admin)
    └── Login Screen
            └── Home Page (Find or Register a Child)
                    ├── Participant Matching (returning child)
                    │       └── Visit Screen
                    └── Register New Child
                            └── Visit Screen
```

---

## Part 1: First-Time Setup (Sync Admin)

The Setup Wizard runs once before operators can use the app. A Sync Admin must complete it.

**Steps in the wizard:**
1. **Permissions** — grant camera, storage, and USB permissions
2. **Backend URL** — enter the server address the app connects to
3. **Sync Configuration** — choose sync settings (which data to download)
4. **Site Selection** — select the vaccination site/location for this tablet

Once setup is complete, the tablet is ready for operator login.

To re-run the wizard later: go to **Settings > Re-run Setup Wizard**.

---

## Part 2: Logging In

1. Open the app — you will reach the **Login Screen**.
2. Enter your **username** and **password** (provided by your administrator).
3. Select your **Visit Place**:
   - **Static** — a fixed clinic location
   - **Outreach** — a temporary outreach post (you will be asked to enter the outreach name)
   - **School** — a school-based vaccination session
4. Tap **Sign In**.

> Your credentials are provided by your administration. You cannot register or reset passwords through the app.

**Session expiry:** If your session expires while working, a dialog will appear asking you to re-enter your password to continue, or terminate the session.

---

## Part 3: Home Page — Finding or Registering a Child

After login, you reach the **Home Page**. This is where every interaction with a child begins.

### Identifying a Returning Child

The app offers multiple identification options — use whichever applies:

| Option | How It Works |
|---|---|
| **Child ID / QR Code** | Scan the barcode on the child's card, or type the ID manually |
| **Phone Number** | Enter the phone number registered for the child |
| **Iris Scan** | Use the attached iris scanner device (left eye, then right eye) |
| **Mother's Name** | Enter the mother's first and/or last name to search |

After entering one of these, the app shows a **matching screen** with possible results. Select the correct child and tap **Complete Return Visit** to proceed to the Visit screen.

### Registering a New Child

If the child is not found in the system, you can register them as a new participant.

**Registration steps:**

1. **Participant Details** — fill in:
   - First name, last name
   - Gender
   - Date of birth (or estimated year)
   - Birth weight (optional)
   - Child category: National / Foreigner / Refugee
   - Language preference
   - Vaccination regimen/program
   - Address (country, region, district, village)
   - Mother's first and last name
   - Father's first and last name (optional)
   - Phone number (optional)
   - National ID (optional)

2. **Photo** — take a photo of the child using the tablet camera

3. **Confirm** — review details and tap **Register**

After successful registration, the app offers to continue directly to a Visit for that child.

---

## Part 4: Recording a Visit

The Visit screen is where you record what happened during a child's appointment.

### Visit Types

| Type | When to Use |
|---|---|
| **Dosing** | Child receives a vaccine dose |
| **Adverse Effects** | Reporting a side effect from a previous dose |
| **Other** | Any other clinical interaction |

### Dosing Visit — Step by Step

1. The screen shows the child's details (name, ID, regimen, visit number, scheduled date).
2. Under **Vaccines tab**, select or scan the vaccine vial:
   - Scan the **barcode** on the vial using the camera
   - Or enter the barcode manually
   - Select the **vaccine manufacturer** from the dropdown
3. Under **Capture Data tab**, enter any additional observations (weight, height, MUAC, etc.) if prompted.
4. Check for **contraindications** — the app asks whether any conditions exist that would prevent vaccination.
5. **Referral** — if the child needs to be sent to a health facility, tap Refer, select the clinic, and enter a reason.
6. **Save Visit** — the app shows a proposed next visit date. Confirm or adjust the date, then tap **Save Visit**.

> If a child is receiving a dose outside the scheduled window, the app will show a confirmation warning before proceeding.

### Adverse Effects Visit

- Select **Adverse Effects** as the visit type
- Describe the adverse effect in the text field
- Tap **Report Adverse Effects**
- Inform parents about potential side effects of vaccines when relevant

### Rescheduling a Visit

If the child cannot attend on the scheduled date:
- On the visit screen, select the option to reschedule
- Enter a reason for rescheduling
- Set the new date
- Tap **Save Scheduled Visit**

---

## Part 5: Visits Overview

Access **Visits Overview** from the main menu to see all scheduled activity.

The overview has three tabs:

| Tab | Contents |
|---|---|
| **Scheduled Visits** | Children with upcoming appointments |
| **Visit History** | Past completed visits |
| **Missed Visits** | Children who did not attend their scheduled appointment |

Tap any row to view the child's details or the visit details.

---

## Part 6: Reports

The Reports section gives supervisors a summary of vaccination activity.

### Available Reports

| Report | What It Shows |
|---|---|
| **Registered Participants** | List of all registered children at the site |
| **Vaccines Overview** | Doses administered, filterable by vaccine and date |
| **HMIS 105 Report** | Standard government health management report by age group and vaccine |

Age groups used in HMIS 105:
- 0–11 months
- 12–59 months
- 5–14 years
- 14+ years

---

## Part 7: Child Health Plus

Child Health Plus records additional health services given to a child beyond standard immunisation (e.g., deworming, vitamin A, LLIN distribution).

**Steps:**
1. Open Child Health Plus from the menu
2. Select the service provided
3. Enter the administration date
4. Confirm the child's details
5. Submit — a success screen confirms the record was saved

---

## Part 8: Data Sync

The app stores data locally on the tablet and syncs with the backend server when online.

- A **sync status banner** appears at the top of the screen when sync is active or paused.
- Sync runs automatically in the background.
- To force an immediate sync: go to **Settings > Sync Now**.
- Sync continues even in airplane mode for local data; it will upload/download once a connection is restored.

---

## Part 9: Settings

Accessible from the overflow menu (top right) on the login or home screen.

| Setting | Purpose |
|---|---|
| **Backend URL** | View or change the server address |
| **Biometric Licenses** | View, activate, or deactivate iris scan licenses |
| **Device ID** | Copy the tablet's device ID (used for support) |
| **Sync Now** | Manually trigger a data sync |
| **Re-run Setup Wizard** | Repeat initial configuration |
| **Share Logs** | Export app logs for troubleshooting |

---

## Quick Reference: Common Workflows

### Returning Child Gets a Dose
1. Home Page → scan child's ID card or enter phone number
2. Select matching child → Complete Return Visit
3. Visit screen → Vaccines tab → scan/enter barcode → select manufacturer
4. Capture Data tab → enter measurements if required
5. Check contraindications → no referral needed → Save Visit → confirm next visit date

### New Child Being Registered
1. Home Page → child not found → Register New Child
2. Fill in all required details → take photo → Register
3. Continue with Visit → follow dosing steps above

### Child Missed Appointment
1. Visits Overview → Missed Visits tab
2. Review the list → contact family if phone number is available

### Reporting Adverse Effect
1. Home Page → find the child
2. Visit screen → select "Adverse Effects" visit type
3. Describe the effect → Report Adverse Effects

---

## Troubleshooting Quick Tips

| Problem | What to Do |
|---|---|
| Cannot log in — no network | If your username was previously synced, log in offline. Otherwise, a network connection is required. |
| Iris scanner not detected | Check the USB OTG connector is properly attached; restart the app |
| Session expired dialog | Enter your password to renew, or terminate and log in again |
| Sync not completing | Go to Settings > Sync Now; check the network connection |
| App update available | A banner will prompt you to download and install the update; do so as soon as possible |
| Child not found in matching | Verify you are using the correct ID/phone; if truly new, register them |

---

## Key Terms

| Term | Meaning |
|---|---|
| **Regimen / Program** | The specific vaccine schedule the child is enrolled in |
| **Dose number** | Which dose in the schedule is being administered (1st, 2nd, etc.) |
| **Visit window** | The acceptable date range before or after the scheduled visit date |
| **Static visit** | Vaccination at a fixed clinic |
| **Outreach visit** | Vaccination at a temporary field location |
| **MUAC** | Mid-upper arm circumference — a nutritional measurement |
| **Z-score** | A statistical measure of a child's growth relative to the reference population |
| **LLIN** | Long-lasting insecticidal net |
| **HMIS 105** | Health Management Information System standard report form |
| **Iris template** | The biometric fingerprint of a child's iris used for identification |