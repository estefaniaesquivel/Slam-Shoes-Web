<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal />

    <div class="flex-grow-1 d-flex justify-content-center p-4">
      <div class="perfil-card">
        <h2 class="mb-4"><i class="bi bi-pencil-square me-2"></i>Mi perfil</h2>

        <div v-if="cargando" class="text-muted">Cargando...</div>

        <form v-else @submit.prevent="guardar">
          <!-- Dirección -->
          <div class="mb-4">
            <label class="form-label text-secondary small">Dirección de envío</label>
            <textarea class="form-control bg-dark text-light border-secondary" rows="3" v-model="perfil.direccion" required></textarea>
            <div class="form-text text-secondary">Los envíos están fuera del alcance del proyecto, Slam Shoes es un prototipo.</div>
          </div>

          <!-- Método de pago (solo texto) -->
          <div class="mb-4">
            <label class="form-label text-secondary small">Método de pago</label>
            <select class="form-select bg-dark text-light border-secondary" v-model="perfil.metodoPago">
              <option value="">Selecciona una opción</option>
              <option value="Tarjeta de débito">Tarjeta de débito</option>
              <option value="Tarjeta de crédito">Tarjeta de crédito</option>
            </select>
            <div class="form-text text-secondary">Los pagos están fuera del alcance del proyecto, Slam Shoes es un prototipo.</div>
          </div>

          <!-- Talla preferida -->
          <div class="mb-4">
            <label class="form-label text-secondary small">Talla de preferencia (cm)</label>
            <select class="form-select bg-dark text-light border-secondary" v-model="perfil.tallaPreferida">
              <option value="">Selecciona una talla</option>
              <option v-for="t in tallas" :key="t" :value="t">{{ t }} cm</option>
            </select>
          </div>

          <div class="d-flex gap-2">
            <button type="submit" class="btn btn-light fw-bold flex-grow-1" :disabled="guardando">Guardar</button>
            <router-link to="/profile" class="btn btn-outline-light">Volver</router-link>
          </div>
        </form>
      </div>
    </div>

    <CarritoComponent />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import CarritoComponent from '@/components/CarritoComponent.vue';
import { obtenerUsuario } from '@/utils/auth.js';

const router = useRouter();
const usuario = obtenerUsuario();
const cargando = ref(true);
const guardando = ref(false);
const perfil = ref({ direccion: '', metodoPago: '', tallaPreferida: '' });

// 22, 22.5, 23 ... 32 cm
const tallas = Array.from({ length: 21 }, (_, i) => String(22 + i * 0.5));

onMounted(async () => {
  if (!usuario) {
    router.push('/login');
    return;
  }
  try {
    const res = await axios.get(`http://localhost:8080/api/perfiles/usuario/${usuario.idUsuario}`);
    perfil.value = {
      direccion: res.data.direccion || '',
      metodoPago: res.data.metodoPago || '',
      tallaPreferida: res.data.tallaPreferida || ''
    };
  } catch (e) {
    console.error('Error al cargar el perfil:', e);
  } finally {
    cargando.value = false;
  }
});

async function guardar() {
  guardando.value = true;
  try {
    await axios.put(`http://localhost:8080/api/perfiles/usuario/${usuario.idUsuario}`, perfil.value);
    alert('Tu perfil se guardó correctamente.');
    router.push('/profile');
  } catch (e) {
    alert(e.response?.data?.error || 'No se pudo guardar el perfil.');
  } finally {
    guardando.value = false;
  }
}
</script>

<style scoped>
.perfil-card {
  width: 100%;
  max-width: 520px;
  height: fit-content;
  background: #1a1a1a;
  padding: 40px;
  border-radius: 12px;
  border: 1px solid #333;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
}
</style>
