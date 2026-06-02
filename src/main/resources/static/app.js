'use strict';

// ══════════════════════════════════════════════
// CONFIG
// ══════════════════════════════════════════════
const API = 'http://localhost:8080';

let _historialData        = [];
let _modalResolve         = null;
let _cuentasClienteId     = null;
let _cuentasCliente       = null;
let _crearCuentaClienteId = null;
let _clienteActual        = null;

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
// MODAL DE CONFIRMACIÓN
// ══════════════════════════════════════════════
function confirmModal(title, msg, confirmLabel = 'Confirmar') {
  return new Promise(resolve => {
    _modalResolve = resolve;
    document.getElementById('modal-title').textContent = title;
    document.getElementById('modal-msg').textContent = msg;
    document.getElementById('modal-btn-confirm').textContent = confirmLabel;
    document.getElementById('modal-overlay').classList.remove('hidden');
  });
}

function _modalClose(val) {
  document.getElementById('modal-overlay').classList.add('hidden');
  if (_modalResolve) { _modalResolve(val); _modalResolve = null; }
}

// ══════════════════════════════════════════════
// COPY TO CLIPBOARD
// ══════════════════════════════════════════════
function copyText(text, el) {
  const exec = () => {
    el.classList.add('copied');
    const icon = el.querySelector('.copy-icon');
    if (icon) icon.textContent = '✓';
    setTimeout(() => {
      el.classList.remove('copied');
      if (icon) icon.textContent = '⎘';
    }, 1500);
  };
  if (navigator.clipboard) {
    navigator.clipboard.writeText(text).then(exec).catch(exec);
  } else {
    const ta = Object.assign(document.createElement('textarea'), {
      value: text, style: 'position:fixed;opacity:0',
    });
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    ta.remove();
    exec();
  }
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
    await fetch(`${API}/`, { method: 'HEAD', signal: AbortSignal.timeout(3000) });
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
  const numId = document.getElementById('c-buscar-id').value.trim();
  if (!numId) { toast('Ingresa el número de identificación', '', 'info'); return; }
  const area = document.getElementById('result-cliente');
  area.innerHTML = '<p style="color:var(--text-3);font-size:13px">Buscando...</p>';
  try {
    const c = await apiFetch('GET', `/api/clientes/identificacion/${encodeURIComponent(numId)}`);
    const cuentas = await apiFetch('GET', `/api/productos/cliente/${c.id}`).catch(() => []);
    _clienteActual = c;
    area.innerHTML = renderClienteCard(c, cuentas);
  } catch (err) {
    area.innerHTML = `<p style="color:var(--danger);font-size:13px;margin-top:4px">⚠ ${err.message}</p>`;
  }
}

function renderClienteCard(c, cuentas = []) {
  const activas = cuentas.filter(cu => cu.estado === 'ACTIVA').length;
  const cuentasBlock = cuentas.length === 0
    ? `<div class="client-accounts-block sin-cuentas">
        <div class="client-accounts-info">
          <span class="client-accounts-icon">🏦</span>
          <div>
            <div class="client-accounts-label">Sin cuentas registradas</div>
            <div class="client-accounts-sub">Este cliente aún no tiene productos financieros</div>
          </div>
        </div>
        <button class="btn btn-primary btn-sm" onclick="irCrearCuenta(${c.id})">+ Crear cuenta</button>
      </div>`
    : `<div class="client-accounts-block con-cuentas">
        <div class="client-accounts-info">
          <span class="client-accounts-icon">💳</span>
          <div>
            <div class="client-accounts-label">${cuentas.length} cuenta${cuentas.length !== 1 ? 's' : ''} · ${activas} activa${activas !== 1 ? 's' : ''}</div>
            <div class="client-accounts-sub">${cuentas.map(cu => cu.numeroCuenta).join(' · ')}</div>
          </div>
        </div>
        <button class="btn btn-secondary btn-sm" onclick="verCuentasDeCliente(${c.id})">Ver cuentas</button>
      </div>`;

  return `
    <div class="result-cliente-card">
      <div class="result-header">
        <div>
          <div class="result-name">${c.nombres} ${c.apellidos}</div>
          <div class="result-id">${c.tipoIdentificacion} ${c.numeroIdentificacion}</div>
        </div>
      </div>
      <div class="result-grid">
        <div class="result-field"><label>Email</label><span>${c.email}</span></div>
        <div class="result-field"><label>Nacimiento</label><span>${c.fechaNacimiento}</span></div>
        <div class="result-field"><label>Creado</label><span>${formatDate(c.fechaCreacion)}</span></div>
        <div class="result-field"><label>Modificado</label><span>${formatDate(c.fechaModificacion)}</span></div>
      </div>
      ${cuentasBlock}
      <div class="result-actions" style="margin-top:10px">
        <button class="btn btn-secondary btn-sm" onclick="editarCliente(${c.id})">Editar</button>
        <button class="btn btn-danger btn-sm" onclick="eliminarCliente(${c.id})">Eliminar cliente</button>
      </div>
    </div>`;
}

async function irCrearCuenta(clienteId) {
  showSection('cuentas');
  _crearCuentaClienteId = clienteId;
  const c = await apiFetch('GET', `/api/clientes/${clienteId}`).catch(() => null);
  if (c) {
    document.getElementById('cu-clienteNumId').value = c.numeroIdentificacion;
    const confirm = document.getElementById('cu-cliente-confirm');
    confirm.className = 'cu-cliente-confirm found';
    confirm.innerHTML = `
      <span class="cu-confirm-check">✓</span>
      <span class="cu-confirm-name">${c.nombres} ${c.apellidos}</span>
      <span class="cu-confirm-tipo">${c.tipoIdentificacion} ${c.numeroIdentificacion}</span>`;
  }
}

async function eliminarCliente(id) {
  const ok = await confirmModal(`¿Eliminar cliente #${id}?`, 'Esta acción es permanente y no se puede deshacer.', 'Eliminar');
  if (!ok) return;
  try {
    await apiFetch('DELETE', `/api/clientes/${id}`);
    toast('Cliente eliminado', `ID #${id} eliminado correctamente`);
    document.getElementById('result-cliente').innerHTML = '';
    document.getElementById('c-buscar-id').value = '';
  } catch (err) {
    toast('No se pudo eliminar', err.message, 'error');
  }
}

function editarCliente(id) {
  const c = _clienteActual;
  const opts = (val) => ['CC','CE','NIT','PP'].map(v =>
    `<option value="${v}"${c.tipoIdentificacion === v ? ' selected' : ''}>${
      {CC:'Cédula de Ciudadanía', CE:'Cédula de Extranjería', NIT:'NIT', PP:'Pasaporte'}[v]
    }</option>`
  ).join('');
  document.getElementById('result-cliente').innerHTML = `
    <div class="result-cliente-card">
      <div class="result-header">
        <div>
          <div class="result-name">Editar Cliente</div>
          <div class="result-id">ID #${c.id}</div>
        </div>
      </div>
      <form id="form-editar-cliente" autocomplete="off">
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">Tipo de Identificación</label>
            <select class="form-input" id="e-tipoId" required>${opts()}</select>
          </div>
          <div class="form-group">
            <label class="form-label">Número de Identificación</label>
            <input type="text" class="form-input" id="e-numId" value="${c.numeroIdentificacion}" required />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">Nombres</label>
            <input type="text" class="form-input" id="e-nombres" value="${c.nombres}" required minlength="2" />
          </div>
          <div class="form-group">
            <label class="form-label">Apellidos</label>
            <input type="text" class="form-input" id="e-apellidos" value="${c.apellidos}" required minlength="2" />
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">Correo Electrónico</label>
          <input type="email" class="form-input" id="e-email" value="${c.email}" required />
        </div>
        <div class="form-group">
          <label class="form-label">Fecha de Nacimiento</label>
          <input type="date" class="form-input" id="e-fechaNac" value="${c.fechaNacimiento}" required />
        </div>
        <div class="result-actions" style="margin-top:10px">
          <button type="button" class="btn btn-secondary btn-sm" onclick="buscarCliente()">Cancelar</button>
          <button type="submit" class="btn btn-primary" id="btn-actualizar-cliente">
            <span>Guardar Cambios</span>
            <div class="btn-spinner hidden"></div>
          </button>
        </div>
      </form>
    </div>`;
  document.getElementById('form-editar-cliente').addEventListener('submit', (e) => actualizarCliente(id, e));
}

async function actualizarCliente(id, e) {
  e.preventDefault();
  const btn = document.getElementById('btn-actualizar-cliente');
  const span = btn.querySelector('span');
  const spinner = btn.querySelector('.btn-spinner');
  btn.disabled = true; span.classList.add('hidden'); spinner.classList.remove('hidden');
  try {
    const data = await apiFetch('PUT', `/api/clientes/${id}`, {
      tipoIdentificacion:  document.getElementById('e-tipoId').value,
      numeroIdentificacion: document.getElementById('e-numId').value.trim(),
      nombres:             document.getElementById('e-nombres').value.trim(),
      apellidos:           document.getElementById('e-apellidos').value.trim(),
      email:               document.getElementById('e-email').value.trim(),
      fechaNacimiento:     document.getElementById('e-fechaNac').value,
    });
    _clienteActual = data;
    toast('Cliente actualizado', `${data.nombres} ${data.apellidos}`);
    const cuentas = await apiFetch('GET', `/api/productos/cliente/${data.id}`).catch(() => []);
    document.getElementById('result-cliente').innerHTML = renderClienteCard(data, cuentas);
  } catch (err) {
    toast('Error al actualizar', err.message, 'error');
    btn.disabled = false; span.classList.remove('hidden'); spinner.classList.add('hidden');
  }
}

async function verCuentasDeCliente(id) {
  showSection('cuentas');
  _cuentasClienteId = id;
  const area = document.getElementById('result-cuentas');
  area.innerHTML = '<p style="color:var(--text-3);font-size:13px">Cargando...</p>';
  _cuentasCliente = await apiFetch('GET', `/api/clientes/${id}`).catch(() => null);
  if (_cuentasCliente) {
    document.getElementById('cu-buscar-id').value = _cuentasCliente.numeroIdentificacion;
  }
  await _refreshCuentas(area);
}

// ══════════════════════════════════════════════
// CUENTAS
// ══════════════════════════════════════════════
async function buscarClienteParaCuenta() {
  const numId = document.getElementById('cu-clienteNumId').value.trim();
  if (!numId) { toast('Ingresa el número de identificación', '', 'info'); return; }
  const confirm = document.getElementById('cu-cliente-confirm');
  confirm.className = 'cu-cliente-confirm';
  confirm.innerHTML = '<span style="color:var(--text-3);font-size:12px">Buscando...</span>';
  try {
    const c = await apiFetch('GET', `/api/clientes/identificacion/${encodeURIComponent(numId)}`);
    _crearCuentaClienteId = c.id;
    confirm.className = 'cu-cliente-confirm found';
    confirm.innerHTML = `
      <span class="cu-confirm-check">✓</span>
      <span class="cu-confirm-name">${c.nombres} ${c.apellidos}</span>
      <span class="cu-confirm-tipo">${c.tipoIdentificacion} ${c.numeroIdentificacion}</span>`;
  } catch (err) {
    _crearCuentaClienteId = null;
    confirm.className = 'cu-cliente-confirm not-found';
    confirm.innerHTML = `<span>⚠ ${err.message}</span>`;
  }
}

async function crearCuenta(e) {
  e.preventDefault();
  if (!_crearCuentaClienteId) {
    toast('Primero busca y confirma el cliente', '', 'info');
    return;
  }
  setLoading('btn-crear-cuenta', true);
  try {
    const tipoCuenta = document.querySelector('input[name="tipoCuenta"]:checked').value;
    const data = await apiFetch('POST', '/api/productos', {
      clienteId: _crearCuentaClienteId,
      tipoCuenta,
      exentaGMF: document.getElementById('cu-gmf').checked,
    });
    toast('Cuenta creada', `${data.tipoCuenta} — ${data.numeroCuenta}`);
    e.target.reset();
    _crearCuentaClienteId = null;
    document.getElementById('cu-cliente-confirm').className = 'cu-cliente-confirm hidden';
  } catch (err) {
    toast('Error al crear cuenta', err.message, 'error');
  } finally {
    setLoading('btn-crear-cuenta', false);
  }
}

async function listarCuentas() {
  const numId = document.getElementById('cu-buscar-id').value.trim();
  if (!numId) { toast('Ingresa el número de identificación', '', 'info'); return; }
  const area = document.getElementById('result-cuentas');
  area.innerHTML = '<p style="color:var(--text-3);font-size:13px">Buscando...</p>';
  try {
    const c = await apiFetch('GET', `/api/clientes/identificacion/${encodeURIComponent(numId)}`);
    _cuentasClienteId = c.id;
    _cuentasCliente   = c;
    await _refreshCuentas(area);
  } catch (err) {
    area.innerHTML = `<p style="color:var(--danger);font-size:13px;margin-top:4px">⚠ ${err.message}</p>`;
  }
}

async function _refreshCuentas(area = document.getElementById('result-cuentas')) {
  if (!_cuentasClienteId) return;
  try {
    const list = await apiFetch('GET', `/api/productos/cliente/${_cuentasClienteId}`);
    const header = _cuentasCliente ? `
      <div class="cuentas-cliente-header">
        <div class="cuentas-cliente-nombre">${_cuentasCliente.nombres} ${_cuentasCliente.apellidos}</div>
        <div class="cuentas-cliente-id">${_cuentasCliente.tipoIdentificacion} ${_cuentasCliente.numeroIdentificacion}</div>
      </div>` : '';
    if (!list.length) {
      area.innerHTML = header + `<div class="empty-state"><div class="empty-icon">🏦</div><p>Este cliente no tiene cuentas registradas.</p></div>`;
      return;
    }
    area.innerHTML = header + list.map(renderCuentaCard).join('');
  } catch (err) {
    area.innerHTML = `<p style="color:var(--danger);font-size:13px;margin-top:4px">⚠ ${err.message}</p>`;
  }
}

function renderCuentaCard(p) {
  const esActiva = p.estado === 'ACTIVA';
  const acciones = p.estado !== 'CANCELADA' ? `
    <button class="btn btn-secondary btn-sm" onclick="cambiarEstado(${p.id}, '${esActiva ? 'INACTIVA' : 'ACTIVA'}')">
      ${esActiva ? 'Inactivar' : 'Activar'}
    </button>
    ${esActiva ? `<button class="btn btn-danger btn-sm" onclick="cancelarCuenta(${p.id})">Cancelar</button>` : ''}
  ` : '';
  const accionesRapidas = esActiva ? `
    <div class="cuenta-quick-actions">
      <button class="btn-quick btn-quick-green" onclick="irOperacion('deposito', '${p.numeroCuenta}')">+ Depositar</button>
      <button class="btn-quick btn-quick-red"   onclick="irOperacion('retiro',   '${p.numeroCuenta}')">− Retirar</button>
      <button class="btn-quick btn-quick-blue"  onclick="irOperacion('transferencia', '${p.numeroCuenta}')">↔ Transferir</button>
    </div>
  ` : '';
  return `
    <div class="cuenta-card">
      <div class="cuenta-card-top">
        <div class="cuenta-tipo">${p.tipoCuenta}</div>
        ${badgeEstado(p.estado)}
      </div>
      <div class="cuenta-numero" onclick="copyText('${p.numeroCuenta}', this)" title="Clic para copiar">
        ${p.numeroCuenta}<em class="copy-icon">⎘</em>
      </div>
      <div class="cuenta-saldo">${formatMoney(p.saldo)}</div>
      ${accionesRapidas}
      <div class="result-actions">${acciones}
        <button class="btn btn-secondary btn-sm" onclick="irHistorial('${p.numeroCuenta}')">Historial</button>
      </div>
    </div>`;
}

async function cambiarEstado(id, nuevoEstado) {
  try {
    await apiFetch('PATCH', `/api/productos/${id}/estado`, { estado: nuevoEstado });
    toast('Estado actualizado', `Cuenta ${nuevoEstado.toLowerCase()}`);
    _refreshCuentas();
  } catch (err) {
    toast('Error', err.message, 'error');
  }
}

async function cancelarCuenta(id) {
  const ok = await confirmModal('¿Cancelar esta cuenta?', 'Solo es posible si el saldo es $0. El estado cambiará a CANCELADA de forma permanente.', 'Cancelar cuenta');
  if (!ok) return;
  try {
    await apiFetch('POST', `/api/productos/${id}/cancelar`);
    toast('Cuenta cancelada', 'El estado se actualizó a CANCELADA');
    _refreshCuentas();
  } catch (err) {
    toast('No se pudo cancelar', err.message, 'error');
  }
}

function irHistorial(numeroCuenta) {
  showSection('historial');
  document.getElementById('h-cuenta').value = numeroCuenta;
  buscarHistorial();
}

function irOperacion(tipo, numeroCuenta) {
  showSection('operaciones');
  showOpTab(tipo);
  const campos = { deposito: 'd-cuenta', retiro: 'r-cuenta', transferencia: 't-origen' };
  const campo = document.getElementById(campos[tipo]);
  campo.value = numeroCuenta;
  campo.focus();
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
    _historialData = list;
    if (!list.length) {
      area.innerHTML = `<div class="empty-state"><div class="empty-icon">📋</div><p>Esta cuenta no tiene movimientos registrados.</p></div>`;
      return;
    }
    renderHistorialResult('TODOS');
  } catch (err) {
    area.innerHTML = `<p style="color:var(--danger);font-size:13px;padding-top:12px">⚠ ${err.message}</p>`;
  }
}

function renderHistorialResult(filter) {
  const area = document.getElementById('result-historial');
  const tipos = ['TODOS', 'DEPOSITO', 'RETIRO', 'TRANSFERENCIA'];
  const labels = { TODOS: 'Todos', DEPOSITO: 'Depósitos', RETIRO: 'Retiros', TRANSFERENCIA: 'Transferencias' };
  const filtered = filter === 'TODOS' ? _historialData : _historialData.filter(t => t.tipo === filter);
  const counts = {};
  tipos.forEach(tp => { counts[tp] = tp === 'TODOS' ? _historialData.length : _historialData.filter(x => x.tipo === tp).length; });

  const filterBar = `<div class="tx-filter-bar">
    ${tipos.map(tp => `
      <button class="tx-filter-btn${filter === tp ? ' active' : ''}" onclick="renderHistorialResult('${tp}')">
        ${labels[tp]} <span class="tx-filter-count">${counts[tp]}</span>
      </button>`).join('')}
  </div>`;

  const tableRows = filtered.map(t => {
    const montoClass = t.tipo === 'DEPOSITO' ? 'monto-positive' : t.tipo === 'RETIRO' ? 'monto-negative' : 'monto-neutral';
    const sign = t.tipo === 'DEPOSITO' ? '+' : t.tipo === 'RETIRO' ? '-' : '↔';
    return `<tr>
      <td style="color:var(--text-3)">#${t.id}</td>
      <td>${badgeTx(t.tipo)}</td>
      <td class="${montoClass}">${sign} ${formatMoney(t.monto)}</td>
      <td style="font-family:monospace;font-size:12px">${t.numeroCuentaOrigen}</td>
      <td style="font-family:monospace;font-size:12px">${t.numeroCuentaDestino || '—'}</td>
      <td style="color:var(--text-2)">${formatDate(t.fecha)}</td>
    </tr>`;
  }).join('');

  const tableHtml = filtered.length === 0
    ? `<div class="empty-state"><div class="empty-icon">📋</div><p>No hay movimientos de tipo "${labels[filter]}".</p></div>`
    : `<table class="history-table">
        <thead><tr><th>#</th><th>Tipo</th><th>Monto</th><th>Origen</th><th>Destino</th><th>Fecha</th></tr></thead>
        <tbody>${tableRows}</tbody>
      </table>`;

  area.innerHTML = filterBar + tableHtml;
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

  document.getElementById('cu-clienteNumId').addEventListener('keydown', e => {
    if (e.key === 'Enter') { e.preventDefault(); buscarClienteParaCuenta(); }
  });
  document.getElementById('cu-clienteNumId').addEventListener('input', () => {
    _crearCuentaClienteId = null;
    document.getElementById('cu-cliente-confirm').className = 'cu-cliente-confirm hidden';
  });

  // Modal buttons
  document.getElementById('modal-btn-confirm').addEventListener('click', () => _modalClose(true));
  document.getElementById('modal-btn-cancel').addEventListener('click', () => _modalClose(false));
  document.getElementById('modal-overlay').addEventListener('click', e => {
    if (e.target === e.currentTarget) _modalClose(false);
  });

  // Monto preview en tiempo real
  ['d', 'r', 't'].forEach(prefix => {
    const input = document.getElementById(`${prefix}-monto`);
    const preview = document.createElement('p');
    preview.className = 'monto-preview hidden';
    input.closest('.form-group').appendChild(preview);
    input.addEventListener('input', () => {
      const val = parseFloat(input.value);
      if (val > 0) { preview.textContent = formatMoney(val); preview.classList.remove('hidden'); }
      else { preview.classList.add('hidden'); }
    });
  });

  // Validación de edad mínima (18 años)
  const fechaNacInput = document.getElementById('c-fechaNac');
  const ageWarn = document.createElement('p');
  ageWarn.className = 'age-warn hidden';
  fechaNacInput.closest('.form-group').appendChild(ageWarn);
  fechaNacInput.addEventListener('change', () => {
    const birth = new Date(fechaNacInput.value);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const m = today.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
    if (age < 18) {
      ageWarn.textContent = `⚠ El cliente tiene ${age} año${age !== 1 ? 's' : ''}. Se requieren al menos 18 años.`;
      ageWarn.classList.remove('hidden');
    } else {
      ageWarn.classList.add('hidden');
    }
  });
});
