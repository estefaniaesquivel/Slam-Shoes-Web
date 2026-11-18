<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal modo-admin />

    <div class="flex-grow-1 container my-4" style="max-width: 900px;">
      <router-link to="/admin/pedidos" class="text-secondary text-decoration-none small">
        <i class="bi bi-arrow-left me-1"></i>Pedidos
      </router-link>
      <h2 class="mb-1 mt-1">
        <i :class="['bi', enProceso ? 'bi-hourglass-split text-warning' : 'bi-check2-circle text-success', 'me-2']"></i>{{ titulo }}
      </h2>
      <p class="text-secondary small mb-4">{{ descripcion }}</p>

      <div v-if="error" class="alert alert-danger small">{{ error }}</div>
      <div v-if="cargando" class="text-muted">Cargando...</div>
      <div v-else-if="pedidos.length === 0" class="text-center text-muted py-5">
        {{ enProceso ? 'No hay pedidos en proceso.' : 'Aún no hay pedidos concretados.' }}
      </div>

      <div v-for="p in pedidos" :key="p.idPedido" class="bg-black border border-secondary rounded p-3 mb-4">
        <div class="d-flex justify-content-between flex-wrap gap-2 mb-3">
          <div>
            <h5 class="mb-0">Pedido #{{ p.idPedido }}</h5>
            <div class="text-white"><i class="bi bi-person me-1"></i>{{ p.nombreCliente }}</div>
            <span class="text-secondary small">{{ formatearFecha(p.fecha) }}</span>
          </div>
          <div class="text-end">
            <span class="badge" :class="enProceso ? 'bg-warning text-dark' : 'bg-success'">{{ p.estado }}</span>
            <div class="fs-5 fw-bold text-success">${{ p.total }} MXN</div>
          </div>
        </div>

        <div v-for="(l, i) in p.lineas" :key="i" class="d-flex align-items-center gap-3 mb-2 pb-2 border-bottom border-secondary">
          <div class="bg-white p-1 rounded d-flex align-items-center justify-content-center" style="width: 60px; height: 60px; min-width: 60px;">
            <img :src="l.imagenUrl || '/logo-slamshoes-blanco.png'" class="w-100 h-100 object-fit-contain rounded" :alt="l.nombre">
          </div>
          <div class="flex-grow-1 overflow-hidden">
            <div class="text-white text-truncate">{{ l.nombre }}</div>
            <span v-if="l.talla" class="badge bg-secondary rounded-0 me-2">{{ l.talla }} cm</span>
            <span v-if="l.color" class="text-secondary small me-2">{{ l.color }}</span>
            <span class="text-secondary small">Cantidad: {{ l.cantidad }}</span>
          </div>
          <span class="text-nowrap small">${{ l.precioUnitario }} c/u</span>
        </div>

        <div class="text-secondary small mt-2">
          <div><i class="bi bi-geo-alt me-1"></i>{{ p.direccionEnvio }}</div>
          <div v-if="p.metodoPago"><i class="bi bi-credit-card me-1"></i>{{ p.metodoPago }}</div>
        </div>

        <!-- Acciones -->
        <div class="d-flex flex-wrap gap-2 mt-3">
          <template v-if="enProceso">
            <button class="btn btn-success" :disabled="ocupado === p.idPedido" @click="concretar(p)">
              <i class="bi bi-check2-circle me-1"></i>Concretado
            </button>
            <button class="btn btn-outline-danger" :disabled="ocupado === p.idPedido" @click="pedirConfirmacion(p)">
              <i class="bi bi-x-circle me-1"></i>No concretado
            </button>
          </template>
          <button v-else class="btn btn-outline-light" :disabled="ocupado === p.idPedido" @click="reabrir(p)">
            <i class="bi bi-arrow-counterclockwise me-1"></i>Regresar a en proceso
          </button>
        </div>
      </div>
    </div>

    <!-- Confirmación de "no concretado": borra el pedido y devuelve el stock -->
    <ConfirmarAccion
      :abierto="!!porEliminar"
      titulo="¿Marcar como no concretado?"
      texto-confirmar="Sí, no se concretó"
      :procesando="eliminando"
      @cancelar="porEliminar = null"
      @confirmar="noConcretado"
    >
      <p v-if="porEliminar" class="text-secondary small text-center mb-0">
        El pedido #{{ porEliminar.idPedido }} de <strong class="text-light">{{ porEliminar.nombreCliente }}</strong> se
        eliminará de la lista y las {{ unidades(porEliminar) }} unidad(es) regresarán al stock.
      </p>
    </ConfirmarAccion>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import ConfirmarAccion from '@/components/ConfirmarAccion.vue';

const API = 'http://localhost:8080/api/admin/pedidos';
const route = useRoute();

const pedidos = ref([]);
const cargando = ref(true);
const error = ref('');
const ocupado = ref(null);      // id del pedido con una acción en curso (evita dobles clics)
const porEliminar = ref(null);  // pedido que espera confirmación
const eliminando = ref(false);

const enProceso = computed(() => route.meta.tipo === 'enProceso');
const titulo = computed(() => (enProceso.value ? 'Pedidos en proceso' : 'Pedidos concretados'));
const descripcion = computed(() =>
  enProceso.value
    ? 'Marca cada encargo como concretado, o como no concretado (se elimina y el stock regresa a la base de datos).'
    : 'Historial de encargos concretados. Si te equivocaste, regrésalo a en proceso.'
);

const formatearFecha = (f) => (f ? new Date(f).toLocaleString('es-MX') : '');
const unidades = (p) => p.lineas.reduce((suma, l) => suma + (l.cantidad || 0), 0);
const mensajeDe = (e, porDefecto) => e.response?.data?.error || porDefecto;

async function cargar() {
  cargando.value = true;
  error.value = '';
  try {
    const estado = enProceso.value ? 'PENDIENTE' : 'CONCRETADO';
    pedidos.value = (await axios.get(API, { params: { estado } })).data;
  } catch (e) {
    error.value = mensajeDe(e, 'No se pudieron cargar los pedidos.');
  } finally {
    cargando.value = false;
  }
}

// Ejecuta una acción sobre un pedido y vuelve a leer la lista (otro administrador pudo haberla cambiado)
async function accion(pedido, peticion, falla) {
  if (ocupado.value !== null) return;
  ocupado.value = pedido.idPedido;
  error.value = '';
  try {
    await peticion();
  } catch (e) {
    error.value = mensajeDe(e, falla);
  } finally {
    ocupado.value = null;
    await cargar();
  }
}

const concretar = (p) => accion(p, () => axios.put(`${API}/${p.idPedido}/concretar`), 'No se pudo marcar el pedido como concretado.');
const reabrir = (p) => accion(p, () => axios.put(`${API}/${p.idPedido}/reabrir`), 'No se pudo regresar el pedido a en proceso.');

const pedirConfirmacion = (p) => { porEliminar.value = p; };

async function noConcretado() {
  const pedido = porEliminar.value;
  if (!pedido) return;
  eliminando.value = true;
  await accion(pedido, () => axios.delete(`${API}/${pedido.idPedido}`), 'No se pudo eliminar el pedido.');
  eliminando.value = false;
  porEliminar.value = null;
}

onMounted(cargar);
</script>
