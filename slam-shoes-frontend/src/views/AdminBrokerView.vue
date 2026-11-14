<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal modo-admin />

    <div class="flex-grow-1 container my-5" style="max-width: 1000px;">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-1">
        <h2 class="mb-0"><i class="bi bi-diagram-3 me-2"></i>Monitor del Broker</h2>
        <router-link to="/admin/modulos" class="btn btn-outline-light btn-sm">Volver</router-link>
      </div>
      <p class="text-secondary mb-4">
        Servicios registrados, métricas y últimas solicitudes. Se actualiza cada 3 segundos.
      </p>

      <div v-if="error" class="alert alert-danger">
        <i class="bi bi-exclamation-octagon me-2"></i>{{ error }}
        <div class="small mt-1">Sin Broker, la tienda no puede atender login, detalle de producto ni pedidos.</div>
      </div>

      <template v-if="estado">
        <div class="row g-3 mb-4">
          <div class="col-6 col-md-3" v-for="m in tarjetas" :key="m.titulo">
            <div class="border border-secondary rounded-3 bg-black p-3 h-100">
              <div class="text-secondary small">{{ m.titulo }}</div>
              <div class="fs-4">{{ m.valor }}</div>
            </div>
          </div>
        </div>

        <h5 class="mb-2">Proveedores registrados ({{ estado.servicios.length }})</h5>
        <div class="table-responsive mb-4">
          <table class="table table-dark table-sm align-middle">
            <thead>
              <tr><th>Servicio</th><th>Destino</th><th>Operaciones</th><th>Fallos seguidos</th><th>Último contacto</th></tr>
            </thead>
            <tbody>
              <tr v-for="s in estado.servicios" :key="s.servicio">
                <td class="fw-bold">{{ s.servicio }}</td>
                <td>{{ s.host ? `${s.host}:${s.puerto}` : s.clase }}</td>
                <td class="small">{{ (s.operaciones || []).join(', ') }}</td>
                <td><span :class="['badge', s.fallosConsecutivos ? 'bg-danger' : 'bg-success']">{{ s.fallosConsecutivos ?? 0 }}</span></td>
                <td class="small text-secondary">{{ formatearHora(s.ultimoContacto) }}</td>
              </tr>
              <tr v-if="estado.servicios.length === 0">
                <td colspan="5" class="text-secondary">No hay proveedores registrados.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <h5 class="mb-2">Solicitudes por estado</h5>
        <div class="d-flex flex-wrap gap-2 mb-4">
          <span v-for="(n, nombre) in estado.metricas.porEstado" :key="nombre"
                :class="['badge', nombre === 'EXITO' ? 'bg-success' : 'bg-secondary']">{{ nombre }}: {{ n }}</span>
          <span v-if="Object.keys(estado.metricas.porEstado).length === 0" class="text-secondary small">Aún no hay solicitudes.</span>
        </div>

        <h5 class="mb-2">Bitácora (últimas solicitudes)</h5>
        <pre class="bitacora border border-secondary rounded-3 bg-black p-3 small">{{ bitacora }}</pre>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import { obtenerEstadoBroker } from '@/services/brokerService.js';

const estado = ref(null);
const error = ref('');
let temporizador = null;

async function actualizar() {
  try {
    estado.value = await obtenerEstadoBroker();
    error.value = '';
  } catch (e) {
    estado.value = null;
    error.value = e.message;
  }
}

const tarjetas = computed(() => {
  const m = estado.value?.metricas || {};
  return [
    { titulo: 'Solicitudes', valor: m.solicitudesTotales ?? 0 },
    { titulo: 'Exitosas', valor: m.exitosas ?? 0 },
    { titulo: 'Con error', valor: m.conError ?? 0 },
    { titulo: 'Tiempo promedio', valor: `${m.tiempoPromedioMs ?? 0} ms` },
    { titulo: 'Duplicadas evitadas', valor: m.duplicadasAtendidasDesdeCache ?? 0 }
  ];
});

const bitacora = computed(() => {
  const lineas = estado.value?.ultimasSolicitudes || [];
  return lineas.length ? [...lineas].reverse().join('\n') : '(sin registros)';
});

const formatearHora = (f) => (f ? new Date(f).toLocaleTimeString('es-MX') : '');

onMounted(() => {
  actualizar();
  temporizador = setInterval(actualizar, 3000);
});
onBeforeUnmount(() => clearInterval(temporizador));
</script>

<style scoped>
.bitacora { max-height: 320px; overflow: auto; color: #b6f0c2; white-space: pre; }
</style>
