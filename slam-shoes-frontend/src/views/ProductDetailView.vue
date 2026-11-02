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

    <!-- Contenido Principal Estilo Way of Wade -->
    <div class="container-fluid my-4 px-4 flex-grow-1">
      <div v-if="cargando" class="text-center text-muted py-5">
        <p>Cargando detalles del sneaker...</p>
      </div>

      <div v-else-if="producto" class="row g-4">
        
        <!-- COLUMNA IZQUIERDA: Miniaturas verticales en columna + Imagen Principal con opción de carrusel -->
        <div class="col-lg-7 d-flex gap-3">
          <!-- Miniaturas verticales ordenadas por 'orden' -->
          <div class="d-flex flex-column gap-2 overflow-auto" style="max-height: 550px; min-width: 80px;">
            <div 
              v-for="(img, index) in galeriaImagenes" 
              :key="img.idImagen || index" 
              class="border rounded p-1 bg-black cursor-pointer thumbnail-box"
              :class="imagenSeleccionada === img.urlImagen ? 'border-light active-thumb' : 'border-secondary'"
              style="width: 75px; height: 75px;"
              @click="seleccionarImagen(img.urlImagen)"
            >
              <img :src="img.urlImagen" class="w-100 h-100 object-fit-cover rounded" :alt="'Vista ' + img.orden" />
            </div>
          </div>

          <!-- Imagen Principal Ampliada sin espacios negros vacíos -->
          <div class="flex-grow-1 bg-black border border-secondary p-3 rounded-3 d-flex align-items-center justify-content-center shadow position-relative" style="min-height: 550px; overflow: hidden;">
            <!-- Botones de carrusel en la imagen principal -->
            <button class="btn btn-dark position-absolute start-0 ms-2 rounded-circle text-white shadow-lg opacity-75" @click="cambiarImagen(-1)" style="z-index: 2;">
              <i class="bi bi-chevron-left"></i>
            </button>
            <img :src="imagenSeleccionada || producto.imagenUrl" class="img-fluid rounded w-100 h-100" style="object-fit: contain; max-height: 500px;" :alt="producto.nombre" />
            <button class="btn btn-dark position-absolute end-0 me-2 rounded-circle text-white shadow-lg opacity-75" @click="cambiarImagen(1)" style="z-index: 2;">
              <i class="bi bi-chevron-right"></i>
            </button>
          </div>
        </div>

        <!-- COLUMNA DERECHA: Información, Estilos y Cuadrícula de Tallas -->
        <div class="col-lg-5 ps-lg-4">
          <span class="badge bg-secondary text-uppercase mb-2">{{ producto.marca }}</span>
          <h1 class="text-white fw-bold mb-2">{{ producto.nombre }}</h1>

          <h3 class="text-success fw-bold mb-3">${{ producto.precio }} MXN</h3>
          <p class="text-secondary small mb-4">{{ producto.descripcion }}</p>

          <hr class="border-secondary mb-4">

          <!-- Estilo / Color actual (Miniatura de variante) -->
          <div class="mb-4">
            <span class="text-muted small text-uppercase d-block mb-2">STYLE: {{ producto.nombre }}</span>
            <div class="d-flex gap-2">
              <div class="border border-light p-1 rounded bg-black" style="width: 60px; height: 60px;">
                <img :src="producto.imagenUrl" class="w-100 h-100 object-fit-cover rounded">
              </div>
            </div>
          </div>

          <!-- Selección de Tallas Estilo Way of Wade (Rectángulos en CM) -->
          <div class="mb-4">
            <div class="d-flex justify-content-between align-items-center mb-2">
              <label class="text-light fw-semibold small">
                TALLA: <span class="text-muted">{{ tallaSeleccionada ? tallaSeleccionada.talla + ' cm (' + tallaSeleccionada.color + ')' : 'Por favor seleccione una talla' }}</span>
              </label>
              <a href="#" class="text-secondary small text-decoration-underline" @click.prevent="alert('Guía de tallas estandarizada en centímetros (CM).')">Guía de Tallas</a>
            </div>

            <!-- Cuadrícula de Tallas en Rectángulos -->
            <div class="row g-2" v-if="tallasDisponibles.length > 0">
              <div class="col-4" v-for="variante in tallasDisponibles" :key="variante.idVariante">
                <button 
                  class="btn w-100 py-2 size-rect-btn text-center"
                  :class="tallaSeleccionada?.idVariante === variante.idVariante ? 'btn-light fw-bold shadow' : 'btn-outline-secondary text-light bg-black'"
                  @click="tallaSeleccionada = variante"
                >
                  {{ variante.talla }} cm
                </button>
              </div>
            </div>
            <div v-else class="text-danger small">
              No hay tallas o stock registrado para este producto.
            </div>
          </div>

          <!-- Estado de Stock en tiempo real -->
          <div class="mb-4">
            <div v-if="tallaSeleccionada">
              <span v-if="tallaSeleccionada.stock > 0" class="text-success small fw-semibold">
                <i class="bi bi-check-circle-fill me-1"></i> En stock (Disponibles: {{ tallaSeleccionada.stock }} unidades)
              </span>
              <span v-else class="text-danger small fw-semibold">
                <i class="bi bi-x-circle-fill me-1"></i> Agotado en esta talla
              </span>
            </div>
          </div>

          <!-- Botón de Añadir al Carrito -->
          <button 
            class="btn btn-light w-100 py-3 fw-bold shadow text-uppercase" 
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

<script>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import { verificarSesionYAccionar } from '@/utils/auth.js';

export default {
  name: 'ProductDetailView',
  setup() {
    const route = useRoute();
    const router = useRouter();

    const producto = ref(null);
    const galeriaImagenes = ref([]);
    const tallasDisponibles = ref([]);
    const tallaSeleccionada = ref(null);
    const imagenSeleccionada = ref('');
    const indiceActualImagen = ref(0);
    const cargando = ref(true);

    const idProducto = route.params.id;

    const cargarDatosProducto = async () => {
      try {
        // 1. Datos principales del producto
        const resProducto = await axios.get(`http://localhost:8080/api/productos/${idProducto}`);
        producto.value = resProducto.data;
        imagenSeleccionada.value = producto.value.imagenUrl;

        // 2. Tallas y stock desde la tabla producto_tallas
        const resTallas = await axios.get(`http://localhost:8080/api/producto-tallas/producto/${idProducto}`).catch(() => ({ data: [] }));
        tallasDisponibles.value = resTallas.data;

        // 3. Imágenes adicionales ordenadas estrictamente por el campo 'orden'
        const resImgs = await axios.get(`http://localhost:8080/api/producto-imagenes/producto/${idProducto}`).catch(() => ({ data: [] }));
        
        let imagenesOrdenadas = resImgs.data.sort((a, b) => a.orden - b.orden);
        
        // Si no hay imágenes en la tabla producto_imagenes, usamos la principal por defecto
        if (imagenesOrdenadas.length === 0 && producto.value.imagenUrl) {
          imagenesOrdenadas = [{ urlImagen: producto.value.imagenUrl, orden: 1 }];
        }
        
        galeriaImagenes.value = imagenesOrdenadas;
        if (galeriaImagenes.value.length > 0) {
          imagenSeleccionada.value = galeriaImagenes.value[0].urlImagen;
        }

      } catch (error) {
        console.error("Error al cargar los detalles del producto:", error);
      } finally {
        cargando.value = false;
      }
    };

    const seleccionarImagen = (url) => {
      imagenSeleccionada.value = url;
      indiceActualImagen.value = galeriaImagenes.value.findIndex(img => img.urlImagen === url);
    };

    const cambiarImage = (direccion) => {
      if (galeriaImagenes.value.length === 0) return;
      indiceActualImagen.value = (indiceActualImagen.value + direccion + galeriaImagenes.value.length) % galeriaImagenes.value.length;
      imagenSeleccionada.value = galeriaImagenes.value[indiceActualImagen.value].urlImagen;
    };

    const agregarAlCarritoConTalla = () => {
      const usuarioActivo = verificarSesionYAccionar();
      if (!usuarioActivo) return;

      if (!tallaSeleccionada.value) {
        alert("Por favor selecciona una talla.");
        return;
      }

      let carrito = JSON.parse(localStorage.getItem('slam_shoes_cart')) || [];
      const itemExistente = carrito.find(item => item.idVariante === tallaSeleccionada.value.idVariante);

      if (itemExistente) {
        itemExistente.cantidad += 1;
      } else {
        carrito.push({
          id: producto.value.idProducto,
          idVariante: tallaSeleccionada.value.idVariante,
          nombre: `${producto.value.nombre} (Talla: ${tallaSeleccionada.value.talla} CM)`,
          precio: producto.value.precio,
          cantidad: 1
        });
      }

      localStorage.setItem('slam_shoes_cart', JSON.stringify(carrito));
      alert(`¡"${producto.value.nombre}" se añadió a tu carrito con éxito!`);
    };

    onMounted(() => {
      cargarDatosProducto();
    });

    return {
      producto,
      galeriaImagenes,
      tallasDisponibles,
      tallaSeleccionada,
      imagenSeleccionada,
      cargando,
      seleccionarImagen,
      cambiarImagen: cambiarImage,
      agregarAlCarritoConTalla
    };
  }
};
</script>

<style scoped>
.thumbnail-box {
  cursor: pointer;
  opacity: 0.6;
  transition: all 0.2s ease;
}
.thumbnail-box:hover, .active-thumb {
  opacity: 1;
  border-color: #fff !important;
}
.size-rect-btn {
  border-radius: 4px;
  font-size: 0.85rem;
  transition: all 0.2s ease-in-out;
}
.fade-img {
  transition: opacity 0.3s ease-in-out;
}
</style>