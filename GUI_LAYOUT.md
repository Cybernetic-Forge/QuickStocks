# Company Settings GUI Layout

## Visual Layout (6 rows x 9 columns = 54 slots)

```
┌───────────────────────────────────────────────────────────────────┐
│ Row 1 (0-8): Header Information                                   │
│ [0] Balance    [1]    [2]    [3]    [4] Company    [5]    [6]    │
│     (Gold      Empty  Empty  Empty   Info          Empty  Empty   │
│      Ingot)                          (Gold Helmet)                │
│                                                                    │
│ [7]    [8] Your Job                                               │
│ Empty  (Name Tag)                                                 │
├───────────────────────────────────────────────────────────────────┤
│ Row 2 (9-17): Empty / Future Use                                  │
│ [Empty slots]                                                     │
├───────────────────────────────────────────────────────────────────┤
│ Row 3 (18-26): Empty / Action Button Row                          │
│ [18]   [19] Employees  [20] Jobs     [21] Deposit                │
│ Empty  (Player Head)   (Book)        (Hopper)                    │
│                                                                    │
│ [22] Withdraw*  [23] Assign*   [24] Invite*    [25] Create Job*  │
│ (Dispenser)     (Ench. Book)   (Paper)         (Book)            │
│                                                                    │
│ [26]                                                              │
│ Empty                                                             │
├───────────────────────────────────────────────────────────────────┤
│ Row 4 (27-35): Empty                                              │
│ [Empty slots]                                                     │
├───────────────────────────────────────────────────────────────────┤
│ Row 5 (36-44): Empty                                              │
│ [Empty slots]                                                     │
├───────────────────────────────────────────────────────────────────┤
│ Row 6 (45-53): Navigation                                         │
│ [45]   [46]   [47]   [48]   [49] Refresh  [50]   [51]   [52]    │
│ Empty  Empty  Empty  Empty   (Clock)      Empty  Empty  Empty    │
│                                                                    │
│ [53] Close                                                        │
│ (Barrier)                                                         │
└───────────────────────────────────────────────────────────────────┘
```

**Note**: Items marked with * are only shown if the player has the required permissions:
- Withdraw: Requires `canWithdraw` permission
- Assign: Requires `canManageCompany` permission
- Invite: Requires `canInvite` permission
- Create Job: Requires `canCreateTitles` permission

## Item Details

### Header Row
- **Slot 0 - Balance Display**
  - Material: `GOLD_INGOT`
  - Name: "Company Balance" (Gold)
  - Lore: Shows balance and usage instructions

- **Slot 4 - Company Info**
  - Material: `GOLDEN_HELMET`
  - Name: Company name (Gold)
  - Lore: Type, Balance, Owner, Creation date

- **Slot 8 - Your Job**
  - Material: `NAME_TAG`
  - Name: "Your Position" (Yellow)
  - Lore: Job title and permissions with checkmarks

### Action Buttons Row
- **Slot 19 - View Employees**
  - Material: `PLAYER_HEAD`
  - Name: "View Employees" (Aqua)
  - Action: Execute `/company employees <company>`

- **Slot 20 - View Jobs**
  - Material: `WRITABLE_BOOK`
  - Name: "View Job Titles" (Aqua)
  - Action: Execute `/company jobs <company>`

- **Slot 21 - Deposit**
  - Material: `HOPPER`
  - Name: "Deposit Funds" (Green)
  - Action: Show command hint

- **Slot 22 - Withdraw** (Permission: canWithdraw)
  - Material: `DISPENSER`
  - Name: "Withdraw Funds" (Gold)
  - Action: Show command hint

- **Slot 23 - Assign Job** (Permission: canManageCompany)
  - Material: `ENCHANTED_BOOK`
  - Name: "Assign Job Title" (Aqua)
  - Action: Show command hint

- **Slot 24 - Invite Player** (Permission: canInvite)
  - Material: `PAPER`
  - Name: "Invite Player" (Green)
  - Action: Show command hint

- **Slot 25 - Create Job** (Permission: canCreateTitles)
  - Material: `BOOK`
  - Name: "Create Job Title" (Light Purple)
  - Action: Show command hint

### Navigation Row
- **Slot 49 - Refresh**
  - Material: `CLOCK`
  - Name: "Refresh" (Yellow)
  - Action: Reload company data

- **Slot 53 - Close**
  - Material: `BARRIER`
  - Name: "Close" (Red)
  - Action: Close inventory

## Color Coding
- **Gold**: Company name, balance, withdraw button
- **Yellow**: Job position, refresh, command hints
- **Aqua**: View/information actions, assign job
- **Green**: Positive actions (deposit, invite)
- **Light Purple**: Creation actions
- **Red**: Close/cancel
- **Gray**: Descriptive text
- **White**: Data values
