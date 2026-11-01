<template>
  <div class="login-wrapper">
    <!-- Columna Izquierda: Imagen de portada (Estilo ANTA) -->
    <div class="login-image-container">
      <img 
        src="https://ckparlqrfkzofsvskmbw.supabase.co/storage/v1/object/public/slam-shoes-storage/login/portada-login.png" 
        alt="Slam Shoes Banner" 
      />
      <div class="image-overlay-text">
        <h2>Slam Shoes Membership</h2>
        <p>Estilo retro, básquetbol y exclusividad en cada sneaker.</p>
      </div>
    </div>

    <!-- Columna Derecha: Formulario de Inicio de Sesión -->
    <div class="login-form-container">
      <div class="login-card">
        <h2>Iniciar Sesión</h2>
        
        <form @submit.prevent="handleLogin">
          <div class="input-group">
            <label>Correo electrónico</label>
            <input type="email" v-model="correoInput" placeholder="a21200707@alumnos.uady.mx" required />
          </div>
          
          <div class="input-group">
            <label>Contraseña</label>
            <input type="password" v-model="contrasenaInput" placeholder="••••••••" required />
          </div>
          
          <button type="submit" class="btn-submit">Ingresar</button>
        </form>

        <div class="login-links">
          <!-- Omitimos recuperación por ahora, dejamos solo el texto visual -->
          <span class="disabled-link">¿Olvidaste tu contraseña?</span>
          <a href="#" @click.prevent="irARegistro">Crear cuenta nueva</a>
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
      router.push('/admin'); // O tu ruta de administración
    } else {
      router.push('/'); // Catálogo principal
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
  min-height: 85vh;
  background-color: #121212;
  color: #fff;
}

/* Columna Izquierda con la imagen */
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

.image-overlay-text {
  position: absolute;
  bottom: 40px;
  left: 40px;
  background: rgba(0, 0, 0, 0.6);
  padding: 20px;
  border-radius: 8px;
  backdrop-filter: blur(5px);
}

.image-overlay-text h2 {
  font-size: 1.8rem;
  margin-bottom: 8px;
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