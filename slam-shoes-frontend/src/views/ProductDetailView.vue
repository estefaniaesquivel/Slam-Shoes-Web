<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">

    <!-- Encabezado (Navbar) -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark border-bottom border-secondary w-100 px-4">
        <div class="container-fluid px-0">
            <router-link class="navbar-brand" to="/">
                <img src="/logo-slamshoes-blanco.png" alt="Slam Shoes Logo" style="height: 45px; width: auto;" class="object-contain">         
            </router-link>
            
            <div class="d-flex align-items-center gap-3">
                <router-link to="/" class="text-light fs-5 text-decoration-none"><i class="bi bi-house"></i> Volver al inicio</router-link>
            </div>
        </div>
    </nav>

    <!-- Contenido del Detalle del Producto -->
    <div class="container my-5 flex-grow-1">
      <div v-if="cargando" class="text-center text-muted py-5">
        <p>Cargando detalles del sneaker...</p>
      </div>

      <div v-else-if="producto" class="row g-5 align-items-center">
        <!-- Columna Izquierda: Imagen Principal e Imágenes Adicionales -->
        <div class="col-md-6 text-center">
          <div class="bg-black border border-secondary p-4 rounded-3 mb-3 shadow" style="height: 400px; display: flex; align-items: center; justify-content: center;">
            <img :src="imagenSeleccionada || producto.imagenUrl" class="img-fluid object-fit-contain rounded" style="max-height: 350px;" :alt="producto.nombre" />
          </div>
          
          <!-- Galería de imágenes adicionales (si las hay) -->
          <div class="d-flex gap-2 justify-content-center overflow-auto pb-2" v-if="galeriaImagenes.length > 0">
            <div 
              v-for="(img, index) in galeriaImagenes" 
              :key="index" 
              class="border border-secondary rounded p-1 bg-black cursor-pointer"
              style="width: 70px; height: 70px; cursor: pointer;"
              @click="imagenSeleccionada = img.urlImagen"
            >
              <img :src="img.urlImagen" class="w-100 h-100 object-fit-cover rounded" />
            </div>
          </div>
        </div>

        <!-- Columna Derecha: Información, Tallas y Stock -->
        <div class="col-md-6">
          <span class="badge bg-secondary text-uppercase mb-2">{{ producto.marca }}</span>
          <h1 class="text-white fw-bold mb-3">{{ producto.nombre }}</h1>
          <h3 class="text-success fw-bold mb-4">${{ producto.precio }} MXN</h3>

          <p class="text-secondary mb-4">{{ producto.descripcion }}</p>

          <!-- Selector de Tallas y Colores -->
          <div class="mb-4" v-if="tallasDisponibles.length > 0">
            <label class="form-label text-light fw-semibold">Selecciona tu Talla:</label>
            <div class="d-flex flex-wrap gap-2">
              <button 
                v-for="variante in tallasDisponibles" 
                :key="variante.idVariante"
                class="btn btn-sm"
                :class="tallaSeleccionada?.idVariante === variante.idVariante ? 'btn-light fw-bold' : 'btn-outline-secondary text-light'"
                @click="tallaSeleccionada = variante"
              >
                {{ variante.talla }} <small>({{ variante.color }})</small>
              </button>
            </div>
          </div>

          <!-- Estado de Stock -->
          <div class="mb-4">
            <span v-if="tallaSeleccionada">
              <span v-if="tallaSeleccionada.stock > 0" class="text-success fw-semibold">
                <i class="bi bi-check-circle-fill me-1"></i> En stock (Disponibles: {{ tallaSeleccionada.stock }})
              </span>
              <span v-else class="text-danger fw-semibold">
                <i class="bi bi-x-circle-fill me-1"></i> Agotado para esta talla
              </span>
            </span>
            <span v-else class="text-muted small">
              Selecciona una talla para ver la disponibilidad.
            </span>
          </div>

          <!-- Botón de compra / carrito -->
          <button 
            class="btn btn-light w-100 py-3 fw-bold shadow" 
            :disabled="!tallaSeleccionada || tallaSeleccionada.stock <= 0"
            @click="agregarAlCarritoConTalla"
          >
            Añadir al carrito
          </button>
        </div>
      </div>

      <div v-else class="text-center text-danger py-5">
        <h4>No se encontró el producto solicitado.</h4>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { verificarSesionYAccionar } from '@/utils/auth.js'

const route = useRoute()
const router = useRouter()

const producto = ref(null)
const galeriaImagenes = ref([])
const tallasDisponibles = ref([])
const tallaSeleccionada = ref(null)
const imagenSeleccionada = ref('')
const cargando = ref(true)

const idProducto = route.params.id

async function cargarDatosProducto() {
  try {
    // 1. Obtener datos principales del producto
    const resProducto = await axios.get(`http://localhost:8080/api/productos/${idProducto}`)
    producto.value = resProducto.data

    // 2. Obtener tallas / stock (puedes ajustar el endpoint según lo tengas en tu backend)
    const resTallas = await axios.get(`http://localhost:8080/api/producto-tallas/producto/${idProducto}`).catch(() => ({ data: [] }))
    tallasDisponibles.value = resTallas.data

    // 3. Obtener imágenes adicionales (galería)
    const resImgs = await axios.get(`http://localhost:8080/api/producto-imagenes/producto/${idProducto}`).catch(() => ({ data: [] }))
    galeriaImagenes.value = resImgs.data

  } catch (error) {
    console.error("Error al cargar los detalles:", error)
  } finally {
    cargando.value = false
  }
}

function agregarAlCarritoConTalla() {
  const usuarioActivo = verificarSesionYAccionar()
  if (!usuarioActivo) return

  if (!tallaSeleccionada.value) {
    alert("Por favor selecciona una talla.")
    return
  }

  // Lógica de almacenamiento local del carrito
  let carrito = JSON.parse(localStorage.getItem('slam_shoes_cart')) || []
  const itemExistente = carrito.find(item => item.idVariante === tallaSeleccionada.value.idVariante)

  if (itemExistente) {
    itemExistente.cantidad += 1
  } else {
    carrito.push({
      id: producto.value.idProducto,
      idVariante: tallaSeleccionada.value.idVariante,
      nombre: `${producto.value.nombre} (Talla: ${tallaSeleccionada.value.talla})`,
      precio: producto.value.precio,
      cantidad: 1
    })
  }

  localStorage.setItem('slam_shoes_cart', JSON.stringify(carrito))
  alert(`¡"${producto.value.nombre}" se añadió a tu carrito con éxito!`)
}

onMounted(() => {
  cargarDatosProducto()
})
</script>