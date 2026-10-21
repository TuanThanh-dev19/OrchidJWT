# SBA301 Lab 06 - Spring Security JWT

Du an Maven/JDK 21 theo Core Lab 06. Package goc `fu.se.lab06jwt`, API chay o `http://localhost:8005`. Phan Advanced (role, refresh token, OAuth2 Resource Server...) trong PDF la tu nghien cuu, khong thuoc Core Lab.

## 1. Tao database SQL Server

Mo SSMS, chay `database.sql`. Script nay giu dung lenh tao `OrchidJWTLab` trong tai lieu. Ung dung dung `spring.jpa.hibernate.ddl-auto=update` de tu tao bang `users` khi ket noi DB thanh cong. Khong can chay Maven de tao database.

Neu SQL Server khong o `localhost:1433`, sua `spring.datasource.url` trong `src/main/resources/application.properties` theo instance/port thuc te.

## 2. Cau hinh bien moi truong

Trong PowerShell dang chay ung dung:

```powershell
$env:DB_USER = '<SQL Server user>'
$env:DB_PASSWORD = '<SQL Server password>'
$keyBytes = New-Object byte[] 32
([System.Security.Cryptography.RandomNumberGenerator]::Create()).GetBytes($keyBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($keyBytes)
```

`JWT_SECRET` phai la Base64 cua it nhat 32 byte vi `JwtService` dung `Decoders.BASE64` va HMAC. Khong commit gia tri that vao Git. Neu dung IntelliJ, dat ba bien tren trong Run Configuration.

## 3. Build, test, run

```powershell
mvn clean test
mvn spring-boot:run
```

Test dung H2 in-memory qua profile `test`, vi database SQL Server se do ban tu tao. Runtime thong thuong van dung SQL Server. `mvn clean test` kiem cac flow signup/login, BCrypt, JWT hop le, khong token, token sai/het han, 409 duplicate, 400 validation va viec response khong lo password.

Khi app da start, chay `verify-users.sql` trong SSMS sau khi dang ky user. Query nay kiem tra row, duplicate va BCrypt hash. Can chup evidence SQL/Postman tren SQL Server thuc te cua ban; test H2 khong thay the evidence nay.

## 4. Postman

Import `postman/Lab06JWT.postman_environment.json` va `postman/Lab06JWT.postman_collection.json`, chon environment `SBA301 Lab06 Local`, chay T01 den T09, T11 den T12 theo thu tu. T03 tu luu `token` vao environment. T01 se tra 409 neu da co `student01@fpt.edu.vn` tu lan chay truoc; dung DB lab moi hoac xoa user test truoc khi chay lai toan bo collection.

T10 expired-token can thao tac thu cong nhu PDF: tam doi `security.jwt.expiration-time=5000`, restart app, login lay token, copy token vao `expiredToken` cua environment, doi hon 5 giay roi chay T10. Sau do tra TTL ve `3600000`.

## 5. API contract

| Request | Thanh cong | Loi chinh |
| --- | --- | --- |
| `POST /auth/signup` | 201 + UserResponse | 400 validation, 409 duplicate |
| `POST /auth/login` | 200 + `token`, `expiresIn` (ms) | 400 validation, 401 credential sai |
| `GET /users/me` | 200 + UserResponse | 401 thieu/sai/het han JWT |
| `GET /users` | 200 + danh sach UserResponse | 401 thieu/sai/het han JWT |

`UserResponse` chi gom `id`, `fullName`, `email`, `createdAt`, `updatedAt`; khong tra password hoac hash. JWT `sub` la email, `iat` va `exp` do JJWT tao; filter xac minh chu ky/thoi han va dat principal vao `SecurityContext` truoc Controller.

## 6. Ghi chu ve ma mau PDF

- Giu nguyen ten class, package, endpoint, DTO va flow JWT theo code trong tai lieu.
- Chuan hoa email truoc khi kiem duplicate va login de email viet hoa khong tao tai khoan trung.
- Them `AuthenticationEntryPoint` tra 401 cho protected endpoint khong token, dung contract test trong tai lieu.
- Bat loi JWT/user khong ton tai trong filter va tra 401; khong dat `SecurityContext` neu token sai.
- Them handler validation 400 va unique-constraint conflict 409 de dap ung test matrix.
