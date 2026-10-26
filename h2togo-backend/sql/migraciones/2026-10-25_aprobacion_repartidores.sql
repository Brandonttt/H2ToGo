-- Aprobación del dueño para repartidores que se unen a una purificadora existente.
-- Ejecutar UNA vez en la base de Azure ANTES de desplegar el backend que incluye este cambio:
-- Hibernate valida el esquema al arrancar y sin esta columna el backend no inicia.
--
--   psql "$DB_URL" -f h2togo-backend/sql/migraciones/2026-10-25_aprobacion_repartidores.sql
--
-- Los repartidores que ya existen quedan aprobados (DEFAULT TRUE).
ALTER TABLE repartidores ADD COLUMN IF NOT EXISTS aprobado BOOLEAN NOT NULL DEFAULT TRUE;
COMMENT ON COLUMN repartidores.aprobado IS
    'FALSE mientras el dueño no aprueba a un repartidor que se unió a su purificadora existente.';
