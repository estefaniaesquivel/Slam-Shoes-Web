import axios from 'axios';

// Datos que casi no cambian (menús, marcas, colecciones...). Cada vista incluye su propio Navbar y
// secciones, y antes cada una volvía a pedirlos al servidor en cada clic. Ahora se piden una vez,
// se comparten entre todos los componentes y se renuevan solo cada VIGENCIA_MS.
const API = 'http://localhost:8080/api';
const VIGENCIA_MS = 60000;
const cache = {};

function pedir(clave, ruta) {
  const e = cache[clave];
  if (e && Date.now() - e.hora < VIGENCIA_MS) return e.promesa;
  const promesa = axios.get(`${API}${ruta}`).then((r) => r.data);
  cache[clave] = { hora: Date.now(), promesa };
  // Si falla no se guarda, para reintentar en la próxima visita
  promesa.catch(() => { if (cache[clave] && cache[clave].promesa === promesa) delete cache[clave]; });
  return promesa;
}

export const obtenerJugadores = () => pedir('jugadores', '/jugadores');
export const obtenerColecciones = () => pedir('colecciones', '/colecciones');
export const obtenerMarcas = () => pedir('marcas', '/marcas');
export const obtenerCategorias = () => pedir('categorias', '/categorias');
export const obtenerProductos = () => pedir('productos', '/productos');
export const obtenerBanners = () => pedir('banners', '/banners');

// Llamar tras crear/editar/eliminar para que las vistas vuelvan a leer del servidor
export const limpiarCatalogos = () => { Object.keys(cache).forEach((k) => delete cache[k]); };
