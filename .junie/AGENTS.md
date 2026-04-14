# 📘 KDoc Guidelines for Professional Kotlin Development

These guidelines define how to write **clear, consistent, and professional KDoc documentation** across the codebase. They are intended for **professional development environments** and tools like JetBrains Junie.

---

## 1. General Principles

* Every **public class, function, and property MUST have KDoc**
* **All KDoc MUST be written in English**
* Documentation should explain **what and why**, not just repeat the code
* Use **clear, precise, and professional language**
* Avoid redundant phrases like “Gets the value of…”
* Write in **complete sentences**
* Documentation may and should be **detailed when necessary** (especially for complex logic, domain rules, or edge cases)
* Prefer clarity over brevity when behavior is non-trivial

---

## 2. Function Documentation (STRICT RULES)

### 🔴 Mandatory Requirements

Every function **MUST** include:

* A description of behavior
* `@param` for **every parameter (NO EXCEPTIONS)**
* `@return` for **every non-Unit function (MANDATORY)**
* `@throws` when exceptions are part of the contract
* `@since` for **every function (MANDATORY)**

### ⚠️ Important

* **`@return` MUST NEVER be omitted**, even if the return value seems obvious
* **`@since` MUST ALWAYS be present** and reflect the version the function was introduced
* Missing `@param`, `@return`, or `@since` is considered **invalid documentation**

---

### ✅ Example

```kotlin id="h1htq9"
/**
 * Calculates the total price including tax.
 *
 * Applies the given tax rate to the base price and returns the final amount.
 * This method validates input values before performing the calculation.
 *
 * @param basePrice The original price before tax. Must be non-negative.
 * @param taxRate The tax rate as a decimal (e.g., 0.19 for 19%).
 * @return The final price including tax.
 * @throws IllegalArgumentException If basePrice or taxRate is negative.
 * @since 1.2.0
 */
fun calculateTotalPrice(basePrice: Double, taxRate: Double): Double {
    require(basePrice >= 0 && taxRate >= 0)
    return basePrice * (1 + taxRate)
}
```

---

## 3. Property Documentation

### 🔴 Mandatory Rules

* **Every property MUST have its own KDoc block**
* **`@property` is strictly forbidden**
* `@since` is **MANDATORY for every property**
* Documentation must be placed **directly above the property**

Each property should describe:

* Purpose and meaning
* Constraints (if any)
* Format or units (if applicable)
* Side effects or lifecycle behavior (if relevant)

### ✅ Example

```kotlin id="8cn8v6"
/**
 * The unique identifier of the user.
 *
 * This value is immutable and assigned during creation.
 * It must remain stable and globally unique.
 *
 * @since 1.0.0
 */
val userId: String

/**
 * The email address of the user.
 *
 * Must be a valid email format. Used for authentication and communication.
 * Updating this value may trigger verification workflows.
 *
 * @since 1.0.0
 */
var email: String
```

---

## 4. Class Documentation

Each class should describe:

* Its responsibility
* Key behavior
* Important constraints or lifecycle notes
* Optional usage examples

### 🔴 Mandatory Rules

* **DO NOT use `@property` in class-level KDoc**
* Properties are documented **individually only**
* `@since` is **MANDATORY for every class**

---

### ✅ Example (Correct)

```kotlin id="02pj4e"
/**
 * Represents a user in the system.
 *
 * This class encapsulates user-related data and validation logic.
 * It belongs to the domain layer and should not contain persistence concerns.
 *
 * Instances should be created through a dedicated service to enforce invariants.
 *
 * @since 1.0.0
 */
class User(

    /**
     * The unique identifier of the user.
     *
     * Immutable and assigned during creation. Must be globally unique.
     *
     * @since 1.0.0
     */
    val userId: String,

    /**
     * The email address of the user.
     *
     * Must follow a valid email format. Used for authentication and notifications.
     *
     * @since 1.0.0
     */
    var email: String
)
```

---

### ❌ Invalid Example

```kotlin id="lgtz4h"
/**
 * Represents a user in the system.
 *
 * @property userId The unique identifier of the user.
 * @property email The user's email address.
 */
class User(
    val userId: String,
    var email: String
)
```

---

## 5. Return Values (STRICT)

* **Every non-Unit function MUST have `@return`**
* Even trivial return values must be documented
* Document edge cases (e.g., `null`, empty results, fallback values)

### ✅ Example

```kotlin id="jq61fj"
/**
 * Finds a user by their ID.
 *
 * @param userId The unique identifier of the user.
 * @return The matching user, or null if no user exists with the given ID.
 * @since 1.1.0
 */
fun findUser(userId: String): User?
```

---

## 6. Exceptions (`@throws`)

Use `@throws` when:

* The exception is part of the expected behavior
* The caller must be aware of it

### ✅ Example

```kotlin id="m4r9d3"
/**
 * Parses a string into an integer.
 *
 * @param input The string to parse.
 * @return The parsed integer value.
 * @throws NumberFormatException If the input is not a valid integer.
 * @since 1.0.0
 */
fun parseInt(input: String): Int = input.toInt()
```

---

## 7. Examples (Recommended)

Examples should be included when:

* The usage is not obvious
* The API is reusable or exposed
* Misuse is likely without clarification

### ✅ Example

````kotlin id="6rm3f4"
/**
 * Formats a username into a display-friendly format.
 *
 * @param username The raw username.
 * @return A formatted username with the first letter capitalized.
 *
 * Example:
 * ```
 * formatUsername("john") // returns "John"
 * ```
 *
 * @since 1.0.0
 */
fun formatUsername(username: String): String {
    return username.replaceFirstChar { it.uppercase() }
}
````

---

## 8. Consistency Rules

* `@param` → **ALWAYS required**
* `@return` → **ALWAYS required for non-Unit**
* `@since` → **ALWAYS required (no exceptions)**
* Never mix documentation styles
* Keep formatting consistent across the project
* Keep documentation in sync with implementation

---

## 9. What to Avoid ❌

* Missing `@param`
* Missing `@return`
* Missing `@since`
* Non-English documentation
* Using `@property`
* Vague or superficial descriptions
* Outdated or incorrect documentation
* Copy-paste KDocs that do not match the code

---

## 10. Summary

Professional KDoc must be:

* **Strict** → all required tags are present
* **English-only** → no mixed language
* **Complete** → parameters and return values documented
* **Versioned** → every element has `@since`
* **Accurate** → reflects real behavior
* **Detailed when necessary** → especially for complex logic
* **Consistent** → same rules applied everywhere

---
