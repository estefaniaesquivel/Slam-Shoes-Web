<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal modo-admin />

    <div class="flex-grow-1 container my-5" style="max-width: 900px;">
      <h2 class="mb-1"><i class="bi bi-box-seam me-2"></i>Pedidos</h2>
      <p class="text-secondary mb-4">Elige qué pedidos quieres revisar.</p>

      <div v-if="error" class="alert alert-danger small">{{ error }}</div>

      <div class="row g-4">
        <div class="col-sm-6" v-for="b in botones" :key="b.ruta">
          <router-link :to="b.ruta" class="modulo-card d-flex align-items-center gap-3 border border-secondary rounded-3 bg-black p-4 text-decoration-none text-light">
            <i :class="['bi', b.icono, 'fs-1', b.color]"></i>
            <div class="flex-grow-1">
              <h5 class="mb-1">{{ b.nombre }}</h5>
              <p class="text-secondary small mb-0">{{ b.descripcion }}</p>
            </div>
            <span class="badge rounded-pill fs-6" :class="b.fondo">{{ b.total }}</span>
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';

const conteo = ref({ enProceso: 0, concretados: 0 });
const error = ref('');

const botones = computed(() => [
  {
    ruta: '/admin/pedidos/en-proceso', nombre: 'Pedidos en proceso', icono: 'bi-hourglass-split', color: 'text-warning',
    fondo: 'bg-warning text-dark', total: conteo.value.enProceso,
    descripcion: 'Encargos pendientes: márcalos como concretados o no concretados'
  },
  {
    ruta: '/admin/pedidos/concretados', nombre: 'Pedidos concretados', icono: 'bi-check2-circle', color: 'text-success',
    fondo: 'bg-success', total: conteo.value.concretados,
    descripcion: 'Historial de encargos ya concretados'
  }
]);

onMounted(async () => {
  try {
    const { data } = await axios.get('http://localhost:8080/api/admin/pedidos/conteo');
    conteo.value = { enProceso: data.enProceso ?? 0, concretados: data.concretados ?? 0 };
  } catch (e) {
    error.value = e.response?.data?.error || 'No se pudo consultar el número de pedidos.';
  }
});
</script>

<style scoped>
.modulo-card { transition: transform 0.2s ease, border-color 0.2s ease; }
.modulo-card:hover { transform: translateY(-4px); border-color: #fff !important; }
</style>
