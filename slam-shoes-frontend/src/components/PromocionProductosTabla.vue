<template>
  <!-- Tabla de productos para aplicar una promoción por periodo: ofertas (nombre + % + fechas) o novedades (fechas) -->
  <div>
    <div class="d-flex flex-wrap gap-2 mb-3">
      <input type="text" class="form-control campo" style="max-width: 380px;" placeholder="Buscar por ID o nombre..." v-model="busqueda" />
      <select class="form-select campo" style="max-width: 230px;" v-model="filtro">
        <option value="todos">Todos los productos</option>
        <option value="con">{{ esOferta ? 'Con oferta' : 'Con novedad' }}</option>
        <option value="sin">{{ esOferta ? 'Sin oferta' : 'Sin novedad' }}</option>
      </select>
    </div>

    <div v-if="cargando" class="text-muted">Cargando...</div>
    <div v-else-if="filtrados.length === 0" class="text-center text-muted py-5">No se encontraron productos.</div>

    <div v-else class="table-responsive">
      <table class="table table-dark table-bordered align-middle">
        <thead>
          <tr>
            <th style="width: 70px;">ID</th>
            <th style="width: 90px;">Portada</th>
            <th>Nombre</th>
            <th style="width: 150px;">Estado</th>
            <th style="width: 190px;"></th>
          </tr>
        </thead>
        <tbody>
          <template v-for="p in filtrados" :key="p.idProducto">
            <tr>
              <td>{{ p.idProducto }}</td>
              <td><img :src="p.imagenUrl || '/logo-slamshoes-blanco.png'" class="rounded bg-white miniatura" :alt="p.nombre" /></td>
              <td>
                {{ p.nombre }}
                <span v-if="p.activo === false" class="badge bg-secondary ms-1">Inactivo</span>
              </td>
              <td>
                <span v-if="promosDe(p).length === 0" class="badge bg-secondary">{{ esOferta ? 'Sin oferta' : 'Sin novedad' }}</span>
                <span v-for="x in promosDe(p)" :key="idDe(x)" class="badge me-1" :class="estado(x).clase">{{ estado(x).texto }}</span>
              </td>
              <td class="text-end">
                <button v-if="promosDe(p).length === 0 && !nuevos[p.idProducto]" class="btn btn-light btn-sm" @click="abrirNuevo(p)">
                  <i class="bi bi-plus-circle me-1"></i>{{ esOferta ? 'Agregar oferta' : 'Marcar como novedad' }}
                </button>
              </td>
            </tr>

            <!-- Formulario nuevo (producto sin promoción) -->
            <tr v-if="nuevos[p.idProducto]" class="table-active">
              <td colspan="5">
                <div class="formulario">
                  <div class="row g-3 align-items-end">
                    <div v-if="esOferta" class="col-md-4">
                      <label class="form-label small text-secondary">Nombre de la oferta</label>
                      <input type="text" class="form-control campo" v-model="nuevos[p.idProducto].nombre" placeholder="Ej. Buen Fin" />
                    </div>
                    <div v-if="esOferta" class="col-6 col-md-2">
                      <label class="form-label small text-secondary">Descuento (%)</label>
                      <input type="number" min="1" max="99" step="0.5" class="form-control campo" v-model.number="nuevos[p.idProducto].porcentaje" />
                    </div>
                    <div class="col-6 col-md-3">
                      <label class="form-label small text-secondary">Fecha de inicio <span class="text-secondary">(sugerida: ahora)</span></label>
                      <input type="datetime-local" class="form-control campo" v-model="nuevos[p.idProducto].inicio" />
                    </div>
                    <div class="col-6 col-md-3">
                      <label class="form-label small text-secondary">Fecha de fin</label>
                      <input type="datetime-local" class="form-control campo" v-model="nuevos[p.idProducto].fin" />
                    </div>
                  </div>
                  <div class="mt-3 d-flex gap-2">
                    <button class="btn btn-success btn-sm" :disabled="guardando" @click="crear(p)">
                      <i class="bi bi-check-lg me-1"></i>{{ esOferta ? 'Guardar oferta' : 'Guardar novedad' }}
                    </button>
                    <button class="btn btn-outline-light btn-sm" @click="cancelarNuevo(p)">Cancelar</button>
                  </div>
                </div>
              </td>
            </tr>

            <!-- Promociones ya aplicadas: campos llenos y editables -->
            <tr v-for="x in promosDe(p)" :key="'e' + idDe(x)" class="table-active">
              <td colspan="5">
                <div class="formulario">
                  <div class="row g-3 align-items-end">
                    <div v-if="esOferta" class="col-md-4">
                      <label class="form-label small text-secondary">Nombre de la oferta</label>
                      <input type="text" class="form-control campo" v-model="borradores[idDe(x)].nombre" />
                    </div>
                    <div v-if="esOferta" class="col-6 col-md-2">
                      <label class="form-label small text-secondary">Descuento (%)</label>
                      <input type="number" min="1" max="99" step="0.5" class="form-control campo" v-model.number="borradores[idDe(x)].porcentaje" />
                    </div>
                    <div class="col-6 col-md-3">
                      <label class="form-label small text-secondary">Fecha de inicio</label>
                      <input type="datetime-local" class="form-control campo" v-model="borradores[idDe(x)].inicio" />
                    </div>
                    <div class="col-6 col-md-3">
                      <label class="form-label small text-secondary">Fecha de fin</label>
                      <input type="datetime-local" class="form-control campo" v-model="borradores[idDe(x)].fin" />
                    </div>
                  </div>
                  <div class="mt-3 d-flex flex-wrap gap-2">
                    <button v-if="cambiado(x)" class="btn btn-success btn-sm" :disabled="guardando" @click="actualizar(x)">
                      <i class="bi bi-check-lg me-1"></i>{{ esOferta ? 'Confirmar edición de oferta' : 'Confirmar edición de novedad' }}
                    </button>
                    <button v-if="cambiado(x)" class="btn btn-outline-light btn-sm" @click="restaurar(x)">Deshacer cambios</button>
                    <button class="btn btn-outline-danger btn-sm ms-auto" @click="aQuitar = { promo: x, producto: p }">
                      <i class="bi bi-trash me-1"></i>{{ esOferta ? 'Eliminar oferta' : 'Descartar novedad' }}
                    </button>
                  </div>
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>

    <!-- Confirmación: se quita solo la promoción, nunca el producto -->
    <ConfirmarAccion :abierto="!!aQuitar" :titulo="esOferta ? '¿Eliminar esta oferta?' : '¿Descartar esta novedad?'"
      :texto-confirmar="esOferta ? 'Sí, eliminar oferta' : 'Sí, descartar novedad'" :procesando="quitando"
      @cancelar="aQuitar = null" @confirmar="quitar">
      <div v-if="aQuitar" class="d-flex align-items-center gap-3 bg-black border border-secondary rounded p-2 mb-2">
        <img :src="aQuitar.producto.imagenUrl" class="rounded bg-white" style="width: 56px; height: 56px; object-fit: cover;" :alt="aQuitar.producto.nombre" />
        <div class="overflow-hidden">
          <div class="text-white text-truncate">{{ aQuitar.producto.nombre }}</div>
          <div class="text-secondary small">ID {{ aQuitar.producto.idProducto }}<template v-if="esOferta && aQuitar.promo.nombre"> · {{ aQuitar.promo.nombre }}</template></div>
        </div>
      </div>
      <p class="text-secondary small mb-0">
        Solo se quita {{ esOferta ? 'la oferta aplicada' : 'la marca de novedad' }}. <strong>El producto NO se elimina</strong> y vuelve a su precio normal.
      </p>
    </ConfirmarAccion>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import axios from 'axios';
import ConfirmarAccion from '@/components/ConfirmarAccion.vue';
import { limpiarCatalogos } from '@/utils/catalogos.js';
import { recargarOfertas } from '@/utils/ofertas.js';
import { coincideTexto } from '@/utils/busqueda.js';

const props = defineProps({ tipo: { type: String, required: true } }); // 'ofertas' | 'novedades'

const API = 'http://localhost:8080/api';
const esOferta = computed(() => props.tipo === 'ofertas');
const rutaAdmin = computed(() => `${API}/admin/${props.tipo}`);

const productos = ref([]);
const promos = ref([]);
const cargando = ref(true);
const guardando = ref(false);
const busqueda = ref('');
const filtro = ref('todos');
const nuevos = reactive({});     // idProducto -> borrador de una promoción nueva
const borradores = reactive({}); // id de promoción -> borrador editable
const aQuitar = ref(null);
const quitando = ref(false);

// ---------- Fechas (datetime-local <-> timestamp del servidor) ----------
const aLocal = (iso) => (iso ? String(iso).slice(0, 16) : '');
const aApi = (local) => (local && local.length === 16 ? `${local}:00` : local);
const ahoraLocal = (masDias = 0) => {
  const d = new Date(Date.now() + masDias * 86400000);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};

const idDe = (x) => x.idOferta ?? x.idNovedad;
const promosDe = (p) => promos.value.filter((x) => x.idProducto === p.idProducto);

function estado(x) {
  const ahora = new Date();
  if (x.activa === false) return { texto: 'Inactiva', clase: 'bg-secondary' };
  if (new Date(x.fechaFin) < ahora) return { texto: 'Vencida', clase: 'bg-secondary' };
  if (new Date(x.fechaInicio) > ahora) return { texto: 'Programada', clase: 'bg-warning text-dark' };
  return { texto: 'Vigente', clase: 'bg-success' };
}

function prepararBorradores() {
  Object.keys(borradores).forEach((k) => delete borradores[k]);
  promos.value.forEach((x) => {
    borradores[idDe(x)] = {
      nombre: x.nombre || '',
      porcentaje: x.porcentajeDescuento,
      inicio: aLocal(x.fechaInicio),
      fin: aLocal(x.fechaFin)
    };
  });
}

async function cargar() {
  try {
    const [resProductos, resPromos] = await Promise.all([axios.get(`${API}/productos`), axios.get(rutaAdmin.value)]);
    productos.value = [...resProductos.data].sort((a, b) => a.idProducto - b.idProducto);
    promos.value = resPromos.data;
    prepararBorradores();
  } catch (e) {
    alert('No se pudieron cargar los datos. Revisa que el backend esté activo.');
  } finally {
    cargando.value = false;
  }
}
onMounted(cargar);

const filtrados = computed(() =>
  productos.value.filter((p) => {
    const q = busqueda.value.trim();
    const coincide = !q || String(p.idProducto) === q || coincideTexto(p, q);
    const tiene = promosDe(p).length > 0;
    return coincide && (filtro.value === 'todos' || (filtro.value === 'con' ? tiene : !tiene));
  })
);

function cambiado(x) {
  const b = borradores[idDe(x)];
  if (!b) return false;
  return b.inicio !== aLocal(x.fechaInicio) || b.fin !== aLocal(x.fechaFin)
    || (esOferta.value && (b.nombre !== (x.nombre || '') || Number(b.porcentaje) !== Number(x.porcentajeDescuento)));
}
const restaurar = (x) => {
  borradores[idDe(x)] = {
    nombre: x.nombre || '', porcentaje: x.porcentajeDescuento, inicio: aLocal(x.fechaInicio), fin: aLocal(x.fechaFin)
  };
};

const cancelarNuevo = (p) => { delete nuevos[p.idProducto]; };

function abrirNuevo(p) {
  nuevos[p.idProducto] = { nombre: '', porcentaje: null, inicio: ahoraLocal(), fin: ahoraLocal(30) };
}

function cuerpo(b, idProducto) {
  const datos = { fechaInicio: aApi(b.inicio), fechaFin: aApi(b.fin) };
  if (idProducto) datos.idProducto = idProducto;
  if (esOferta.value) {
    datos.nombre = (b.nombre || '').trim();
    datos.porcentajeDescuento = b.porcentaje;
  }
  return datos;
}

function validar(b) {
  if (esOferta.value) {
    if (!(b.nombre || '').trim()) return 'Escribe el nombre de la oferta.';
    if (!(b.porcentaje > 0 && b.porcentaje < 100)) return 'El descuento debe estar entre 1 y 99 %.';
  }
  if (!b.inicio || !b.fin) return 'Indica la fecha de inicio y la de fin.';
  if (new Date(b.fin) <= new Date(b.inicio)) return 'La fecha de fin debe ser posterior a la de inicio.';
  return null;
}

async function refrescarTodo() {
  limpiarCatalogos();
  await recargarOfertas();
  await cargar();
}

async function crear(p) {
  const b = nuevos[p.idProducto];
  const problema = validar(b);
  if (problema) return alert(problema);
  guardando.value = true;
  try {
    await axios.post(rutaAdmin.value, cuerpo(b, p.idProducto));
    delete nuevos[p.idProducto];
    await refrescarTodo();
  } catch (e) {
    alert(e.response?.data?.error || 'No se pudo guardar.');
  } finally {
    guardando.value = false;
  }
}

async function actualizar(x) {
  const b = borradores[idDe(x)];
  const problema = validar(b);
  if (problema) return alert(problema);
  guardando.value = true;
  try {
    await axios.put(`${rutaAdmin.value}/${idDe(x)}`, cuerpo(b));
    await refrescarTodo();
  } catch (e) {
    alert(e.response?.data?.error || 'No se pudo actualizar.');
  } finally {
    guardando.value = false;
  }
}

async function quitar() {
  quitando.value = true;
  try {
    await axios.delete(`${rutaAdmin.value}/${idDe(aQuitar.value.promo)}`);
    aQuitar.value = null;
    await refrescarTodo();
  } catch (e) {
    alert(e.response?.data?.error || 'No se pudo eliminar.');
    aQuitar.value = null;
  } finally {
    quitando.value = false;
  }
}
</script>

<style scoped>
.miniatura { width: 64px; height: 64px; object-fit: cover; }
.campo { background: #000; color: #fff; border-color: #555; }
.campo:focus { background: #000; color: #fff; }
.formulario { padding: 0.25rem 0.5rem; }
</style>
