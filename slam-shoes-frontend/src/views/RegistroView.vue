<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal />

    <div class="flex-grow-1 d-flex justify-content-center align-items-center p-4">
      <div class="registro-card">
        <h2 class="text-center mb-4">Crear cuenta</h2>

        <form @submit.prevent="registrar">
          <div class="mb-3">
            <label class="form-label text-secondary small">Nombre completo</label>
            <input type="text" class="form-control bg-dark text-light border-secondary" v-model="nombre" required />
          </div>
          <div class="mb-3">
            <label class="form-label text-secondary small">Correo electrónico</label>
            <input type="email" class="form-control bg-dark text-light border-secondary" v-model="correo" required />
          </div>
          <div class="mb-3">
            <label class="form-label text-secondary small">Contraseña (mínimo 6 caracteres)</label>
            <input type="password" class="form-control bg-dark text-light border-secondary" v-model="contrasena" minlength="6" required />
          </div>

          <p v-if="error" class="text-danger small">{{ error }}</p>
          <button type="submit" class="btn btn-light w-100 fw-bold" :disabled="enviando">Registrarme</button>
        </form>

        <p class="text-center text-secondary small mt-3 mb-0">
          ¿Ya tienes cuenta? <router-link to="/login" class="text-light">Inicia sesión</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { usuariosBroker } from '@/services/brokerService.js';
import { useRouter } from 'vue-router';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';

const router = useRouter();
const nombre = ref('');
const correo = ref('');
const contrasena = ref('');
const error = ref('');
const enviando = ref(false);

// Nota: el formulario NO envía rol. El backend siempre crea el usuario como CLIENTE.
async function registrar() {
  error.value = '';
  enviando.value = true;
  try {
    // Broker: usuarios.registrar (el rol siempre lo asigna el proveedor: CLIENTE)
    await usuariosBroker.registrar(nombre.value, correo.value, contrasena.value);
    alert('¡Cuenta creada! Ahora inicia sesión.');
    router.push('/login');
  } catch (e) {
    error.value = e.message || 'No se pudo crear la cuenta. Intenta de nuevo.';
  } finally {
    enviando.value = false;
  }
}
</script>

<style scoped>
.registro-card {
  width: 100%;
  max-width: 420px;
  background: #1a1a1a;
  padding: 40px;
  border-radius: 12px;
  border: 1px solid #333;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
}
</style>
