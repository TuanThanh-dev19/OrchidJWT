-- Chay trong SQL Server Management Studio truoc khi khoi dong ung dung.
-- Script tao database duoc giu dung nhu Step 0.1 trong Lab 06.
IF DB_ID(N'OrchidJWTLab') IS NULL
    CREATE DATABASE OrchidJWTLab;
GO
USE OrchidJWTLab;
GO
SELECT DB_NAME() AS current_db;
GO
