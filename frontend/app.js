'use strict';

// ══════════════════════════════════════════════
// CONFIG
// ══════════════════════════════════════════════
const API = 'http://localhost:8080';

// ══════════════════════════════════════════════
// NAVIGATION
// ══════════════════════════════════════════════
const sectionMeta = {
  dashboard:   { title: 'Dashboard',    sub: 'Bienvenido al sistema bancario' },
  clientes:    { title: 'Clientes',     sub: 'Registro y consulta de clientes' },
  cuentas:     { title: 'Cuentas',      sub: 'Gestión de productos financieros' },
  operaciones: { title: 'Operaciones',  sub: 'Depósitos, retiros y transferencias' },
  historial:   { title: 'Historial',    sub: 'Movimientos de cuentas' },
};

function showSection(name) {
  document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

  document.getElementById(`section-${name}`).classList.add('active');
  document.querySelector(`[data-section="${name}"]`).classList.add('active');

  const meta = sectionMeta[name];
  document.getElementById('page-title').textContent = meta.title;
  document.getElementById('page-sub').textContent = meta.sub;
}

// ══════════════════════════════════════════════
// TOAST
// ══════════════════════════════════════════════
function toast(title, msg = '', type = 'success') {
  const icons = { success: '✓', error: '✗', info: 'i' };
  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  el.innerHTML = `
    <div class="toast-icon-wrap">${icons[type]}</div>
    <div class="toast-body">
      <div class="toast-title">${title}</div>
      ${msg ? `<div class="toast-msg">${msg}</div>` : ''}
    </div>
  `;
  document.getElementById('toast-container').appendChild(el);
  requestAnimationFrame(() => el.classList.add('show'));
  setTimeout(() => {
    el.classList.add('hide');
    setTimeout(() => el.remove(), 320);
  }, 4500);
}

// ══════════════════════════════════════════════
// BUTTON LOADING STATE
// ══════════════════════════════════════════════
function setLoading(btnId, loading) {
  const btn = document.getElementById(btnId);
  const span = btn.querySelector('span');
  const spinner = btn.querySelector('.btn-spinner');
  btn.disabled = loading;
  span.classList.toggle('hidden', loading);
  spinner.classList.toggle('hidden', !loading);
}

// ══════════════════════════════════════════════
// API WRAPPER
// ══════════════════════════════════════════════
async function apiFetch(method, path, body = null) {
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(`${API}${path}`, opts);
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.mensaje || `Error ${res.status}`);
  return data;
}

// ══════════════════════════════════════════════
// API STATUS
// ══════════════════════════════════════════════
async function checkApi() {
  const dot  = document.getElementById('status-dot');
  const text = document.getElementById('status-text');
  try {
    await fetch(`${API}/api/clientes/1`, { signal: AbortSignal.timeout(3000) });
    dot.className = 'status-dot online';
    text.textContent = 'API Conectada';
  } catch {
    dot.className = 'status-dot offline';
    text.textContent = 'Sin conexión';
  }
}

// ══════════════════════════════════════════════
// DATE
// ══════════════════════════════════════════════
function updateDate() {
  const now = new Date();
  document.getElementById('topbar-date').textContent =
    now.toLocaleDateString('es-CO', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
}

// ══════════════════════════════════════════════
// FORMATTERS
// ══════════════════════════════════════════════
function formatMoney(n) {
  return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0 }).format(n);
}

function formatDate(isoStr) {
  if (!isoStr) return '—';
  return new Date(isoStr).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' });
}

function badgeEstado(estado) {
  const map = { ACTIVA: 'activa', INACTIVA: 'inactiva', CANCELADA: 'cancelada' };
  return `<span class="badge badge-${(map[estado] || 'cancelada')}">${estado}</span>`;
}

function badgeTx(tipo) {
  const map = { DEPOSITO: 'deposito', RETIRO: 'retiro', TRANSFERENCIA: 'transferencia' };
  return `<span class="badge badge-${map[tipo] || 'deposito'}">${tipo}</span>`;
}

// ══════════════════════════════════════════════
// CLIENTES
// ══════════════════════════════════════════════
async function crearCliente(e) {
  e.preventDefault();
  setLoading('btn-crear-cliente', true);
  try {
    const data = await apiFetch('POST', '/api/clientes', {
      tipoIdentificacion: document.getElementById('c-tipoId').value,
      numeroIdentificacion: document.getElementById('c-numId').value.trim(),
      nombres:   document.getElementById('c-nombres').value.trim(),
      apellidos: document.getElementById('c-apellidos').value.trim(),
      email:     document.getElementById('c-email').value.trim(),
      fechaNacimiento: document.getElementById('c-fechaNac').value,
    });
    toast('Cliente registrado', `${data.nombres} ${data.apellidos} — ID: ${data.id}`);
    e.target.reset();
  } catch (err) {
    toast('Error al registrar', err.message, 'error');
  } finally {
    setLoading('btn-crear-cliente', false);
  }
}

async function buscarCliente() {
  const id = document.getElementById('c-buscar-id').value.trim();
  if (!id) { toast('Ingresa un ID', '', 'info'); return; }
  const area = document.getElementById('result-cliente');
  area.innerHTML = '<p style="color:var(--text-3);font-size:13px">Buscando...</p>';
  try {
    const c = await apiFetch('GET', `/api/clientes/${id}`);
    area.innerHTML = renderClienteCard(c);
  } catch (err) {
    area.innerHTML = `<p style="color:var(--danger);font-size:13px;margin-top:4px">⚠ ${err.message}</p>`;
  }
}

function renderClienteCard(c) {
  return `
    <div class="result-cliente-card">
      <div class="result-header">
        <div>
          <div class="result-name">${c.nombres} ${c.apellidos}</div>
          <div class="result-id">ID #${c.id}</div>
        </div>
      </div>
      <div class="result-grid">
        <div class="result-field"><label>Tipo ID</label><span>${c.tipoIdentificacion}</span></div>
        <div class="result-field"><label>Número ID</label><span>${c.numeroIdentificacion}</span></div>
        <div class="result-field"><label>Email</label><span>${c.email}</span></div>
        <div class="result-field"><label>Nacimiento</label><span>${c.fechaNacimiento}</span></div>
        <div class="result-field"><label>Creado</label><span>${formatDate(c.fechaCreacion)}</span></div>
        <div class="result-field"><label>Modificado</label><span>${formatDate(c.fechaModificacion)}</span></div>
      </div>
      <div class="result-actions">
        <button class="btn btn-danger btn-sm" onclick="eliminarCliente(${c.id})">Eliminar cliente</button>
        <button class="btn btn-secondary btn-sm" onclick="verCuentasDeCliente(${c.id})">Ver cuentas</button>
      </div>
    </div>`;
}

async function eliminarCliente(id) {
  if (!confirm(`¿Eliminar el cliente #${id}? Esta acción no se puede deshacer.`)) return;
  try {
    await apiFetch('DELETE', `/api/clientes/${id}`);
    toast('Cliente eliminado', `ID #${id} eliminado correctamente`);
    document.getElementById('result-cliente').innerHTML = '';
    document.getElementById('c-buscar-id').value = '';
  } catch (err) {
    toast('No se pudo eliminar', err.message, 'error');
  }
}

function verCuentasDeCliente(id) {
  showSection('cuentas');
  document.getElementById('cu-buscar-id').value = id;
  listarCuentas();
}

// ══════════════════════════════════════════════
// CUENTAS
// ══════════════════════════════════════════════
async function crearCuenta(e) {
  e.preventDefault();
  setLoading('btn-crear-cuenta', true);
  try {
    const tipoCuenta = document.querySelector('input[name="tipoCuenta"]:checked').value;
    const data = await apiFetch('POST', '/api/productos', {
      clienteId: parseInt(document.getElementById('cu-clienteId').value),
      tipoCuenta,
      exentaGMF: document.getElementById('cu-gmf').checked,
    });
    toast('Cuenta creada', `${data.tipoCuenta} — ${data.numeroCuenta}`);
    e.target.reset();
  } catch (err) {
    toast('Error al crear cuenta', err.message, 'error');
  } finally {
    setLoading('btn-crear-cuenta', false);
  }
}

async function listarCuentas() {
  const id = document.getElementById('cu-buscar-id').value.trim();
  if (!id) { toast('Ingresa el ID del cliente', '', 'info'); return; }
  const area = document.getElementById('result-cuentas');
  area.innerHTML = '<p style="color:var(--text-3);font-size:13px">Cargando...</p>';
  try {
    const list = await apiFetch('GET', `/api/productos/cliente/${id}`);
    if (!list.length) {
      area.innerHTML = `<div class="empty-state"><div class="empty-icon">🏦</div><p>Este cliente no tiene cuentas registradas.</p></div>`;
      return;
    }
    area.innerHTML = list.map(renderCuentaCard).join('');
  } catch (err) {
    area.innerHTML = `<p style="color:var(--danger);font-size:13px;margin-top:4px">⚠ ${err.message}</p>`;
  }
}

function renderCuentaCard(p) {
  const acciones = p.estado !== 'CANCELADA' ? `
    <button class="btn btn-secondary btn-sm" onclick="cambiarEstado(${p.id}, '${p.estado === 'ACTIVA' ? 'INACTIVA' : 'ACTIVA'}')">
      ${p.estado === 'ACTIVA' ? 'Inactivar' : 'Activar'}
    </button>
    ${p.estado === 'ACTIVA' ? `<button class="btn btn-danger btn-sm" onclick="cancelarCuenta(${p.id})">Cancelar</button>` : ''}
  ` : '';
  return `
    <div class="cuenta-card">
      <div class="cuenta-card-top">
        <div class="cuenta-tipo">${p.tipoCuenta}</div>
        ${badgeEstado(p.estado)}
      </div>
      <div class="cuenta-numero">${p.numeroCuenta}</div>
      <div class="cuenta-saldo">${formatMoney(p.saldo)}</div>
      <div class="result-actions">${acciones}
        <button class="btn btn-secondary btn-sm" onclick="irHistorial('${p.numeroCuenta}')">Historial</button>
      </div>
    </div>`;
}

async function cambiarEstado(id, nuevoEstado) {
  try {
    await apiFetch('PATCH', `/api/productos/${id}/estado`, { estado: nuevoEstado });
    toast('Estado actualizado', `Cuenta ${nuevoEstado.toLowerCase()}`);
    const clienteId = document.getElementById('cu-buscar-id').value;
    if (clienteId) listarCuentas();
  } catch (err) {
    toast('Error', err.message, 'error');
  }
}

async function cancelarCuenta(id) {
  if (!confirm('¿Cancelar esta cuenta? Solo es posible si el saldo es $0.')) return;
  try {
    await apiFetch('POST', `/api/productos/${id}/cancelar`);
    toast('Cuenta cancelada', 'El estado se actualizó a CANCELADA');
    const clienteId = document.getElementById('cu-buscar-id').value;
    if (clienteId) listarCuentas();
  } catch (err) {
    toast('No se pudo cancelar', err.message, 'error');
  }
}

function irHistorial(numeroCuenta) {
  showSection('historial');
  document.getElementById('h-cuenta').value = numeroCuenta;
  buscarHistorial();
}

// ══════════════════════════════════════════════
// OPERACIONES - TABS
// ══════════════════════════════════════════════
function showOpTab(tab) {
  document.querySelectorAll('.ops-tab').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.op-panel').forEach(p => p.classList.remove('active'));
  document.getElementById(`tab-${tab}`).classList.add('active');
  document.getElementById(`op-${tab}`).classList.add('active');
}

// ══════════════════════════════════════════════
// DEPÓSITO
// ══════════════════════════════════════════════
async function realizarDeposito(e) {
  e.preventDefault();
  setLoading('btn-deposito', true);
  const area = document.getElementById('result-deposito');
  area.innerHTML = '';
  try {
    const data = await apiFetch('POST', '/api/transacciones/deposito', {
      numeroCuenta: document.getElementById('d-cuenta').value.trim(),
      monto: parseFloat(document.getElementById('d-monto').value),
    });
    toast('Depósito exitoso', formatMoney(data.monto));
    area.innerHTML = renderTxResult(data, '✓ Depósito realizado');
    e.target.reset();
  } catch (err) {
    toast('Error en depósito', err.message, 'error');
  } finally {
    setLoading('btn-deposito', false);
  }
}

// ══════════════════════════════════════════════
// RETIRO
// ══════════════════════════════════════════════
async function realizarRetiro(e) {
  e.preventDefault();
  setLoading('btn-retiro', true);
  const area = document.getElementById('result-retiro');
  area.innerHTML = '';
  try {
    const data = await apiFetch('POST', '/api/transacciones/retiro', {
      numeroCuenta: document.getElementById('r-cuenta').value.trim(),
      monto: parseFloat(document.getElementById('r-monto').value),
    });
    toast('Retiro exitoso', formatMoney(data.monto));
    area.innerHTML = renderTxResult(data, '✓ Retiro realizado');
    e.target.reset();
  } catch (err) {
    toast('Error en retiro', err.message, 'error');
  } finally {
    setLoading('btn-retiro', false);
  }
}

// ══════════════════════════════════════════════
// TRANSFERENCIA
// ══════════════════════════════════════════════
async function realizarTransferencia(e) {
  e.preventDefault();
  setLoading('btn-transferencia', true);
  const area = document.getElementById('result-transferencia');
  area.innerHTML = '';
  try {
    const data = await apiFetch('POST', '/api/transacciones/transferencia', {
      cuentaOrigen:  document.getElementById('t-origen').value.trim(),
      cuentaDestino: document.getElementById('t-destino').value.trim(),
      monto: parseFloat(document.getElementById('t-monto').value),
    });
    toast('Transferencia exitosa', formatMoney(data.monto));
    area.innerHTML = renderTxResult(data, '✓ Transferencia realizada');
    e.target.reset();
  } catch (err) {
    toast('Error en transferencia', err.message, 'error');
  } finally {
    setLoading('btn-transferencia', false);
  }
}

function renderTxResult(t, title) {
  return `
    <div class="tx-result-card" style="margin-top:16px">
      <div class="tx-result-title">${title}</div>
      <div class="tx-result-grid">
        <div><label>Tipo</label><span>${badgeTx(t.tipo)}</span></div>
        <div><label>Monto</label><span>${formatMoney(t.monto)}</span></div>
        <div><label>Cuenta Origen</label><span>${t.numeroCuentaOrigen}</span></div>
        <div><label>Cuenta Destino</label><span>${t.numeroCuentaDestino || '—'}</span></div>
        <div><label>Fecha</label><span>${formatDate(t.fecha)}</span></div>
        <div><label>ID Transacción</label><span>#${t.id}</span></div>
      </div>
    </div>`;
}

// ══════════════════════════════════════════════
// HISTORIAL
// ══════════════════════════════════════════════
async function buscarHistorial() {
  const cuenta = document.getElementById('h-cuenta').value.trim();
  if (!cuenta) { toast('Ingresa un número de cuenta', '', 'info'); return; }
  const area = document.getElementById('result-historial');
  area.innerHTML = '<p style="color:var(--text-3);font-size:13px;padding-top:12px">Cargando movimientos...</p>';
  try {
    const list = await apiFetch('GET', `/api/transacciones/cuenta/${cuenta}`);
    if (!list.length) {
      area.innerHTML = `<div class="empty-state"><div class="empty-icon">📋</div><p>Esta cuenta no tiene movimientos registrados.</p></div>`;
      return;
    }
    area.innerHTML = `
      <table class="history-table">
        <thead>
          <tr>
            <th>#</th>
            <th>Tipo</th>
            <th>Monto</th>
            <th>Origen</th>
            <th>Destino</th>
            <th>Fecha</th>
          </tr>
        </thead>
        <tbody>
          ${list.map(t => {
            const montoClass = t.tipo === 'DEPOSITO' ? 'monto-positive' : t.tipo === 'RETIRO' ? 'monto-negative' : 'monto-neutral';
            const sign = t.tipo === 'DEPOSITO' ? '+' : t.tipo === 'RETIRO' ? '-' : '↔';
            return `
              <tr>
                <td style="color:var(--text-3)">#${t.id}</td>
                <td>${badgeTx(t.tipo)}</td>
                <td class="${montoClass}">${sign} ${formatMoney(t.monto)}</td>
                <td style="font-family:monospace;font-size:12px">${t.numeroCuentaOrigen}</td>
                <td style="font-family:monospace;font-size:12px">${t.numeroCuentaDestino || '—'}</td>
                <td style="color:var(--text-2)">${formatDate(t.fecha)}</td>
              </tr>`;
          }).join('')}
        </tbody>
      </table>`;
  } catch (err) {
    area.innerHTML = `<p style="color:var(--danger);font-size:13px;padding-top:12px">⚠ ${err.message}</p>`;
  }
}

// ══════════════════════════════════════════════
// INIT
// ══════════════════════════════════════════════
document.addEventListener('DOMContentLoaded', () => {
  updateDate();
  setInterval(updateDate, 60_000);
  checkApi();
  setInterval(checkApi, 30_000);

  document.getElementById('form-cliente').addEventListener('submit', crearCliente);
  document.getElementById('form-cuenta').addEventListener('submit', crearCuenta);
  document.getElementById('form-deposito').addEventListener('submit', realizarDeposito);
  document.getElementById('form-retiro').addEventListener('submit', realizarRetiro);
  document.getElementById('form-transferencia').addEventListener('submit', realizarTransferencia);

  document.getElementById('h-cuenta').addEventListener('keydown', e => {
    if (e.key === 'Enter') buscarHistorial();
  });
  document.getElementById('c-buscar-id').addEventListener('keydown', e => {
    if (e.key === 'Enter') buscarCliente();
  });
  document.getElementById('cu-buscar-id').addEventListener('keydown', e => {
    if (e.key === 'Enter') listarCuentas();
  });
});
