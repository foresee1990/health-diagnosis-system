# Frontend UI Design Specification

Design a **web frontend UI** for an **AI-based Health Consultation System**.

The style should be a combination of:

* Professional **medical system UI**
* **Chat-based consultation interface**
* Light inspiration from **Apple Health design language**

Avoid strong "AI futuristic" styles.
The interface should feel **professional, trustworthy, calm, and health-focused**.

---

# Overall Layout

Use a **three-part layout**:

Top Navigation Bar
Left Sidebar
Main Chat Area

```
-----------------------------------------
Top Navigation Bar
Logo | System Name | User Avatar
-----------------------------------------
Sidebar |   Chat Consultation Area
        |
        |
        |
-----------------------------------------
```

---

# Top Navigation Bar

Elements:

* Logo (simple medical style)
* System Name: "Health AI Consultation"
* User avatar on the right
* Optional dropdown for profile / logout

Design style:

* Clean
* Minimal
* Light shadow
* White background

Height:

```
64px
```

---

# Left Sidebar (Consultation Navigation)

Functions:

1. New Consultation Button
2. Consultation History List

Structure:

```
[ + New Consultation ]

Recent Consultations
- Consultation 1
- Consultation 2
- Consultation 3
```

Design requirements:

* Vertical layout
* Scrollable history list
* Highlight active consultation
* Soft rounded corners

Sidebar width:

```
260px
```

---

# Main Area (AI Chat Consultation)

The right side is the **chat consultation interface**.

Layout:

```
--------------------------------
Chat Header
--------------------------------
Message List
--------------------------------
Input Box
--------------------------------
```

### Chat Header

Displays:

* Consultation title
* Optional status indicator

Example:

```
AI Health Consultation
```

---

### Message Area

Two message roles:

User message
AI doctor response

Design:

User message:

* Align right
* Light blue background

AI response:

* Align left
* White background
* Slight border

Example:

User bubble:

```
background: #E8F3FF
```

AI bubble:

```
background: #FFFFFF
border: 1px solid #E5E7EB
```

---

### Chat Input Area

Bottom input box similar to modern chat apps.

Components:

* Text input
* Send button
* Optional attachment icon

Input style:

* Rounded corners
* Large padding
* Clear and friendly

---

# Design Style

Primary UI Goals:

* Professional
* Medical-grade trust
* Calm and clean
* Easy to read
* Minimal distractions

Avoid:

* Neon colors
* Cyberpunk style
* Overly futuristic AI visuals

---

# Color System

Primary Color

```
#2F80ED
```

Use for:

* Buttons
* Active states
* Important highlights

Background

```
#F5FAFF
```

Use for:

* Page background
* Soft medical atmosphere

Neutral Colors

```
#FFFFFF  (cards / chat bubbles)
#F3F4F6  (light UI surfaces)
#E5E7EB  (borders)
#374151  (text)
```

---

# Apple Health Design Influence

Subtle inspiration only.

Include:

* Soft rounded corners
* Spacious padding
* Simple icons
* Card-style layout
* Calm color palette

Avoid copying exact Apple UI.

---

# Component Style

Buttons:

```
border-radius: 10px
font-weight: 500
```

Cards:

```
border-radius: 12px
box-shadow: subtle
```

Chat bubbles:

```
border-radius: 14px
padding: 12px 16px
```

---

# Typography

Font style:

Clean sans-serif.

Recommended:

```
Inter
System UI
```

Text hierarchy:

Title

```
18px
font-weight: 600
```

Normal text

```
14px
```

---

# UX Principles

The interface should feel like:

* Talking to a **professional medical assistant**
* Calm and reassuring
* Easy for patients to use

The chat interface must feel **comfortable and natural**, similar to modern messaging apps.

---

# Responsive Behavior

Desktop-first layout.

Minimum supported width:

```
1280px
```

Sidebar collapsible on smaller screens.

---

# Deliverables

Generate:

1. Page layout structure
2. Component hierarchy
3. CSS styling
4. Chat UI components
5. Sidebar consultation list
6. Top navigation bar

Use **clean, modular component design**.
