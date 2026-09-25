/* ═══════════════════════════════════════════════════════════════════
   Zuany WMS — Front-end Application Logic
   Handles navigation, tab switching, API calls, and data rendering.
   ═══════════════════════════════════════════════════════════════════ */

const API = '';  // Same origin — no prefix needed

// ── Navigation ─────────────────────────────────────────────────────

function navigateTo(section) {
    // Deactivate everything
    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));

    // Activate target
    const sec = document.getElementById('section-' + section);
    const link = document.querySelector(`.nav-link[data-section="${section}"]`);
    if (sec) sec.classList.add('active');
    if (link) link.classList.add('active');

    // Auto-load data when entering a module
    if (section === 'productos')   loadProductos();
    if (section === 'ubicaciones') loadUbicaciones();
    if (section === 'movimientos') { loadEmpleados('rec-operador'); loadEmpleados('cnt-operador'); }
    if (section === 'reportes')    loadReporteStock();
}

// Wire up nav links
document.querySelectorAll('.nav-link').forEach(link => {
    link.addEventListener('click', e => {
        e.preventDefault();
        navigateTo(link.dataset.section);
    });
});

// ── Tab switching ──────────────────────────────────────────────────

document.querySelectorAll('.tab-bar').forEach(bar => {
    bar.querySelectorAll('.tab').forEach(tab => {
        tab.addEventListener('click', () => {
            // Deactivate siblings
            bar.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');

            // Find parent section
            const section = bar.closest('.section');
            section.querySelectorAll('.tab-content').forEach(tc => tc.classList.remove('active'));
            const target = section.querySelector('#' + tab.dataset.tab);
            if (target) target.classList.add('active');

            // Auto-load data for specific tabs
            const tabId = tab.dataset.tab;
            if (tabId === 'productos-list')       loadProductos();
            if (tabId === 'productos-add')        loadCategorias();
            if (tabId === 'ubicaciones-list')      loadUbicaciones();
            if (tabId === 'movimientos-historial') loadMovimientos();
            if (tabId === 'reportes-stock')        loadReporteStock();
            if (tabId === 'reportes-ocupacion')    loadOcupacion();
            if (tabId === 'reportes-auditoria')    loadAuditoria();
        });
    });
});

// ── Toast notifications ────────────────────────────────────────────

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => {
        toast.classList.add('fade-out');
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

// ── Generic fetch helper ───────────────────────────────────────────

async function apiFetch(url, options = {}) {
    try {
        const res = await fetch(API + url, {
            headers: { 'Content-Type': 'application/json' },
            ...options
        });
        const data = await res.json();
        return data;
    } catch (err) {
        showToast('Error de conexión: ' + err.message, 'error');
        return null;
    }
}

// ═══════════════════════════════════════════════════════════════════
//  PRODUCTOS
// ═══════════════════════════════════════════════════════════════════

async function loadProductos() {
    const tbody = document.querySelector('#table-productos tbody');
    tbody.innerHTML = '<tr class="loading-row"><td colspan="3">Cargando...</td></tr>';
    const data = await apiFetch('/api/productos');
    if (!data) return;
    if (data.length === 0) {
        tbody.innerHTML = '<tr class="loading-row"><td colspan="3">Sin productos registrados</td></tr>';
        return;
    }
    tbody.innerHTML = data.map(p =>
        `<tr><td>${p.id}</td><td>${esc(p.sku)}</td><td>${esc(p.descripcion)}</td></tr>`
    ).join('');
}

async function loadCategorias() {
    const select = document.getElementById('add-prod-cat');
    const data = await apiFetch('/api/categorias');
    if (!data) return;
    select.innerHTML = data.map(c =>
        `<option value="${c.id}">${c.id} — ${esc(c.nombre)}</option>`
    ).join('');
}

async function addProducto(e) {
    e.preventDefault();
    const body = {
        sku: document.getElementById('add-prod-sku').value,
        descripcion: document.getElementById('add-prod-desc').value,
        sku_fabricante: document.getElementById('add-prod-skufab').value,
        id_categoria: document.getElementById('add-prod-cat').value
    };
    const res = await apiFetch('/api/productos', {
        method: 'POST',
        body: JSON.stringify(body)
    });
    if (res && res.ok) {
        showToast(res.message, 'success');
        document.getElementById('form-add-producto').reset();
    } else if (res) {
        showToast(res.message || res.error, 'error');
    }
    return false;
}

async function editProducto(e) {
    e.preventDefault();
    const body = {
        sku: document.getElementById('edit-prod-sku').value,
        descripcion: document.getElementById('edit-prod-desc').value
    };
    const res = await apiFetch('/api/productos', {
        method: 'PUT',
        body: JSON.stringify(body)
    });
    if (res && res.ok) {
        showToast(res.message, 'success');
        document.getElementById('form-edit-producto').reset();
    } else if (res) {
        showToast(res.message || res.error, 'error');
    }
    return false;
}

async function deleteProducto(e) {
    e.preventDefault();
    const sku = document.getElementById('del-prod-sku').value.toUpperCase();
    if (!confirm(`¿Estás seguro de eliminar el producto ${sku}?`)) return false;
    const res = await apiFetch(`/api/productos?sku=${encodeURIComponent(sku)}`, {
        method: 'DELETE'
    });
    if (res && res.ok) {
        showToast(res.message, 'success');
        document.getElementById('form-delete-producto').reset();
    } else if (res) {
        showToast(res.message || res.error, 'error');
    }
    return false;
}

// ═══════════════════════════════════════════════════════════════════
//  UBICACIONES
// ═══════════════════════════════════════════════════════════════════

async function loadUbicaciones() {
    const tbody = document.querySelector('#table-ubicaciones tbody');
    tbody.innerHTML = '<tr class="loading-row"><td colspan="9">Cargando...</td></tr>';
    const data = await apiFetch('/api/ubicaciones');
    if (!data) return;
    if (data.length === 0) {
        tbody.innerHTML = '<tr class="loading-row"><td colspan="9">Sin ubicaciones registradas</td></tr>';
        return;
    }
    tbody.innerHTML = data.map(u =>
        `<tr>
            <td>${u.id}</td><td>${esc(u.codigo)}</td><td>${esc(u.estrato)}</td>
            <td>${esc(u.pasillo)}</td><td>${esc(u.bahia)}</td><td>${esc(u.lado)}</td>
            <td>${esc(u.tipo_almacenaje)}</td><td>${esc(u.nivel)}</td><td>${esc(u.consecutivo)}</td>
        </tr>`
    ).join('');
}

async function addUbicacion(e) {
    e.preventDefault();
    const body = {
        estrato: document.getElementById('add-ubi-estrato').value,
        pasillo: document.getElementById('add-ubi-pasillo').value,
        bahia: document.getElementById('add-ubi-bahia').value,
        lado: document.getElementById('add-ubi-lado').value,
        tipo_almacenaje: document.getElementById('add-ubi-tipo').value,
        nivel: document.getElementById('add-ubi-nivel').value,
        consecutivo: document.getElementById('add-ubi-consec').value
    };
    const res = await apiFetch('/api/ubicaciones', {
        method: 'POST',
        body: JSON.stringify(body)
    });
    if (res) {
        showToast(res.message, res.ok ? 'success' : 'error');
        if (res.ok) document.getElementById('form-add-ubicacion').reset();
    }
    return false;
}

async function editUbicacion(e) {
    e.preventDefault();
    const body = {
        codigo: document.getElementById('edit-ubi-codigo').value,
        tipo_almacenaje: document.getElementById('edit-ubi-tipo').value
    };
    const res = await apiFetch('/api/ubicaciones', {
        method: 'PUT',
        body: JSON.stringify(body)
    });
    if (res && res.ok) {
        showToast(res.message, 'success');
        document.getElementById('form-edit-ubicacion').reset();
    } else if (res) {
        showToast(res.message || res.error, 'error');
    }
    return false;
}

async function deleteUbicacion(e) {
    e.preventDefault();
    const codigo = document.getElementById('del-ubi-codigo').value.toUpperCase();
    if (!confirm(`¿Estás seguro de eliminar la ubicación ${codigo}?`)) return false;
    const res = await apiFetch(`/api/ubicaciones?codigo=${encodeURIComponent(codigo)}`, {
        method: 'DELETE'
    });
    if (res && res.ok) {
        showToast(res.message, 'success');
        document.getElementById('form-delete-ubicacion').reset();
    } else if (res) {
        showToast(res.message || res.error, 'error');
    }
    return false;
}

// ═══════════════════════════════════════════════════════════════════
//  EMPLEADOS (load into selects)
// ═══════════════════════════════════════════════════════════════════

async function loadEmpleados(selectId) {
    const select = document.getElementById(selectId);
    if (!select) return;
    const data = await apiFetch('/api/empleados');
    if (!data) return;
    select.innerHTML = data.map(e =>
        `<option value="${e.id}">${e.id} — ${esc(e.nombres)} ${esc(e.apellido_paterno)} ${esc(e.apellido_materno)}</option>`
    ).join('');
}

// ═══════════════════════════════════════════════════════════════════
//  MOVIMIENTOS
// ═══════════════════════════════════════════════════════════════════

async function registrarRecepcion(e) {
    e.preventDefault();
    const body = {
        operador: document.getElementById('rec-operador').value,
        sku: document.getElementById('rec-sku').value,
        ubicacion: document.getElementById('rec-ubicacion').value,
        cantidad: document.getElementById('rec-cantidad').value,
        condicion: document.getElementById('rec-condicion').value
    };
    const res = await apiFetch('/api/movimientos/recepcion', {
        method: 'POST',
        body: JSON.stringify(body)
    });
    if (res && res.ok) {
        showToast(res.message, 'success');
        document.getElementById('form-recepcion').reset();
        document.getElementById('rec-condicion').value = 'A';
    } else if (res) {
        showToast(res.message || res.error, 'error');
    }
    return false;
}

async function registrarConteo(e) {
    e.preventDefault();
    const body = {
        operador: document.getElementById('cnt-operador').value,
        sku: document.getElementById('cnt-sku').value,
        ubicacion: document.getElementById('cnt-ubicacion').value,
        cantidad_fisica: document.getElementById('cnt-cantidad').value
    };
    const res = await apiFetch('/api/movimientos/conteo', {
        method: 'POST',
        body: JSON.stringify(body)
    });
    if (res && res.ok) {
        showToast(res.message, 'success');
        document.getElementById('form-conteo').reset();
    } else if (res) {
        showToast(res.message || res.error, 'error');
    }
    return false;
}

async function loadMovimientos() {
    const tbody = document.querySelector('#table-movimientos tbody');
    tbody.innerHTML = '<tr class="loading-row"><td colspan="6">Cargando...</td></tr>';
    const data = await apiFetch('/api/movimientos');
    if (!data) return;
    if (data.length === 0) {
        tbody.innerHTML = '<tr class="loading-row"><td colspan="6">Sin movimientos registrados</td></tr>';
        return;
    }
    tbody.innerHTML = data.map(m => {
        const badge = m.accion === 'AJUSTE'
            ? '<span style="color:var(--amber);">⚠️ AJUSTE</span>'
            : m.accion;
        return `<tr>
            <td>${m.id}</td><td>${m.id_usuario}</td><td>${m.id_inventario}</td>
            <td>${badge}</td><td>${m.cantidad}</td><td>${esc(m.fecha)}</td>
        </tr>`;
    }).join('');
}

// ═══════════════════════════════════════════════════════════════════
//  REPORTES
// ═══════════════════════════════════════════════════════════════════

async function loadReporteStock() {
    const tbody = document.querySelector('#table-stock tbody');
    tbody.innerHTML = '<tr class="loading-row"><td colspan="4">Cargando...</td></tr>';
    const data = await apiFetch('/api/reportes/stock');
    if (!data) return;
    if (data.error) { tbody.innerHTML = `<tr class="loading-row"><td colspan="4">${esc(data.error)}</td></tr>`; return; }
    if (data.length === 0) {
        tbody.innerHTML = '<tr class="loading-row"><td colspan="4">Sin datos de stock</td></tr>';
        return;
    }
    tbody.innerHTML = data.map(r =>
        `<tr><td>${r.id}</td><td>${esc(r.sku)}</td><td>${esc(r.descripcion)}</td><td style="font-weight:600;color:var(--accent);">${r.stock}</td></tr>`
    ).join('');
}

async function loadOcupacion() {
    const tbody = document.querySelector('#table-ocupacion tbody');
    tbody.innerHTML = '<tr class="loading-row"><td colspan="3">Cargando...</td></tr>';
    const data = await apiFetch('/api/reportes/ocupacion');
    if (!data) return;
    if (data.error) { tbody.innerHTML = `<tr class="loading-row"><td colspan="3">${esc(data.error)}</td></tr>`; return; }
    if (data.length === 0) {
        tbody.innerHTML = '<tr class="loading-row"><td colspan="3">Sin datos de ocupación</td></tr>';
        return;
    }
    tbody.innerHTML = data.map(r =>
        `<tr><td>${esc(r.estrato)}</td><td>${r.ubicaciones}</td><td>${r.piezas}</td></tr>`
    ).join('');
}

async function loadAuditoria() {
    const tbody = document.querySelector('#table-auditoria tbody');
    tbody.innerHTML = '<tr class="loading-row"><td colspan="3">Cargando...</td></tr>';
    const data = await apiFetch('/api/reportes/auditoria');
    if (!data) return;
    if (data.error) { tbody.innerHTML = `<tr class="loading-row"><td colspan="3">${esc(data.error)}</td></tr>`; return; }
    if (data.length === 0) {
        tbody.innerHTML = '<tr class="loading-row"><td colspan="3">Sin datos de auditoría</td></tr>';
        return;
    }
    tbody.innerHTML = data.map(r =>
        `<tr><td>${esc(r.evento)}</td><td>${esc(r.accion)}</td><td>${r.cantidad}</td></tr>`
    ).join('');
}

// ── Utility ────────────────────────────────────────────────────────

function esc(str) {
    if (str == null) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// ── Connection check on load ───────────────────────────────────────

async function checkConnection() {
    try {
        const res = await fetch(API + '/api/productos', { method: 'GET' });
        if (res.ok) {
            document.getElementById('nav-status').querySelector('.status-text').textContent = 'Conectado';
            document.getElementById('nav-status').querySelector('.status-dot').style.background = 'var(--accent)';
        }
    } catch {
        document.getElementById('nav-status').querySelector('.status-text').textContent = 'Sin conexión';
        document.getElementById('nav-status').querySelector('.status-dot').style.background = 'var(--red)';
    }
}

// Run on load
checkConnection();
