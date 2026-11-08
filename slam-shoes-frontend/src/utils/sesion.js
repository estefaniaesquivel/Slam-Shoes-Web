import axios from 'axios';

const API = 'http://localhost:8080/api';

const CLAVE_CARRITO = 'slam_shoes_cart';
const CLAVE_FAVORITOS = 'slam_shoes_favoritos';

// Inicia sesión en el navegador y restaura el carrito y los favoritos de ESE cliente desde la BD.
// Así cada cliente mantiene su propio estado aunque otro haya usado antes el mismo navegador.
export async function iniciarSesionLocal(usuario) {
  localStorage.setItem('usuario', JSON.stringify(usuario));

  let carrito = [];
  let favoritos = [];
  if (usuario.rol !== 'ADMIN') {
    try {
      carrito = (await axios.get(`${API}/carrito/usuario/${usuario.idUsuario}/items`)).data;
    } catch (e) {
      console.error('No se pudo restaurar el carrito:', e);
    }
    try {
      favoritos = (await axios.get(`${API}/favoritos/usuario/${usuario.idUsuario}/productos`)).data;
    } catch (e) {
      console.error('No se pudieron restaurar los favoritos:', e);
    }
  }

  localStorage.setItem(CLAVE_CARRITO, JSON.stringify(carrito));
  localStorage.setItem(CLAVE_FAVORITOS, JSON.stringify(favoritos));
  window.dispatchEvent(new Event('storage'));
}

// Cierra sesión: el carrito y los favoritos se vacían en el navegador (siguen guardados en la BD del cliente)
export function cerrarSesionLocal() {
  localStorage.removeItem('usuario');
  localStorage.removeItem(CLAVE_CARRITO);
  localStorage.removeItem(CLAVE_FAVORITOS);
  window.dispatchEvent(new Event('storage'));
}
