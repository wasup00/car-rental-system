# CI/CD Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add JaCoCo coverage enforcement (≥80%), new tests to reach that threshold, and a GitHub Actions workflow that builds, validates coverage, checks version bump, and publishes a GitHub Release on every master push.

**Architecture:** JaCoCo is added to `pom.xml` as a Maven build plugin with a hard 80% instruction coverage gate. `AuthControllerTest` and `AdminControllerTest` cover the untested auth/admin surface. The GitHub Actions workflow has two jobs: `build` (always, on PRs and master) and `release` (master push only, compares pom version vs latest git tag).

**Tech Stack:** Maven, JaCoCo 0.8.12, JUnit 5, Spring Boot Test, MockMvc, GitHub Actions, `gh` CLI

---

## File Map

| Action | File |
|--------|------|
| Modify | `pom.xml` |
| Create | `src/test/java/com/wasup/car_rental_system/controller/AuthControllerTest.java` |
| Create | `src/test/java/com/wasup/car_rental_system/controller/AdminControllerTest.java` |
| Create | `.github/workflows/ci.yml` |

---

### Task 1: Add JaCoCo to pom.xml

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add JaCoCo plugin inside `<build><plugins>`**

Replace the closing `</plugins>` in `pom.xml` with:

```xml
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.12</version>
            <executions>
                <execution>
                    <id>prepare-agent</id>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>verify</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
                <execution>
                    <id>check</id>
                    <phase>verify</phase>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>BUNDLE</element>
                                <limits>
                                    <limit>
                                        <counter>INSTRUCTION</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.80</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
```

The full `<build>` section should now look like:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.12</version>
            <executions>
                <execution>
                    <id>prepare-agent</id>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>verify</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
                <execution>
                    <id>check</id>
                    <phase>verify</phase>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>BUNDLE</element>
                                <limits>
                                    <limit>
                                        <counter>INSTRUCTION</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.80</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

- [ ] **Step 2: Run tests (report only, gate will likely fail — that's expected)**

```bash
cd car-rental-system-backend
./mvnw verify 2>&1 | tail -20
```

Expected: tests pass but coverage check fails with something like:
```
[ERROR] Rule violated for bundle car-rental-system: instructions covered ratio is 0.XX, but expected minimum is 0.80
```

Note the actual ratio — if it's already ≥0.80, skip Tasks 2 and 3.

---

### Task 2: Add AuthControllerTest

**Files:**
- Create: `src/test/java/com/wasup/car_rental_system/controller/AuthControllerTest.java`

- [ ] **Step 1: Create the test file**

```java
package com.wasup.car_rental_system.controller;

import tools.jackson.databind.ObjectMapper;
import com.wasup.car_rental_system.dto.LoginRequest;
import com.wasup.car_rental_system.dto.RegisterRequest;
import com.wasup.car_rental_system.model.*;
import com.wasup.car_rental_system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private CarRepository carRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .name("Auth Test Tenant")
                .slug("auth-test")
                .active(true)
                .build());

        userRepository.save(User.builder()
                .email("user@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .fullName("Test User")
                .role(Role.CUSTOMER)
                .tenant(tenant)
                .build());
    }

    @Test
    void login_returns200WithTokens() throws Exception {
        LoginRequest request = new LoginRequest("user@test.com", "password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("user@test.com"));
    }

    @Test
    void login_returns401WithBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest("user@test.com", "wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_returns201WithTokens() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "newuser@test.com", "securePass1", "New User", "auth-test");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("newuser@test.com"));
    }

    @Test
    void register_returns409WhenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "user@test.com", "password123", "Dup User", "auth-test");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void listTenants_returns200WithActiveTenants() throws Exception {
        mockMvc.perform(get("/auth/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("auth-test"));
    }
}
```

- [ ] **Step 2: Run just this test to verify it compiles and passes**

```bash
./mvnw test -Dtest=AuthControllerTest -pl . 2>&1 | tail -15
```

Expected: `BUILD SUCCESS`, 5 tests pass.

---

### Task 3: Add AdminControllerTest

**Files:**
- Create: `src/test/java/com/wasup/car_rental_system/controller/AdminControllerTest.java`

- [ ] **Step 1: Create the test file**

```java
package com.wasup.car_rental_system.controller;

import tools.jackson.databind.ObjectMapper;
import com.wasup.car_rental_system.dto.CreateTenantRequest;
import com.wasup.car_rental_system.model.*;
import com.wasup.car_rental_system.repository.*;
import com.wasup.car_rental_system.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private CarRepository carRepository;

    private Authentication adminAuth;
    private Authentication customerAuth;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        UserPrincipal adminPrincipal = new UserPrincipal(
                "admin-id", "admin@system.com", "Admin", null, Role.ADMIN, null);
        adminAuth = new UsernamePasswordAuthenticationToken(
                adminPrincipal, null, adminPrincipal.getAuthorities());

        UserPrincipal customerPrincipal = new UserPrincipal(
                "cust-id", "cust@test.com", "Customer", "some-tenant", Role.CUSTOMER, null);
        customerAuth = new UsernamePasswordAuthenticationToken(
                customerPrincipal, null, customerPrincipal.getAuthorities());
    }

    @Test
    void createTenant_returns201AsAdmin() throws Exception {
        CreateTenantRequest request = new CreateTenantRequest(
                "New Corp", "new-corp", "client@newcorp.com", "securePass1", "Corp Client");

        mockMvc.perform(post("/admin/tenants").with(authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("new-corp"))
                .andExpect(jsonPath("$.name").value("New Corp"));
    }

    @Test
    void createTenant_returns403AsCustomer() throws Exception {
        CreateTenantRequest request = new CreateTenantRequest(
                "New Corp", "new-corp", "client@newcorp.com", "securePass1", "Corp Client");

        mockMvc.perform(post("/admin/tenants").with(authentication(customerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllTenants_returns200AsAdmin() throws Exception {
        tenantRepository.save(Tenant.builder()
                .name("Existing Corp").slug("existing").active(true).build());

        mockMvc.perform(get("/admin/tenants").with(authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("existing"));
    }

    @Test
    void getAllUsers_returns200AsAdmin() throws Exception {
        mockMvc.perform(get("/admin/users").with(authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
```

- [ ] **Step 2: Run just this test to verify it compiles and passes**

```bash
./mvnw test -Dtest=AdminControllerTest -pl . 2>&1 | tail -15
```

Expected: `BUILD SUCCESS`, 4 tests pass.

---

### Task 4: Verify coverage gate passes

- [ ] **Step 1: Run full verify**

```bash
./mvnw verify 2>&1 | grep -E "(Tests run|Coverage|BUILD|ERROR|ratio)"
```

Expected output includes:
```
BUILD SUCCESS
```
And no line like `instructions covered ratio is 0.XX, but expected minimum is 0.80`.

- [ ] **Step 2: If coverage gate fails, check the HTML report**

Open `target/site/jacoco/index.html` in a browser to see per-class coverage.
Add tests targeting the lowest-covered classes until `./mvnw verify` succeeds.

---

### Task 5: Create GitHub Actions workflow

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: Create the workflow directory and file**

```bash
mkdir -p .github/workflows
```

Create `.github/workflows/ci.yml`:

```yaml
name: CI

on:
  push:
    branches: [master]
  pull_request:
    branches: [master]

jobs:
  build:
    name: Build & Test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Make mvnw executable
        run: chmod +x mvnw

      - name: Build and verify coverage
        run: ./mvnw verify

      - name: Upload JaCoCo report on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-report
          path: target/site/jacoco/

  release:
    name: Release
    needs: build
    if: github.event_name == 'push' && github.ref == 'refs/heads/master'
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Make mvnw executable
        run: chmod +x mvnw

      - name: Extract version from pom.xml
        id: version
        run: |
          VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)
          echo "version=$VERSION" >> $GITHUB_OUTPUT
          echo "Detected version: $VERSION"

      - name: Get latest git tag
        id: latest_tag
        run: |
          TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
          echo "tag=$TAG" >> $GITHUB_OUTPUT
          echo "Latest tag: ${TAG:-<none>}"

      - name: Check version is bumped
        run: |
          CURRENT="v${{ steps.version.outputs.version }}"
          LATEST="${{ steps.latest_tag.outputs.tag }}"
          if [ -n "$LATEST" ] && [ "$CURRENT" = "$LATEST" ]; then
            echo "ERROR: Version $CURRENT in pom.xml matches the latest tag."
            echo "Bump the version in pom.xml before pushing to master."
            exit 1
          fi
          echo "Version check passed: $CURRENT (latest tag: ${LATEST:-<none>})"

      - name: Build JAR
        run: ./mvnw package -DskipTests

      - name: Tag and publish release
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          VERSION="${{ steps.version.outputs.version }}"
          TAG="v${VERSION}"
          JAR="target/car-rental-system-${VERSION}.jar"

          git tag "$TAG"
          git push origin "$TAG"

          gh release create "$TAG" \
            --title "Release $TAG" \
            --notes "Release $TAG" \
            "$JAR"
```

- [ ] **Step 2: Verify the YAML is valid**

```bash
cat .github/workflows/ci.yml | python3 -c "import sys, yaml; yaml.safe_load(sys.stdin); print('YAML valid')"
```

Expected: `YAML valid`

---

### Task 6: Bump version and commit everything

- [ ] **Step 1: Set the initial release version in pom.xml**

Change line 13 of `pom.xml` from:
```xml
<version>0.0.1-SNAPSHOT</version>
```
to:
```xml
<version>1.0.0</version>
```

This is required: since there are no git tags yet, the first push will create `v1.0.0`. All future pushes must change this value or the build fails.

- [ ] **Step 2: Commit all changes**

```bash
git add pom.xml \
  src/test/java/com/wasup/car_rental_system/controller/AuthControllerTest.java \
  src/test/java/com/wasup/car_rental_system/controller/AdminControllerTest.java \
  .github/workflows/ci.yml \
  docs/

git commit -m "ci: add JaCoCo coverage gate, new tests, and GitHub Actions pipeline

- Add JaCoCo 0.8.12 with 80% instruction coverage enforcement
- Add AuthControllerTest (login, register, list tenants)
- Add AdminControllerTest (create tenant, list, RBAC guard)
- Add CI workflow: build+coverage on PRs, release on master push
- Initial release version 1.0.0"
```

- [ ] **Step 3: Push and watch the workflow**

```bash
git push origin master
```

Then check: `gh run watch` or visit the Actions tab on GitHub.

Expected: `build` job passes, `release` job creates tag `v1.0.0` and a GitHub Release with the JAR attached.
