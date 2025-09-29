Good question — and you’ve already sniffed out the core problem: **test base classes are a double-edged sword**.

### Why base classes seem appealing

* They centralize common setup (e.g., test data, mocks, configuration).
* They reduce boilerplate — fewer lines copied across test classes.
* They feel “DRY.”

### Why they usually backfire

* **Hidden coupling**: Suddenly, your tests depend on behavior defined *elsewhere*, making failures harder to debug.
* **Over-generalization**: Different test classes often diverge subtly. The base class balloons with conditionals to “support” everyone.
* **Fragility**: Change one thing in the base class, and you break 20 test classes.
* **Readability cost**: When you open a test, you don’t immediately see the setup; you have to chase inheritance.

If your test code becomes harder to understand than the production code, you’ve lost.

### Better alternatives (JUnit 5 specific)

1. **Composition over inheritance**

    * Move common setup into **utility methods** or **factory/helper classes**.
    * Example: instead of `AbstractDatabaseTest`, create `TestDatabaseFactory` that provides `newTestDb()`, `cleanDb()`, etc.

2. **Parameterized tests / dynamic tests**

    * Often the “need for a base class” is just multiple variations of the same logic. Parameterization is more explicit.

3. **`@TestInstance(Lifecycle.PER_CLASS)` and `@BeforeAll`**

    * If setup cost is the issue, you can configure lifecycle once per class without inheritance.

4. **Extensions (JUnit 5 feature)**

    * Create a `BeforeEachCallback` or `ParameterResolver`.
    * Example: instead of a `BaseWebTest` that wires up mock MVC, write an extension:

      ```java
      @ExtendWith(MockMvcExtension.class)
      class MyWebTest {
          @Autowired MockMvc mvc;
      }
      ```
    * This is *much* cleaner than inheritance and scales better.

5. **Builder/test data pattern**

    * Instead of shared base setup, define builders for your domain objects: `UserBuilder.aDefaultUser().withName("Mark").build()`.
    * Keeps test code explicit and readable.

### Rule of thumb

* If your base class is just sharing utility **methods** (not state), that’s fine — but at that point, it shouldn’t be a base class, it should be a helper.
* If it’s sharing **state or lifecycle** setup, you’re probably better off with JUnit 5 **extensions**.

---

👉 My recommendation: **don’t use test base classes** for setup. They’re a legacy pattern from JUnit 3/4 days. With JUnit 5’s extensions and better composition support, you can get the same benefits without the headaches.

---

Do you want me to show you a concrete before/after example of replacing a `BaseTest` class with a JUnit 5 extension, so you can see the difference in readability and maintainability?
