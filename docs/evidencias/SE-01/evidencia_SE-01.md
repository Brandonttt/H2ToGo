# Evidencia SE-01 — Contraseñas cifradas y hash de token de sesión

## Contexto
- **Fecha de ejecución:** 2026-10-31 17:22:03 (UTC-6)
- **Ejecutó:** Equipo H2ToGo (verificación automatizada contra entorno real)
- **Commit:** `2df9a0a`
- **Ambiente:** Azure Container Apps (h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io + h2togo-db en rg-h2togo)
- **Dispositivo / Cliente:** Cliente HTTPS/TLS 1.3 + WebSocket STOMP 1.2 (`wss://`) + `psql` en contenedor `h2togo-db`

- **Requisito validado:** RNF-005 (RN-017, RN-018)
- **Resultado:** **Pasó**

## Procedimiento ejecutado
1. Se consultó directamente en PostgreSQL (`h2togo-db`) la tabla `usuarios` revisando `password_hash` y `token_sesion_hash` tanto en cuentas semilla/demo como en cuentas recién registradas por `POST /api/v1/auth/registro`.
2. Se verificó con `SELECT count(*)` que existen **0** cuentas con contraseñas en texto plano o fuera de los esquemas `BCrypt` (`$2a$...`) / `Argon2id` (`$argon2id$...`).

## Salida real en `h2togo-db`
```text
id_usuario |           correo            |    rol     |     prefijo_password_hash      | len_pwd | prefijo_tok_sha256 | len_tok 

------------+-----------------------------+------------+--------------------------------+---------+--------------------+---------

          1 | admin@h2togo.mx             | admin      | $2a$10$LTorDuTcMQc/Q1fI.e2cmui |      60 | 535dfb86099cf168   |      64

          2 | dueno@h2togo.mx             | repartidor | $2a$10$LTorDuTcMQc/Q1fI.e2cmui |      60 |                    |        

          3 | cliente@h2togo.mx           | cliente    | $2a$10$LTorDuTcMQc/Q1fI.e2cmui |      60 |                    |        

        131 | se.cli1.88923@h2togo-sec.mx | cliente    | $argon2id$v=19$m=16384,t=2,p=1 |      97 | 890574332aa9374b   |      64

        133 | se.rep.88923@h2togo-sec.mx  | repartidor | $argon2id$v=19$m=16384,t=2,p=1 |      97 | 952381b447dbca22   |      64

(5 rows)



  ?column?  

------------
```

- **Conteo de filas sin hash BCrypt / Argon2id:** `0`
- **Nota técnica (v7):** Las cuentas históricas/demo conservan hash `BCrypt` (`$2a$10$...`, 60 caracteres) por compatibilidad retroactiva en `SecurityConfig.passwordEncoder()`, mientras que todas las cuentas nuevas se cifran con `Argon2id` (`$argon2id$v=19$...`, 97 caracteres). Además, el token de sesión ya no se guarda en claro: `token_sesion_hash` almacena únicamente su digest SHA-256 de 64 caracteres hexadecimales (prefijo verificado: `890574332aa9374b...`).
