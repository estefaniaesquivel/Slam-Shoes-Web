<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">

    <NavbarPrincipal />

    <!-- Contenido Principal -->
    <div class="container-fluid my-4 px-4 flex-grow-1">
      <div v-if="cargando" class="text-center text-muted py-5">
        <p>Cargando detalles del sneaker...</p>
      </div>

      <div v-else-if="producto" class="row g-4">

        <!-- COLUMNA IZQUIERDA: Galería (miniaturas + imagen principal 1:1) -->
        <div class="col-lg-7">
          <div class="gallery">

            <!-- Miniaturas: misma altura que la imagen principal, con scroll interno -->
            <div class="thumbs-wrapper">
              <div class="thumbs-scroll">
                <div
                  v-for="(img, index) in galeriaImagenes"
                  :key="img.idImagen || index"
                  class="thumbnail-box"
                  :class="imagenSeleccionada === img.urlImagen ? 'active-thumb' : ''"
                  @click="seleccionarImagen(img.urlImagen)"
                >
                  <img :src="img.urlImagen" class="img-fill" :alt="'Vista ' + (img.orden || index + 1)" />
                </div>
              </div>
            </div>

            <!-- Imagen principal: SIEMPRE cuadrada 1:1 (las imágenes son 2000x2000) -->
            <div class="main-square-box">
              <img :src="imagenSeleccionada || producto.imagenUrl" class="img-fill" :alt="producto.nombre" />

              <button
                class="btn btn-dark nav-arrow nav-arrow-left rounded-circle text-white shadow-lg opacity-75"
                @click="cambiarImagen(-1)"
              >
                <i class="bi bi-chevron-left"></i>
              </button>
              <button
                class="btn btn-dark nav-arrow nav-arrow-right rounded-circle text-white shadow-lg opacity-75"
                @click="cambiarImagen(1)"
              >
                <i class="bi bi-chevron-right"></i>
              </button>
            </div>

          </div>
        </div>

        <!-- COLUMNA DERECHA: Información, Categoría, Tallas -->
        <div class="col-lg-5 ps-lg-4">
          <span class="badge bg-secondary text-uppercase mb-2">{{ producto.marca }}</span>
          <h1 class="text-white fw-bold mb-2">{{ producto.nombre }}</h1>

          <div class="mb-3 p-3 bg-black border border-secondary rounded" v-if="categoriaInfo.nombre">
            <span class="text-warning small text-uppercase fw-bold d-block mb-1">
              CATEGORÍA: {{ categoriaInfo.nombre }}
            </span>
            <p class="text-secondary small mb-0">{{ categoriaInfo.descripcion || producto.descripcion }}</p>
          </div>

          <h3 class="text-success fw-bold mb-3">
            ${{ precioFinal }} MXN
            <template v-if="ofertaVigente">
              <span class="text-secondary text-decoration-line-through fs-6 ms-2">${{ producto.precio }}</span>
              <span class="badge bg-danger ms-2 fs-6">-{{ ofertaVigente.porcentajeDescuento }}%</span>
              <span v-if="ofertaVigente.nombre" class="badge bg-secondary ms-1 fs-6">{{ ofertaVigente.nombre }}</span>
            </template>
          </h3>

          <hr class="border-secondary mb-4">

          <!-- Estilo / Color actual -->
          <div class="mb-4">
            <span class="text-muted small text-uppercase d-block mb-2">STYLE: {{ producto.nombre }}</span>
            <div class="d-flex gap-2">
              <div class="style-box">
                <img :src="producto.imagenUrl" class="img-fill" :alt="producto.nombre">
              </div>
            </div>
          </div>

          <!-- Selección de Tallas (CM) -->
          <div class="mb-4">
            <div class="d-flex justify-content-between align-items-center mb-2">
              <label class="text-light fw-semibold small">
                TALLA:
                <span class="text-muted">
                  {{ tallaSeleccionada ? tallaSeleccionada.talla + ' cm' : 'Por favor seleccione una talla' }}
                </span>
              </label>
              <a href="#" class="text-secondary small text-decoration-underline" @click.prevent="mostrarGuiaTallas">
                Guía de Tallas
              </a>
            </div>

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

          <!-- Estado de Stock -->
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

    <!-- Componente del Carrito Desplegable -->
    <CarritoComponent />

  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';
import { verificarSesionYAccionar } from '@/utils/auth.js';
import CarritoComponent from '@/components/CarritoComponent.vue';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';

export default {
  name: 'ProductDetailView',
  components: {
    CarritoComponent,
    NavbarPrincipal
  },
  setup() {
    const route = useRoute();

    const producto = ref(null);
    const ofertaVigente = ref(null); // oferta activa del producto (tabla ofertas)
    const precioFinal = computed(() => (ofertaVigente.value ? ofertaVigente.value.precioConDescuento : producto.value?.precio));
    const categoriaInfo = ref({});
    const galeriaImagenes = ref([]);
    const tallasDisponibles = ref([]);
    const tallaSeleccionada = ref(null);
    const imagenSeleccionada = ref('');
    const indiceActualImagen = ref(0);
    const cargando = ref(true);

    const idProducto = route.params.id;

    const cargarDatosProducto = async () => {
      try {
        const resProducto = await axios.get(`http://localhost:8080/api/productos/${idProducto}`);
        producto.value = resProducto.data;
        imagenSeleccionada.value = producto.value.imagenUrl;

        // Oferta vigente (si existe): 404 = sin oferta
        const resOferta = await axios
          .get(`http://localhost:8080/api/ofertas/producto/${idProducto}`)
          .catch(() => ({ data: null }));
        ofertaVigente.value = resOferta.data;

        if (producto.value.idCategoria) {
          const resCat = await axios
            .get(`http://localhost:8080/api/categorias/${producto.value.idCategoria}`)
            .catch(() => ({ data: {} }));
          categoriaInfo.value = resCat.data;
        }

        const resTallas = await axios
          .get(`http://localhost:8080/api/producto-tallas/producto/${idProducto}`)
          .catch(() => ({ data: [] }));
        tallasDisponibles.value = resTallas.data;

        const resImgs = await axios
          .get(`http://localhost:8080/api/producto-imagenes/producto/${idProducto}`)
          .catch(() => ({ data: [] }));
        let imagenesOrdenadas = [...resImgs.data].sort((a, b) => a.orden - b.orden);

        if (imagenesOrdenadas.length === 0 && producto.value.imagenUrl) {
          imagenesOrdenadas = [{ urlImagen: producto.value.imagenUrl, orden: 1 }];
        }

        galeriaImagenes.value = imagenesOrdenadas;
        if (galeriaImagenes.value.length > 0) {
          imagenSeleccionada.value = galeriaImagenes.value[0].urlImagen;
          indiceActualImagen.value = 0;
        }

      } catch (error) {
        console.error("Error al cargar los detalles del producto:", error);
      } finally {
        cargando.value = false;
      }
    };

    const seleccionarImagen = (url) => {
      imagenSeleccionada.value = url;
      const idx = galeriaImagenes.value.findIndex(img => img.urlImagen === url);
      indiceActualImagen.value = idx >= 0 ? idx : 0;
    };

    const cambiarImagen = (direccion) => {
      if (galeriaImagenes.value.length === 0) return;
      const total = galeriaImagenes.value.length;
      indiceActualImagen.value = (indiceActualImagen.value + direccion + total) % total;
      imagenSeleccionada.value = galeriaImagenes.value[indiceActualImagen.value].urlImagen;
    };

    const mostrarGuiaTallas = () => {
      alert('Guía de tallas estandarizada en centímetros (CM).');
    };

    const agregarAlCarritoConTalla = async () => {
      const usuarioActivo = verificarSesionYAccionar();
      if (!usuarioActivo) return;

      if (!tallaSeleccionada.value) {
        alert("Por favor selecciona una talla.");
        return;
      }

      const idUsuario = usuarioActivo.idUsuario || usuarioActivo.id || usuarioActivo.id_usuario;
      if (!idUsuario) {
        alert("Error: No se encontró el ID del usuario activo en la sesión.");
        return;
      }

      try {
        const resCarrito = await axios.get(`http://localhost:8080/api/carrito/usuario/${idUsuario}`);
        const idCarrito = resCarrito.data.idCarrito;

        let carrito = JSON.parse(localStorage.getItem('slam_shoes_cart')) || [];
        const itemExistente = carrito.find(item => item.idVariante === tallaSeleccionada.value.idVariante);

        if (itemExistente) {
          itemExistente.cantidad += 1;
        } else {
          carrito.push({
            id: producto.value.idProducto,
            idVariante: tallaSeleccionada.value.idVariante,
            nombre: producto.value.nombre,
            precio: precioFinal.value,
            imagenUrl: producto.value.imagenUrl,
            talla: tallaSeleccionada.value.talla,
            color: tallaSeleccionada.value.color,
            cantidad: 1
          });
        }

        localStorage.setItem('slam_shoes_cart', JSON.stringify(carrito));

        const itemsParaBD = carrito.map(item => ({
          idCarrito: idCarrito,
          idVariante: item.idVariante,
          cantidad: item.cantidad
        }));

        await axios.post(`http://localhost:8080/api/carrito/${idCarrito}/sincronizar`, itemsParaBD);

        window.dispatchEvent(new Event('storage'));
        alert(`¡"${producto.value.nombre}" se añadió a tu carrito con éxito!`);

      } catch (error) {
        console.error("Error detallado al sincronizar carrito con el backend:", error.response?.data || error.message);
        alert("No se pudo guardar el producto en el carrito del servidor. Revisa la consola para más detalles.");
      }
    };

    onMounted(() => {
      cargarDatosProducto();
    });

    return {
      producto,
      categoriaInfo,
      ofertaVigente,
      precioFinal,
      galeriaImagenes,
      tallasDisponibles,
      tallaSeleccionada,
      imagenSeleccionada,
      cargando,
      seleccionarImagen,
      cambiarImagen,
      mostrarGuiaTallas,
      agregarAlCarritoConTalla
    };
  }
};
</script>

<style scoped>
/* Todas las imágenes (2000x2000) llenan su contenedor cuadrado sin bordes ni recortes:
   como la fuente ya es 1:1, "cover" en una caja 1:1 no corta nada. */
.img-fill {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* Galería: la altura la define SOLO la imagen principal (cuadrada) */
.gallery {
  display: flex;
  align-items: stretch;
  gap: 1rem;
  width: 100%;
  max-width: 700px;
}

/* Imagen principal: cuadrada perfecta, sin padding, sin estirarse */
.main-square-box {
  position: relative;
  flex: 1 1 auto;
  min-width: 0;
  aspect-ratio: 1 / 1;
  background: #fff;
  border: 1px solid #6c757d;
  border-radius: 0.5rem;
  overflow: hidden;
  box-shadow: 0 .125rem .25rem rgba(0, 0, 0, .075);
}

/* Columna de miniaturas: no aporta altura, copia la de la imagen principal */
.thumbs-wrapper {
  position: relative;
  flex: 0 0 84px;
  width: 84px;
}
.thumbs-scroll {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 2px;
}

/* Miniaturas cuadradas, sin padding */
.thumbnail-box {
  flex: 0 0 auto;
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #fff;
  border: 1px solid #6c757d;
  border-radius: 0.375rem;
  overflow: hidden;
  cursor: pointer;
  opacity: 0.7;
  transition: all 0.2s ease;
}
.thumbnail-box:hover,
.active-thumb {
  opacity: 1;
  border-color: #fff;
}
.active-thumb {
  border-width: 2px;
}

/* Cuadro de estilo actual */
.style-box {
  width: 60px;
  height: 60px;
  background: #fff;
  border: 1px solid #f8f9fa;
  border-radius: 0.375rem;
  overflow: hidden;
}

/* Flechas centradas verticalmente sobre la imagen */
.nav-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  width: 40px;
  height: 40px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.nav-arrow-left { left: 0.5rem; }
.nav-arrow-right { right: 0.5rem; }

.size-rect-btn {
  border-radius: 4px;
  font-size: 0.85rem;
  transition: all 0.2s ease-in-out;
}

/* Móvil: miniaturas debajo de la imagen */
@media (max-width: 575.98px) {
  .gallery {
    flex-direction: column-reverse;
  }
  .thumbs-wrapper {
    flex: 0 0 auto;
    width: 100%;
    height: 72px;
  }
  .thumbs-scroll {
    flex-direction: row;
    overflow-x: auto;
    overflow-y: hidden;
  }
  .thumbnail-box {
    width: 72px;
    height: 72px;
  }
}
</style>