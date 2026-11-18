<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal />

    <div class="flex-grow-1 d-flex justify-content-center align-items-center p-4">
      <div class="registro-card">
        <h2 class="text-center mb-4">Crear cuenta</h2>

        <!-- Aviso: la recuperación de cuenta no existe en el prototipo -->
        <div class="alert alert-warning small" role="alert">
          <i class="bi bi-exclamation-triangle me-1"></i>
          <strong>Importante:</strong> la opción de recuperar cuenta no está habilitada. Recuerda muy bien tus datos.
        </div>

        <form @submit.prevent="registrar">
          <div class="mb-3">
            <label class="form-label text-secondary small">Nombre de usuario</label>
            <input type="text" class="form-control bg-dark text-light border-secondary" v-model.trim="nombre" maxlength="100" required />
          </div>
          <div class="mb-3">
            <label class="form-label text-secondary small">Correo electrónico (con él iniciarás sesión)</label>
            <input type="email" class="form-control bg-dark text-light border-secondary" v-model.trim="correo" required />
          </div>
          <div class="mb-3">
            <label class="form-label text-secondary small">Contraseña (mínimo 6 caracteres)</label>
            <input type="password" class="form-control bg-dark text-light border-secondary" v-model="contrasena" minlength="6" required />
          </div>
          <div class="mb-3">
            <label class="form-label text-secondary small">Confirmar contraseña</label>
            <input type="password" class="form-control bg-dark text-light border-secondary" v-model="confirmacion" minlength="6" required />
          </div>

          <p v-if="error" class="text-danger small">{{ error }}</p>
          <button type="submit" class="btn btn-light w-100 fw-bold" :disabled="enviando">
            {{ enviando ? 'Creando cuenta...' : 'Confirmar datos' }}
          </button>
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
const confirmacion = ref('');
const error = ref('');
const enviando = ref(false);

// El formulario NO envía rol: el proveedor de usuarios siempre crea la cuenta como CLIENTE.
async function registrar() {
  error.value = '';
  if (contrasena.value !== confirmacion.value) {
    error.value = 'Las contraseñas no coinciden.';
    return;
  }
  enviando.value = true;
  try {
    // Broker: usuarios.registrar
    await usuariosBroker.registrar(nombre.value, correo.value, contrasena.value);
    // Vuelve al inicio de sesión para que el cliente ingrese con los datos que acaba de crear
    router.push({ path: '/login', query: { registro: 'ok' } });
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
