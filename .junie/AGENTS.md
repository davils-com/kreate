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
* Documentation may and should be **detailed when necessary** (e.g., for complex logic, domain rules, or edge cases)
* Prefer clarity over brevity when the behavior is non-trivial

---

## 2. Function Documentation

Every function must include:

* A short description of what it does
* `@param` for **every parameter (mandatory)**
* `@return` if the function returns a value
* `@throws` if exceptions are relevant
* Optional `@since` for versioning
* Optional example if helpful

### ✅ Example

```kotlin
/**
 * Calculates the total price including tax.
 *
 * Applies the given tax rate to the base price and returns the final amount.
 * This method performs validation to ensure that only valid numeric inputs are processed.
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

### 🔴 Rule

**Every property MUST have its own KDoc block.**
**Do NOT use `@property` for documentation.**

Each property should describe:

* Purpose and meaning
* Constraints (if any)
* Format or units (if applicable)
* Default behavior (if relevant)

### ✅ Example

```kotlin
/**
 * The unique identifier of the user.
 *
 * This value is immutable and assigned during creation.
 * It is used as the primary reference across the system and must remain stable.
 */
val userId: String

/**
 * The email address of the user.
 *
 * Must be a valid email format. Used for authentication and communication.
 * Changing this value may trigger verification workflows.
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

### 🔴 Important Rule

**Do NOT use `@property` in class-level KDoc.**
Properties must be documented individually.

### ✅ Example (Preferred)

```kotlin
/**
 * Represents a user in the system.
 *
 * This class encapsulates user-related data and basic validation logic.
 * It is part of the domain layer and should not contain persistence logic.
 *
 * Instances are typically created via the UserService to ensure invariants are enforced.
 */
class User(

    /**
     * The unique identifier of the user.
     *
     * This value is immutable and assigned during creation.
     * It must be globally unique within the system.
     */
    val userId: String,

    /**
     * The email address of the user.
     *
     * Must be a valid email format. Used for authentication and communication.
     * Updates may require re-verification depending on business rules.
     */
    var email: String
)
```

### ❌ Avoid This

```kotlin
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

## 5. Return Values

Document return values when:

* The result is not obvious
* There are edge cases
* Special values may be returned (e.g., `null`, empty list)

### ✅ Example

```kotlin
/**
 * Finds a user by their ID.
 *
 * @param userId The unique identifier of the user.
 * @return The matching user, or null if no user exists with the given ID.
 */
fun findUser(userId: String): User?
```

---

## 6. Exceptions (`@throws`)

Document exceptions when they are:

* Part of the expected contract
* Not obvious from the function signature

### ✅ Example

```kotlin
/**
 * Parses a string into an integer.
 *
 * @param input The string to parse.
 * @return The parsed integer value.
 * @throws NumberFormatException If the input is not a valid integer.
 */
fun parseInt(input: String): Int = input.toInt()
```

---

## 7. Versioning (`@since`)

Use `@since` for:

* Public APIs
* Features that evolve over time

### Example

```kotlin
/**
 * @since 2.0.0
 */
```

---

## 8. Examples (Recommended)

Include examples when:

* Usage is not obvious
* The API is reusable
* It helps prevent misuse

### ✅ Example

````kotlin
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
 */
fun formatUsername(username: String): String {
    return username.replaceFirstChar { it.uppercase() }
}
````

---

## 9. Consistency Rules

* Always use:

  * `@param` for all parameters (**mandatory**)
  * `@return` when applicable
  * `@since` for versioned APIs
* Never mix documentation styles
* Keep formatting consistent across the project
* Ensure documentation stays in sync with implementation

---

## 10. What to Avoid ❌

* Missing `@param`
* Non-English documentation
* Overly vague descriptions
* Redundant comments
* Outdated documentation
* Copy-paste KDocs that don’t match the code
* Using KDoc instead of writing clean, self-explanatory code

---

## 11. Summary

Professional KDoc should be:

* **Complete** → all parameters and behavior documented
* **Accurate** → reflects actual implementation
* **Clear** → written in precise English
* **Detailed when necessary** → especially for complex logic
* **Consistent** → same structure across the project

---
