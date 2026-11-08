<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">

    <NavbarPrincipal />

    <!-- Contenido Principal: Perfil del Usuario -->
    <div class="login-wrapper flex-grow-1">
      <!-- Columna Izquierda: Imagen de portada limpia -->
      <div class="login-image-container">
        <img 
          src="https://ckparlqrfkzofsvskmbw.supabase.co/storage/v1/object/public/slam-shoes-storage/login/portada-login.png" 
          alt="Slam Shoes Banner" 
        />
      </div>

      <!-- Columna Derecha: Tarjeta con los Datos del Usuario -->
      <div class="login-form-container">
        <div class="login-card text-center">
          <div class="mb-4">
            <div class="user-avatar-icon mx-auto mb-3">
              <i class="bi bi-person-circle fs-1 text-light"></i>
            </div>
            <h2>Mi Cuenta</h2>
          </div>
          
          <div v-if="usuario" class="user-info text-start mb-4">
            <div class="info-group mb-3">
              <label class="text-secondary small">Nombre completo</label>
              <p class="fs-5 fw-semibold text-white mb-0">{{ usuario.nombre }}</p>
            </div>

            <div class="info-group mb-3">
              <label class="text-secondary small">Correo electrónico</label>
              <p class="fs-5 text-white mb-0">{{ usuario.correo }}</p>
            </div>

            <div class="info-group mb-3">
              <label class="text-secondary small">Rol de usuario</label>
              <p class="mb-0"><span class="badge bg-secondary text-uppercase">{{ usuario.rol }}</span></p>
            </div>
          </div>

          <div class="d-grid gap-2">
            <!-- Solo clientes con sesión iniciada -->
            <router-link v-if="usuario && usuario.rol === 'CLIENTE'" to="/mi-perfil" class="btn btn-outline-light">
              <i class="bi bi-pencil-square me-1"></i> Mi perfil
            </router-link>
            <router-link v-if="usuario && usuario.rol === 'ADMIN'" to="/admin" class="btn btn-outline-light">Panel de administrador</router-link>
            <button class="btn btn-outline-light" @click="irAFavoritos">Mis Favoritos</button>
            <button class="btn btn-danger fw-bold py-2 mt-2" @click="cerrarSesion">Cerrar Sesión</button>
          </div>
        </div>
      </div>
    </div>

    <CarritoComponent />

  </div>
</template>

<script setup>
import NavbarPrincipal from '@/components/NavbarPrincipal.vue'
import CarritoComponent from '@/components/CarritoComponent.vue'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const usuario = ref(null)

onMounted(() => {
  const datosUsuario = localStorage.getItem('usuario')
  if (!datosUsuario) {
    router.push('/login') // Si no hay sesión, regresa al login
  } else {
    usuario.value = JSON.parse(datosUsuario)
  }
})

function cerrarSesion() {
  // Eliminamos solo los datos de usuario; el carrito y favoritos (si los manejas en storage) se quedan intactos
  localStorage.removeItem('usuario')
  alert("Has cerrado sesión correctamente. Tus artículos en el carrito se mantienen guardados.")
  router.push('/')
}

function irAFavoritos() {
  router.push('/favoritos')
}
</script>

<style scoped>
.login-wrapper {
  display: flex;
  width: 100%;
}

.login-image-container {
  flex: 1;
  position: relative;
  display: none;
}

@media (min-width: 768px) {
  .login-image-container {
    display: block;
  }
}

.login-image-container img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: brightness(0.85);
}

.login-form-container {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: #1a1a1a;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.5);
  border: 1px solid #333;
}

.login-card h2 {
  margin-bottom: 10px;
  font-size: 2rem;
  font-weight: 600;
}

.info-group {
  background: #2a2a2a;
  padding: 12px 15px;
  border-radius: 6px;
  border: 1px solid #444;
}
</style>