import { ref } from 'vue';
import axios from 'axios';

// Mejor oferta de cada producto (vigente hoy o programada). Se pide una vez y se comparte
// con todas las tarjetas, para que la etiqueta de descuento aparezca en cualquier módulo.
const mapa = ref({});
let promesa = null;
let ultimaCarga = 0;
const VIGENCIA_CACHE_MS = 30000;

export function cargarOfertas() {
  if (!promesa || Date.now() - ultimaCarga > VIGENCIA_CACHE_MS) {
    ultimaCarga = Date.now();
    promesa = axios
      .get('http://localhost:8080/api/ofertas/resumen')
      .then((r) => {
        const nuevo = {};
        r.data.forEach((o) => { nuevo[o.idProducto] = o; });
        mapa.value = nuevo;
      })
      .catch(() => { promesa = null; });
  }
  return promesa;
}

export const ofertaDe = (idProducto) => mapa.value[idProducto] || null;

export const porcentajeTexto = (o) => Number(o.porcentajeDescuento).toString();

// "20 nov" para las ofertas que todavía no empiezan
export const fechaCorta = (iso) =>
  new Date(iso).toLocaleDateString('es-MX', { day: 'numeric', month: 'short' }).replace('.', '');
