<template>
  <form @submit.prevent="enviar">

    <!-- 1. Datos principales -->
    <section class="bloque">
      <h5 class="mb-3">Datos del producto</h5>
      <div class="row g-3">
        <div class="col-md-8">
          <label class="form-label small text-secondary">Nombre</label>
          <input type="text" class="form-control campo" v-model="nombre" required />
          <div class="form-text text-secondary">Con este nombre se crea la carpeta de imágenes en Supabase Storage.</div>
        </div>
        <div class="col-md-4">
          <label class="form-label small text-secondary">Precio (MXN)</label>
          <input type="number" min="0.01" step="0.01" class="form-control campo" v-model.number="precio" required />
        </div>
        <div class="col-md-6">
          <label class="form-label small text-secondary">Categoría</label>
          <select class="form-select campo" v-model="idCategoria" required>
            <option value="">Selecciona una categoría</option>
            <option v-for="c in categorias" :key="c.idCategoria" :value="c.idCategoria">{{ c.nombre }}</option>
          </select>
        </div>
        <div class="col-md-6">
          <label class="form-label small text-secondary">Marca</label>
          <select class="form-select campo" v-model="idMarca" required>
            <option value="">Selecciona una marca</option>
            <option v-for="m in marcas" :key="m.idMarca" :value="m.idMarca">{{ m.nombre }}</option>
          </select>
        </div>
        <div class="col-12">
          <label class="form-label small text-secondary">Descripción (opcional)</label>
          <textarea class="form-control campo" rows="2" v-model="descripcion"></textarea>
        </div>
      </div>
    </section>

    <!-- 2. Colección y jugador (opcionales, pero deben existir) -->
    <section class="bloque">
      <h5 class="mb-3">Colección y jugador</h5>
      <div class="row g-3">
        <div class="col-md-6">
          <label class="form-label small text-secondary">Nombre de la colección (opcional)</label>
          <input type="text" class="form-control campo" list="lista-colecciones" v-model="coleccion" />
          <datalist id="lista-colecciones"><option v-for="c in colecciones" :key="c.idColeccion" :value="c.nombre"></option></datalist>
        </div>
        <div class="col-md-6">
          <label class="form-label small text-secondary">Nombre del jugador (opcional)</label>
          <input type="text" class="form-control campo" list="lista-jugadores" v-model="jugador" />
          <datalist id="lista-jugadores"><option v-for="j in jugadores" :key="j.idJugador" :value="j.nombre"></option></datalist>
        </div>
      </div>
      <div class="form-text text-secondary">Déjalos vacíos si el producto no pertenece a ninguna colección o jugador.</div>
    </section>

    <!-- 3. Colores -->
    <section class="bloque">
      <h5 class="mb-3">Colores</h5>
      <div class="colores-grid">
        <button
          v-for="c in coloresDisponibles" :key="c.nombre" type="button"
          class="swatch" :class="{ activo: coloresSel.includes(c.nombre) }"
          :style="{ background: c.fondo }" :data-nombre="c.nombre" :aria-label="c.nombre"
          @click="alternarColor(c.nombre)"
        >
          <i v-if="coloresSel.includes(c.nombre)" class="bi bi-check-lg" :class="c.claro ? 'text-dark' : 'text-white'"></i>
        </button>
      </div>
      <div class="form-text text-secondary">Puedes elegir varios. Pasa el cursor sobre un color para ver su nombre.</div>
    </section>

    <!-- 4. Tallas por color -->
    <section class="bloque" v-for="color in coloresSel" :key="'t-' + color">
      <h5 class="mb-3 d-flex align-items-center gap-2">
        <span class="punto" :style="{ background: fondoDe(color) }"></span> Tallas disponibles: {{ color }}
      </h5>
      <div class="tallas-grid">
        <button v-for="t in TALLAS" :key="t" type="button" class="btn btn-sm"
          :class="tallasPorColor[color][t] !== undefined ? 'btn-light' : 'btn-outline-secondary'"
          @click="alternarTalla(color, t)">{{ t }}</button>
      </div>
      <div class="mt-3 d-flex flex-wrap gap-3" v-if="tallasOrdenadas(color).length">
        <div v-for="t in tallasOrdenadas(color)" :key="t" class="d-flex align-items-center gap-2">
          <span class="badge bg-secondary rounded-0">{{ t }} cm</span>
          <label class="small text-secondary mb-0">Stock</label>
          <input type="number" min="0" class="form-control form-control-sm campo" style="width: 80px;" v-model.number="tallasPorColor[color][t]" />
        </div>
      </div>
      <div v-else class="form-text text-warning">Selecciona al menos una talla para este color.</div>
    </section>

    <!-- 5. Imágenes en orden (se habilita cuando hay un color con tallas) -->
    <section class="bloque" v-if="hayTallas">
      <h5 class="mb-0">Ingresar imágenes en orden</h5>
      <p class="text-secondary small mb-3">Las imágenes deben medir 2000x2000 px en formato .webp</p>

      <div class="d-flex flex-wrap align-items-start gap-3">
        <div v-for="(img, i) in imagenes" :key="i" class="slot text-center">
          <label class="slot-caja" :class="{ 'con-imagen': img.preview }">
            <img v-if="img.preview" :src="img.preview" alt="Vista previa" />
            <template v-else>
              <i class="bi bi-plus-lg fs-2"></i>
              <span class="small">Subir imagen</span>
            </template>
            <input type="file" accept=".webp,image/webp" class="d-none" @change="seleccionarArchivo(i, $event)" />
          </label>
          <div class="small text-secondary mt-1">Orden {{ i + 1 }}</div>
          <button v-if="imagenes.length > 1" type="button" class="btn btn-link btn-sm text-danger p-0" @click="quitarSlot(i)">Quitar</button>
        </div>

        <button type="button" class="btn btn-outline-light align-self-center" @click="agregarSlot">
          <i class="bi bi-plus me-1"></i>Añadir otra foto
        </button>
      </div>
    </section>

    <!-- 6. Resto de campos -->
    <section class="bloque">
      <h5 class="mb-3">Otros datos</h5>
      <div class="row g-3 align-items-end">
        <div class="col-md-4">
          <label class="form-label small text-secondary d-block">Estado</label>
          <div class="btn-group" role="group">
            <button type="button" class="btn" :class="activo ? 'btn-light' : 'btn-outline-secondary'" @click="activo = true">Activo</button>
            <button type="button" class="btn" :class="!activo ? 'btn-light' : 'btn-outline-secondary'" @click="activo = false">Inactivo</button>
          </div>
        </div>
        <div class="col-md-4">
          <label class="form-label small text-secondary">Modelo</label>
          <input type="text" class="form-control campo" v-model="modelo" />
        </div>
        <div class="col-md-4">
          <button type="button" class="btn w-100" :class="esTendencia ? 'btn-danger' : 'btn-outline-secondary'" @click="esTendencia = !esTendencia">
            <i :class="esTendencia ? 'bi bi-check-circle-fill me-1' : 'bi bi-circle me-1'"></i>Marcar como tendencia
          </button>
        </div>
      </div>
    </section>

    <button type="submit" class="btn btn-light btn-lg w-100 fw-bold" :disabled="enviando">
      {{ enviando ? 'Subiendo imágenes y guardando...' : textoBoton }}
    </button>
  </form>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import axios from 'axios';

const API = 'http://localhost:8080/api';

const props = defineProps({
  // 'crear' | 'editar'
  modo: { type: String, default: 'crear' },
  // Producto guardado (solo en modo editar): se usa para rellenar todos los campos
  inicial: { type: Object, default: null },
  textoBoton: { type: String, default: 'Añadir producto al catálogo' },
  enviando: { type: Boolean, default: false }
});
const emit = defineEmits(['enviar']);

// Colores de la paleta (mismo orden y tonos de la imagen de referencia)
const COLORES = [
  { nombre: 'Naranja', fondo: '#ff5722' },
  { nombre: 'Amarillo', fondo: '#ffc107' },
  { nombre: 'Verde', fondo: '#22c55e' },
  { nombre: 'Azul', fondo: '#6674f5' },
  { nombre: 'Rojo', fondo: '#c81e1e' },
  { nombre: 'Blanco', fondo: '#ffffff', claro: true },
  { nombre: 'Gris', fondo: '#6b7099' },
  { nombre: 'Negro', fondo: '#000000' },
  { nombre: 'Café', fondo: '#6b3a12' },
  { nombre: 'Morado', fondo: '#a855f7' },
  { nombre: 'Rosa', fondo: '#ec4899' },
  { nombre: 'Multicolor', fondo: 'linear-gradient(90deg, #ff5722, #ffc107, #22c55e, #6674f5, #a855f7, #ec4899)', claro: true }
];
// Colores de la paleta + los que ya tenga guardados el producto y no estén en la paleta
const coloresDisponibles = computed(() => {
  const extras = coloresSel.value
    .filter((n) => !COLORES.some((c) => c.nombre === n))
    .map((n) => ({ nombre: n, fondo: '#555555' }));
  return [...COLORES, ...extras];
});
const fondoDe = (nombre) => coloresDisponibles.value.find((c) => c.nombre === nombre)?.fondo;

// Tallas en cm: 22, 22.5, ... 32
const TALLAS = Array.from({ length: 21 }, (_, i) => String(22 + i * 0.5));
const STOCK_INICIAL = 10;

// Catálogos desde la BD
const categorias = ref([]);
const marcas = ref([]);
const colecciones = ref([]);
const jugadores = ref([]);

// Formulario
const nombre = ref('');
const precio = ref(null);
const idCategoria = ref('');
const idMarca = ref('');
const descripcion = ref('');
const coleccion = ref('');
const jugador = ref('');
const coloresSel = ref([]);
const tallasPorColor = reactive({}); // { Negro: { '26': 10, '26.5': 5 } }
const imagenes = ref([{ archivo: null, preview: '', url: null }]); // el orden es la posición + 1; url = imagen ya guardada
const activo = ref(true);
const modelo = ref('');
const esTendencia = ref(false);

onMounted(async () => {
  const pedir = (ruta) => axios.get(`${API}/${ruta}`).then((r) => r.data).catch(() => []);
  [categorias.value, marcas.value, colecciones.value, jugadores.value] = await Promise.all([
    pedir('categorias'), pedir('marcas'), pedir('colecciones'), pedir('jugadores')
  ]);
  if (props.inicial) cargarInicial(props.inicial);
});

// Rellena el formulario con lo que ya está guardado del producto (modo editar)
function cargarInicial(p) {
  nombre.value = p.nombre || '';
  precio.value = p.precio;
  idCategoria.value = p.idCategoria ?? '';
  idMarca.value = p.idMarca ?? '';
  descripcion.value = p.descripcion || '';
  coleccion.value = p.coleccion || '';
  jugador.value = p.jugador || '';
  activo.value = p.activo !== false;
  modelo.value = p.modelo || '';
  esTendencia.value = !!p.esTendencia;

  coloresSel.value = p.colores.map((c) => c.color);
  p.colores.forEach((c) => {
    tallasPorColor[c.color] = {};
    c.tallas.forEach((t) => { tallasPorColor[c.color][String(t.talla)] = t.stock; });
  });

  if (p.imagenes && p.imagenes.length) {
    imagenes.value = p.imagenes.map((i) => ({ archivo: null, preview: i.url, url: i.url }));
  }
}

// ---------- Colores y tallas ----------
function alternarColor(color) {
  if (coloresSel.value.includes(color)) {
    coloresSel.value = coloresSel.value.filter((c) => c !== color);
    delete tallasPorColor[color];
  } else {
    coloresSel.value.push(color);
    tallasPorColor[color] = {};
  }
}

function alternarTalla(color, talla) {
  if (tallasPorColor[color][talla] !== undefined) {
    delete tallasPorColor[color][talla];
  } else {
    tallasPorColor[color][talla] = STOCK_INICIAL;
  }
}

const tallasOrdenadas = (color) => Object.keys(tallasPorColor[color] || {}).sort((a, b) => parseFloat(a) - parseFloat(b));
const hayTallas = computed(() => coloresSel.value.some((c) => tallasOrdenadas(c).length > 0));

// ---------- Imágenes ----------
function medirImagen(archivo) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(archivo);
    const img = new Image();
    img.onload = () => resolve({ ancho: img.naturalWidth, alto: img.naturalHeight, url });
    img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('No se pudo leer la imagen')); };
    img.src = url;
  });
}

async function seleccionarArchivo(indice, evento) {
  const archivo = evento.target.files[0];
  evento.target.value = ''; // permite volver a elegir el mismo archivo
  if (!archivo) return;

  if (!(archivo.type === 'image/webp' || archivo.name.toLowerCase().endsWith('.webp'))) {
    alert('La imagen debe estar en formato .webp');
    return;
  }
  try {
    const { ancho, alto, url } = await medirImagen(archivo);
    if (ancho !== 2000 || alto !== 2000) {
      URL.revokeObjectURL(url);
      alert(`La imagen debe medir 2000x2000 px (la que elegiste mide ${ancho}x${alto} px).`);
      return;
    }
    if (imagenes.value[indice].archivo) URL.revokeObjectURL(imagenes.value[indice].preview);
    imagenes.value[indice] = { archivo, preview: url, url: null };
  } catch (e) {
    alert('No se pudo leer la imagen seleccionada.');
  }
}

function agregarSlot() {
  const ultima = imagenes.value[imagenes.value.length - 1];
  if (!ultima.archivo && !ultima.url) {
    alert('Primero sube la imagen del orden ' + imagenes.value.length + '.');
    return;
  }
  imagenes.value.push({ archivo: null, preview: '', url: null });
}

function quitarSlot(indice) {
  if (imagenes.value[indice].archivo) URL.revokeObjectURL(imagenes.value[indice].preview);
  imagenes.value.splice(indice, 1);
}

// ---------- Envío ----------
const mismoNombre = (a, b) => a.trim().toLowerCase() === b.trim().toLowerCase();

async function enviar() {
  // Colección y jugador: pueden quedar vacíos, pero si se escriben deben existir en la BD
  if (coleccion.value.trim() && !colecciones.value.some((c) => mismoNombre(c.nombre, coleccion.value))) {
    alert(`La colección "${coleccion.value}" no existe. Escribe el nombre de una colección registrada o deja el campo vacío.`);
    return;
  }
  if (jugador.value.trim() && !jugadores.value.some((j) => mismoNombre(j.nombre, jugador.value))) {
    alert(`El jugador "${jugador.value}" no existe. Escribe el nombre de un jugador registrado o deja el campo vacío.`);
    return;
  }

  if (coloresSel.value.length === 0) {
    alert('Selecciona al menos un color.');
    return;
  }
  const sinTallas = coloresSel.value.find((c) => tallasOrdenadas(c).length === 0);
  if (sinTallas) {
    alert(`Selecciona al menos una talla para el color ${sinTallas}.`);
    return;
  }
  if (imagenes.value.some((i) => !i.archivo && !i.url)) {
    alert('Hay recuadros de imagen vacíos. Sube todas las imágenes o quita los recuadros que sobren.');
    return;
  }

  const datos = {
    nombre: nombre.value,
    descripcion: descripcion.value,
    precio: precio.value,
    idCategoria: Number(idCategoria.value),
    idMarca: Number(idMarca.value),
    coleccion: coleccion.value.trim(),
    jugador: jugador.value.trim(),
    activo: activo.value,
    modelo: modelo.value,
    esTendencia: esTendencia.value,
    colores: coloresSel.value.map((color) => ({
      color,
      tallas: tallasOrdenadas(color).map((t) => ({ talla: t, stock: Number(tallasPorColor[color][t]) || 0 }))
    }))
  };

  // En modo editar, "imagenes" define la galería final en orden: url = se conserva, null = archivo nuevo
  if (props.modo === 'editar') {
    datos.imagenes = imagenes.value.map((i) => ({ url: i.url || null, orden: null }));
  }

  // El orden de los archivos nuevos en el FormData es el orden en que aparecen en la galería
  const formulario = new FormData();
  formulario.append('producto', new Blob([JSON.stringify(datos)], { type: 'application/json' }));
  imagenes.value.filter((i) => !i.url).forEach((i) => formulario.append('imagenes', i.archivo));

  emit('enviar', formulario);
}
</script>

<style scoped>
.bloque {
  background: #1a1a1a;
  border: 1px solid #333;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}
.campo {
  background: #2a2a2a;
  color: #fff;
  border-color: #444;
}
.campo:focus { background: #2a2a2a; color: #fff; border-color: #fff; box-shadow: none; }

/* Paleta de colores: 4 por fila, sin texto; el nombre aparece en una burbuja al pasar el cursor */
.colores-grid { display: grid; grid-template-columns: repeat(4, 56px); gap: 12px; }
.swatch {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 0;
}
.swatch.activo { outline: 3px solid #fff; outline-offset: 2px; }
.swatch::after {
  content: attr(data-nombre);
  position: absolute;
  bottom: 110%;
  left: 50%;
  transform: translateX(-50%);
  background: #fff;
  color: #000;
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 6px;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s ease;
  z-index: 5;
}
.swatch:hover::after { opacity: 1; }

.punto { display: inline-block; width: 18px; height: 18px; border-radius: 50%; border: 1px solid #fff; }
.tallas-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(56px, 1fr)); gap: 8px; }

.slot { width: 140px; }
.slot-caja {
  width: 140px;
  height: 140px;
  border: 2px dashed #666;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #aaa;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s ease;
}
.slot-caja:hover { border-color: #fff; color: #fff; }
.slot-caja.con-imagen { border-style: solid; background: #fff; }
.slot-caja img { width: 100%; height: 100%; object-fit: cover; }
</style>
