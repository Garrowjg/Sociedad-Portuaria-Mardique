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
    displayName: "Adelino Aragon",
    mail: "AdelinoAragon@spmardique.co",
    jobTitle: "Analista de TI",
    department: "Tecnología e Informática",
    officeLocation: "TI",
    userPrincipalName: "AdelinoAragon@spmardique.co"
};

const MOCK_DEPARTMENTS = [
    { id: "rrhh", name: "Talento Humano", icon: "fa-users" },
    { id: "finanzas", name: "Contabilidad", icon: "fa-chart-line" },
    { id: "ti", name: "Tecnología e Informática", icon: "fa-laptop-code" }
];

const MOCK_NEWS = [
    { id: "n1", fields: { Title: "Arribo de buque con 35.000 toneladas de maíz", Description: "El M/V Grain Star atracó esta madrugada en el muelle 4 con 35.000 toneladas de maíz amarillo proveniente de Estados Unidos. La operación de descarga se estima en 48 horas continuas.", Created: "2026-07-12T06:00:00Z" }, coverUrl: "/api/placeholder/buque/800/450", webUrl: "#" },
    { id: "n2", fields: { Title: "Semana Ambiental 2026 — Un éxito rotundo", Description: "Del 23 al 29 de junio celebramos la Semana Ambiental con jornadas de reforestación, limpieza de muelles y charlas sobre sostenibilidad. Participaron más de 120 colaboradores.", Created: "2026-06-30T10:00:00Z" }, coverUrl: "/api/placeholder/ambiente/800/450", webUrl: "#" },
    { id: "n3", fields: { Title: "Nuevo sistema de Gestión Documental", Description: "Informamos a todos los colaboradores que hemos implementado un nuevo sistema de gestión documental integrado con SharePoint. Pronto recibirán capacitación sobre su uso.", Created: "2026-07-10T08:00:00Z" }, coverUrl: "/api/placeholder/documentos/800/450", webUrl: "#" },
    { id: "n4", fields: { Title: "Llegada de grúa móvil Liebherr LHM 550", Description: "La nueva grúa móvil Liebherr LHM 550 llegó al puerto para reforzar la capacidad operativa de descarga de graneles. Tiene un alcance de 54 metros y capacidad de 124 toneladas.", Created: "2026-07-05T14:00:00Z" }, coverUrl: "/api/placeholder/grua/800/450", webUrl: "#" },
    { id: "n5", fields: { Title: "Jornada de Vacunación Empresarial", Description: "La próxima semana se realizará la jornada de vacunación contra la influenza en las instalaciones de la bodega. Inscríbanse con RRHH.", Created: "2026-07-08T10:30:00Z" }, coverUrl: "/api/placeholder/vacuna/800/450", webUrl: "#" },
    { id: "n6", fields: { Title: "Exportación de contenedores con productos petroquímicos", Description: "Zona Franca de Mardique despachó 120 contenedores con productos petroquímicos con destino a Brasil. La operación generó 80 empleos directos durante la carga.", Created: "2026-07-02T09:00:00Z" }, coverUrl: "/api/placeholder/contenedores/800/450", webUrl: "#" },
    { id: "n7", fields: { Title: "Capacitación en primeros auxilios para brigadistas", Description: "15 colaboradores de las brigadas de emergencia recibieron certificación en primeros auxilios básicos y RCP, dictada por la Defensa Civil.", Created: "2026-06-25T15:00:00Z" }, coverUrl: "/api/placeholder/brigada/800/450", webUrl: "#" },
    { id: "n8", fields: { Title: "Actualización de la Política de Teletrabajo", Description: "Se ha publicado la nueva versión de la política de teletrabajo. Todos los empleados deben leerla y firmar el acuse de recibo antes del 30 de julio.", Created: "2026-07-01T09:00:00Z" }, coverUrl: "/api/placeholder/teletrabajo/800/450", webUrl: "#" }
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
        coverUrl: "/api/placeholder/id-1015/600/400",
        createdBy: { user: { displayName: "Carlos Martínez" } },
        items: [
            { id: "ind-01", name: "izamiento_bandera.jpg", size: 512000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "/api/placeholder/id-1015/1200/800", lastModifiedDateTime: "2026-07-20T10:30:00Z" },
            { id: "ind-02", name: "acto_central.jpg", size: 384000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "/api/placeholder/id-1016/1200/800", lastModifiedDateTime: "2026-07-20T11:00:00Z" },
            { id: "ind-03", name: "personal_muelle.jpg", size: 620000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "/api/placeholder/id-1018/1200/800", lastModifiedDateTime: "2026-07-20T11:30:00Z" },
            { id: "ind-04", name: "discurso_gerencia.jpg", size: 445000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "/api/placeholder/id-1020/1200/800", lastModifiedDateTime: "2026-07-20T12:00:00Z" },
            { id: "ind-05", name: "integracion_empleados.jpg", size: 512000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "/api/placeholder/id-1024/1200/800", lastModifiedDateTime: "2026-07-20T14:00:00Z" },
            { id: "ind-06", name: "show_cultural.jpg", size: 398000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "/api/placeholder/id-1025/1200/800", lastModifiedDateTime: "2026-07-20T15:30:00Z" },
            { id: "ind-07", name: "cierre_evento.jpg", size: 356000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "/api/placeholder/id-1035/1200/800", lastModifiedDateTime: "2026-07-20T17:00:00Z" },
            { id: "ind-08", name: "foto_grupal.jpg", size: 780000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "/api/placeholder/id-1039/1200/800", lastModifiedDateTime: "2026-07-20T18:00:00Z" }
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
        coverUrl: "/api/placeholder/id-1076/600/400",
        createdBy: { user: { displayName: "Andrés Ramírez" } },
        items: [
            { id: "inc-01", name: "teoria_prevencion.jpg", size: 420000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "/api/placeholder/id-1076/1200/800", lastModifiedDateTime: "2026-07-10T08:30:00Z" },
            { id: "inc-02", name: "demostracion_extintor.jpg", size: 510000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "/api/placeholder/id-1077/1200/800", lastModifiedDateTime: "2026-07-10T10:00:00Z" },
            { id: "inc-03", name: "practica_dirigida.jpg", size: 485000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "/api/placeholder/id-1084/1200/800", lastModifiedDateTime: "2026-07-10T11:00:00Z" },
            { id: "inc-04", name: "brigada_seguridad.jpg", size: 490000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "/api/placeholder/id-1027/1200/800", lastModifiedDateTime: "2026-07-10T12:00:00Z" },
            { id: "inc-05", name: "maniobra_manguera.jpg", size: 530000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "/api/placeholder/id-1068/1200/800", lastModifiedDateTime: "2026-07-10T14:00:00Z" },
            { id: "inc-06", name: "certificacion_personal.jpg", size: 410000, file: { mimeType: "image/jpeg" }, image: {}, webUrl: "/api/placeholder/id-1070/1200/800", lastModifiedDateTime: "2026-07-10T16:30:00Z" }
        ]
    }
];

let MOCK_SUPPORT_TICKETS = [
    { id: "st-1", fields: { Title: "Soporte de Carlos Martínez", Nombre: "Carlos Martínez", Categoria: "Hardware", Descripcion: "El monitor de mi estación de trabajo no enciende.", Estado: "EN CURSO", Created: "2026-04-11T08:15:00Z" } },
    { id: "st-2", fields: { Title: "Soporte de Carlos Martínez", Nombre: "Carlos Martínez", Categoria: "Software", Descripcion: "No puedo acceder al módulo de facturación en el ERP.", Estado: "RESUELTO", Created: "2026-04-09T14:30:00Z" } }
];

const MOCK_AREAS_REAL_LIST = {
    "value": [
        { "id": "1", "fields": { "Title": "Sistemas", "Sitio": "", "Contactos": "Ramiro Rodelo del Valle - Coordinador de Tecnologia de Informacion\nJohnnier Gomez - Aprendiz TI\nAdelino Aragon - Tecnico de Sistemas\nOmar Caicedo Martinez - Analista TI", "Informe": "<iframe title=\"Dashboard\" width=\"600\" height=\"373.5\" src=\"https://app.powerbi.com/view?r=eyJrIjoiYTc1OTUxYTMtY2E0NS00MWI1LTlkN2MtZDllMDBjMzZiNmRmIiwidCI6ImYzM2JlZDlmLTIyYmQtNDM1MC1iN2RhLTY2YmQ4OGZjNjQ1OCIsImMiOjR9\" frameborder=\"0\" allowFullScreen=\"true\"></iframe>", "cover": "/api/placeholder/server-room_53876-97067", "descripcion_larga": "Encargada de la infraestructura tecnológica, soporte a usuarios, administración de sistemas ERP y seguridad informática." } },
        { "id": "2", "fields": { "Title": "Contabilidad", "Sitio": "", "Contactos": "Carlos Molina Lozano - Coordinador Contable\nYordanis Chavez - Analista Contable\nSebastián Vasquez - Analista Contable\nYuliana Ramirez - Aprendiz Contable", "Informe": "", "cover": "/api/placeholder/accountants-working-late-office_1098-18496", "descripcion_larga": "Gestión de cuentas por pagar y cobrar, conciliaciones bancarias, elaboración de estados financieros y reportes fiscales." } },
        { "id": "3", "fields": { "Title": "Talento Humano", "Sitio": "", "Contactos": "Adriana Meola - Coordinadora Talento Humano\nDuván Simancas - Analista Talento Humano\nValentina Ospino - Aprendiz Talento Humano", "Informe": "", "cover": "/api/placeholder/group-people-working-out-business-meeting_1303-15780", "descripcion_larga": "Administración del personal, nómina, bienestar laboral, seguridad y salud en el trabajo, y desarrollo organizacional." } }
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
    // En la vista "Todos", los sectores se muestran como carpetas del drive.
    return MOCK_DEPARTMENTS.map(dept => ({
        id: dept.id,
        name: dept.name,
        folder: { childCount: 0 },
        lastModifiedDateTime: "2026-04-14T17:30:00Z",
        webUrl: "#",
        createdBy: { user: { displayName: "Admin" } }
    }));
}

function getMockDriveItems(folderId) {
    // Ya no hay documentos de ejemplo: el contenido real viene del backend
    // (/api/intranet/documents) y se combina en la plantilla.
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

async function updateReportIframe(areaId, iframeHtml) {
    const url = GRAPH_BASE + "/sites/" + SHAREPOINT_SITE_ID + "/lists/Areas/items/" + areaId;
    const data = await graphFetch(url, {
        method: "PATCH",
        body: JSON.stringify({ fields: { Informe: iframeHtml } })
    });
    if (data) return data;
    const area = MOCK_AREAS_REAL_LIST.value.find(a => a.id === areaId);
    if (area) area.fields.Informe = iframeHtml;
    return { success: true };
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

/* ---------- Intranet: documentos subidos (backend local) ---------- */

async function getIntranetDocs(sector) {
    try {
        const url = "/api/intranet/documents" + (sector ? "?sector=" + encodeURIComponent(sector) : "");
        const res = await fetch(url);
        if (!res.ok) return [];
        return await res.json();
    } catch (e) {
        return [];
    }
}

async function getIntranetFolderItems(parentId) {
    try {
        const res = await fetch("/api/intranet/documents?parentId=" + encodeURIComponent(parentId));
        if (!res.ok) return [];
        return await res.json();
    } catch (e) {
        return [];
    }
}

async function getIntranetAllDocs(sector) {
    try {
        const url = "/api/intranet/documents?all=true" + (sector ? "&sector=" + encodeURIComponent(sector) : "");
        const res = await fetch(url);
        if (!res.ok) return [];
        return await res.json();
    } catch (e) {
        return [];
    }
}

async function uploadIntranetDoc(sector, file, uploader, parentId) {
    const fd = new FormData();
    fd.append("sector", sector);
    fd.append("file", file);
    if (uploader) fd.append("uploader", uploader);
    if (parentId) fd.append("parentId", parentId);
    const res = await fetch("/api/intranet/documents", { method: "POST", body: fd });
    const json = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(json.message || "No se pudo subir el archivo.");
    return json;
}

async function createIntranetFolder(sector, nombre, uploader, parentId) {
    const fd = new FormData();
    fd.append("sector", sector);
    fd.append("nombre", nombre);
    if (uploader) fd.append("uploader", uploader);
    if (parentId) fd.append("parentId", parentId);
    const res = await fetch("/api/intranet/documents/folder", { method: "POST", body: fd });
    const json = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(json.message || "No se pudo crear la carpeta.");
    return json;
}

async function deleteIntranetDoc(id) {
    try {
        const res = await fetch("/api/intranet/documents/" + id, { method: "DELETE" });
        const json = await res.json().catch(() => ({}));
        return json.ok === true;
    } catch (e) {
        return false;
    }
}

function intranetDocContentUrl(id) {
    return "/api/intranet/documents/" + id + "/content";
}

function intranetDocThumbUrl(id) {
    return "/api/intranet/documents/" + id + "/thumbnail";
}

function intranetDocQrUrl(id) {
    return "/api/intranet/documents/" + id + "/qr";
}

function intranetSectorQrUrl(sector) {
    return "/api/intranet/documents/qr/sector/" + encodeURIComponent(sector);
}

/* ---------- Intranet: registro de vistas y acceso ---------- */

async function recordIntranetDocView(documentId, email, name) {
    try {
        const res = await fetch("/api/intranet/documents/" + documentId + "/view", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email: email || "", name: name || "" })
        });
        return res.ok;
    } catch (e) {
        return false;
    }
}

async function getIntranetDocViews(documentId, email) {
    try {
        const res = await fetch("/api/intranet/documents/" + documentId + "/views?email=" + encodeURIComponent(email || ""));
        if (!res.ok) return null;
        return await res.json();
    } catch (e) {
        return null;
    }
}

async function getIntranetAccessStatus(email) {
    try {
        const res = await fetch("/api/intranet/documents/views/access?email=" + encodeURIComponent(email || ""));
        if (!res.ok) return null;
        return await res.json();
    } catch (e) {
        return null;
    }
}

async function getIntranetViewStats(email) {
    try {
        const res = await fetch("/api/intranet/documents/views/stats?email=" + encodeURIComponent(email || ""));
        if (!res.ok) return null;
        return await res.json();
    } catch (e) {
        return null;
    }
}

async function getIntranetViewEvents(email) {
    try {
        const res = await fetch("/api/intranet/documents/views/events?email=" + encodeURIComponent(email || ""));
        if (!res.ok) return null;
        return await res.json();
    } catch (e) {
        return null;
    }
}

async function grantIntranetRole(email, role, adminEmail) {
    try {
        const res = await fetch("/api/intranet/documents/views/grant?email=" + encodeURIComponent(email)
            + "&role=" + encodeURIComponent(role)
            + "&adminEmail=" + encodeURIComponent(adminEmail || ""), { method: "POST" });
        const json = await res.json().catch(() => ({}));
        return json.ok === true;
    } catch (e) {
        return false;
    }
}

async function revokeIntranetRole(email, adminEmail) {
    try {
        const res = await fetch("/api/intranet/documents/views/revoke?email=" + encodeURIComponent(email)
            + "&adminEmail=" + encodeURIComponent(adminEmail || ""), { method: "DELETE" });
        const json = await res.json().catch(() => ({}));
        return json.ok === true;
    } catch (e) {
        return false;
    }
}

/* ── Galería de eventos intranet ─────────────────────────── */
async function getGalleryEvents() {
    try {
        const res = await fetch("/api/intranet/gallery");
        return await res.json();
    } catch (e) {
        return [];
    }
}

async function createGalleryEvent(data) {
    try {
        const res = await fetch("/api/intranet/gallery", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        });
        return await res.json();
    } catch (e) {
        return null;
    }
}

async function likeGalleryEvent(id) {
    try {
        const res = await fetch("/api/intranet/gallery/" + id + "/like", { method: "POST" });
        return await res.json();
    } catch (e) {
        return null;
    }
}

async function deleteGalleryEvent(id) {
    try {
        const res = await fetch("/api/intranet/gallery/" + id, { method: "DELETE" });
        const json = await res.json().catch(() => ({}));
        return json.ok === true;
    } catch (e) {
        return false;
    }
}

/* ── Calendario intranet ──────────────────────────── */
async function getCalendarEvents() {
    try {
        const res = await fetch("/api/intranet/calendar");
        return await res.json();
    } catch (e) {
        return [];
    }
}

async function createCalendarEvent(data) {
    try {
        const res = await fetch("/api/intranet/calendar", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        });
        return await res.json();
    } catch (e) {
        return null;
    }
}

async function updateCalendarEvent(id, data) {
    try {
        const res = await fetch("/api/intranet/calendar/" + id, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        });
        return await res.json();
    } catch (e) {
        return null;
    }
}

async function deleteCalendarEvent(id) {
    try {
        const res = await fetch("/api/intranet/calendar/" + id, { method: "DELETE" });
        const json = await res.json().catch(() => ({}));
        return json.ok === true;
    } catch (e) {
        return false;
    }
}

/* ── Conversaciones / Foro intranet ────────────────── */
async function getConversations(userId) {
    try {
        const url = userId ? "/api/intranet/conversations?userId=" + encodeURIComponent(userId) : "/api/intranet/conversations";
        const res = await fetch(url);
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return []; }
    } catch (e) {
        return [];
    }
}

async function submitConversation(data) {
    try {
        const res = await fetch("/api/intranet/conversations", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        });
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return null; }
    } catch (e) {
        return null;
    }
}

async function likeConversation(id, userId) {
    try {
        const res = await fetch("/api/intranet/conversations/" + id + "/like", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ userId: userId || "user-" + Date.now() })
        });
        if (!res.ok) return null;
        return await res.json();
    } catch (e) {
        return null;
    }
}

async function reactToConversation(id, userId, type) {
    try {
        const res = await fetch("/api/intranet/conversations/" + id + "/reaction", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ userId: userId, type: type })
        });
        if (!res.ok) return null;
        return await res.json();
    } catch (e) {
        return null;
    }
}

async function getConversationComments(id) {
    try {
        const res = await fetch("/api/intranet/conversations/" + id + "/comments");
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return { comments: [], count: 0 }; }
    } catch (e) {
        return { comments: [], count: 0 };
    }
}

async function addConversationComment(id, text, authorName, authorEmail) {
    try {
        const res = await fetch("/api/intranet/conversations/" + id + "/comments", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ text: text, authorName: authorName || "Empleado", authorEmail: authorEmail || "" })
        });
        const responseText = await res.text();
        try { return JSON.parse(responseText); } catch(e) { return null; }
    } catch (e) {
        return null;
    }
}

async function deleteConversation(id) {
    try {
        const res = await fetch("/api/intranet/conversations/" + id, { method: "DELETE" });
        const json = await res.json().catch(() => ({}));
        return json.ok === true;
    } catch (e) {
        return false;
    }
}

async function updateConversation(id, data) {
    try {
        const res = await fetch("/api/intranet/conversations/" + id, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        });
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return null; }
    } catch (e) {
        return null;
    }
}

async function pinConversation(id) {
    try {
        const res = await fetch("/api/intranet/conversations/" + id + "/pin", {
            method: "POST",
            headers: { "Content-Type": "application/json" }
        });
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return null; }
    } catch (e) {
        return null;
    }
}

/* ── Áreas intranet ───────────────────────────────── */
async function getAreas() {
    try {
        const res = await fetch("/api/intranet/areas");
        const text = await res.text();
        let data;
        try { data = JSON.parse(text); } catch(e) { return []; }
        return (data || []).map(a => ({
            id: String(a.id),
            Title: a.nombre,
            nombre: a.nombre,
            descripcion: a.descripcion,
            descripcion_larga: a.descripcion,
            Contactos: a.contactos,
            cover: a.cover,
            Sitio: a.sitio,
            informe: a.informe
        }));
    } catch (e) {
        return [];
    }
}

async function createArea(data) {
    try {
        const res = await fetch("/api/intranet/areas", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        });
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return null; }
    } catch (e) {
        return null;
    }
}

async function updateArea(id, data) {
    try {
        const res = await fetch("/api/intranet/areas/" + id, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        });
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return null; }
    } catch (e) {
        return null;
    }
}

async function deleteArea(id) {
    try {
        const res = await fetch("/api/intranet/areas/" + id, { method: "DELETE" });
        const json = await res.json().catch(() => ({}));
        return json.ok === true;
    } catch (e) {
        return false;
    }
}

/* ── Upload intranet images ──────────────────────── */
async function uploadIntranetImage(file) {
    try {
        const formData = new FormData();
        formData.append("file", file);
        const res = await fetch("/api/intranet/upload", { method: "POST", body: formData });
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return { error: "Respuesta inesperada del servidor (" + res.status + ")" }; }
    } catch (e) {
        return { error: e.message || "Error de red" };
    }
}

/* ── Autoservicio Talento Humano ──────────────────── */
function intranetHrQrUrl(sectionId) {
    return "/api/intranet/hr/" + encodeURIComponent(sectionId) + "/qr";
}

async function recordHrSectionView(sectionId, email, name) {
    try {
        const res = await fetch("/api/intranet/hr/" + encodeURIComponent(sectionId) + "/view", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email: email || "", name: name || "" })
        });
        return res.ok;
    } catch (e) {
        return false;
    }
}

async function getHrSectionViews(sectionId) {
    try {
        const res = await fetch("/api/intranet/hr/" + encodeURIComponent(sectionId) + "/views");
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return []; }
    } catch (e) {
        return [];
    }
}

async function getHrViewStats() {
    try {
        const res = await fetch("/api/intranet/hr/views/stats");
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return null; }
    } catch (e) {
        return null;
    }
}

async function getHrDocs(sectionId) {
    try {
        const res = await fetch("/api/intranet/hr/" + encodeURIComponent(sectionId) + "/docs");
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return []; }
    } catch (e) {
        return [];
    }
}

async function addHrDoc(sectionId, fileName, fileUrl, description, uploadedBy) {
    try {
        const res = await fetch("/api/intranet/hr/" + encodeURIComponent(sectionId) + "/docs", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ fileName: fileName, fileUrl: fileUrl, description: description || "", uploadedBy: uploadedBy || "" })
        });
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return null; }
    } catch (e) {
        return null;
    }
}

async function deleteHrDoc(docId) {
    try {
        const res = await fetch("/api/intranet/hr/docs/" + docId, { method: "DELETE" });
        const text = await res.text();
        try { const json = JSON.parse(text); return json.ok === true; } catch(e) { return false; }
    } catch (e) {
        return false;
    }
}

/* ═══════════════════════════════════════════════════════════════════════
   SOLICITUDES HR — envío, recibidos, respuesta, firma
   ═══════════════════════════════════════════════════════════════════════ */

async function getStaffDirectory() {
    try {
        const res = await fetch("/api/intranet/solicitudes-hr/staff");
        return await res.json();
    } catch (e) {
        return [];
    }
}

async function createSolicitudHr(data) {
    try {
        const res = await fetch("/api/intranet/solicitudes-hr", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        });
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return null; }
    } catch (e) {
        return null;
    }
}

async function getSentSolicitudes(email) {
    try {
        const res = await fetch("/api/intranet/solicitudes-hr/sent?email=" + encodeURIComponent(email));
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return []; }
    } catch (e) {
        return [];
    }
}

async function getReceivedSolicitudes(email) {
    try {
        const res = await fetch("/api/intranet/solicitudes-hr/received?email=" + encodeURIComponent(email));
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return []; }
    } catch (e) {
        return [];
    }
}

async function getSolicitudHr(id) {
    try {
        const res = await fetch("/api/intranet/solicitudes-hr/" + id);
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return null; }
    } catch (e) {
        return null;
    }
}

async function respondToSolicitudHr(id, data) {
    try {
        const res = await fetch("/api/intranet/solicitudes-hr/" + id + "/respond", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        });
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return null; }
    } catch (e) {
        return null;
    }
}
