# Render: staff sign-in diagnosis and recovery

The `/api/auth/login` endpoint does not branch by role. It normalizes the submitted phone, loads one `users` row, checks `enabled`, and verifies the password with the same `BCryptPasswordEncoder` used by registration and staff creation. Doctor/nurse profile activity is not part of password verification. `SecurityConfig` checks the role after login when a protected endpoint is requested.

The local DEMO accounts are different from production staff accounts. `DevDataInitializer` only runs under the `dev` profile. The Docker image starts with `prod`, where no demo accounts are inserted. Do not copy demo staff into production. Create real production staff from an authenticated ADMIN session using the existing `/api/admin/doctors` and `/api/admin/nurses` endpoints; those flows normalize the phone, assign the server-controlled role, enable the account, and BCrypt-hash the submitted password.

## Read-only production check

In Render's PostgreSQL shell, paste the query below and replace the phone with the affected staff member's canonical `+7` phone. It prints no password hash, JWT, or password. Do not paste the account phone or query output into public logs or issues.

```sql
SELECT
    u.id,
    u.phone,
    u.role,
    u.enabled,
    CASE WHEN u.phone ~ '^\+7[0-9]{10}$' THEN 'normalized' ELSE 'unexpected-format' END AS phone_format,
    CASE
        WHEN u.password_hash ~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$' THEN 'bcrypt'
        ELSE 'non-bcrypt-or-invalid'
    END AS password_format,
    dp.active AS doctor_profile_active,
    np.active AS nurse_profile_active
FROM users u
LEFT JOIN doctor_profiles dp ON dp.user_id = u.id
LEFT JOIN nurse_profiles np ON np.user_id = u.id
WHERE u.phone = '+77000000000';
```

Interpret the result:

- No row: that phone is not provisioned in the Render database. Create the staff account through the ADMIN interface. This does not duplicate or delete user data; the API rejects duplicate phones.
- Wrong role: stop and verify the employee's intended role and profile records before any data change. Never change a JWT claim or create another account to work around a mismatch.
- `enabled = false`: verify that the employee is still authorized to use the service before re-enabling the account. The staff CRUD endpoints do not change this user-level flag, so use a separately reviewed, narrowly scoped data operation if reactivation is approved:

  ```sql
  BEGIN;
  UPDATE users SET enabled = true
  WHERE id = <verified-user-id>
    AND phone = '+77000000000'
    AND role IN ('DOCTOR', 'NURSE')
    AND enabled = false;
  -- Confirm exactly one affected row, then COMMIT; otherwise ROLLBACK.
  ```
- `non-bcrypt-or-invalid`: the saved hash cannot be validated by the current encoder. Reset only that account's password to a newly chosen value and BCrypt hash. Never store a plaintext password or copy a hash from another account.
- A disabled doctor/nurse profile can sign in if `users.enabled` is true, but its role-protected features may then report that the profile is unavailable. Profile activity is separate from login.
- A valid enabled row with a bcrypt hash means the cause is likely the submitted phone/password pair. Retype it without leading/trailing whitespace and use the password currently set in this database.

## Password reset when a stored hash is invalid

Prefer recreating no account: keep the same `users.id`, phone, role, and profile relationships. Generate a new BCrypt hash locally with the same Spring Security library version used by this backend, then run a narrowly scoped transaction in Render's SQL shell. On a machine with JDK 21 and Maven, from PowerShell:

```powershell
cd backend
mvn -q dependency:build-classpath -Dmdep.outputFile=target/password-classpath.txt
$authClasspath = "target/classes;" + (Get-Content target/password-classpath.txt -Raw).Trim()
jshell --class-path $authClasspath
```

At the JShell prompt, enter these commands. The password is read without echoing; copy the resulting BCrypt hash into the SQL update below, not the plaintext password:

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
var chars = System.console().readPassword("New staff password: ");
System.out.println(new BCryptPasswordEncoder().encode(new String(chars)));
/exit
```

Substitute only the user ID returned by the read-only query and the generated hash; never substitute a plaintext password.

```sql
BEGIN;
UPDATE users
SET password_hash = '<new-bcrypt-hash>'
WHERE id = <verified-user-id>
  AND phone = '+77000000000'
  AND role IN ('DOCTOR', 'NURSE')
  AND enabled = true;
-- Confirm exactly one affected row, then:
COMMIT;
-- If the row count is not exactly one, issue ROLLBACK instead.
```

This reset changes one password only. It preserves the user's ID, role, status, staff profile, appointments, and all related records. Do not run it for demo accounts or as a bulk migration. A schema migration is not appropriate because the application schema already stores BCrypt hashes and no production account data was inspected.

## Render deployment

1. Deploy the backend from `backend/Dockerfile`; it sets `SPRING_PROFILES_ACTIVE=prod`. Keep the existing Render PostgreSQL database and its persistent disk/service.
2. Keep `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `CORS_ALLOWED_ORIGINS` configured as Render secrets/environment values. Do not add any `QAMQORMED_DEMO_*` variables to production.
3. Do not run local development initialization or demo SQL against production. No Flyway migration is needed for this auth fix.
4. After deploy, log in as ADMIN and create only staff accounts that are actually missing. For existing accounts, use the read-only query above before any reset.
5. Test one PATIENT, DOCTOR, NURSE, and ADMIN login, then verify each role's own endpoint and a forbidden cross-role endpoint. Restart the Render backend and verify the same staff accounts still sign in; persistent users live in PostgreSQL and are not recreated by the `prod` profile.
