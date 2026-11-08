<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal modo-admin />

    <div class="flex-grow-1 container-fluid px-4 my-4">
      <h2 class="mb-1"><i class="bi bi-box-seam me-2"></i>Pedidos</h2>
      <p class="text-secondary small mb-3">Tablas <code>pedidos</code> y <code>pedido_detalle</code> unidas: una fila por producto de cada pedido.</p>

      <div v-if="cargando" class="text-muted">Cargando...</div>
      <div v-else-if="error" class="text-danger">{{ error }}</div>
      <div v-else-if="filas.length === 0" class="text-center text-muted py-5">Aún no hay pedidos registrados.</div>

      <div v-else class="table-responsive">
        <table class="table table-dark table-bordered table-hover align-middle small">
          <thead>
            <tr>
              <th>id_pedido</th><th>id_usuario</th><th>fecha</th><th>total</th><th>estado</th>
              <th>direccion_envio</th><th>metodo_pago</th>
              <th>id_detalle</th><th>id_variante</th><th>cantidad</th><th>precio_unitario</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(f, i) in filas" :key="i">
              <td>{{ f.idPedido }}</td>
              <td>{{ f.idUsuario }}</td>
              <td class="text-nowrap">{{ formatearFecha(f.fecha) }}</td>
              <td>${{ f.total }}</td>
              <td><span class="badge bg-secondary">{{ f.estado }}</span></td>
              <td>{{ f.direccionEnvio }}</td>
              <td>{{ f.metodoPago ?? '—' }}</td>
              <td>{{ f.idDetalle ?? '—' }}</td>
              <td>{{ f.idVariante ?? '—' }}</td>
              <td>{{ f.cantidad ?? '—' }}</td>
              <td>{{ f.precioUnitario != null ? '$' + f.precioUnitario : '—' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';

const filas = ref([]);
const cargando = ref(true);
const error = ref('');

const formatearFecha = (f) => (f ? new Date(f).toLocaleString('es-MX') : '—');

onMounted(async () => {
  try {
    filas.value = (await axios.get('http://localhost:8080/api/admin/pedidos')).data;
  } catch (e) {
    error.value = 'No se pudieron cargar los pedidos.';
  } finally {
    cargando.value = false;
  }
});
</script>
