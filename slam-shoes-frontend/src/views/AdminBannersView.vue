<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal modo-admin />

    <div class="flex-grow-1 container-fluid px-4 my-4" style="max-width: 1300px;">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
        <h2 class="mb-0"><i class="bi bi-images me-2"></i>Banners de inicio</h2>
        <router-link to="/admin/modulos" class="btn btn-outline-light btn-sm">Volver a módulos</router-link>
      </div>
      <p class="text-secondary small">
        El carrusel del inicio muestra los banners activos en el orden indicado. El título es solo para identificarlos aquí: no aparece en el inicio.
      </p>

      <div class="table-responsive">
        <table class="table table-dark table-bordered align-middle mb-0">
          <thead>
            <tr>
              <th style="width: 70px;">Orden</th>
              <th style="width: 230px;">Imagen</th>
              <th>Título</th>
              <th>Enlace (a dónde lleva)</th>
              <th style="width: 80px;" class="text-center">Activo</th>
              <th style="width: 130px;"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="cargando"><td colspan="6" class="text-center text-muted py-4">Cargando...</td></tr>

            <!-- Banners existentes -->
            <tr v-for="b in banners" :key="b.idBanner">
              <template v-if="editando === b.idBanner">
                <td><input type="number" min="1" class="form-control form-control-sm campo" v-model.number="borrador.orden" /></td>
                <td><SubirImagen :ancho="2400" :alto="800" etiqueta="Cambiar imagen" :url-actual="b.imagenUrl" :ancho-visual="210" @seleccionado="(f) => (borrador.archivo = f)" /></td>
                <td><input type="text" class="form-control form-control-sm campo" v-model="borrador.titulo" /></td>
                <td><input type="text" class="form-control form-control-sm campo" v-model="borrador.enlace" placeholder="/producto/12" /></td>
                <td class="text-center"><input type="checkbox" class="form-check-input" v-model="borrador.activo" /></td>
                <td class="text-end text-nowrap">
                  <button class="btn btn-success btn-sm me-1" :disabled="guardando" title="Guardar cambios" @click="guardarEdicion(b)">
                    <i class="bi bi-check-lg"></i>
                  </button>
                  <button class="btn btn-outline-light btn-sm" title="Cancelar" @click="editando = null"><i class="bi bi-x-lg"></i></button>
                </td>
              </template>
              <template v-else>
                <td>{{ b.orden }}</td>
                <td><img :src="b.imagenUrl" class="vista rounded" :alt="b.titulo" /></td>
                <td>{{ b.titulo }}</td>
                <td class="text-break"><code v-if="b.enlaceDestino">{{ b.enlaceDestino }}</code><span v-else class="text-secondary">Sin enlace</span></td>
                <td class="text-center"><i :class="['bi', b.activo ? 'bi-check-circle-fill text-success' : 'bi-dash-circle text-secondary']"></i></td>
                <td class="text-end text-nowrap">
                  <button class="btn btn-outline-danger btn-sm me-2" title="Eliminar banner" @click="aEliminar = b"><i class="bi bi-trash"></i></button>
                  <button class="btn btn-light btn-sm" title="Editar banner" @click="empezarEdicion(b)"><i class="bi bi-pencil-square"></i></button>
                </td>
              </template>
            </tr>

            <!-- Filas nuevas (cada clic en "Agregar banner" despliega una más) -->
            <tr v-for="(n, i) in nuevos" :key="n.clave" class="table-active">
              <td><input type="number" min="1" class="form-control form-control-sm campo" v-model.number="n.orden" placeholder="Auto" /></td>
              <td><SubirImagen :ancho="2400" :alto="800" etiqueta="Agregar imagen" :ancho-visual="210" @seleccionado="(f) => (n.archivo = f)" /></td>
              <td><input type="text" class="form-control form-control-sm campo" v-model="n.titulo" placeholder="Título del banner" /></td>
              <td><input type="text" class="form-control form-control-sm campo" v-model="n.enlace" placeholder="/producto/12" /></td>
              <td class="text-center"><input type="checkbox" class="form-check-input" v-model="n.activo" /></td>
              <td class="text-end text-nowrap">
                <button class="btn btn-success btn-sm me-1" :disabled="guardando" title="Guardar banner" @click="guardarNuevo(n, i)">
                  <i class="bi bi-cloud-arrow-up me-1"></i>Guardar
                </button>
                <button class="btn btn-outline-light btn-sm" title="Quitar fila" @click="nuevos.splice(i, 1)"><i class="bi bi-x-lg"></i></button>
              </td>
            </tr>

            <!-- Última fila: siempre visible -->
            <tr>
              <td colspan="6" class="text-center">
                <button class="btn btn-outline-light" @click="agregarFila"><i class="bi bi-plus-circle me-1"></i>Agregar banner</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Ayuda del campo "enlace" -->
      <div class="border border-secondary rounded-3 bg-black p-3 mt-4 small">
        <h6 class="mb-2"><i class="bi bi-link-45deg me-1"></i>¿Qué escribo en el enlace?</h6>
        <p class="text-secondary mb-2">
          Escribe la <strong>ruta interna</strong> de la página (empieza con <code>/</code>). Así funciona igual en tus pruebas (localhost)
          y cuando la tienda esté publicada, porque no depende del dominio. También puedes copiar y pegar la dirección completa de la barra
          del navegador (por ejemplo <code>http://localhost:5173/producto/12</code>): se convierte sola a <code>/producto/12</code>.
          Una página externa sí debe llevar la dirección completa (<code>https://...</code>). Déjalo vacío si el banner no debe llevar a ningún lado.
        </p>
        <ul class="text-secondary mb-0">
          <li v-for="e in ejemplos" :key="e.ruta"><code>{{ e.ruta }}</code> — {{ e.texto }}</li>
        </ul>
      </div>
    </div>

    <ConfirmarAccion :abierto="!!aEliminar" titulo="¿Eliminar este banner?" :procesando="eliminando"
      @cancelar="aEliminar = null" @confirmar="eliminar">
      <div v-if="aEliminar" class="d-flex align-items-center gap-3 bg-black border border-secondary rounded p-2 mb-2">
        <img :src="aEliminar.imagenUrl" class="rounded bg-white" style="width: 120px; height: 40px; object-fit: cover;" :alt="aEliminar.titulo" />
        <div class="overflow-hidden"><div class="text-white text-truncate">{{ aEliminar.titulo }}</div></div>
      </div>
      <p class="text-secondary small mb-0">Dejará de mostrarse en el inicio y se borrará su imagen. Esta acción no se puede deshacer.</p>
    </ConfirmarAccion>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import axios from 'axios';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import SubirImagen from '@/components/SubirImagen.vue';
import ConfirmarAccion from '@/components/ConfirmarAccion.vue';
import { limpiarCatalogos } from '@/utils/catalogos.js';
import { normalizarEnlace, EJEMPLOS_ENLACE } from '@/utils/enlaces.js';

const API = 'http://localhost:8080/api/admin/banners';
const ejemplos = EJEMPLOS_ENLACE;

const banners = ref([]);
const cargando = ref(true);
const guardando = ref(false);
const editando = ref(null);
const borrador = reactive({ titulo: '', enlace: '', orden: 1, activo: true, archivo: null });
const nuevos = ref([]);
let contador = 0;
const aEliminar = ref(null);
const eliminando = ref(false);

async function cargar() {
  try {
    banners.value = (await axios.get(API)).data;
  } catch (e) {
    alert('No se pudieron cargar los banners. Revisa que el backend esté activo.');
  } finally {
    cargando.value = false;
  }
}
onMounted(cargar);

function armarFormulario(d, archivo) {
  const f = new FormData();
  f.append('banner', new Blob([JSON.stringify({
    titulo: d.titulo.trim(),
    enlaceDestino: normalizarEnlace(d.enlace),
    orden: d.orden || null,
    activo: d.activo
  })], { type: 'application/json' }));
  if (archivo) f.append('imagen', archivo);
  return f;
}

function agregarFila() {
  nuevos.value.push({ clave: ++contador, titulo: '', enlace: '', orden: null, activo: true, archivo: null });
}

async function guardarNuevo(n, indice) {
  if (!n.titulo.trim()) return alert('Escribe el título del banner.');
  if (!n.archivo) return alert('Agrega la imagen del banner (2400x800 px, .webp).');
  guardando.value = true;
  try {
    await axios.post(API, armarFormulario(n, n.archivo));
    nuevos.value.splice(indice, 1);
    limpiarCatalogos();
    await cargar();
  } catch (e) {
    alert(e.response?.data?.error || 'No se pudo guardar el banner.');
  } finally {
    guardando.value = false;
  }
}

function empezarEdicion(b) {
  editando.value = b.idBanner;
  Object.assign(borrador, { titulo: b.titulo || '', enlace: b.enlaceDestino || '', orden: b.orden, activo: b.activo !== false, archivo: null });
}

async function guardarEdicion(b) {
  if (!borrador.titulo.trim()) return alert('El título no puede quedar vacío.');
  guardando.value = true;
  try {
    await axios.put(`${API}/${b.idBanner}`, armarFormulario(borrador, borrador.archivo));
    editando.value = null;
    limpiarCatalogos();
    await cargar();
  } catch (e) {
    alert(e.response?.data?.error || 'No se pudo actualizar el banner.');
  } finally {
    guardando.value = false;
  }
}

async function eliminar() {
  eliminando.value = true;
  try {
    await axios.delete(`${API}/${aEliminar.value.idBanner}`);
    aEliminar.value = null;
    limpiarCatalogos();
    await cargar();
  } catch (e) {
    alert(e.response?.data?.error || 'No se pudo eliminar el banner.');
    aEliminar.value = null;
  } finally {
    eliminando.value = false;
  }
}
</script>

<style scoped>
.vista { width: 210px; aspect-ratio: 3 / 1; object-fit: cover; background: #fff; }
.campo { background: #000; color: #fff; border-color: #555; }
.campo:focus { background: #000; color: #fff; }
</style>
