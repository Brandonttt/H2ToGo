-- ============================================================================
-- H2ToGo: Inserción de Marcas Predeterminadas en Catálogo Global
-- Compatible con PostgreSQL y seguro de re-ejecutar (ON CONFLICT DO NOTHING)
-- ============================================================================

INSERT INTO marcas (nombre, activo)
VALUES
    ('Ciel 20L', TRUE),
    ('Bonafont 20L', TRUE),
    ('e-pura 20L', TRUE),
    ('Santorini 20L', TRUE),
    ('Santa María 20L', TRUE),
    ('Pureza Aga 20L', TRUE),
    ('Agua Purificada Genérica 20L', TRUE),
    ('Agua Alcalina 20L', TRUE),
    ('Garrafón Cristal 20L', TRUE),
    ('Agua Demo 20L', TRUE)
ON CONFLICT (nombre) DO NOTHING;

-- Asegurar secuencia de id_marca si se requiere
SELECT setval(pg_get_serial_sequence('marcas', 'id_marca'), COALESCE((SELECT MAX(id_marca) FROM marcas), 1));

-- Mostrar marcas registradas actualmente
SELECT id_marca, nombre, activo FROM marcas ORDER BY id_marca ASC;
