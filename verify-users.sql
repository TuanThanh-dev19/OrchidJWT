-- Chay sau khi ung dung da khoi dong va da signup user.
USE OrchidJWTLab;
GO
SELECT TOP 10 id, full_name, email, password, created_at, updated_at
FROM dbo.users
ORDER BY id DESC;
GO
SELECT COUNT(*) AS matching_users
FROM dbo.users
WHERE email = N'student01@fpt.edu.vn';
GO
SELECT email,
       CASE WHEN password <> N'P@ssword123' AND password LIKE N'$2%'
            THEN N'PASS' ELSE N'FAIL' END AS password_hash_check
FROM dbo.users
WHERE email = N'student01@fpt.edu.vn';
GO
