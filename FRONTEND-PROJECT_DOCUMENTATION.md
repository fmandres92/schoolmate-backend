# SchoolMate Hub - Documentación Técnica Completa

> **Versión**: 1.1.0  
> **Última Actualización**: Febrero 2026  
> **Estado**: Desarrollo Activo - Frontend Completo con Portal del Apoderado  

---

## 1. VISIÓN GENERAL DEL PROYECTO

### 1.1 Descripción
**SchoolMate Hub** es un Sistema de Gestión Escolar (SGE) completo diseñado para administrar todos los aspectos de una institución educativa. El sistema implementa una arquitectura de **triple rol** (Administrador, Profesor y Apoderado) con interfaces completamente diferenciadas.

### 1.2 Características Principales
- **Gestión Académica Completa**: Años escolares, grados, cursos, materias, profesores, alumnos
- **Portal del Profesor**: Interfaz mobile-first para toma de asistencia en tiempo real
- **Portal del Apoderado**: Interfaz mobile-first para padres/tutores con calendario de asistencia
- **Control de Asistencia**: Sistema binario (presente/ausente) con ventanas de tiempo
- **Reportes de Comportamiento**: Gestión de incidentes y seguimiento
- **Carga Académica**: Análisis de horarios y distribución de profesores

### 1.3 Stack Tecnológico

#### Core
| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| React | 18.3.1 | Framework UI |
| TypeScript | 5.8.3 | Tipado estático |
| Vite | 5.4.19 | Build tool y dev server |
| React Router | 6.30.1 | Enrutamiento |

#### Estado y Datos
| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| React Context API | - | Estado global |
| TanStack Query | 5.83.0 | Caching y server state |
| React Hook Form | 7.61.1 | Manejo de formularios |
| Zod | 3.25.76 | Validación de schemas |

#### UI/UX
| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Tailwind CSS | 3.4.17 | Estilos utilitarios |
| Radix UI | ^1.x | Componentes accesibles |
| shadcn/ui | - | Biblioteca de componentes |
| Lucide React | 0.462.0 | Iconos |
| Recharts | 2.15.4 | Gráficos |
| date-fns | 3.6.0 | Manejo de fechas |

---

## 2. ARQUITECTURA DEL SISTEMA

### 2.1 Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENTE (Navegador)                       │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                   REACT APPLICATION                       │  │
│  │                                                           │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
  │  │  │  SISTEMA DE RUTAS                                   │  │  │
  │  │  │  ┌─────────────┐  ┌─────────────────────────────┐  │  │  │
  │  │  │  │ Rutas Admin │  │ Rutas Profesor              │  │  │  │
  │  │  │  │ /dashboard  │  │ /profesor/inicio            │  │  │  │
  │  │  │  │ /cursos     │  │ /profesor/horario           │  │  │  │
  │  │  │  │ /alumnos    │  │ /profesor/asistencia        │  │  │  │
  │  │  │  │ /reportes   │  │ /profesor/reportes          │  │  │  │
  │  │  │  └─────────────┘  └─────────────────────────────┘  │  │  │
  │  │  │  ┌─────────────────────────────────────────────┐  │  │  │
  │  │  │  │ Rutas Apoderado                             │  │  │  │
  │  │  │  │ /apoderado/inicio                           │  │  │  │
  │  │  │  │ /apoderado/horario                          │  │  │  │
  │  │  │  │ /apoderado/asistencia                       │  │  │  │
  │  │  │  └─────────────────────────────────────────────┘  │  │  │
  │  │  └────────────────────────────────────────────────────┘  │  │
│  │                          ↓                                │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
  │  │  │  LAYOUTS POR ROL                                    │  │  │
  │  │  │  ┌──────────────┐     ┌─────────────────────────┐  │  │  │
  │  │  │  │ Admin Layout │     │ Profesor Layout         │  │  │  │
  │  │  │  │ - Sidebar    │     │ - Header móvil          │  │  │  │
  │  │  │  │ - Header     │     │ - Bottom navigation     │  │  │  │
  │  │  │  │ - Desktop    │     │ - Mobile-first          │  │  │  │
  │  │  │  └──────────────┘     └─────────────────────────┘  │  │  │
  │  │  │  ┌─────────────────────────────────────────────┐  │  │  │
  │  │  │  │ Apoderado Layout                            │  │  │  │
  │  │  │  │ - Header móvil con nombre del alumno        │  │  │  │
  │  │  │  │ - Bottom navigation (3 tabs)                │  │  │  │
  │  │  │  │ - Mobile-first                              │  │  │  │
  │  │  │  └─────────────────────────────────────────────┘  │  │  │
  │  │  └────────────────────────────────────────────────────┘  │  │
│  │                          ↓                                │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  CONTEXTOS (Estado Global)                          │  │  │
│  │  │  ┌────────────────┐  ┌──────────────────────────┐  │  │  │
│  │  │  │ AuthContext    │  │ DataContext              │  │  │  │
│  │  │  │ - user         │  │ - Toda la data mock      │  │  │  │
│  │  │  │ - rol          │  │ - Funciones CRUD         │  │  │  │
│  │  │  │ - login/logout │  │ - Helpers de consulta    │  │  │  │
│  │  │  └────────────────┘  └──────────────────────────┘  │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  │                          ↓                                │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  DATOS (Mock en Memoria)                            │  │  │
│  │  │  - 15 Profesores, 72 Alumnos, 18 Cursos            │  │  │
│  │  │  - 90+ Asignaciones, Reportes, Asistencias         │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  PERSISTENCIA                                            │  │
│  │  - localStorage: Sesión de usuario (edugestio_user)      │  │
│  │  - Datos: En memoria (se pierden al recargar)            │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Flujo de Datos

```
Usuario Interactúa
        ↓
Componente React
        ↓
Custom Hook (useAuth / useData)
        ↓
Context Provider (AuthContext / DataContext)
        ↓
Estado Local (useState) → Re-render
        ↓
localStorage (solo auth)
```

### 2.3 Patrones de Diseño

1. **Provider Pattern**: Context API para estado global
2. **Layout Pattern**: Layouts diferenciados por rol de usuario
3. **Compound Components**: Componentes UI componibles (shadcn/ui)
4. **Custom Hooks**: `useAuth()`, `useData()` para lógica reutilizable
5. **Role-Based Access Control (RBAC)**: Rutas y layouts protegidos por rol
6. **Time-Based State Machine**: Estados de clases basados en tiempo real

---

## 3. ESTRUCTURA DE CARPETAS

```
schoolmate-hub/
├── 📁 src/                           # Código fuente principal
│   ├── 📁 components/
│   │   ├── 📁 layout/               # Layouts por rol
│   │   │   ├── AppLayout.tsx        # Router de layouts (detecta rol)
│   │   │   ├── AppSidebar.tsx       # Navegación admin (desktop)
│   │   │   ├── AppHeader.tsx        # Header admin
│   │   │   ├── ProfesorLayout.tsx   # Layout profesor
│   │   │   ├── ProfesorBottomNav.tsx # Navegación móvil profesor
│   │   │   ├── ApoderadoLayout.tsx   # Layout apoderado
│   │   │   └── ApoderadoBottomNav.tsx # Navegación móvil apoderado
│   │   ├── 📁 ui/                   # 48+ componentes shadcn/ui
│   │   └── NavLink.tsx              # Link de navegación
│   ├── 📁 contexts/
│   │   ├── AuthContext.tsx          # Autenticación y roles
│   │   └── DataContext.tsx          # Datos mock y funciones (~39k líneas)
│   ├── 📁 hooks/
│   │   ├── use-mobile.tsx           # Detección mobile
│   │   └── use-toast.ts             # Sistema de notificaciones
│   ├── 📁 lib/
│   │   ├── utils.ts                 # Funciones utilitarias (cn, etc)
│   │   └── mockTime.ts              # Control de tiempo para testing
│   ├── 📁 pages/
│   │   ├── 📁 profesor/             # Páginas del portal profesor
│   │   │   ├── ProfesorDashboard.tsx    # Dashboard profesor
│   │   │   ├── ProfesorHorario.tsx      # Horario semanal
│   │   │   ├── ProfesorAsistencia.tsx   # Toma de asistencia
│   │   │   ├── ProfesorReportes.tsx     # Gestión de reportes
│   │   │   ├── ProfesorPerfil.tsx       # Perfil del profesor
│   │   │   ├── ListaClasesHoy.tsx       # Lista de clases del día
│   │   │   └── components/
│   │   │       └── FilaAlumno.tsx       # Fila de alumno en asistencia
│   │   ├── 📁 apoderado/            # Páginas del portal apoderado
│   │   │   ├── ApoderadoDashboard.tsx   # Dashboard apoderado
│   │   │   ├── ApoderadoHorario.tsx     # Horario del alumno
│   │   │   └── ApoderadoAsistencia.tsx  # Calendario de asistencia
│   │   ├── Dashboard.tsx            # Dashboard admin
│   │   ├── Login.tsx                # Login unificado
│   │   ├── AnosEscolares.tsx        # Gestión años escolares
│   │   ├── Cursos.tsx               # Lista de cursos
│   │   ├── DetalleCurso.tsx         # Detalle de curso
│   │   ├── Materias.tsx             # Catálogo de materias
│   │   ├── Profesores.tsx           # Gestión de profesores
│   │   ├── Alumnos.tsx              # Gestión de alumnos
│   │   ├── Reportes.tsx             # Reportes de comportamiento
│   │   ├── CargaAcademica.tsx       # Análisis de carga académica
│   │   ├── Detalle*.tsx             # Páginas de detalle
│   │   └── ReporteAsistencia*.tsx   # Reportes de asistencia
│   ├── App.tsx                      # Router principal
│   ├── main.tsx                     # Punto de entrada
│   └── index.css                    # Estilos globales + CSS variables
├── 📄 package.json                  # Dependencias
├── 📄 vite.config.ts                # Configuración Vite
├── 📄 tailwind.config.ts            # Configuración Tailwind
└── 📄 PROJECT_DOCUMENTATION.md      # Este archivo
```

---

## 4. SISTEMA DE AUTENTICACIÓN Y ROLES

### 4.1 Modelo de Usuarios

```typescript
// AuthContext.tsx
type UserRole = 'admin' | 'profesor' | 'apoderado';

interface User {
  id: string;
  email: string;
  nombre: string;
  apellido: string;
  rol: UserRole;
  avatar?: string;
  alumnoId?: string; // Solo para rol apoderado - ID del alumno vinculado
}
```

### 4.2 Usuarios de Prueba

| Rol | Email | Contraseña | Nombre | ID Vinculado |
|-----|-------|------------|--------|--------------|
| Admin | admin@edugestio.cl | admin123 | Carlos Mendoza | admin-1 |
| Profesor | profesor@edugestio.cl | prof123 | Carlos Rodríguez | p2 |
| Apoderado | apoderado@edugestio.cl | apod123 | Carlos Soto | apod-1 (vinculado a al1) |

**IMPORTANTE**: 
- El usuario profesor (`p2`) está vinculado al profesor Carlos Rodríguez en DataContext
- El usuario apoderado está vinculado al alumno Benjamín Soto Pérez (al1) del 1° Básico A

### 4.3 Flujo de Autenticación

```
1. USUARIO ACCEDE A /login
   ↓
2. INGRESA CREDENCIALES
   ↓
3. AuthContext.login(email, password)
   ├── Valida contra mockUsers
   ├── Si válido: Guarda en localStorage
   └── Retorna objeto User (sin password)
   ↓
4. REDIRECCIÓN BASADA EN ROL
    ├── Admin → /dashboard
    ├── Profesor → /profesor/inicio
    └── Apoderado → /apoderado/inicio
    ↓
5. AppLayout DETECTA ROL Y RENDERIZA
    ├── Admin: Sidebar + Header desktop
    ├── Profesor: Header móvil + Bottom nav
    └── Apoderado: Header móvil + Bottom nav
```

### 4.4 Protección de Rutas

```typescript
// AppLayout.tsx
const AppLayout = () => {
  const { user, isAuthenticated } = useAuth();
  const location = useLocation();
  
  // 1. Verificar autenticación
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  
  // 2. Verificar acceso según rol
  if (user?.rol === 'profesor' && !location.pathname.startsWith('/profesor')) {
    return <Navigate to="/profesor/inicio" replace />;
  }
  
  if (user?.rol === 'apoderado' && !location.pathname.startsWith('/apoderado')) {
    return <Navigate to="/apoderado/inicio" replace />;
  }
  
  // 3. Renderizar layout correspondiente
  if (user?.rol === 'profesor') return <ProfesorLayout />;
  if (user?.rol === 'apoderado') return <ApoderadoLayout />;
  return <AdminLayout />;
};
```

---

## 5. MODELO DE DATOS

### 5.1 Entidades Principales

#### AnoEscolar
```typescript
interface AnoEscolar {
  id: string;              // '1', '2', '3'
  ano: number;             // 2025, 2026, 2027
  fechaInicio: string;     // '2026-03-01'
  fechaFin: string;        // '2026-12-15'
  activo: boolean;         // Solo uno activo a la vez
}
```

#### Grado
```typescript
interface Grado {
  id: string;              // '1' - '8'
  nombre: string;          // '1° Básico' - '8° Básico'
  nivel: number;           // 1-8
  cursosActivos: number;   // Contador
}
```

#### Materia
```typescript
interface Materia {
  id: string;              // '1' - '11'
  nombre: string;          // 'Matemáticas', 'Lenguaje y Comunicación'
  icono: string;           // Nombre del icono Lucide
  grados: number[];        // [1,2,3,4,5,6,7,8] o [3,4,5,6,7,8]
}
```

#### Curso
```typescript
interface Curso {
  id: string;              // 'c1' - 'c18'
  nombre: string;          // '1° Básico A'
  letra: string;           // 'A', 'B', 'C'
  gradoId: string;         // FK → Grado.id
  anoEscolarId: string;    // FK → AnoEscolar.id
  numEstudiantes: number;  // 23-40
  activo: boolean;
}
```

#### Profesor
```typescript
interface Profesor {
  id: string;              // 'p1' - 'p15'
  rut: string;             // '12.345.678-9'
  nombre: string;
  apellido: string;
  email: string;           // 'nombre@colegio.cl'
  telefono: string;        // '+56 9 XXXX XXXX'
  materias: string[];      // ['Matemáticas', 'Física']
  fechaContratacion: string;
  activo: boolean;
}
```

#### Alumno
```typescript
interface Alumno {
  id: string;              // 'al1' - 'al72'
  rut: string;
  nombre: string;
  apellido: string;
  fechaNacimiento: string;
  fechaInscripcion: string;
  cursoId: string;         // FK → Curso.id
  apoderado: {
    nombre: string;
    apellido: string;
    email: string;
    telefono: string;
    vinculo: string;       // 'Padre', 'Madre', 'Tutor'
  };
  activo: boolean;
}
```

#### Asignacion (Horario)
```typescript
interface Asignacion {
  id: string;              // 'a1' - 'a90+'
  cursoId: string;         // FK → Curso.id
  profesorId: string;      // FK → Profesor.id
  materia: string;         // Nombre de la materia
  diaSemana: number;       // 1=Lunes, 2=Martes, ..., 7=Domingo
  horaInicio: string;      // '08:00'
  horaFin: string;         // '09:00'
}
```

#### AsistenciaClase (Sistema de Asistencia Binaria)
```typescript
interface AsistenciaClase {
  id: string;                    // UUID
  fecha: string;                 // '2026-02-10'
  asignacionId: string;          // FK → Asignacion.id
  profesorId: string;            // FK → Profesor.id
  cursoId: string;               // FK → Curso.id
  materia: string;
  horaInicio: string;
  horaFin: string;
  registros: {
    alumnoId: string;
    estado: 'presente' | 'ausente';  // Sistema binario: solo 2 estados
    observacion?: string;
    horaRegistro: string;        // '08:05'
  }[];
  completada: boolean;
  fechaCreacion: string;         // ISO timestamp
  fechaModificacion: string;     // ISO timestamp
}
```

**Nota importante**: El sistema utiliza solo dos estados de asistencia: **presente** y **ausente**. No existe el estado "tardanza" o "retardo" en el sistema actual.

#### Reporte
```typescript
interface Reporte {
  id: string;
  alumnoId: string;              // FK → Alumno.id
  asunto: string;
  descripcion: string;
  fecha: string;
  hora: string;
  profesorId: string;            // FK → Profesor.id
  profesorNombre: string;        // Denormalizado
  profesorApellido: string;      // Denormalizado
  materia: string;
  gravedad: 'leve' | 'media' | 'alta';
  estado: 'abierto' | 'en_proceso' | 'cerrado';
}
```

### 5.2 Relaciones Entre Entidades

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ AnoEscolar  │1───*│   Curso     │*───1│   Grado     │
│  (3 años)   │     │  (18 cursos)│     │ (8 grados)  │
└─────────────┘     └──────┬──────┘     └─────────────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           ▼               ▼               ▼
    ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
    │   Alumno    │ │  Asignacion │ │   Reporte   │
    │  (72 alums) │ │  (90+ asig) │ │             │
    └─────────────┘ └──────┬──────┘ └─────────────┘
                           │
                    ┌──────┴──────┐
                    │  Profesor   │
                    │  (15 profs) │
                    └─────────────┘
                           │
                           ▼
              ┌─────────────────────┐
              │   AsistenciaClase   │
              │   (Registro diario) │
              └─────────────────────┘
```

### 5.3 Datos Iniciales (Mock)

| Entidad | Cantidad | Notas |
|---------|----------|-------|
| Años Escolares | 3 | 2025, 2026 (activo), 2027 |
| Grados | 8 | 1° Básico a 8° Básico |
| Materias | 11 | Con iconos Lucide asignados |
| Cursos | 18 | 2-3 cursos por grado |
| Profesores | 15 | 14 activos, 1 inactivo |
| Alumnos | 72 | Distribuidos en todos los cursos |
| Asignaciones | 90+ | Horarios completos Lunes-Viernes |

---

## 6. SISTEMA DE ASISTENCIA (FEATURE PRINCIPAL)

### 6.1 Estados de Clase (Time-Based State Machine)

El sistema utiliza la hora actual para determinar el estado de cada clase:

```
┌─────────────────────────────────────────────────────────────┐
│                    LÍNEA DE TIEMPO                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ─10min      Inicio        Duración        Fin     +15min  │
│    │           │              │             │        │      │
│    ▼           ▼              ▼             ▼        ▼      │
│ ┌──────┐  ┌────────┐    ┌──────────┐  ┌────────┐ ┌──────┐  │
│ │BLOQUE│  │DISPONI-│    │ EN CURSO │  │FINALIZ-│ │EXPIRA│  │
│ │  ADA │  │  BLE   │    │          │  │  ANDO  │ │  DA  │  │
│ └──────┘  └────────┘    └──────────┘  └────────┘ └──────┘  │
│                                                             │
│  • No se     • Se puede     • Clase en    • Últimos    • Ya │
│    puede       tomar          progreso      5 min +    no   │
│    tomar       asistencia                   15 min     se   │
│    lista                                    grace      puede│
│                                             period     editar│
└─────────────────────────────────────────────────────────────┘
```

### 6.2 Estados Detallados

| Estado | Descripción | Acciones Permitidas |
|--------|-------------|---------------------|
| **bloqueada** | Más de 10 min antes del inicio | Ver lista, no tomar asistencia |
| **disponible** | 10 min antes hasta inicio | Tomar asistencia completa |
| **en_curso** | Duración de la clase (hasta últimos 5 min) | Tomar asistencia completa |
| **finalizando** | Últimos 5 min + 15 min grace | Tomar y editar asistencia |
| **expirada** | Después del grace period | Solo ver, no editar |
| **completada** | Asistencia ya guardada | Ver detalles, no editar |

### 6.3 Funciones de Control de Tiempo

```typescript
// src/lib/mockTime.ts

// Variable para testing (puede ser null para usar hora real)
let horaMock: string | null = '09:50';

export const setHoraMock = (hora: string | null) => { 
  horaMock = hora; 
};

export const getHoraActual = () => {
  if (horaMock) {
    const [h, m] = horaMock.split(':').map(Number);
    const fecha = new Date();
    fecha.setHours(h, m, 0, 0);
    return fecha;
  }
  return new Date();
};
```

### 6.4 Flujo de Toma de Asistencia

```
PROFESOR ACCEDE A /profesor/asistencia/:asignacionId
              ↓
VERIFICAR ESTADO DE LA CLASE
              ↓
    ┌─────────────────────┐
    │ ¿Estado disponible? │
    └─────────────────────┘
         SÍ        NO
          ↓         ↓
    ┌─────────┐  ┌──────────────────┐
    │ Mostrar │  │ Mostrar mensaje  │
    │ lista   │  │ de bloqueo con   │
    │ alumnos │  │ tiempo restante  │
    └────┬────┘  └──────────────────┘
         ↓
PROFESOR SELECCIONA ESTADO
(Presente/Ausente)
         ↓
PUEDE AGREGAR OBSERVACIONES
         ↓
CLICK EN "GUARDAR ASISTENCIA"
         ↓
DataContext.guardarAsistenciaClase()
         ↓
ACTUALIZAR ESTADO LOCAL
         ↓
MOSTRAR CONFIRMACIÓN
```

---

## 7. PORTAL DEL APODERADO

### 7.1 Estructura de Rutas

```
/apoderado/inicio           → Dashboard con información del alumno
/apoderado/horario          → Horario semanal del alumno
/apoderado/asistencia       → Calendario mensual de asistencia
```

### 7.2 Layout del Apoderado

```
┌─────────────────────────────┐
│  SchoolMate Hub      👤 🚪  │  ← Header sticky con nombre del alumno
├─────────────────────────────┤
│                             │
│  [CONTENIDO SCROLLEABLE]    │  ← Main content
│                             │
│  ┌─────────────────────┐    │
│  │ Benjamín Soto       │    │  ← Card de info del alumno
│  │ 1° Básico A         │    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │ Asistencia General  │    │
│  │ 92%                 │    │
│  │ 🟢 138 · 🔴 5       │    │
│  └─────────────────────┘    │
│                             │
├─────────────────────────────┤
│ 🏠  📅  ✅                 │  ← Bottom navigation (3 tabs)
│ Inicio Horario Asistencia
└─────────────────────────────┘
```

### 7.3 Componentes Específicos del Apoderado

#### ApoderadoDashboard (`/apoderado/inicio`)
- **Propósito**: Vista general del alumno vinculado
- **Datos**: Información del alumno, curso, resumen de asistencia
- **Features**:
  - Card con información del alumno (nombre, RUT, curso)
  - Clases del día con estado de asistencia
  - Resumen de asistencia general (presentes/ausentes, sin retardos)
  - Información del curso

#### ApoderadoHorario (`/apoderado/horario`)
- **Propósito**: Ver horario semanal completo del alumno
- **Datos**: Asignaciones del curso organizadas por día
- **Features**:
  - Tabs por día de la semana (Lunes a Viernes)
  - Lista de clases con hora, materia y profesor
  - Indicador del día actual
  - Detección de bloques libres entre clases

#### ApoderadoAsistencia (`/apoderado/asistencia`)
- **Propósito**: Calendario mensual visual de asistencia
- **Datos**: Registros de asistencia del alumno agrupados por día
- **Features**:
  - **Calendario mensual tipo grilla** (solo Lunes-Viernes)
  - Navegación entre meses (◀ Mes ▶)
  - **Colores por estado**:
    - 🟢 Verde: Asistió (presente en ≥1 clase)
    - 🔴 Rojo: No asistió (ausente en todas)
    - ⚪ Gris: Sin registro
  - **Día actual** destacado con ring
  - **Tocar un día** muestra detalle expandible:
    - Fecha completa
    - Lista de materias del día
    - Estado en cada materia
    - Observaciones del profesor
  - **Resumen del mes** con porcentaje y conteo

### 7.4 Lógica de Asistencia por Día

```
Para determinar si un alumno "asistió" un día:

1. Buscar todos los registros de AsistenciaClase de ese día
2. Si el alumno está presente en AL MENOS UNA clase → ASISTIÓ ✅
3. Si el alumno está ausente en TODAS las clases → NO ASISTIÓ ❌
4. Si no hay registros → SIN INFORMACIÓN ⚪
```

### 7.5 Diseño Mobile-First del Apoderado

- Ancho objetivo: 375px - 430px
- Bottom navigation fija con safe-area-inset
- Cards con bordes redondeados (rounded-xl)
- Calendario compacto con celdas cuadradas (min 60x60px)
- Scroll vertical para contenido extenso

---

## 8. PORTAL DEL PROFESOR

### 8.1 Estructura de Rutas

```
/profesor/inicio           → Dashboard con clases del día
/profesor/horario          → Horario semanal completo
/profesor/asistencia       → Lista de clases de hoy
/profesor/asistencia/:id   → Toma de asistencia específica
/profesor/reportes         → Reportes del profesor
/profesor/perfil           → Perfil y logout
```

### 8.2 Componentes Específicos del Profesor

#### ProfesorDashboard (`/profesor/inicio`)
- **Propósito**: Vista rápida del día del profesor
- **Datos**: Clases de hoy con estados en tiempo real
- **Features**:
  - Cards de clases con indicadores de estado
  - Acceso directo a "Tomar Lista" cuando está disponible
  - Visualización de progreso del día

#### ProfesorHorario (`/profesor/horario`)
- **Propósito**: Ver horario semanal completo
- **Datos**: Asignaciones del profesor organizadas por día
- **Features**:
  - Tabs por día de la semana
  - Indicadores visuales de asistencia tomada
  - Detalles de curso y materia

#### ProfesorAsistencia (`/profesor/asistencia/:asignacionId`)
- **Propósito**: Tomar asistencia de una clase específica
- **Datos**: Alumnos del curso, asignación actual
- **Features**:
  - Lista de alumnos con botones de estado
  - Búsqueda/filtrado de alumnos
  - Campo de observaciones por alumno
  - Validación de ventana de tiempo
  - Persistencia de cambios

#### ListaClasesHoy (`/profesor/asistencia`)
- **Propósito**: Overview de todas las clases del día
- **Datos**: Asignaciones del profesor para hoy
- **Features**:
  - Separación en "Completadas" y "Pendientes"
  - Badges de estado (Bloqueada, Disponible, En curso, etc.)
  - Acceso rápido a toma de asistencia

### 8.3 Diseño Mobile-First del Profesor

```
┌─────────────────────────────┐
│  SchoolMate Hub      🔔 👤  │  ← Header sticky
├─────────────────────────────┤
│                             │
│  [CONTENIDO SCROLLEABLE]    │  ← Main content
│                             │
│  ┌─────────────────────┐    │
│  │ Clase de Matemáticas│    │
│  │ 1° Básico A         │    │
│  │ 08:00 - 09:00       │    │
│  │ [Tomar Lista]       │    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │ Clase de Ciencias   │    │
│  │ 2° Básico B         │    │
│  │ 10:00 - 11:00       │    │
│  │ [Bloqueada]         │    │
│  └─────────────────────┘    │
│                             │
├─────────────────────────────┤
│ 🏠  📅  ✓  ⚠️  👤          │  ← Bottom navigation
│ Inicio Horario Asist Rep Perfil
└─────────────────────────────┘
```

---

## 9. PANEL DE ADMINISTRACIÓN

### 9.1 Estructura de Rutas

```
/dashboard                 → Panel principal con estadísticas
/anos-escolares            → Gestión de períodos académicos
/cursos                    → Lista y gestión de cursos
/cursos/:id                → Detalle de curso (alumnos, horario)
/materias                  → Catálogo de materias
/materias/:nombre          → Detalle de materia
/grados                    → Gestión de niveles
/profesores                → Gestión de docentes
/profesores/:id            → Detalle de profesor
/alumnos                   → Gestión de estudiantes
/alumnos/:id               → Detalle de alumno
/carga-academica           → Análisis de horarios
/reportes                  → Reportes de comportamiento
/reportes/asistencia-cursos    → Reporte por curso
/reportes/asistencia-alumnos   → Reporte por alumno
```

### 9.2 Layout Admin

```
┌────────────────────────────────────────────────────────────┐
│  [☰]  SchoolMate Hub                    👤 Carlos Mendoza  │ ← Header
├──────────┬─────────────────────────────────────────────────┤
│          │                                                 │
│  [🏠]    │                                                 │
│  Dashboard│    [CONTENIDO PRINCIPAL]                      │
│          │                                                 │
│  [📅]    │    ┌─────────────────────────────────────┐     │
│  Años    │    │                                     │     │
│  Escolares│    │                                     │     │
│          │    │                                     │     │
│  [👥]    │    │                                     │     │
│  Cursos  │    │                                     │     │
│          │    └─────────────────────────────────────┘     │
│  [📚]    │                                                 │
│  Materias│                                                 │
│          │                                                 │
│  [...]   │                                                 │
│          │                                                 │
│  ────────┤                                                 │
│  [🚪]    │                                                 │
│  Logout  │                                                 │
│          │                                                 │
└──────────┴─────────────────────────────────────────────────┘
   Sidebar        Main Content Area
   (colapsable)
```

---

## 9. API Y FUNCIONES DEL DATACONTEXT

### 9.1 Funciones de Consulta (Getters)

```typescript
// Cursos y Asignaciones
getAsignacionesByCurso(cursoId: string): Asignacion[]
getProfesoresByCurso(cursoId: string): Profesor[]
getMateriasByCurso(cursoId: string): string[]
getHorarioByCurso(cursoId: string): HorarioClase[]
getAlumnosByCurso(cursoId: string): Alumno[]
getCursosByAno(anoEscolarId: string): Curso[]
getCursosByGrado(gradoId: string, anoEscolarId: string): Curso[]

// Alumnos
getNotasByAlumno(alumnoId: string): Nota[]
getAsistenciaByAlumno(alumnoId: string): Asistencia | undefined
getReportesByAlumno(alumnoId: string): Reporte[]

// Profesores
getAsignacionesProfesor(profesorId: string): Asignacion[]
getAsignacionesProfesorHoy(profesorId: string): Asignacion[]
getAsignacionesProfesorPorDia(profesorId: string, diaSemana: number): Asignacion[]
getAsistenciaByProfesor(profesorId: string): AsistenciaProfesor | undefined
getAsistenciaPromedioProfesores(): number
getReportesByProfesor(profesorId: string): Reporte[]

// Asistencia Clase (Sistema Binario)
getAsistenciaClase(asignacionId: string, fecha: string): AsistenciaClase | undefined
isAsistenciaTomada(asignacionId: string, fecha: string): boolean
getAsistenciasClaseByProfesor(profesorId: string): AsistenciaClase[]

// Apoderados
getAlumnoById(alumnoId: string): Alumno | undefined
getCursoById(cursoId: string): Curso | undefined
getAsistenciasClaseByAlumno(alumnoId: string): AsistenciaClase[]
getAsistenciaDiariaAlumno(alumnoId: string, mes: number, ano: number): {
  fecha: string;
  asistio: boolean;
  totalClases: number;
  clasesPresente: number;
  detalle: {
    materia: string;
    horaInicio: string;
    horaFin: string;
    estado: 'presente' | 'ausente';
    observacion?: string;
  }[];
}[]

// Reportes
getReporteById(reporteId: string): Reporte | undefined

// Estadísticas
getPromedioCurso(cursoId: string): number
getAsistenciaPromedioCurso(cursoId: string): number

// Recursos
getLibrosByMateriaAndAno(materiaId: string, anoEscolarId: string): Libro[]
getRecursosByMateria(materiaId: string): RecursoDigital[]
```

### 9.2 Funciones de Mutación

```typescript
// Años Escolares
setAnoActivo(id: string): void

// Materias
addMateria(materia: Omit<Materia, 'id'>): void
updateMateria(id: string, materia: Partial<Materia>): void
deleteMateria(id: string): void

// Asistencia Clase
// Crea o actualiza el registro de asistencia para una clase
guardarAsistenciaClase(asistencia: AsistenciaClase): void
```

### 9.3 Uso del DataContext

```typescript
import { useData } from '@/contexts/DataContext';

const MiComponente = () => {
  const { 
    alumnos, 
    getAlumnosByCurso, 
    guardarAsistenciaClase 
  } = useData();
  
  // Obtener alumnos de un curso específico
  const alumnosCurso = getAlumnosByCurso('c1');
  
  // Guardar asistencia
  const handleGuardar = () => {
    guardarAsistenciaClase({
      id: 'uuid-generado',
      fecha: '2026-02-10',
      asignacionId: 'a1',
      profesorId: 'p2',
      cursoId: 'c1',
      materia: 'Matemáticas',
      horaInicio: '08:00',
      horaFin: '09:00',
      registros: [
        { alumnoId: 'al1', estado: 'presente', horaRegistro: '08:05' },
        { alumnoId: 'al2', estado: 'ausente', horaRegistro: '08:05' },
      ],
      completada: true,
      fechaCreacion: new Date().toISOString(),
      fechaModificacion: new Date().toISOString(),
    });
  };
  
  return (...);
};
```

---

## 10. CONFIGURACIÓN Y DESARROLLO

### 10.1 Scripts Disponibles

```bash
# Desarrollo
npm run dev              # Inicia servidor de desarrollo (puerto 8080)

# Build
npm run build            # Build de producción
npm run build:dev        # Build modo desarrollo
npm run preview          # Previsualizar build

# Calidad de código
npm run lint             # Ejecutar ESLint
npm run test             # Ejecutar tests (Vitest)
npm run test:watch       # Tests en modo watch
```

### 10.2 Configuración de Vite

```typescript
// vite.config.ts
export default {
  server: {
    host: "::",          // Todas las interfaces
    port: 8080,          // Puerto de desarrollo
    hmr: {
      overlay: false,    // Sin overlay de errores
    },
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),  // Alias @/ → src/
    },
  },
}
```

### 10.3 Configuración de Tailwind

```typescript
// tailwind.config.ts
export default {
  darkMode: ["class"],
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      colors: {
        // Sistema de colores con CSS variables
        primary: 'hsl(var(--primary))',
        secondary: 'hsl(var(--secondary))',
        // ... más colores
      },
    },
  },
}
```

### 10.4 Variables de Entorno

**Actualmente no hay archivo .env configurado.**

Para futura integración con backend, crear `.env`:

```env
VITE_API_URL=http://localhost:3000/api
VITE_APP_NAME=SchoolMate Hub
VITE_APP_VERSION=1.0.0
VITE_ENABLE_MOCK_DATA=false
```

---

## 11. TESTING

### 11.1 Stack de Testing

- **Framework**: Vitest 3.2.4
- **Testing Library**: @testing-library/react 16.0.0
- **Jest DOM**: @testing-library/jest-dom 6.6.0
- **Environment**: jsdom 20.0.3

### 11.2 Configuración

```typescript
// vitest.config.ts
export default defineConfig({
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
});
```

### 11.3 Ejecutar Tests

```bash
# Una vez
npm run test

# Modo watch
npm run test:watch
```

---

## 12. DESPLIEGUE

### 12.1 Build de Producción

```bash
npm run build
```

Genera carpeta `dist/` con:
- Assets optimizados (JS, CSS)
- HTML entry point
- Archivos estáticos

### 12.2 Despliegue en Vercel

```json
// vercel.json
{
  "rewrites": [
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ]
}
```

### 12.3 Requisitos de Servidor

- Servidor web estático (Nginx, Apache, Vercel, Netlify)
- HTTPS recomendado para producción
- SPA routing configurado (todas las rutas → index.html)

---

## 13. ROADMAP Y MEJORAS FUTURAS

### 13.1 Backend Integration
- [ ] API REST con Node.js/Express o Fastify
- [ ] Base de datos PostgreSQL con Prisma ORM
- [ ] Autenticación JWT
- [ ] WebSockets para tiempo real

### 13.2 Features Completados
- [x] **Portal de apoderados** - Calendario mensual de asistencia, horario del alumno, dashboard con información del alumno

### 13.3 Features Pendientes
- [ ] Exportación de reportes (PDF, Excel)
- [ ] Notificaciones push
- [ ] Calendario académico integrado
- [ ] Sistema de mensajería
- [ ] Gestión de calificaciones detallada

### 13.3 Mejoras Técnicas
- [ ] Migrar a React Query para todas las operaciones
- [ ] Implementar service workers para offline
- [ ] Tests E2E con Playwright
- [ ] Storybook para componentes
- [ ] CI/CD pipeline

---

## 14. GUÍA PARA DESARROLLADORES

### 14.1 Convenciones de Código

**Nombres de archivos:**
- Componentes: PascalCase (`ProfesorDashboard.tsx`)
- Hooks: camelCase con prefijo `use` (`useAuth.ts`)
- Utilidades: camelCase (`utils.ts`)
- Contextos: PascalCase con sufijo (`AuthContext.tsx`)

**Estructura de componentes:**
```typescript
// 1. Imports
import React from 'react';
import { useAuth } from '@/contexts/AuthContext';

// 2. Interfaces
interface Props {
  cursoId: string;
}

// 3. Componente
export const MiComponente: React.FC<Props> = ({ cursoId }) => {
  // Hooks
  const { user } = useAuth();
  
  // Estado
  const [data, setData] = useState([]);
  
  // Efectos
  useEffect(() => {
    // Lógica
  }, [cursoId]);
  
  // Render
  return (
    <div>...</div>
  );
};
```

### 14.2 Agregar Nuevas Páginas

1. **Crear archivo** en `src/pages/NuevaPagina.tsx`
2. **Agregar ruta** en `src/App.tsx`:
   ```typescript
   <Route path="/nueva-ruta" element={<NuevaPagina />} />
   ```
3. **Agregar al sidebar** (si es admin) en `AppSidebar.tsx`
4. **Agregar a bottom nav** (si es profesor) en `ProfesorBottomNav.tsx`

### 14.3 Agregar Nuevos Datos Mock

1. **Definir interfaz** en `DataContext.tsx`
2. **Crear array inicial** (ej: `initialNuevaEntidad`)
3. **Agregar al estado** del provider
4. **Crear funciones** getter/setter
5. **Exportar en el context value**

### 14.4 Debugging

**Hora mock para testing:**
```typescript
import { setHoraMock, getHoraMock } from '@/lib/mockTime';

// Establecer hora específica
setHoraMock('09:50');

// Usar hora real
setHoraMock(null);

// Ver hora actual mock
console.log(getHoraMock());
```

**Inspeccionar datos:**
```typescript
const dataContext = useData();
console.log('DataContext:', dataContext);
```

---

## 15. REFERENCIAS RÁPIDAS

### 15.1 URLs Importantes

| URL | Descripción |
|-----|-------------|
| `http://localhost:8080` | App en desarrollo |
| `http://localhost:8080/login` | Login |
| `http://localhost:8080/dashboard` | Dashboard admin |
| `http://localhost:8080/profesor/inicio` | Dashboard profesor |
| `http://localhost:8080/apoderado/inicio` | Dashboard apoderado |
| `http://localhost:8080/apoderado/horario` | Horario del alumno |
| `http://localhost:8080/apoderado/asistencia` | Calendario de asistencia |

### 15.2 Comandos Útiles

```bash
# Instalar dependencias
npm install

# Iniciar desarrollo
npm run dev

# Lint
npm run lint

# Tests
npm run test

# Build
npm run build
```

### 15.3 Contacto y Soporte

Para dudas técnicas o reportar issues:
- Revisar este documento primero
- Verificar logs en consola del navegador
- Revisar estructura de datos en DataContext

---

**Fin de la Documentación**

*Documento actualizado para SchoolMate Hub v1.1.0 - Febrero 2026*
