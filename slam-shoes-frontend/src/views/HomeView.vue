<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden">

    <!-- Encabezado (Navbar) compartido -->
    <NavbarPrincipal />

    <!-- Carrusel Principal (Banners) -->
    <div id="primerCarrusel" class="carousel slide my-4 container-fluid px-4" data-bs-ride="carousel" data-bs-interval="3000">
        <div class="carousel-inner">
            <div class="carousel-item active">
                <img src="/Nike JA 3.png" class="d-block w-100 object-fit-cover rounded shadow" style="max-height: 500px;" alt="Nike JA 3 - Cobalt Bliss">
            </div>
            <div class="carousel-item">
                <img src="/promo-halloween.png" class="d-block w-100 object-fit-cover rounded shadow" style="max-height: 500px; object-position: center;" alt="Promoción Halloween">
            </div>
        </div>
        <button class="carousel-control-prev" type="button" data-bs-target="#primerCarrusel" data-bs-slide="prev">
            <span class="carousel-control-prev-icon bg-secondary rounded p-3" aria-hidden="true"></span>
        </button>
        <button class="carousel-control-next" type="button" data-bs-target="#primerCarrusel" data-bs-slide="next">
            <span class="carousel-control-next-icon bg-secondary rounded p-3" aria-hidden="true"></span>
        </button>
    </div>

    <!-- Ofertas y Novedades -->
    <div class="container-fluid px-4 my-4">
        <div class="row g-3">
            <div class="col-md-6">
                <div class="border border-secondary bg-black p-5 text-center rounded">
                    <h4>[Ofertas]</h4>
                    <a href="#" class="btn btn-outline-light mt-3">[Enlace a Ofertas]</a>
                </div>
            </div>
            <div class="col-md-6">
                <div class="border border-secondary bg-black p-5 text-center rounded">
                    <h4>[Novedades]</h4>
                    <a href="#" class="btn btn-outline-light mt-3">[Enlace a Novedades]</a>
                </div>
            </div>
        </div>
    </div>

    <!-- SECCIÓN: COLECCIONES -->
    <div class="container-fluid py-5 my-4 border-top border-bottom border-secondary px-4">
        <div class="mb-4">
            <h3 class="text-center fw-bold">COLECCIONES</h3>
        </div>
        <div class="row g-4" v-if="colecciones.length > 0">
            <div class="col-md-3" v-for="col in colecciones" :key="col.idColeccion">
                <!-- Imagen 1:1 completa (2000x2000), sin texto encima; la descripción va como pie de imagen -->
                <figure class="collection-card m-0 border border-secondary bg-black rounded-3 overflow-hidden" @click="irAColeccion(col.idColeccion)">
                    <div class="square-box bg-white">
                        <img v-if="col.imagenUrl" :src="col.imagenUrl" class="collection-img" :alt="col.nombre">
                    </div>
                    <figcaption class="p-3 text-start">
                        <h5 class="text-white mb-1">{{ col.nombre }}</h5>
                        <p class="text-secondary small mb-0">{{ col.descripcion }}</p>
                    </figcaption>
                </figure>
            </div>
        </div>
        <div v-else class="text-center text-muted py-3">
            <p>{{ cargandoColecciones ? 'Cargando colecciones...' : 'Aún no hay colecciones registradas.' }}</p>
        </div>
    </div>

    <!-- SECCIÓN: NUESTRO CATÁLOGO -->
    <div class="container-fluid px-4 my-5 pt-3">
        <h2 class="mb-4 text-center">NUESTRO CATÁLOGO</h2>
        
        <div v-if="productos.length === 0" class="text-center text-muted py-4">
            <p>Cargando productos de la base de datos o sin registros activos...</p>
        </div>

        <div class="row g-4" v-else>
            <div class="col-md-3" v-for="producto in productos" :key="producto.idProducto">
                <div class="border border-secondary bg-black p-3 rounded-3 text-center d-flex flex-column align-items-center justify-content-between position-relative">
                    
                    <!-- Botón Favorito -->
                    <button class="btn btn-sm btn-dark position-absolute top-0 end-0 m-2 rounded-circle text-white shadow" @click="marcarFavorito(producto)" title="Añadir a favoritos" style="z-index: 3;">
                        <i class="bi bi-heart"></i>
                    </button>

                    <!-- Contenedor Cuadrado Perfecto 1:1 sin sobrantes negros -->
                    <div 
                        class="bg-white w-100 rounded-3 d-flex align-items-center justify-content-center overflow-hidden position-relative square-box mb-3" 
                        style="cursor: pointer;" 
                        @click="irADetalle(producto.idProducto)"
                        @mouseenter="cargarSegundaImagen(producto)"
                        @mouseleave="restaurarImagen(producto)"
                    >
                        <img 
                            :src="producto.imagenActual || producto.imagenUrl || '/logo-slamshoes-blanco.png'" 
                            class="img-fluid rounded catalog-img fade-transition" 
                            :alt="producto.nombre" 
                        />
                    </div>

                    <!-- Nombre del producto, Categoría real y Precio -->
                    <div class="text-start w-100">
                        <h6 class="text-white text-truncate mb-1" style="cursor: pointer;" @click="irADetalle(producto.idProducto)">
                            {{ producto.nombre }}
                        </h6>
                        <p class="text-secondary small mb-1">{{ producto.categoriaNombre || 'Calzado Deportivo' }}</p>
                        <p class="text-success fw-bold mb-0">${{ producto.precio }} MXN</p>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Pie -->
    <footer class="border-top border-secondary bg-dark pt-4 pb-2 mt-5 w-100">
        <div class="container-fluid text-center px-4">
            <div class="row mb-3">
                <div class="col">
                    <a href="#" class="mx-2 text-secondary text-decoration-none">[Enlace pie 1]</a>
                    <a href="#" class="mx-2 text-secondary text-decoration-none">[Enlace pie 2]</a>
                    <a href="#" class="mx-2 text-secondary text-decoration-none">[Enlace pie 3]</a>
                </div>
            </div>
            <p class="mb-0 text-muted">[Texto xd sit seven]</p>
        </div>
    </footer>

    <!-- Componente del Carrito Desplegable -->
    <CarritoComponent />

  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import productoService from '@/services/productoService';
import { verificarSesionYAccionar } from '@/utils/auth.js';
import CarritoComponent from '@/components/CarritoComponent.vue';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';

export default {
  name: 'HomeView',
  components: {
    CarritoComponent,
    NavbarPrincipal
  },
  setup() {
    const router = useRouter();
    const productos = ref([]);
    const colecciones = ref([]);
    const cargandoColecciones = ref(true);

    const cargarDatos = async () => {
      try {
        const respuestaProductos = await productoService.obtenerProductos();
        
        productos.value = await Promise.all(respuestaProductos.data.map(async (p) => {
          let categoriaNombre = "Calzado Deportivo";
          if (p.idCategoria) {
            try {
              const resCat = await axios.get(`http://localhost:8080/api/categorias/${p.idCategoria}`);
              categoriaNombre = resCat.data.nombre;
            } catch (e) {
              console.error("Error al cargar categoría", e);
            }
          }
          return {
            ...p,
            categoriaNombre,
            imagenActual: p.imagenUrl,
            segundaImagen: null
          };
        }));

        const respuestaColecciones = await axios.get('http://localhost:8080/api/colecciones').catch(() => ({ data: [] }));
        colecciones.value = respuestaColecciones.data;
        cargandoColecciones.value = false;

      } catch (error) {
        console.error("Error al cargar los datos de la API:", error);
      }
    };

    const cargarSegundaImagen = async (producto) => {
      if (producto.segundaImagen) {
        producto.imagenActual = producto.segundaImagen;
        return;
      }
      try {
        const resImgs = await axios.get(`http://localhost:8080/api/producto-imagenes/producto/${producto.idProducto}`);
        const imgOrden2 = resImgs.data.find(img => img.orden === 2);
        if (imgOrden2) {
          producto.segundaImagen = imgOrden2.urlImagen;
          producto.imagenActual = producto.segundaImagen;
        } else {
          producto.segundaImagen = producto.imagenUrl;
        }
      } catch (error) {
        producto.segundaImagen = producto.imagenUrl;
      }
    };

    const restaurarImagen = (producto) => {
      producto.imagenActual = producto.imagenUrl;
    };

    const irAColeccion = (id) => {
      router.push(`/coleccion/${id}`);
    };

    const irADetalle = (id) => {
      router.push(`/producto/${id}`);
    };

    const marcarFavorito = (producto) => {
      const usuarioActivo = verificarSesionYAccionar();
      if (!usuarioActivo) return;

      let favs = JSON.parse(localStorage.getItem('slam_shoes_favoritos')) || [];
      const existe = favs.some(p => p.idProducto === producto.idProducto);

      if (!existe) {
        favs.push(producto);
        localStorage.setItem('slam_shoes_favoritos', JSON.stringify(favs));
        // Avisa al encabezado para que actualice el contador de favoritos
        window.dispatchEvent(new Event('storage'));
        alert(`¡"${producto.nombre}" fue agregado a tus favoritos con éxito!`);
      } else {
        alert(`Este producto ya se encuentra en tus favoritos.`);
      }
    };

    onMounted(() => {
      cargarDatos();
    });

    return {
      productos,
      colecciones,
      cargandoColecciones,
      irAColeccion,
      irADetalle,
      cargarSegundaImagen,
      restaurarImagen,
      marcarFavorito
    };
  }
};
</script>

<style scoped>
.square-box {
  width: 100%;
  aspect-ratio: 1 / 1;
}
.catalog-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.fade-transition {
  transition: opacity 0.4s ease-in-out;
}
.collection-card {
  cursor: pointer;
  transition: transform 0.3s ease;
}
.collection-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}
.collection-card:hover {
  transform: translateY(-5px);
}
</style>