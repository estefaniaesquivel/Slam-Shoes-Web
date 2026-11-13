import { ref } from 'vue';
import axios from 'axios';

// Variantes (color + talla) con stock de todos los productos: { idProducto: [{ color, talla }] }
// Alimenta los filtros de talla y color del catálogo.
const mapa = ref({});
const cargado = ref(false);
let promesa = null;

export function cargarVariantes() {
  if (!promesa) {
    promesa = axios
      .get('http://localhost:8080/api/producto-tallas/resumen')
      .then((r) => {
        const nuevo = {};
        r.data.forEach((v) => {
          (nuevo[v.idProducto] ||= []).push({ color: v.color, talla: v.talla });
        });
        mapa.value = nuevo;
        cargado.value = true;
      })
      .catch(() => { promesa = null; });
  }
  return promesa;
}

export const variantesDe = (idProducto) => mapa.value[idProducto] || [];
export const variantesCargadas = cargado;
