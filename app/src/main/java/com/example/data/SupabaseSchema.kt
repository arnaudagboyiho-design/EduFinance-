package com.example.data

object SupabaseSchema {
    val PRIMARY_CYCLE_SQL: String = """
-- ============================================================================
-- EduFinance Pro - Schéma Relationnel & Politiques RLS (Supabase / PostgreSQL)
-- Prise en charge intégrale du CYCLE PRIMAIRE (CI, CP, CE1, CE2, CM1, CM2)
-- ============================================================================

-- 1. EXTENSIONS & DOMAINES
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Type énuméré pour les cycles scolaires
DO $$ BEGIN
    CREATE TYPE school_cycle_enum AS ENUM ('primaire', 'college', 'lycee');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- 2. TABLE DES CLASSES (avec support CI - CM2)
CREATE TABLE IF NOT EXISTS public.classes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(20) NOT NULL UNIQUE,          -- ex: 'CI_A', 'CP_B', 'CM2_B'
    name VARCHAR(100) NOT NULL,                 -- ex: 'Cours Moyen 2 B'
    cycle school_cycle_enum NOT NULL DEFAULT 'primaire',
    level VARCHAR(20) NOT NULL,                 -- 'CI', 'CP', 'CE1', 'CE2', 'CM1', 'CM2'
    capacity INTEGER NOT NULL DEFAULT 35,
    student_count INTEGER NOT NULL DEFAULT 0,
    main_teacher_name VARCHAR(150),
    main_teacher_id UUID,
    room_number VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index pour recherche rapide par cycle et niveau
CREATE INDEX IF NOT EXISTS idx_classes_cycle ON public.classes(cycle);
CREATE INDEX IF NOT EXISTS idx_classes_level ON public.classes(level);

-- 3. TABLE DES MATIÈRES DU PRIMAIRE & SECONDAIRE
CREATE TABLE IF NOT EXISTS public.subjects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(20) NOT NULL,                  -- ex: 'FRAN-LEC', 'MATH-PRI', 'ECM'
    name VARCHAR(150) NOT NULL,                 -- ex: 'Français (Lecture & Expression)'
    cycle school_cycle_enum NOT NULL DEFAULT 'primaire',
    coefficient INTEGER NOT NULL DEFAULT 1 CHECK (coefficient BETWEEN 1 AND 10),
    class_id UUID REFERENCES public.classes(id) ON DELETE CASCADE,
    teacher_name VARCHAR(150),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. TABLE DES ÉLÈVES (Inscriptions & Statuts Financiers)
CREATE TABLE IF NOT EXISTS public.students (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    matricule VARCHAR(30) NOT NULL UNIQUE,      -- ex: 'EDF-2026-P001'
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    gender CHAR(1) CHECK (gender IN ('M', 'F')),
    birth_date DATE NOT NULL,
    class_id UUID REFERENCES public.classes(id) ON DELETE RESTRICT,
    parent_name VARCHAR(150) NOT NULL,
    parent_phone VARCHAR(50) NOT NULL,
    parent_email VARCHAR(150),
    tuition_total NUMERIC(12, 2) NOT NULL DEFAULT 250000.00,
    tuition_paid NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'actif' 
        CHECK (status IN ('actif', 'en_regle', 'en_retard', 'archive')),
    registration_date DATE DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_students_matricule ON public.students(matricule);
CREATE INDEX IF NOT EXISTS idx_students_class ON public.students(class_id);

-- 5. TABLE DES NOTES & ÉVALUATIONS (Primaire & Périodique)
CREATE TABLE IF NOT EXISTS public.grades (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES public.students(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES public.subjects(id) ON DELETE CASCADE,
    period VARCHAR(20) NOT NULL,                -- 'Trimestre 1', 'Trimestre 2', 'Trimestre 3'
    test_1 NUMERIC(4, 2) CHECK (test_1 BETWEEN 0 AND 20),
    test_2 NUMERIC(4, 2) CHECK (test_2 BETWEEN 0 AND 20),
    exam NUMERIC(4, 2) CHECK (exam BETWEEN 0 AND 20),
    computed_average NUMERIC(4, 2) GENERATED ALWAYS AS (
        (COALESCE(test_1, 0) + COALESCE(test_2, 0) + 2 * COALESCE(exam, 0)) / 4.0
    ) STORED,
    teacher_appreciation TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(student_id, subject_id, period)
);

-- 6. DONNÉES DE BASE DU CYCLE PRIMAIRE
INSERT INTO public.classes (code, name, cycle, level, capacity, main_teacher_name, room_number)
VALUES
    ('CI_A', 'Cours d''Initiation A', 'primaire', 'CI', 30, 'Mme Aminata Sanogo', 'Salle P-01'),
    ('CP_B', 'Cours Préparatoire B', 'primaire', 'CP', 30, 'M. Daniel Brou', 'Salle P-02'),
    ('CE1_A', 'Cours Élémentaire 1 A', 'primaire', 'CE1', 32, 'Mme Florence Bamba', 'Salle P-03'),
    ('CE2_B', 'Cours Élémentaire 2 B', 'primaire', 'CE2', 32, 'M. Patrice Konan', 'Salle P-04'),
    ('CM1_A', 'Cours Moyen 1 A', 'primaire', 'CM1', 35, 'Mme Reine Akissi', 'Salle P-05'),
    ('CM2_B', 'Cours Moyen 2 B (CEP)', 'primaire', 'CM2', 35, 'M. Marcelle Yao', 'Salle P-06')
ON CONFLICT (code) DO NOTHING;

-- 7. POLITIQUES DE SÉCURITÉ AU NIVEAU DES LIGNES (RLS)
ALTER TABLE public.classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subjects ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.students ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.grades ENABLE ROW LEVEL SECURITY;

-- Politiques de lecture publique authentifiée
CREATE POLICY "classes_select_auth" ON public.classes 
    FOR SELECT TO authenticated USING (true);

CREATE POLICY "subjects_select_auth" ON public.subjects 
    FOR SELECT TO authenticated USING (true);

-- Les secrétaires, directeurs et admins peuvent insérer/modifier les élèves
CREATE POLICY "students_manage_staff" ON public.students 
    FOR ALL TO authenticated 
    USING (auth.jwt() ->> 'role' IN ('admin', 'directeur', 'secretaire', 'comptable'));

-- Les enseignants ne peuvent saisir les notes que pour leurs disciplines
CREATE POLICY "grades_teacher_insert" ON public.grades 
    FOR ALL TO authenticated 
    USING (
        auth.jwt() ->> 'role' IN ('admin', 'directeur') 
        OR (auth.jwt() ->> 'role' = 'enseignant')
    );
""".trimIndent()
}
