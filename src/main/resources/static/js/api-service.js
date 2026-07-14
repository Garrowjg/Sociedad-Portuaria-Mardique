const IS_PRODUCTION = false;
const SHAREPOINT_SITE_ID = "spmardiquesa.sharepoint.com:/sites/prueba";
const GRAPH_BASE = "https://graph.microsoft.com/v1.0";

function logReady(label) {
    if (!IS_PRODUCTION) {
        console.log('%c[API] ' + label + ' — Feature listo para producción. Actualmente en modo simulado por etapa de prueba.', 'color: #f09a36; font-weight: bold');
    }
}

async function graphFetch(url, options) {
    if (!IS_PRODUCTION) {
        logReady("Graph API: " + (url ? url.split("?")[0].substring(0, 80) : "no-url"));
        await new Promise(resolve => setTimeout(resolve, 300));
        return null;
    }
    const token = await getMsalToken();
    const res = await fetch(url, {
        ...options,
        headers: {
            "Authorization": "Bearer " + token,
            "Content-Type": "application/json",
            ...(options?.headers || {})
        }
    });
    if (!res.ok) throw new Error("Graph API error " + res.status);
    return res.json();
}

const MOCK_USER = {
    displayName: "Carlos Martínez",
    mail: "carlos.martinez@mardique.com.co",
    jobTitle: "Analista de TI",
    department: "Tecnología e Informática",
    officeLocation: "TI",
    userPrincipalName: "carlos.martinez@mardique.com.co"
};

const MOCK_DEPARTMENTS = [
    { id: "rrhh", name: "Recursos Humanos", icon: "fa-users" },
    { id: "finanzas", name: "Finanzas", icon: "fa-chart-line" },
    { id: "ti", name: "Tecnología e Informática", icon: "fa-laptop-code" }
];

const MOCK_FILES_BY_SECTOR = {
    "rrhh": [
        { id: "rh-001", name: "Contrato Laboral Modelo.pdf", size: 245760, file: { mimeType: "application/pdf" }, lastModifiedDateTime: "2026-03-10T14:30:00Z", webUrl: "#", createdBy: { user: { displayName: "Ana Gómez" } } },
        { id: "rh-002", name: "Reglamento Interno de Trabajo.pdf", size: 512000, file: { mimeType: "application/pdf" }, lastModifiedDateTime: "2026-02-15T10:00:00Z", webUrl: "#", createdBy: { user: { displayName: "Ana Gómez" } } },
        { id: "rh-003", name: "Formato Evaluación Desempeño.docx", size: 189440, file: { mimeType: "application/vnd.openxmlformats-officedocument.wordprocessingml.document" }, lastModifiedDateTime: "2026-04-01T08:45:00Z", webUrl: "#", createdBy: { user: { displayName: "Pedro Ruiz" } } },
        { id: "rh-004", name: "Calendario Vacaciones 2026.xlsx", size: 36864, file: { mimeType: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" }, lastModifiedDateTime: "2026-01-05T09:00:00Z", webUrl: "#", createdBy: { user: { displayName: "María López" } } },
        { id: "rh-005", name: "Política de Bienestar.pdf", size: 102400, file: { mimeType: "application/pdf" }, lastModifiedDateTime: "2026-03-20T11:30:00Z", webUrl: "#", createdBy: { user: { displayName: "Pedro Ruiz" } } },
        { id: "rh-006", name: "Capacitaciones 2026", folder: { childCount: 3 }, lastModifiedDateTime: "2026-04-10T16:00:00Z", webUrl: "#", createdBy: { user: { displayName: "Ana Gómez" } } }
    ],
    "finanzas": [
        { id: "fn-001", name: "Presupuesto Anual 2026.xlsx", size: 286720, file: { mimeType: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" }, lastModifiedDateTime: "2026-01-02T07:00:00Z", webUrl: "#", createdBy: { user: { displayName: "Laura Jiménez" } } },
        { id: "fn-002", name: "Informe de Gestión Q1 2026.pdf", size: 1540000, file: { mimeType: "application/pdf" }, lastModifiedDateTime: "2026-04-05T13:00:00Z", webUrl: "#", createdBy: { user: { displayName: "Laura Jiménez" } } },
        { id: "fn-003", name: "Plantilla Facturación.docx", size: 65536, file: { mimeType: "application/vnd.openxmlformats-officedocument.wordprocessingml.document" }, lastModifiedDateTime: "2026-02-20T10:30:00Z", webUrl: "#", createdBy: { user: { displayName: "Carlos Vega" } } },
        { id: "fn-004", name: "Comprobantes de Pago", folder: { childCount: 12 }, lastModifiedDateTime: "2026-04-12T09:15:00Z", webUrl: "#", createdBy: { user: { displayName: "Carlos Vega" } } },
        { id: "fn-005", name: "Informe Tributario 2025.pdf", size: 2100000, file: { mimeType: "application/pdf" }, lastModifiedDateTime: "2026-03-01T08:00:00Z", webUrl: "#", createdBy: { user: { displayName: "Laura Jiménez" } } }
    ],
    "ti": [
        { id: "ti-001", name: "Manual Usuario SAP.pdf", size: 3200000, file: { mimeType: "application/pdf" }, lastModifiedDateTime: "2026-02-01T14:00:00Z", webUrl: "#", createdBy: { user: { displayName: "Carlos Martínez" } } },
        { id: "ti-002", name: "Política Seguridad Informática.pdf", size: 880000, file: { mimeType: "application/pdf" }, lastModifiedDateTime: "2026-01-15T11:00:00Z", webUrl: "#", createdBy: { user: { displayName: "Carlos Martínez" } } },
        { id: "ti-003", name: "Inventario de Equipos.xlsx", size: 122880, file: { mimeType: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" }, lastModifiedDateTime: "2026-04-08T16:45:00Z", webUrl: "#", createdBy: { user: { displayName: "Andrés Ramírez" } } },
        { id: "ti-004", name: "Guía Acceso Remoto.pdf", size: 450560, file: { mimeType: "application/pdf" }, lastModifiedDateTime: "2026-03-25T10:00:00Z", webUrl: "#", createdBy: { user: { displayName: "Andrés Ramírez" } } },
        { id: "ti-005", name: "Backup de Configuración", folder: { childCount: 20 }, lastModifiedDateTime: "2026-04-12T23:00:00Z", webUrl: "#", createdBy: { user: { displayName: "Carlos Martínez" } } },
        { id: "ti-006", name: "Solicitudes de Soporte", folder: { childCount: 5 }, lastModifiedDateTime: "2026-04-14T17:30:00Z", webUrl: "#", createdBy: { user: { displayName: "Andrés Ramírez" } } }
    ]
};

const MOCK_NEWS = [
    { id: "n1", fields: { Title: "Arribo de buque con 35.000 toneladas de maíz", Description: "El M/V Grain Star atracó esta madrugada en el muelle 4 con 35.000 toneladas de maíz amarillo proveniente de Estados Unidos. La operación de descarga se estima en 48 horas continuas.", Created: "2026-07-12T06:00:00Z" }, coverUrl: "https://picsum.photos/seed/buque/800/450", webUrl: "#" },
    { id: "n2", fields: { Title: "Semana Ambiental 2026 — Un éxito rotundo", Description: "Del 23 al 29 de junio celebramos la Semana Ambiental con jornadas de reforestación, limpieza de muelles y charlas sobre sostenibilidad. Participaron más de 120 colaboradores.", Created: "2026-06-30T10:00:00Z" }, coverUrl: "https://picsum.photos/seed/ambiente/800/450", webUrl: "#" },
    { id: "n3", fields: { Title: "Nuevo sistema de Gestión Documental", Description: "Informamos a todos los colaboradores que hemos implementado un nuevo sistema de gestión documental integrado con SharePoint. Pronto recibirán capacitación sobre su uso.", Created: "2026-07-10T08:00:00Z" }, coverUrl: "https://picsum.photos/seed/documentos/800/450", webUrl: "#" },
    { id: "n4", fields: { Title: "Llegada de grúa móvil Liebherr LHM 550", Description: "La nueva grúa móvil Liebherr LHM 550 llegó al puerto para reforzar la capacidad operativa de descarga de graneles. Tiene un alcance de 54 metros y capacidad de 124 toneladas.", Created: "2026-07-05T14:00:00Z" }, coverUrl: "https://picsum.photos/seed/grua/800/450", webUrl: "#" },
    { id: "n5", fields: { Title: "Jornada de Vacunación Empresarial", Description: "La próxima semana se realizará la jornada de vacunación contra la influenza en las instalaciones de la bodega. Inscríbanse con RRHH.", Created: "2026-07-08T10:30:00Z" }, coverUrl: "https://picsum.photos/seed/vacuna/800/450", webUrl: "#" },
    { id: "n6", fields: { Title: "Exportación de contenedores con productos petroquímicos", Description: "Zona Franca de Mardique despachó 120 contenedores con productos petroquímicos con destino a Brasil. La operación generó 80 empleos directos durante la carga.", Created: "2026-07-02T09:00:00Z" }, coverUrl: "https://picsum.photos/seed/contenedores/800/450", webUrl: "#" },
    { id: "n7", fields: { Title: "Capacitación en primeros auxilios para brigadistas", Description: "15 colaboradores de las brigadas de emergencia recibieron certificación en primeros auxilios básicos y RCP, dictada por la Defensa Civil.", Created: "2026-06-25T15:00:00Z" }, coverUrl: "https://picsum.photos/seed/brigada/800/450", webUrl: "#" },
    { id: "n8", fields: { Title: "Actualización de la Política de Teletrabajo", Description: "Se ha publicado la nueva versión de la política de teletrabajo. Todos los empleados deben leerla y firmar el acuse de recibo antes del 30 de julio.", Created: "2026-07-01T09:00:00Z" }, coverUrl: "https://picsum.photos/seed/teletrabajo/800/450", webUrl: "#" }
];

let MOCK_LEAVE_REQUESTS = [
    { id: "lv-1", fields: { Title: "Solicitud de Carlos Martínez", Nombre: "Carlos Martínez", FechaInicio: "2026-05-04", FechaFin: "2026-05-10", Motivo: "Vacaciones anuales", Estado: "APROBADO", Created: "2026-03-15T09:00:00Z" } },
    { id: "lv-2", fields: { Title: "Solicitud de Carlos Martínez", Nombre: "Carlos Martínez", FechaInicio: "2026-08-10", FechaFin: "2026-08-14", Motivo: "Asuntos personales", Estado: "PENDIENTE", Created: "2026-04-02T11:30:00Z" } }
];

const MOCK_GALLERY_ALBUMS = [
    {
        id: "gal-independencia",
        name: "Día de la Independencia",
        description: "Celebración y conmemoración patria con el personal operativo y administrativo en muelle.",
        eventDate: "2026-07-20T10:00:00Z",
        folder: { childCount: 8 },
        lastModifiedDateTime: "2026-07-20T18:00:00Z",
        webUrl: "#",
        coverUrl: "https://picsum.photos/id/1015/600/400",
        createdBy: { user: { displayName: "Carlos Martínez" } },
        items: [
            { id: "ind-01", name: "izamiento_bandera.jpg", size: 512000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "https://picsum.photos/id/1015/1200/800", lastModifiedDateTime: "2026-07-20T10:30:00Z" },
            { id: "ind-02", name: "acto_central.jpg", size: 384000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "https://picsum.photos/id/1016/1200/800", lastModifiedDateTime: "2026-07-20T11:00:00Z" },
            { id: "ind-03", name: "personal_muelle.jpg", size: 620000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "https://picsum.photos/id/1018/1200/800", lastModifiedDateTime: "2026-07-20T11:30:00Z" },
            { id: "ind-04", name: "discurso_gerencia.jpg", size: 445000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "https://picsum.photos/id/1020/1200/800", lastModifiedDateTime: "2026-07-20T12:00:00Z" },
            { id: "ind-05", name: "integracion_empleados.jpg", size: 512000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "https://picsum.photos/id/1024/1200/800", lastModifiedDateTime: "2026-07-20T14:00:00Z" },
            { id: "ind-06", name: "show_cultural.jpg", size: 398000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "https://picsum.photos/id/1025/1200/800", lastModifiedDateTime: "2026-07-20T15:30:00Z" },
            { id: "ind-07", name: "cierre_evento.jpg", size: 356000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "https://picsum.photos/id/1035/1200/800", lastModifiedDateTime: "2026-07-20T17:00:00Z" },
            { id: "ind-08", name: "foto_grupal.jpg", size: 780000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "https://picsum.photos/id/1039/1200/800", lastModifiedDateTime: "2026-07-20T18:00:00Z" }
        ]
    },
    {
        id: "gal-incendios",
        name: "Capacitación de Control de Incendios y Extintores",
        description: "Registro técnico de la jornada práctica de brigadas de seguridad e inducción de uso de extintores.",
        eventDate: "2026-07-10T08:00:00Z",
        folder: { childCount: 6 },
        lastModifiedDateTime: "2026-07-10T17:00:00Z",
        webUrl: "#",
        coverUrl: "https://picsum.photos/id/1076/600/400",
        createdBy: { user: { displayName: "Andrés Ramírez" } },
        items: [
            { id: "inc-01", name: "teoria_prevencion.jpg", size: 420000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "https://picsum.photos/id/1076/1200/800", lastModifiedDateTime: "2026-07-10T08:30:00Z" },
            { id: "inc-02", name: "demostracion_extintor.jpg", size: 510000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "https://picsum.photos/id/1077/1200/800", lastModifiedDateTime: "2026-07-10T10:00:00Z" },
            { id: "inc-03", name: "practica_dirigida.jpg", size: 485000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "https://picsum.photos/id/1084/1200/800", lastModifiedDateTime: "2026-07-10T11:00:00Z" },
            { id: "inc-04", name: "brigada_seguridad.jpg", size: 490000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "https://picsum.photos/id/1027/1200/800", lastModifiedDateTime: "2026-07-10T12:00:00Z" },
            { id: "inc-05", name: "maniobra_manguera.jpg", size: 530000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "https://picsum.photos/id/1068/1200/800", lastModifiedDateTime: "2026-07-10T14:00:00Z" },
            { id: "inc-06", name: "certificacion_personal.jpg", size: 410000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "https://picsum.photos/id/1070/1200/800", lastModifiedDateTime: "2026-07-10T16:30:00Z" }
        ]
    }
];

let MOCK_SUPPORT_TICKETS = [
    { id: "st-1", fields: { Title: "Soporte de Carlos Martínez", Nombre: "Carlos Martínez", Categoria: "Hardware", Descripcion: "El monitor de mi estación de trabajo no enciende.", Estado: "EN CURSO", Created: "2026-04-11T08:15:00Z" } },
    { id: "st-2", fields: { Title: "Soporte de Carlos Martínez", Nombre: "Carlos Martínez", Categoria: "Software", Descripcion: "No puedo acceder al módulo de facturación en el ERP.", Estado: "RESUELTO", Created: "2026-04-09T14:30:00Z" } }
];

const MOCK_AREAS_REAL_LIST = {
    "value": [
        { "id": "1", "fields": { "Title": "Sistemas", "Sitio": "", "Contactos": "Ramiro Rodelo del Valle - Coordinador de Tecnologia de Informacion\nJohnnier Gomez - Aprendiz TI\nAdelino Aragon - Tecnico de Sistemas\nOmar Caicedo Martinez - Analista TI", "Informe": "", "cover": "https://img.freepik.com/free-photo/server-room_53876-97067.jpg", "descripcion_larga": "Encargada de la infraestructura tecnológica, soporte a usuarios, administración de sistemas ERP y seguridad informática." } },
        { "id": "2", "fields": { "Title": "Contabilidad", "Sitio": "", "Contactos": "Carlos Molina Lozano - Coordinador Contable\nYordanis Chavez - Analista Contable\nSebastián Vasquez - Analista Contable\nYuliana Ramirez - Aprendiz Contable", "Informe": "", "cover": "https://img.freepik.com/free-photo/accountants-working-late-office_1098-18496.jpg", "descripcion_larga": "Gestión de cuentas por pagar y cobrar, conciliaciones bancarias, elaboración de estados financieros y reportes fiscales." } },
        { "id": "3", "fields": { "Title": "Talento Humano", "Sitio": "", "Contactos": "Adriana Meola - Coordinadora Talento Humano\nDuván Simancas - Analista Talento Humano\nValentina Ospino - Aprendiz Talento Humano", "Informe": "", "cover": "https://img.freepik.com/free-photo/group-people-working-out-business-meeting_1303-15780.jpg", "descripcion_larga": "Administración del personal, nómina, bienestar laboral, seguridad y salud en el trabajo, y desarrollo organizacional." } }
    ]
};

let MOCK_CONVERSATIONS = [
    { author: "Johnier Gómez", time: "hace 24 m", text: "Probando el nuevo foro colaborativo de la intranet. ¡Compartan sus ideas!", likes: 3, comments: 2, type: "Discusión" },
    { author: "Carlos Martínez", time: "hace 2 h", text: "Recordatorio: la capacitación de SAP será este viernes a las 10am en la sala de juntas.", likes: 8, comments: 1, type: "Anuncio" },
    { author: "Ana Gómez", time: "hace 3 h", text: "¿Alguien sabe cómo solicitar los formatos de evaluación de desempeño? Necesito para mi equipo.", likes: 5, comments: 4, type: "Pregunta" },
    { author: "Laura Jiménez", time: "hace 5 h", text: "¡Excelente trabajo del equipo de operaciones! La productividad del mes subió un 15%.", likes: 12, comments: 3, type: "Elogio" },
    { author: "Pedro Ruiz", time: "ayer", text: "¿Qué opinan de implementar un día de teletrabajo a la semana? Me gustaría conocer sus experiencias.", likes: 7, comments: 6, type: "Sondeo" }
];

function getMockDriveRoot() {
    const allItems = [];
    MOCK_DEPARTMENTS.forEach(dept => {
        allItems.push({
            id: dept.id,
            name: dept.name,
            folder: { childCount: MOCK_FILES_BY_SECTOR[dept.id].length },
            lastModifiedDateTime: "2026-04-14T17:30:00Z",
            webUrl: "#",
            createdBy: { user: { displayName: "Admin" } }
        });
    });
    return allItems;
}

function getMockDriveItems(folderId) {
    const folderKey = MOCK_DEPARTMENTS.find(d => d.id === folderId)?.id;
    if (folderKey && MOCK_FILES_BY_SECTOR[folderKey]) {
        return MOCK_FILES_BY_SECTOR[folderKey];
    }
    return [];
}

async function getCurrentUser() {
    const data = await graphFetch("https://graph.microsoft.com/v1.0/me?$select=displayName,mail,jobTitle,department,officeLocation,userPrincipalName");
    if (data) return data;
    return MOCK_USER;
}

async function getDriveRoot() {
    const data = await graphFetch(GRAPH_BASE + "/sites/" + SHAREPOINT_SITE_ID + "/drive/root/children?$select=id,name,size,file,folder,lastModifiedDateTime,webUrl,createdBy");
    if (data) return data.value || [];
    return getMockDriveRoot();
}

async function getDriveItems(folderId) {
    const data = await graphFetch(GRAPH_BASE + "/sites/" + SHAREPOINT_SITE_ID + "/drive/items/" + folderId + "/children?$select=id,name,size,file,folder,lastModifiedDateTime,webUrl,createdBy");
    if (data) return data.value || [];
    return getMockDriveItems(folderId);
}

async function getNewsPosts() {
    const data = await graphFetch(GRAPH_BASE + "/sites/" + SHAREPOINT_SITE_ID + "/pages?$select=id,title,description,createdDateTime,lastModifiedDateTime,webUrl&$orderby=createdDateTime desc&$top=20");
    if (data) return data.value || [];
    return MOCK_NEWS;
}

async function getLeaveRequests() {
    const data = await graphFetch(GRAPH_BASE + "/sites/" + SHAREPOINT_SITE_ID + "/lists/SolicitudesVacaciones/items?$expand=fields&$orderby=fields/Created desc&$top=50");
    if (data) return data.value || [];
    return MOCK_LEAVE_REQUESTS;
}

async function submitLeaveRequest(formData) {
    const url = GRAPH_BASE + "/sites/" + SHAREPOINT_SITE_ID + "/lists/SolicitudesVacaciones/items";
    const data = await graphFetch(url, {
        method: "POST",
        body: JSON.stringify({
            fields: {
                Title: "Solicitud de " + formData.nombre,
                Nombre: formData.nombre,
                FechaInicio: formData.fechaInicio,
                FechaFin: formData.fechaFin,
                Motivo: formData.motivo,
                Email: "",
                Estado: "PENDIENTE",
                Created: new Date().toISOString()
            }
        })
    });
    if (data) return data;
    const newEntry = {
        id: "lv-" + Date.now(),
        fields: {
            Title: "Solicitud de " + formData.nombre,
            Nombre: formData.nombre,
            FechaInicio: formData.fechaInicio,
            FechaFin: formData.fechaFin,
            Motivo: formData.motivo,
            Estado: "PENDIENTE",
            Created: new Date().toISOString()
        }
    };
    MOCK_LEAVE_REQUESTS.unshift(newEntry);
    return newEntry;
}

async function getSupportTickets() {
    const data = await graphFetch(GRAPH_BASE + "/sites/" + SHAREPOINT_SITE_ID + "/lists/SoporteTecnico/items?$expand=fields&$orderby=fields/Created desc&$top=50");
    if (data) return data.value || [];
    return MOCK_SUPPORT_TICKETS;
}

async function submitSupportTicket(formData) {
    const url = GRAPH_BASE + "/sites/" + SHAREPOINT_SITE_ID + "/lists/SoporteTecnico/items";
    const data = await graphFetch(url, {
        method: "POST",
        body: JSON.stringify({
            fields: {
                Title: "Soporte de " + formData.nombre,
                Nombre: formData.nombre,
                Categoria: formData.categoria,
                Descripcion: formData.descripcion,
                Email: "",
                Estado: "PENDIENTE",
                Created: new Date().toISOString()
            }
        })
    });
    if (data) return data;
    const newEntry = {
        id: "st-" + Date.now(),
        fields: {
            Title: "Soporte de " + formData.nombre,
            Nombre: formData.nombre,
            Categoria: formData.categoria,
            Descripcion: formData.descripcion,
            Estado: "PENDIENTE",
            Created: new Date().toISOString()
        }
    };
    MOCK_SUPPORT_TICKETS.unshift(newEntry);
    return newEntry;
}

async function getAreas() {
    const data = await graphFetch(GRAPH_BASE + "/sites/" + SHAREPOINT_SITE_ID + "/lists/Areas/items?$expand=fields&$top=50");
    if (data) {
        const items = data.value || [];
        return items.map(item => ({
            id: item.id,
            Title: item.fields.Title || "",
            Sitio: item.fields.Sitio || "",
            Contactos: item.fields.Contactos || "",
            Informe: item.fields.Informe || "",
            cover: item.fields.cover || "",
            descripcion_larga: item.fields.descripcion_larga || ""
        }));
    }
    return (MOCK_AREAS_REAL_LIST.value || []).map(item => ({
        id: item.id,
        Title: item.fields.Title || "",
        Sitio: item.fields.Sitio || "",
        Contactos: item.fields.Contactos || "",
        Informe: item.fields.Informe || "",
        cover: item.fields.cover || "",
        descripcion_larga: item.fields.descripcion_larga || ""
    }));
}

async function getConversations() {
    const data = await graphFetch(GRAPH_BASE + "/sites/" + SHAREPOINT_SITE_ID + "/lists/Conversaciones/items?$expand=fields&$orderby=fields/Created desc&$top=50");
    if (data) return data.value || [];
    return MOCK_CONVERSATIONS;
}

async function submitConversation(formData) {
    const url = GRAPH_BASE + "/sites/" + SHAREPOINT_SITE_ID + "/lists/Conversaciones/items";
    const data = await graphFetch(url, {
        method: "POST",
        body: JSON.stringify({
            fields: {
                Title: formData.text.slice(0, 100),
                AuthorName: formData.author,
                Message: formData.text,
                Type: formData.type || "Discusión",
                Created: new Date().toISOString()
            }
        })
    });
    if (data) return data;
    const newEntry = {
        id: "conv-" + Date.now(),
        author: formData.author,
        text: formData.text,
        type: formData.type || "Discusión",
        likes: 0,
        comments: 0,
        time: "justo ahora"
    };
    MOCK_CONVERSATIONS.unshift(newEntry);
    return newEntry;
}

async function getGalleryAlbums() {
    const data = await graphFetch(GRAPH_BASE + "/sites/" + SHAREPOINT_SITE_ID + "/drive/root/children?$select=id,name,size,folder,lastModifiedDateTime,webUrl,createdBy&$filter=folder/childCount gt 0&$top=20");
    if (data) return data.value || [];
    return MOCK_GALLERY_ALBUMS;
}

async function getGalleryAlbumItems(albumId) {
    const data = await graphFetch(GRAPH_BASE + "/sites/" + SHAREPOINT_SITE_ID + "/drive/items/" + albumId + "/children?$select=id,name,size,file,image,webUrl,lastModifiedDateTime");
    if (data) return { items: data.value || [] };
    const album = MOCK_GALLERY_ALBUMS.find(a => a.id === albumId);
    return album || null;
}
