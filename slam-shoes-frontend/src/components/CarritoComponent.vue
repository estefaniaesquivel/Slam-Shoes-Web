<template>
  <!-- Panel lateral del carrito (Offcanvas desplegable) -->
  <div class="offcanvas offcanvas-end text-bg-dark" tabindex="-1" id="offcanvasCarrito" aria-labelledby="offcanvasCarritoLabel">
      <div class="offcanvas-header border-bottom border-secondary">
          <h5 class="offcanvas-title" id="offcanvasCarritoLabel">CARRITO DE COMPRAS</h5>
          <button type="button" class="btn-close btn-close-white" data-bs-dismiss="offcanvas" aria-label="Cerrar"></button>
      </div>
      <div class="offcanvas-body d-flex flex-column">
          <div id="lista-carrito" class="flex-grow-1 overflow-auto pe-2">
              <div v-if="carrito.length === 0" class="text-center text-muted my-4">El carrito está vacío</div>
              
              <div v-for="(item, index) in carrito" :key="index" class="d-flex align-items-center justify-content-between mb-3 pb-3 border-bottom border-secondary gap-2">
                  <!-- Imagen de portada real (orden = 1) -->
                  <div class="bg-white p-1 rounded d-flex align-items-center justify-content-center" style="width: 75px; height: 75px; min-width: 75px;">
                      <img :src="item.imagenUrl || '/logo-slamshoes-blanco.png'" class="w-100 h-100 object-fit-contain rounded" alt="Sneaker">
                  </div>

                  <!-- Información y recuadro de talla -->
                  <div class="flex-grow-1 overflow-hidden">
                      <h6 class="mb-1 text-white text-truncate small fw-bold" :title="item.nombre">{{ item.nombre }}</h6>
                      
                      <!-- Recuadro minimalista con la talla seleccionada -->
                      <div v-if="item.talla" class="badge bg-secondary text-light px-2 py-1 mb-2 rounded-0 small" style="font-size: 0.75rem;">
                          {{ item.talla }} cm
                      </div>

                      <!-- Controles de cantidad estilo Way of Wade (+, -, Papelera) -->
                      <div class="d-flex align-items-center border border-secondary rounded bg-black" style="width: fit-content;">
                          <button class="btn btn-sm text-light px-2 py-0 border-end border-secondary" @click="decrementarCantidad(index)" title="Disminuir / Eliminar">
                              <i :class="item.cantidad === 1 ? 'bi bi-trash text-danger' : 'bi bi-dash'"></i>
                          </button>
                          <span class="px-3 text-light small fw-bold">{{ item.cantidad }}</span>
                          <button class="btn btn-sm text-light px-2 py-0 border-start border-secondary" @click="incrementarCantidad(index)" title="Aumentar">
                              <i class="bi bi-plus"></i>
                          </button>
                      </div>
                  </div>

                  <!-- Subtotal -->
                  <div class="text-end ms-2">
                      <span class="fw-bold text-success d-block mb-1 small">${{ item.precio * item.cantidad }} MXN</span>
                  </div>
              </div>
          </div>

          <div class="border-top border-secondary pt-3 mt-auto">
              <div class="d-flex justify-content-between fw-bold mb-3">
                  <span>Total:</span>
                  <span class="text-success">${{ totalCarrito }} MXN</span>
              </div>
              <div class="d-grid gap-2">
                  <button class="btn btn-outline-danger btn-sm" @click="vaciarCarrito">Vaciar carrito</button>
                  <button class="btn btn-light" @click="continuarCompra">Continuar compra</button>
              </div>
          </div>
      </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { verificarSesionYAccionar } from '@/utils/auth.js';
import axios from 'axios';

export default {
  name: 'CarritoComponent',
  setup() {
    const router = useRouter();
    const carrito = ref(JSON.parse(localStorage.getItem('slam_shoes_cart')) || []);

    // Función unificada que envía el estado actual completo al backend para limpiar registros viejos y actualizar los vigentes
    const sincronizarTotalConBackend = async () => {
      const usuarioActivo = verificarSesionYAccionar();
      if (!usuarioActivo) return;
      const idUsuario = usuarioActivo.idUsuario || usuarioActivo.id || usuarioActivo.id_usuario;

      try {
        const resCarrito = await axios.get(`http://localhost:8080/api/carrito/usuario/${idUsuario}`);
        const idCarrito = resCarrito.data.idCarrito;

        // Mapeamos los elementos actuales del carrito local
        const itemsParaBD = carrito.value.map(item => ({
          idCarrito: idCarrito,
          idVariante: item.idVariante,
          cantidad: item.cantidad
        }));

        // Enviamos la lista completa al endpoint de sincronización/reemplazo total
        await axios.post(`http://localhost:8080/api/carrito/${idCarrito}/sincronizar`, itemsParaBD);
      } catch (e) {
        console.error("Error al sincronizar el carrito con el backend:", e);
      }
    };

    const guardarCarritoStorage = async () => {
      localStorage.setItem('slam_shoes_cart', JSON.stringify(carrito.value));
      window.dispatchEvent(new Event('storage'));
      await sincronizarTotalConBackend(); // Sincroniza y limpia la BD de inmediato
    };

    const incrementarCantidad = async (index) => {
      carrito.value[index].cantidad += 1;
      await guardarCarritoStorage();
    };

    const decrementarCantidad = async (index) => {
      const item = carrito.value[index];
      if (item.cantidad > 1) {
        item.cantidad -= 1;
      } else {
        carrito.value.splice(index, 1);
      }
      await guardarCarritoStorage();
    };

    const vaciarCarrito = async () => {
      carrito.value = [];
      await guardarCarritoStorage();
    };

    const totalCarrito = computed(() => {
      return carrito.value.reduce((total, item) => total + (item.precio * item.cantidad), 0);
    });

    const continuarCompra = () => {
      const usuarioActivo = verificarSesionYAccionar();
      if (!usuarioActivo) return;

      if (carrito.value.length === 0) {
        alert("Tu carrito está vacío.");
        return;
      }

      // Cierra el panel lateral y abre el resumen del pedido
      const panel = document.getElementById('offcanvasCarrito');
      const instancia = panel && window.bootstrap ? window.bootstrap.Offcanvas.getInstance(panel) : null;
      if (instancia) instancia.hide();
      router.push('/checkout');
    };

    onMounted(() => {
      window.addEventListener('storage', () => {
        carrito.value = JSON.parse(localStorage.getItem('slam_shoes_cart')) || [];
      });
    });

    return {
      carrito,
      incrementarCantidad,
      decrementarCantidad,
      vaciarCarrito,
      totalCarrito,
      continuarCompra
    };
  }
};
</script>