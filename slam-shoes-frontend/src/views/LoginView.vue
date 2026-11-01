<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">

    <!-- Encabezado (Navbar idéntico al de HomeView) -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark border-bottom border-secondary w-100 px-4">
        <div class="container-fluid px-0">
            <router-link class="navbar-brand" to="/">
                <img src="/logo-slamshoes-blanco.png" alt="Slam Shoes Logo" style="height: 45px; width: auto;" class="object-contain">         
            </router-link>
            
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#menuPrincipal" aria-controls="menuPrincipal" aria-expanded="false" aria-label="Toggle navigation">
                <span class="navbar-toggler-icon"></span>
            </button>
            
            <div class="collapse navbar-collapse justify-content-center" id="menuPrincipal">
                <ul class="navbar-nav mb-2 mb-lg-0">
                    <li class="nav-item"><a class="nav-link text-light" href="#">Novedades</a></li>
                    
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle text-light" href="#" id="dropdownMarcas" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                            Marcas
                        </a>
                        <ul class="dropdown-menu dropdown-menu-dark bg-dark border-secondary" aria-labelledby="dropdownMarcas">
                            <li><a class="dropdown-item" href="#">Nike</a></li>
                            <li><a class="dropdown-item" href="#">Adidas</a></li>
                            <li><a class="dropdown-item" href="#">Li-ning</a></li>
                            <li><a class="dropdown-item" href="#">Under Armour</a></li>
                            <li><a class="dropdown-item" href="#">ANTA</a></li>
                            <li><hr class="dropdown-divider border-secondary"></li>
                            <li><a class="dropdown-item" href="#">Ver todas las marcas</a></li>
                        </ul>
                    </li>

                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle text-light" href="#" id="dropdownPlayer" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                            Por Jugador
                        </a>
                        <ul class="dropdown-menu dropdown-menu-dark bg-dark border-secondary" aria-labelledby="dropdownPlayer">
                            <li><a class="dropdown-item" href="#">Lebron James</a></li>
                            <li><a class="dropdown-item" href="#">Kevin Durant</a></li>
                            <li><a class="dropdown-item" href="#">Stephen Curry</a></li>
                            <li><a class="dropdown-item" href="#">Dwyane Wade</a></li>
                            <li><a class="dropdown-item" href="#">Kyrie Irving</a></li>
                            <li><a class="dropdown-item" href="#">Giannis A.</a></li>
                            <li><hr class="dropdown-divider border-secondary"></li>
                            <li><a class="dropdown-item" href="#">Ver todos</a></li>
                        </ul>
                    </li>
                    <li class="nav-item"><a class="nav-link text-light" href="#">Accesorios</a></li>
                    <li class="nav-item"><a class="nav-link text-light" href="#">Ofertas</a></li>
                </ul>
            </div>

            <!-- Iconos de la derecha -->
            <div class="d-flex align-items-center gap-3">
                <a href="#" class="text-light fs-5"><i class="bi bi-search"></i></a>
                <router-link to="/login" class="text-light fs-5"><i class="bi bi-person"></i></router-link>
                <a href="#" class="position-relative text-light text-decoration-none fs-5" aria-label="Carrito">
                    <i class="bi bi-bag"></i>
                    <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="font-size: 0.6rem;">0</span>
                </a>
            </div>
        </div>
    </nav>

    <!-- Contenido Principal: Dos columnas (Estilo ANTA sin la caja de texto flotante) -->
    <div class="login-wrapper flex-grow-1">
      <!-- Columna Izquierda: Imagen limpia -->
      <div class="login-image-container">
        <img 
          src="https://ckparlqrfkzofsvskmbw.supabase.co/storage/v1/object/public/slam-shoes-storage/login/portada-login.png" 
          alt="Slam Shoes Banner" 
        />
      </div>

      <!-- Columna Derecha: Formulario de Inicio de Sesión -->
      <div class="login-form-container">
        <div class="login-card">
          <h2>Iniciar Sesión</h2>
          
          <form @submit.prevent="handleLogin">
            <div class="input-group">
              <label>Correo electrónico</label>
              <!-- Sin placeholder / sin texto sugerido -->
              <input type="email" v-model="correoInput" required />
            </div>
            
            <div class="input-group">
              <label>Contraseña</label>
              <!-- Sin placeholder / sin texto sugerido -->
              <input type="password" v-model="contrasenaInput" required />
            </div>
            
            <button type="submit" class="btn-submit">Ingresar</button>
          </form>

          <div class="login-links">
            <span class="disabled-link">¿Olvidaste tu contraseña?</span>
            <a href="#" @click.prevent="irARegistro">Crear cuenta nueva</a>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

const router = useRouter()
const correoInput = ref('')
const contrasenaInput = ref('')

async function handleLogin() {
  try {
    const response = await axios.post('http://localhost:8080/api/usuarios/login', {
      correo: correoInput.value,
      contrasena: contrasenaInput.value
    });

    const usuario = response.data;
    
    // Guardamos la sesión en el navegador
    localStorage.setItem('usuario', JSON.stringify(usuario));

    alert(`¡Bienvenido de nuevo, ${usuario.nombre}!`);

    // Redirección según rol
    if (usuario.rol === 'ADMIN') {
      router.push('/admin'); 
    } else {
      router.push('/'); 
    }

  } catch (error) {
    const mensaje = error.response?.data?.error || "Credenciales incorrectas o servidor inactivo";
    alert("Error al iniciar sesión: " + mensaje);
  }
}

function irARegistro() {
  alert("Próximamente habilitaremos el módulo de registro de cuentas nuevas.");
}
</script>

<style scoped>
.login-wrapper {
  display: flex;
  width: 100%;
}

/* Columna Izquierda con la imagen limpia */
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

/* Columna Derecha con el Formulario */
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
  text-align: center;
  margin-bottom: 30px;
  font-size: 2rem;
  font-weight: 600;
}

.input-group {
  margin-bottom: 20px;
}

.input-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 0.9rem;
  color: #aaa;
}

.input-group input {
  width: 100%;
  padding: 12px 15px;
  background: #2a2a2a;
  border: 1px solid #444;
  border-radius: 6px;
  color: #fff;
  font-size: 1rem;
}

.input-group input:focus {
  outline: none;
  border-color: #fff;
}

.btn-submit {
  width: 100%;
  padding: 14px;
  background: #fff;
  color: #000;
  border: none;
  border-radius: 6px;
  font-weight: bold;
  font-size: 1rem;
  cursor: pointer;
  margin-top: 10px;
  transition: background 0.2s;
}

.btn-submit:hover {
  background: #e0e0e0;
}

.login-links {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 25px;
  font-size: 0.9rem;
}

.login-links a {
  color: #aaa;
  text-decoration: none;
  transition: color 0.2s;
}

.login-links a:hover {
  color: #fff;
  text-decoration: underline;
}

.disabled-link {
  color: #666;
  cursor: not-allowed;
}
</style>