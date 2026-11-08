import axios from 'axios';
import { verificarSesionYAccionar, obtenerUsuario } from '@/utils/auth.js';

const API = 'http://localhost:8080/api';
const CLAVE = 'slam_shoes_favoritos';

export function obtenerFavoritos() {
  try {
    return JSON.parse(localStorage.getItem(CLAVE)) || [];
  } catch (e) {
    return [];
  }
}

export function esFavorito(idProducto) {
  return obtenerFavoritos().some((p) => p.idProducto === idProducto);
}

function guardar(favs) {
  localStorage.setItem(CLAVE, JSON.stringify(favs));
  window.dispatchEvent(new Event('storage')); // avisa al encabezado y a los corazones
}

// Agrega o quita un favorito del cliente con sesión (y lo sincroniza con la BD).
// Devuelve true si quedó como favorito, false si se quitó, null si no hay sesión.
export async function alternarFavorito(producto) {
  const usuario = verificarSesionYAccionar();
  if (!usuario) return null;

  const favs = obtenerFavoritos();
  const existe = favs.some((p) => p.idProducto === producto.idProducto);

  try {
    if (existe) {
      guardar(favs.filter((p) => p.idProducto !== producto.idProducto));
      await axios.delete(`${API}/favoritos`, { params: { idUsuario: usuario.idUsuario, idProducto: producto.idProducto } });
    } else {
      guardar([...favs, producto]);
      await axios.post(`${API}/favoritos`, { idUsuario: usuario.idUsuario, idProducto: producto.idProducto });
    }
  } catch (e) {
    console.error('No se pudo sincronizar el favorito con el servidor:', e);
  }
  return !existe;
}

export async function quitarFavorito(idProducto) {
  const usuario = obtenerUsuario();
  guardar(obtenerFavoritos().filter((p) => p.idProducto !== idProducto));
  if (!usuario) return;
  try {
    await axios.delete(`${API}/favoritos`, { params: { idUsuario: usuario.idUsuario, idProducto } });
  } catch (e) {
    console.error('No se pudo quitar el favorito en el servidor:', e);
  }
}
