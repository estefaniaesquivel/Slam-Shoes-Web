<template>
  <!-- Tabla de jugadores o marcas: nombre + imagen, guardar cambios y eliminar (con doble confirmación) -->
  <div>
    <div v-if="cargando" class="text-muted">Cargando...</div>

    <div v-else class="table-responsive">
      <table class="table table-dark table-bordered align-middle">
        <thead>
          <tr>
            <th style="width: 70px;">ID</th>
            <th :style="{ width: (anchoVisual + 40) + 'px' }">Imagen</th>
            <th>Nombre</th>
            <th style="width: 190px;">Guardar</th>
            <th style="width: 120px;">Eliminar</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="x in lista" :key="x.id">
            <td>{{ x.id }}</td>
            <td>
              <SubirImagen :ancho="ancho" :alto="alto" :etiqueta="etiquetaImagen" :url-actual="x.imagen" :ancho-visual="anchoVisual"
                @seleccionado="(f) => (editados[x.id].archivo = f)" />
            </td>
            <td><input type="text" class="form-control campo" v-model="editados[x.id].nombre" /></td>
            <td>
              <button v-if="cambiado(x)" class="btn btn-success btn-sm" :disabled="guardando" @click="guardar(x)">
                <i class="bi bi-check-lg me-1"></i>Guardar cambios
              </button>
              <span v-else class="text-secondary small">Sin cambios</span>
            </td>
            <td class="text-center">
              <button class="btn btn-outline-danger btn-sm" :title="`Eliminar ${singular}`" @click="aEliminar = x">
                <i class="bi bi-trash"></i>
              </button>
            </td>
          </tr>

          <!-- Última fila: dar de alta uno nuevo -->
          <tr class="table-active">
            <td class="text-secondary">Nuevo</td>
            <td>
              <SubirImagen :ancho="ancho" :alto="alto" :etiqueta="etiquetaImagen" :ancho-visual="anchoVisual" :key="claveNuevo"
                @seleccionado="(f) => (nuevo.archivo = f)" />
            </td>
            <td><input type="text" class="form-control campo" v-model="nuevo.nombre" placeholder="Nombre" /></td>
            <td colspan="2">
              <button class="btn btn-light btn-sm" :disabled="guardando" @click="crear">
                <i class="bi bi-plus-circle me-1"></i>Agregar {{ singular }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <ConfirmarAccion :abierto="!!aEliminar" :titulo="`¿Eliminar ${deSingular}?`" doble :procesando="eliminando"
      texto-confirmar="Sí, eliminar definitivamente" :mensaje-doble="mensajeDoble"
      @cancelar="aEliminar = null" @confirmar="eliminar">
      <div v-if="aEliminar" class="d-flex align-items-center gap-3 bg-black border border-secondary rounded p-2 mb-2">
        <img :src="aEliminar.imagen" class="rounded bg-white" style="width: 90px; height: 40px; object-fit: cover;" :alt="aEliminar.nombre" />
        <div class="text-white text-truncate">{{ aEliminar.nombre }}</div>
      </div>
      <p class="text-secondary small mb-0">Se quitará del catálogo y se borrará su imagen. Esta acción no se puede deshacer.</p>
    </ConfirmarAccion>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import axios from 'axios';
import SubirImagen from '@/components/SubirImagen.vue';
import ConfirmarAccion from '@/components/ConfirmarAccion.vue';
import { limpiarCatalogos } from '@/utils/catalogos.js';

const props = defineProps({ tipo: { type: String, required: true } }); // 'jugadores' | 'marcas'

// Lo que cambia entre jugadores y marcas
const CONFIG = {
  jugadores: {
    id: 'idJugador', imagen: 'imagenUrl', ancho: 2400, alto: 800, anchoVisual: 200,
    etiquetaImagen: 'Añadir imagen del jugador', singular: 'jugador', deSingular: 'al jugador', tabla: 'productos.id_jugador',
    mensajeDoble: 'ESTE JUGADOR YA NO SERÁ PARTE DEL CATÁLOGO. NO ELIMINARÁ A LOS PRODUCTOS EXISTENTES RELACIONADOS, SOLO id_jugador QUEDARÁ VACÍO EN LA TABLA DE PRODUCTOS. SI QUIERES DAR DE BAJA PRODUCTOS, HAZLO MANUALMENTE EN EL MÓDULO DE EDICIÓN DE PRODUCTOS.'
  },
  marcas: {
    id: 'idMarca', imagen: 'logoUrl', ancho: 2000, alto: 2000, anchoVisual: 90,
    etiquetaImagen: 'Añadir imagen de la marca', singular: 'marca', deSingular: 'a la marca', tabla: 'productos.id_marca',
    mensajeDoble: 'ESTA MARCA YA NO SERÁ PARTE DEL CATÁLOGO. NO ELIMINARÁ A LOS PRODUCTOS EXISTENTES RELACIONADOS, SOLO id_marca QUEDARÁ VACÍO EN LA TABLA DE PRODUCTOS. SI QUIERES DAR DE BAJA PRODUCTOS, HAZLO MANUALMENTE EN EL MÓDULO DE EDICIÓN DE PRODUCTOS.'
  }
};
const cfg = computed(() => CONFIG[props.tipo]);
const { ancho, alto, anchoVisual, etiquetaImagen, singular, deSingular, mensajeDoble } = CONFIG[props.tipo];

const API = `http://localhost:8080/api/admin/${props.tipo}`;

const lista = ref([]);
const cargando = ref(true);
const guardando = ref(false);
const editados = reactive({}); // id -> { nombre, archivo }
const nuevo = reactive({ nombre: '', archivo: null });
const claveNuevo = ref(0);
const aEliminar = ref(null);
const eliminando = ref(false);

async function cargar() {
  try {
    const datos = (await axios.get(API)).data;
    lista.value = datos.map((d) => ({ id: d[cfg.value.id], nombre: d.nombre, imagen: d[cfg.value.imagen] }));
    lista.value.forEach((x) => { editados[x.id] = { nombre: x.nombre, archivo: null }; });
  } catch (e) {
    alert('No se pudo cargar la lista. Revisa que el backend esté activo.');
  } finally {
    cargando.value = false;
  }
}
onMounted(cargar);

const cambiado = (x) => !!editados[x.id] && (editados[x.id].nombre !== x.nombre || !!editados[x.id].archivo);

function formulario(nombre, archivo) {
  const f = new FormData();
  f.append('datos', new Blob([JSON.stringify({ nombre: nombre.trim() })], { type: 'application/json' }));
  if (archivo) f.append('imagen', archivo);
  return f;
}

async function guardar(x) {
  const e = editados[x.id];
  if (!e.nombre.trim()) return alert('El nombre no puede quedar vacío.');
  guardando.value = true;
  try {
    await axios.put(`${API}/${x.id}`, formulario(e.nombre, e.archivo));
    limpiarCatalogos();
    await cargar();
  } catch (err) {
    alert(err.response?.data?.error || 'No se pudieron guardar los cambios.');
  } finally {
    guardando.value = false;
  }
}

async function crear() {
  if (!nuevo.nombre.trim()) return alert('Escribe el nombre.');
  if (!nuevo.archivo) return alert(`Agrega la imagen (${ancho}x${alto} px, .webp).`);
  guardando.value = true;
  try {
    await axios.post(API, formulario(nuevo.nombre, nuevo.archivo));
    nuevo.nombre = '';
    nuevo.archivo = null;
    claveNuevo.value++; // reinicia el recuadro de imagen
    limpiarCatalogos();
    await cargar();
  } catch (err) {
    alert(err.response?.data?.error || 'No se pudo agregar.');
  } finally {
    guardando.value = false;
  }
}

async function eliminar() {
  eliminando.value = true;
  try {
    await axios.delete(`${API}/${aEliminar.value.id}`);
    aEliminar.value = null;
    limpiarCatalogos();
    await cargar();
  } catch (err) {
    alert(err.response?.data?.error || 'No se pudo eliminar.');
    aEliminar.value = null;
  } finally {
    eliminando.value = false;
  }
}
</script>

<style scoped>
.campo { background: #000; color: #fff; border-color: #555; }
.campo:focus { background: #000; color: #fff; }
</style>
