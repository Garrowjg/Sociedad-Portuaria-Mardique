function loadGallery() {
    const container = document.getElementById("gallery-container");
    if (!container) return;
    showLoading("gallery-container");
    document.getElementById("gallery-detail-view")?.classList.remove("active");
    document.getElementById("gallery-albums-view")?.classList.add("active");
    getGalleryAlbums().then(albums => {
        renderGalleryAlbums(albums);
    }).catch(err => {
        container.innerHTML = '<div class="empty-state error"><i class="fas fa-exclamation-triangle"></i><p>Error al cargar galería: ' + err.message + '</p></div>';
    });
}

function renderGalleryAlbums(albums) {
    const container = document.getElementById("gallery-container");
    if (!container) return;
    if (!albums || albums.length === 0) {
        container.innerHTML = '<div class="empty-state"><i class="fas fa-images"></i><p>No hay álbumes disponibles</p></div>';
        return;
    }
    let html = '<div class="gallery-grid">';
    albums.forEach(album => {
        const imgSrc = album.coverUrl || "https://picsum.photos/seed/" + album.id + "/600/400";
        const count = album.folder?.childCount || album.items?.length || 0;
        const date = formatDate(album.eventDate || album.lastModifiedDateTime);
        html += '<div class="gallery-album" data-id="' + escapeHtml(album.id) + '">';
        html += '<div class="gallery-album-cover"><img src="' + imgSrc + '" alt="' + escapeHtml(album.name) + '" loading="lazy"></div>';
        html += '<div class="gallery-album-body">';
        html += '<div class="gallery-album-count"><i class="fas fa-image"></i> ' + count + ' fotos</div>';
        html += '<h3 class="gallery-album-title">' + escapeHtml(album.name) + '</h3>';
        html += '<div class="gallery-album-date"><i class="far fa-calendar-alt"></i> ' + date + '</div>';
        if (album.description) html += '<p class="gallery-album-desc">' + escapeHtml(album.description) + '</p>';
        html += '<button class="gallery-album-btn"><i class="fas fa-eye"></i> Ver Álbum</button>';
        html += '</div></div>';
    });
    html += '</div>';
    container.innerHTML = html;
    container.querySelectorAll(".gallery-album").forEach(card => {
        card.addEventListener("click", function() {
            openGalleryAlbum(this.dataset.id);
        });
    });
}

function openGalleryAlbum(albumId) {
    getGalleryAlbumItems(albumId).then(album => {
        if (!album) {
            showIntranetToast("Álbum no encontrado", "error");
            return;
        }
        renderGalleryLightbox(album);
    }).catch(err => {
        showIntranetToast("Error al abrir álbum: " + err.message, "error");
    });
}

function renderGalleryLightbox(album) {
    const view = document.getElementById("gallery-detail-view");
    const albumsView = document.getElementById("gallery-albums-view");
    if (!view || !albumsView) return;
    const items = album.items || [];
    if (!items || items.length === 0) {
        showIntranetToast("Este álbum no contiene imágenes", "error");
        return;
    }
    const titleEl = document.getElementById("gallery-detail-title");
    const descEl = document.getElementById("gallery-detail-desc");
    const gridEl = document.getElementById("gallery-detail-grid");
    const date = formatDate(album.eventDate || album.lastModifiedDateTime);
    if (titleEl) titleEl.textContent = album.name;
    if (descEl) descEl.innerHTML = '<i class="far fa-calendar-alt"></i> ' + date + ' &middot; ' + items.length + ' fotos' + (album.description ? '<br>' + escapeHtml(album.description) : '');
    if (gridEl) {
        gridEl.innerHTML = items.map(item => {
            const url = item.webUrl || "https://picsum.photos/seed/" + item.id + "/1200/800";
            const thumb = url.replace(/\/\d+\/\d+$/, "/400/300");
            return '<div class="gallery-detail-item" data-url="' + url + '" data-name="' + escapeHtml(item.name) + '">' +
                '<img src="' + thumb + '" alt="' + escapeHtml(item.name) + '" loading="lazy">' +
                '<div class="gallery-detail-overlay"><i class="fas fa-expand"></i></div>' +
                '</div>';
        }).join("");
        gridEl.querySelectorAll(".gallery-detail-item").forEach(item => {
            item.addEventListener("click", function() {
                openGalleryPreview(this.dataset.url, this.dataset.name);
            });
        });
    }
    albumsView.classList.remove("active");
    view.classList.add("active");
}

function openGalleryPreview(url, name) {
    const modal = document.getElementById("gallery-preview-modal");
    if (!modal) return;
    const img = modal.querySelector(".gallery-preview-img");
    const cap = modal.querySelector(".gallery-preview-caption");
    if (img) img.src = url;
    if (cap) cap.textContent = name || "";
    modal.classList.add("active");
    document.body.style.overflow = "hidden";
}

function closeGalleryPreview() {
    const modal = document.getElementById("gallery-preview-modal");
    if (!modal) return;
    modal.classList.remove("active");
    document.body.style.overflow = "";
}

function closeGalleryDetail() {
    const view = document.getElementById("gallery-detail-view");
    const albumsView = document.getElementById("gallery-albums-view");
    if (view) view.classList.remove("active");
    if (albumsView) albumsView.classList.add("active");
}

function formatFileSize(bytes) {
    if (!bytes) return "-";
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + " KB";
    return (bytes / 1048576).toFixed(1) + " MB";
}

function formatDate(dateStr) {
    if (!dateStr) return "-";
    const d = new Date(dateStr);
    return d.toLocaleDateString("es-CO", { year: "numeric", month: "short", day: "numeric" });
}

function getFileIcon(name) {
    const ext = name.split(".").pop().toLowerCase();
    const map = {
        pdf: "fa-file-pdf", doc: "fa-file-word", docx: "fa-file-word",
        xls: "fa-file-excel", xlsx: "fa-file-excel",
        ppt: "fa-file-powerpoint", pptx: "fa-file-powerpoint",
        jpg: "fa-file-image", jpeg: "fa-file-image", png: "fa-file-image", gif: "fa-file-image", webp: "fa-file-image",
        mp4: "fa-file-video", mov: "fa-file-video",
        zip: "fa-file-archive", rar: "fa-file-archive", "7z": "fa-file-archive",
        txt: "fa-file-lines", csv: "fa-file-csv"
    };
    return map[ext] || "fa-file";
}

function renderDocuments(items, containerId, isRoot) {
    const container = document.getElementById(containerId);
    if (!container) return;
    if (!items || items.length === 0) {
        container.innerHTML = '<div class="empty-state"><i class="fas fa-folder-open"></i><p>No hay documentos disponibles</p></div>';
        return;
    }
    let html = '<div class="doc-grid">';
    items.forEach(item => {
        const isFolder = item.folder;
        const icon = isFolder ? "fa-folder" : getFileIcon(item.name);
        const seed = encodeURIComponent(item.name || item.id || "doc");
        const thumbUrl = "https://picsum.photos/seed/" + seed + "/400/250";
        html += '<div class="doc-card" data-id="' + (item.id || "") + '" data-folder="' + (isFolder ? "true" : "false") + '" data-url="' + (item.webUrl || "") + '">';
        html += '<div class="doc-thumb"><img src="' + thumbUrl + '" alt="" loading="lazy"><div class="doc-thumb-icon"><i class="fas ' + icon + '"></i></div></div>';
        html += '<div class="doc-info"><div class="doc-name">' + escapeHtml(item.name) + '</div>';
        if (!isFolder) html += '<div class="doc-meta">' + formatFileSize(item.size) + ' &middot; ' + formatDate(item.lastModifiedDateTime) + '</div>';
        if (isFolder) html += '<div class="doc-meta">' + (item.folder?.childCount || 0) + ' elementos</div>';
        html += '</div></div>';
    });
    html += '</div>';
    container.innerHTML = html;
    container.querySelectorAll(".doc-card").forEach(card => {
        const isFolder = card.dataset.folder === "true";
        const id = card.dataset.id;
        const url = card.dataset.url;
        card.addEventListener("click", function() {
            if (isFolder) {
                loadFolder(id);
            } else if (url) {
                window.open(url, "_blank");
            }
        });
    });
}

function renderDocumentsWithFilter(items, containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;
    const sectorFilter = document.getElementById("sectorFilter");
    const currentSector = sectorFilter ? sectorFilter.value : "todos";
    let filtered = items;
    if (currentSector !== "todos") {
        filtered = MOCK_FILES_BY_SECTOR[currentSector] || [];
    }
    if (!filtered || filtered.length === 0) {
        container.innerHTML = '<div class="empty-state"><i class="fas fa-folder-open"></i><p>No hay documentos para este sector</p></div>';
        return;
    }
    let html = '<div class="doc-grid">';
    filtered.forEach(item => {
        const isFolder = item.folder;
        const icon = isFolder ? "fa-folder" : getFileIcon(item.name);
        const seed = encodeURIComponent(item.name || item.id || "doc");
        const thumbUrl = "https://picsum.photos/seed/" + seed + "/400/250";
        html += '<div class="doc-card" data-id="' + (item.id || "") + '" data-folder="' + (isFolder ? "true" : "false") + '" data-url="' + (item.webUrl || "") + '">';
        html += '<div class="doc-thumb"><img src="' + thumbUrl + '" alt="" loading="lazy"><div class="doc-thumb-icon"><i class="fas ' + icon + '"></i></div></div>';
        html += '<div class="doc-info"><div class="doc-name">' + escapeHtml(item.name) + '</div>';
        if (!isFolder) html += '<div class="doc-meta">' + formatFileSize(item.size) + ' &middot; ' + formatDate(item.lastModifiedDateTime) + '</div>';
        if (isFolder) html += '<div class="doc-meta">' + (item.folder?.childCount || 0) + ' elementos</div>';
        html += '</div></div>';
    });
    html += '</div>';
    container.innerHTML = html;
    container.querySelectorAll(".doc-card").forEach(card => {
        if (card.dataset.folder === "true") {
            card.addEventListener("click", function() {
                loadFolder(this.dataset.id);
            });
        } else if (card.dataset.url) {
            card.addEventListener("click", function() {
                window.open(this.dataset.url, "_blank");
            });
        }
    });
}

let docBreadcrumb = [];

function loadDrive() {
    docBreadcrumb = [{ id: "root", name: "Documentos" }];
    renderBreadcrumb();
    showLoading("documents-container");
    getDriveRoot().then(items => {
        renderDocuments(items, "documents-container", true);
    }).catch(err => {
        document.getElementById("documents-container").innerHTML = '<div class="empty-state error"><i class="fas fa-exclamation-triangle"></i><p>Error al cargar: ' + err.message + '</p></div>';
    });
}

function loadFolder(folderId) {
    showLoading("documents-container");
    const dept = MOCK_DEPARTMENTS.find(d => d.id === folderId);
    getDriveItems(folderId).then(items => {
        docBreadcrumb.push({ id: folderId, name: dept?.name || items[0]?.name || "..." });
        renderBreadcrumb();
        renderDocuments(items, "documents-container", false);
    }).catch(err => {
        document.getElementById("documents-container").innerHTML = '<div class="empty-state error"><i class="fas fa-exclamation-triangle"></i><p>Error: ' + err.message + '</p></div>';
    });
}

function renderBreadcrumb() {
    const el = document.getElementById("doc-breadcrumb");
    if (!el) return;
    el.innerHTML = docBreadcrumb.map((b, i) => {
        if (i === docBreadcrumb.length - 1) return '<span class="crumb-current">' + escapeHtml(b.name) + '</span>';
        return '<span class="crumb-link" data-idx="' + i + '">' + escapeHtml(b.name) + '</span> <span class="crumb-sep">/</span> ';
    }).join("");
    el.querySelectorAll(".crumb-link").forEach(link => {
        link.addEventListener("click", function() {
            const idx = parseInt(this.dataset.idx);
            docBreadcrumb = docBreadcrumb.slice(0, idx + 1);
            renderBreadcrumb();
            if (idx === 0) loadDrive();
        });
    });
}

function loadNews() {
    const container = document.getElementById("news-container");
    if (!container) return;
    showLoading("news-container");
    getNewsPosts().then(posts => {
        if (!posts || posts.length === 0) {
            container.innerHTML = '<div class="empty-state"><i class="fas fa-newspaper"></i><p>No hay noticias publicadas</p></div>';
            return;
        }
        let html = '<div class="news-grid">';
        posts.forEach(post => {
            const fields = post.fields || post;
            const title = fields.Title || fields.title || "Sin título";
            const desc = fields.Description || fields.description || "";
            const date = formatDate(fields.Created || fields.createdDateTime || post.createdDateTime);
            const url = post.webUrl || fields.webUrl || "#";
            html += '<div class="news-card" onclick="window.open(\'' + url + '\',\'_blank\')">';
            html += '<div class="news-date">' + date + '</div>';
            html += '<h3 class="news-title">' + escapeHtml(title) + '</h3>';
            if (desc) html += '<p class="news-desc">' + escapeHtml(desc) + '</p>';
            html += '<div class="news-footer"><span>Ver más <i class="fas fa-arrow-right"></i></span></div>';
            html += '</div>';
        });
        html += '</div>';
        container.innerHTML = html;
    }).catch(err => {
        container.innerHTML = '<div class="empty-state error"><i class="fas fa-exclamation-triangle"></i><p>Error al cargar noticias: ' + err.message + '</p></div>';
    });
}

function renderLeaveHistory(items) {
    const tbody = document.getElementById("leave-history-body");
    if (!tbody) return;
    if (!items || items.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:32px;color:var(--mx);">No hay solicitudes anteriores</td></tr>';
        return;
    }
    tbody.innerHTML = items.map(item => {
        const f = item.fields || item;
        const estado = (f.Estado || "PENDIENTE").toLowerCase();
        const estadoClass = "status-" + estado;
        return '<tr>' +
            '<td>' + formatDate(f.Created || f.createdDateTime) + '</td>' +
            '<td>' + (f.FechaInicio || "-") + '</td>' +
            '<td>' + (f.FechaFin || "-") + '</td>' +
            '<td>' + escapeHtml(f.Motivo || f.Title || "") + '</td>' +
            '<td><span class="status-badge ' + estadoClass + '">' + estado.toUpperCase() + '</span></td>' +
        '</tr>';
    }).join("");
}

function renderSupportHistory(items) {
    const tbody = document.getElementById("support-history-body");
    if (!tbody) return;
    if (!items || items.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:32px;color:var(--mx);">No hay tickets de soporte</td></tr>';
        return;
    }
    tbody.innerHTML = items.map(item => {
        const f = item.fields || item;
        const estado = (f.Estado || "PENDIENTE").toLowerCase();
        const estadoClass = "status-" + estado;
        return '<tr>' +
            '<td>' + formatDate(f.Created || f.createdDateTime) + '</td>' +
            '<td>' + escapeHtml(f.Categoria || "-") + '</td>' +
            '<td>' + escapeHtml(f.Descripcion || "") + '</td>' +
            '<td><span class="status-badge ' + estadoClass + '">' + estado.toUpperCase() + '</span></td>' +
        '</tr>';
    }).join("");
}

function initLeaveForm() {
    const form = document.getElementById("leave-form");
    if (!form || form.dataset.initialized) return;
    form.dataset.initialized = "true";
    form.addEventListener("submit", function(e) {
        e.preventDefault();
        const btn = form.querySelector(".btn-submit");
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Enviando...';
        const data = {
            nombre: document.getElementById("leave-nombre").value,
            fechaInicio: document.getElementById("leave-fecha-inicio").value,
            fechaFin: document.getElementById("leave-fecha-fin").value,
            motivo: document.getElementById("leave-motivo").value
        };
        submitLeaveRequest(data).then(() => {
            form.reset();
            btn.innerHTML = '<i class="fas fa-check"></i> Enviado';
            btn.style.background = "#16a34a";
            setTimeout(() => {
                btn.disabled = false;
                btn.innerHTML = '<i class="fas fa-paper-plane"></i> Enviar Solicitud';
                btn.style.background = "";
            }, 3000);
            showIntranetToast("Solicitud enviada correctamente");
            getLeaveRequests().then(items => renderLeaveHistory(items));
        }).catch(err => {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-paper-plane"></i> Enviar Solicitud';
            showIntranetToast("Error: " + err.message, "error");
        });
    });
}

function initSupportForm() {
    const form = document.getElementById("support-form");
    if (!form || form.dataset.initialized) return;
    form.dataset.initialized = "true";
    form.addEventListener("submit", function(e) {
        e.preventDefault();
        const btn = form.querySelector(".btn-submit");
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Enviando...';
        const data = {
            nombre: document.getElementById("support-nombre").value,
            categoria: document.getElementById("support-categoria").value,
            descripcion: document.getElementById("support-descripcion").value
        };
        submitSupportTicket(data).then(() => {
            form.reset();
            btn.innerHTML = '<i class="fas fa-check"></i> Enviado';
            btn.style.background = "#16a34a";
            setTimeout(() => {
                btn.disabled = false;
                btn.innerHTML = '<i class="fas fa-paper-plane"></i> Enviar Solicitud';
                btn.style.background = "";
            }, 3000);
            showIntranetToast("Ticket de soporte enviado correctamente");
            getSupportTickets().then(items => renderSupportHistory(items));
        }).catch(err => {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-paper-plane"></i> Enviar Solicitud';
            showIntranetToast("Error: " + err.message, "error");
        });
    });
}

function showLoading(containerId) {
    const el = document.getElementById(containerId);
    if (el) el.innerHTML = '<div class="intranet-loading"><i class="fas fa-spinner fa-spin"></i><span>Cargando...</span></div>';
}

function showIntranetError(msg) {
    const toast = document.getElementById("intranet-toast");
    if (!toast) return;
    toast.textContent = msg;
    toast.className = "intranet-toast error";
    toast.style.display = "block";
    setTimeout(() => { toast.style.display = "none"; }, 5000);
}

function showIntranetToast(msg, type) {
    const toast = document.getElementById("intranet-toast");
    if (!toast) return;
    toast.textContent = msg;
    toast.className = "intranet-toast " + (type === "error" ? "error" : "success");
    toast.style.display = "block";
    setTimeout(() => { toast.style.display = "none"; }, 4000);
}

function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str;
    return div.innerHTML;
}
