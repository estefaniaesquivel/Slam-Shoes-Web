<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal />

    <div class="flex-grow-1 container my-4" style="max-width: 900px;">
      <h2 class="mb-4"><i class="bi bi-box-seam me-2"></i>Mis pedidos</h2>

      <div v-if="cargando" class="text-muted">Cargando...</div>
      <div v-else-if="pedidos.length === 0" class="text-center text-muted py-5">
        <p>Aún no has realizado ningún pedido.</p>
        <router-link to="/" class="btn btn-outline-light">Ir a comprar</router-link>
      </div>

      <div v-else>
        <div v-for="p in pedidos" :key="p.idPedido" class="bg-black border border-secondary rounded p-3 mb-4">
          <div class="d-flex justify-content-between flex-wrap gap-2 mb-3">
            <div>
              <h5 class="mb-0">Pedido #{{ p.idPedido }}</h5>
              <span class="text-secondary small">{{ formatearFecha(p.fecha) }}</span>
            </div>
            <div class="text-end">
              <span class="badge bg-secondary">{{ p.estado }}</span>
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
        </div>
      </div>
    </div>

    <CarritoComponent />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { pedidosBroker } from '@/services/brokerService.js';
import { useRouter } from 'vue-router';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import CarritoComponent from '@/components/CarritoComponent.vue';
import { obtenerUsuario } from '@/utils/auth.js';

const router = useRouter();
const pedidos = ref([]);
const cargando = ref(true);

const formatearFecha = (f) => (f ? new Date(f).toLocaleString('es-MX') : '');

onMounted(async () => {
  const usuario = obtenerUsuario();
  if (!usuario) {
    router.push('/login');
    return;
  }
  try {
    // Broker: pedidos.consultarHistorial
    pedidos.value = (await pedidosBroker.consultarHistorial(usuario.idUsuario)).pedidos || [];
  } catch (e) {
    console.error('Error al cargar los pedidos:', e);
  } finally {
    cargando.value = false;
  }
});
</script>
