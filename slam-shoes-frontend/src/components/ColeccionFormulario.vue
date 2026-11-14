<template>
  <!-- Formulario de colecciones (sirve para agregar y para editar) -->
  <form @submit.prevent="enviarFormulario">
    <div class="row g-3 mb-4">
      <div class="col-md-6">
        <label class="form-label small text-secondary">Nombre de la colección</label>
        <input type="text" class="form-control campo" v-model="nombre" maxlength="100" />
      </div>
      <div class="col-12">
        <label class="form-label small text-secondary">Descripción</label>
        <textarea class="form-control campo" rows="3" v-model="descripcion" placeholder="Aparece como pie de la imagen en el inicio"></textarea>
      </div>
    </div>

    <!-- Buscador de tenis -->
    <h5 class="mb-2">Tenis de la colección</h5>
    <input type="text" class="form-control campo mb-2" style="max-width: 420px;" placeholder="Buscar tenis por nombre, marca o modelo..." v-model="busqueda" />

    <div v-if="busqueda.trim()" class="resultados border border-secondary rounded-3 bg-black mb-3">
      <div v-if="resultados.length === 0" class="text-secondary small p-3">No se encontraron tenis (o ya están en la colección).</div>
      <button v-for="p in resultados" :key="p.idProducto" type="button" class="resultado d-flex align-items-center gap-3 w-100 text-start" @click="agregar(p)">
        <img :src="p.imagenUrl || '/logo-slamshoes-blanco.png'" class="rounded bg-white miniatura" :alt="p.nombre" />
        <span class="flex-grow-1 overflow-hidden">
          <span class="d-block text-white text-truncate">{{ p.nombre }}</span>
          <span class="d-block text-secondary small">Haz clic para agregar a la colección</span>
          <span v-if="enOtra(p)" class="badge bg-warning text-dark">Hoy está en: {{ enOtra(p) }} (se moverá)</span>
        </span>
        <i class="bi bi-plus-circle fs-4 text-light"></i>
      </button>
    </div>

    <!-- Tabla de los tenis que ya están en la colección -->
    <div class="table-responsive mb-4">
      <table class="table table-dark table-bordered align-middle mb-0">
        <thead>
          <tr>
            <th style="width: 70px;">ID</th>
            <th style="width: 90px;">Portada</th>
            <th>Nombre</th>
            <th>Colección</th>
            <th style="width: 90px;">Quitar</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="seleccionados.length === 0">
            <td colspan="5" class="text-center text-secondary py-3">Aún no hay tenis en la colección. Búscalos arriba.</td>
          </tr>
          <tr v-for="p in seleccionados" :key="p.idProducto">
            <td>{{ p.idProducto }}</td>
            <td><img :src="p.imagenUrl || '/logo-slamshoes-blanco.png'" class="rounded bg-white miniatura" :alt="p.nombre" /></td>
            <td>{{ p.nombre }}</td>
            <td>{{ nombre.trim() || '(sin nombre)' }}</td>
            <td class="text-center">
              <button type="button" class="btn btn-outline-danger btn-sm" title="Quitar de la colección" @click="quitar(p)">
                <i class="bi bi-x-lg"></i>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Portada -->
    <h5 class="mb-2">Portada</h5>
    <SubirImagen :ancho="2000" :alto="2000" etiqueta="Añadir portada" :url-actual="inicial.imagenUrl || ''" :ancho-visual="200"
      @seleccionado="(f) => (archivo = f)" />
    <p class="text-secondary small mt-1 mb-4">La foto debe ser de 2000x2000px y estar en formato .webp.</p>

    <button type="submit" class="btn btn-light btn-lg" :disabled="enviando">
      <i class="bi bi-cloud-arrow-up me-2"></i>{{ enviando ? 'Subiendo...' : textoBoton }}
    </button>
  </form>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import SubirImagen from '@/components/SubirImagen.vue';
import { coincideTexto } from '@/utils/busqueda.js';

const props = defineProps({
  // { nombre, descripcion, imagenUrl, productos: [{ idProducto, nombre, imagenUrl }] } al editar
  inicial: { type: Object, default: () => ({}) },
  textoBoton: { type: String, default: 'Subir colección al catálogo' },
  enviando: { type: Boolean, default: false }
});
const emit = defineEmits(['enviar']);

const nombre = ref(props.inicial.nombre || '');
const descripcion = ref(props.inicial.descripcion || '');
const seleccionados = ref([...(props.inicial.productos || [])]);
const archivo = ref(null);
const busqueda = ref('');

const catalogo = ref([]);        // todos los productos
const colecciones = ref([]);     // para avisar si un tenis ya está en otra colección
onMounted(async () => {
  try {
    const [p, c] = await Promise.all([
      axios.get('http://localhost:8080/api/productos'),
      axios.get('http://localhost:8080/api/admin/colecciones')
    ]);
    catalogo.value = p.data;
    colecciones.value = c.data;
  } catch (e) {
    alert('No se pudo cargar el catálogo de tenis. Revisa que el backend esté activo.');
  }
});

const resultados = computed(() => {
  const ya = new Set(seleccionados.value.map((p) => p.idProducto));
  const q = busqueda.value.trim();
  return catalogo.value
    .filter((p) => !ya.has(p.idProducto) && (String(p.idProducto) === q || coincideTexto(p, q)))
    .slice(0, 8);
});

// Nombre de la colección donde está hoy el tenis, si es distinta de la que se edita
function enOtra(p) {
  if (!p.idColeccion || p.idColeccion === props.inicial.idColeccion) return '';
  return colecciones.value.find((c) => c.idColeccion === p.idColeccion)?.nombre || 'otra colección';
}

function agregar(p) {
  seleccionados.value.push({ idProducto: p.idProducto, nombre: p.nombre, imagenUrl: p.imagenUrl });
  busqueda.value = '';
}
const quitar = (p) => { seleccionados.value = seleccionados.value.filter((x) => x.idProducto !== p.idProducto); };

function enviarFormulario() {
  if (!nombre.value.trim()) return alert('Escribe el nombre de la colección.');
  if (!archivo.value && !props.inicial.imagenUrl) return alert('Añade la portada de la colección (2000x2000 px, .webp).');

  const f = new FormData();
  f.append('coleccion', new Blob([JSON.stringify({
    nombre: nombre.value.trim(),
    descripcion: descripcion.value.trim(),
    idsProductos: seleccionados.value.map((p) => p.idProducto)
  })], { type: 'application/json' }));
  if (archivo.value) f.append('imagen', archivo.value);
  emit('enviar', f);
}
</script>

<style scoped>
.campo { background: #000; color: #fff; border-color: #555; }
.campo:focus { background: #000; color: #fff; }
.miniatura { width: 56px; height: 56px; object-fit: cover; }
.resultados { max-height: 420px; overflow-y: auto; }
.resultado { background: transparent; border: 0; border-bottom: 1px solid #333; padding: 0.6rem 1rem; color: inherit; }
.resultado:hover { background: #1c1c1c; }
</style>
