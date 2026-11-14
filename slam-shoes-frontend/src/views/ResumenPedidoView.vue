<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal />

    <div class="flex-grow-1 container my-4" style="max-width: 900px;">

      <!-- Compra confirmada -->
      <div v-if="pedidoCreado" class="text-center py-5">
        <i class="bi bi-check-circle text-success" style="font-size: 4rem;"></i>
        <h2 class="mt-3">¡Compra confirmada!</h2>
        <p class="text-secondary mb-1">Pedido #{{ pedidoCreado.idPedido }} · Estado: {{ pedidoCreado.estado }}</p>
        <p class="fs-4 text-success fw-bold">Total: ${{ pedidoCreado.total }} MXN</p>
        <router-link to="/" class="btn btn-light mt-3">Volver a inicio</router-link>
      </div>

      <template v-else>
        <h2 class="mb-4">Resumen del pedido</h2>

        <div v-if="carrito.length === 0" class="text-center text-muted py-5">
          <p>Tu carrito está vacío.</p>
          <router-link to="/" class="btn btn-outline-light">Ir a comprar</router-link>
        </div>

        <div v-else class="row g-4">
          <!-- Datos del cliente -->
          <div class="col-lg-5">
            <div class="bg-black border border-secondary rounded p-3 mb-3">
              <span class="text-secondary small d-block">Cliente</span>
              <p class="fw-semibold mb-0">{{ usuario?.nombre }}</p>
            </div>

            <div class="bg-black border border-secondary rounded p-3 mb-3">
              <div class="d-flex justify-content-between align-items-center mb-1">
                <span class="text-secondary small">Dirección de envío</span>
                <button v-if="!editandoDireccion" class="btn btn-sm btn-outline-light" @click="editandoDireccion = true">
                  <i class="bi bi-pencil"></i> Editar dirección
                </button>
              </div>

              <template v-if="editandoDireccion">
                <textarea class="form-control bg-dark text-light border-secondary mb-2" rows="3" v-model="direccionEditada"></textarea>
                <div class="d-flex gap-2">
                  <button class="btn btn-sm btn-light" @click="guardarDireccion" :disabled="!direccionEditada.trim()">Guardar</button>
                  <button v-if="perfil.direccion" class="btn btn-sm btn-outline-secondary" @click="editandoDireccion = false">Cancelar</button>
                </div>
              </template>
              <p v-else class="mb-0">{{ perfil.direccion }}</p>
              <div class="form-text text-secondary">Los envíos están fuera del alcance del proyecto, Slam Shoes es un prototipo.</div>
            </div>

            <div class="bg-black border border-secondary rounded p-3">
              <span class="text-secondary small d-block">Método de pago</span>
              <p class="mb-0" v-if="perfil.metodoPago">{{ perfil.metodoPago }}</p>
              <p class="mb-0 text-secondary" v-else>
                No definido. <router-link to="/mi-perfil" class="text-light">Elegir en Mi perfil</router-link>
              </p>
              <div class="form-text text-secondary">Los pagos están fuera del alcance del proyecto, Slam Shoes es un prototipo.</div>
            </div>
          </div>

          <!-- Productos y recibo -->
          <div class="col-lg-7">
            <div class="bg-black border border-secondary rounded p-3">
              <h5 class="mb-3">Productos</h5>

              <div v-for="(item, i) in carrito" :key="i" class="d-flex align-items-center gap-3 mb-3 pb-3 border-bottom border-secondary">
                <div class="bg-white p-1 rounded d-flex align-items-center justify-content-center" style="width: 75px; height: 75px; min-width: 75px;">
                  <img :src="item.imagenUrl || '/logo-slamshoes-blanco.png'" class="w-100 h-100 object-fit-contain rounded" :alt="item.nombre">
                </div>
                <div class="flex-grow-1 overflow-hidden">
                  <h6 class="mb-1 text-white text-truncate">{{ item.nombre }}</h6>
                  <span v-if="item.talla" class="badge bg-secondary rounded-0 me-2">{{ item.talla }} cm</span>
                  <span class="text-secondary small">Cantidad: {{ item.cantidad }}</span>
                </div>
                <span class="fw-bold text-success text-nowrap">${{ redondear(item.precio * item.cantidad) }} MXN</span>
              </div>

              <!-- Recibo -->
              <div class="d-flex justify-content-between text-secondary mb-1">
                <span>Artículos</span><span>{{ totalArticulos }}</span>
              </div>
              <div class="d-flex justify-content-between fs-5 fw-bold">
                <span>Total</span><span class="text-success">${{ total }} MXN</span>
              </div>

              <p v-if="error" class="text-danger small mt-3 mb-0">{{ error }}</p>

              <button class="btn btn-light fw-bold w-100 mt-3" @click="confirmarCompra" :disabled="enviando || !perfil.direccion">
                {{ enviando ? 'Procesando...' : 'Confirmar compra' }}
              </button>
              <p v-if="!perfil.direccion" class="text-warning small mt-2 mb-0">Agrega una dirección para poder confirmar.</p>
            </div>
          </div>
        </div>
      </template>
    </div>

    <CarritoComponent />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import CarritoComponent from '@/components/CarritoComponent.vue';
import { obtenerUsuario } from '@/utils/auth.js';
import { pedidosBroker, nuevoIdSolicitud } from '@/services/brokerService.js';

const API = 'http://localhost:8080/api';
const router = useRouter();
const usuario = obtenerUsuario();

const carrito = ref(JSON.parse(localStorage.getItem('slam_shoes_cart')) || []);
const perfil = ref({ direccion: '', metodoPago: '', tallaPreferida: '' });
const editandoDireccion = ref(false);
const direccionEditada = ref('');
const enviando = ref(false);
const error = ref('');
const pedidoCreado = ref(null);
// Mismo id en cada reintento de ESTA compra: si el clic se repite, el Broker no crea otro pedido
const idSolicitudCompra = nuevoIdSolicitud();

const redondear = (n) => Math.round(n * 100) / 100;
const total = computed(() => redondear(carrito.value.reduce((s, i) => s + i.precio * i.cantidad, 0)));
const totalArticulos = computed(() => carrito.value.reduce((s, i) => s + i.cantidad, 0));

onMounted(async () => {
  if (!usuario) {
    router.push('/login');
    return;
  }
  try {
    const res = await axios.get(`${API}/perfiles/usuario/${usuario.idUsuario}`);
    perfil.value = {
      direccion: res.data.direccion || '',
      metodoPago: res.data.metodoPago || '',
      tallaPreferida: res.data.tallaPreferida || ''
    };
  } catch (e) {
    console.error('Error al cargar el perfil:', e);
  }
  // Sin dirección guardada se abre directamente el editor
  if (!perfil.value.direccion) editandoDireccion.value = true;
});

async function guardarDireccion() {
  try {
    const res = await axios.put(`${API}/perfiles/usuario/${usuario.idUsuario}`, {
      ...perfil.value,
      direccion: direccionEditada.value.trim()
    });
    perfil.value.direccion = res.data.direccion;
    editandoDireccion.value = false;
  } catch (e) {
    alert(e.response?.data?.error || 'No se pudo guardar la dirección.');
  }
}

async function confirmarCompra() {
  error.value = '';
  enviando.value = true;
  try {
    // Broker: pedidos.crearPedido (valida usuario y stock, calcula precios con ofertas y descuenta existencias)
    pedidoCreado.value = await pedidosBroker.crearPedido({
      idUsuario: usuario.idUsuario,
      direccionEnvio: perfil.value.direccion,
      metodoPago: perfil.value.metodoPago || null,
      items: carrito.value.map((i) => ({ idVariante: i.idVariante, cantidad: i.cantidad }))
    }, idSolicitudCompra);

    // El carrito se vacía en el navegador (el backend ya lo vació en la BD)
    localStorage.setItem('slam_shoes_cart', JSON.stringify([]));
    carrito.value = [];
    window.dispatchEvent(new Event('storage'));
  } catch (e) {
    error.value = e.message || 'No se pudo registrar el pedido. Intenta de nuevo.';
  } finally {
    enviando.value = false;
  }
}
</script>
