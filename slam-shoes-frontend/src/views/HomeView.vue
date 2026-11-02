<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden">

    <!-- Encabezado (Navbar) -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark border-bottom border-secondary w-100 px-4">
        <div class="container-fluid px-0">
            <router-link class="navbar-brand" to="/">
                <img src="/logo-slamshoes-blanco.png" alt="Slam Shoes Logo" style="height: 45px; width: auto;" class="object-contain">         
            </router-link>
            
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#menuPrincipal" aria-controls="menuPrincipal" aria-expanded="false" aria-label="Toggle navigation">
                <span class="navbar-toggler-icon"></span>
            </button>
            
            <div class="collapse navbar-collapse justify-content-center" id="menuPrincipal">
                <ul class="navbar-nav mb-2 mb-lg-0">
                    <li class="nav-item"><a class="nav-link text-light" href="#">Novedades</a></li>
                    
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle text-light" href="#" id="dropdownMarcas" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                            Marcas
                        </a>
                        <ul class="dropdown-menu dropdown-menu-dark bg-dark border-secondary" aria-labelledby="dropdownMarcas">
                            <li><a class="dropdown-item" href="#">Nike</a></li>
                            <li><a class="dropdown-item" href="#">Adidas</a></li>
                            <li><a class="dropdown-item" href="#">Li-ning</a></li>
                            <li><a class="dropdown-item" href="#">Under Armour</a></li>
                            <li><a class="dropdown-item" href="#">ANTA</a></li>
                            <li><hr class="dropdown-divider border-secondary"></li>
                            <li><a class="dropdown-item" href="#">Ver todas las marcas</a></li>
                        </ul>
                    </li>

                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle text-light" href="#" id="dropdownPlayer" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                            Por Jugador
                        </a>
                        <ul class="dropdown-menu dropdown-menu-dark bg-dark border-secondary" aria-labelledby="dropdownPlayer">
                            <li><a class="dropdown-item" href="#">Lebron James</a></li>
                            <li><a class="dropdown-item" href="#">Kevin Durant</a></li>
                            <li><a class="dropdown-item" href="#">Stephen Curry</a></li>
                            <li><a class="dropdown-item" href="#">Dwyane Wade</a></li>
                            <li><a class="dropdown-item" href="#">Kyrie Irving</a></li>
                            <li><a class="dropdown-item" href="#">Giannis A.</a></li>
                            <li><hr class="dropdown-divider border-secondary"></li>
                            <li><a class="dropdown-item" href="#">Ver todos</a></li>
                        </ul>
                    </li>
                    <li class="nav-item"><a class="nav-link text-light" href="#">Accesorios</a></li>
                    <li class="nav-item"><a class="nav-link text-light" href="#">Ofertas</a></li>
                </ul>
            </div>

            <!-- Iconos de la derecha -->
            <div class="d-flex align-items-center gap-3">
                <a href="#" class="text-light fs-5"><i class="bi bi-search"></i></a>
                
                <!-- Icono de Favoritos con contador -->
                <a href="#" class="position-relative text-light text-decoration-none fs-5" @click.prevent="irAFavoritos" aria-label="Favoritos">
                    <i class="bi bi-heart"></i>
                    <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="font-size: 0.6rem;">
                        {{ totalItemsFavoritos }}
                    </span>
                </a>

                <!-- Icono de Usuario -->
                <a href="#" class="text-light fs-5 text-decoration-none" @click.prevent="manejarClickUsuario" aria-label="Perfil de usuario">
                    <i class="bi bi-person"></i>
                </a>
                
                <!-- Botón de Carrito Desplegable -->
                <a href="#offcanvasCarrito" data-bs-toggle="offcanvas" class="position-relative text-light text-decoration-none fs-5" aria-label="Abrir carrito">
                    <i class="bi bi-bag"></i>
                    <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="font-size: 0.6rem;">
                        {{ totalItemsCarrito }}
                    </span>
                </a>
            </div>
        </div>
    </nav>

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
        <div class="row g-3 text-center" v-if="colecciones.length > 0">
            <div class="col-md-3" v-for="col in colecciones" :key="col.idColeccion">
                <div class="border border-secondary bg-black p-3 rounded position-relative overflow-hidden collection-card" style="height: 220px; display: flex; flex-direction: column; justify-content: flex-end;">
                    <img v-if="col.imagenUrl" :src="col.imagenUrl" class="position-absolute top-0 start-0 w-100 h-100 object-fit-cover opacity-50" :alt="col.nombre">
                    <div class="position-relative z-1 bg-dark bg-opacity-75 p-2 rounded">
                        <h5 class="text-white mb-1">{{ col.nombre }}</h5>
                        <p class="text-secondary small mb-0">{{ col.descripcion }}</p>
                    </div>
                </div>
            </div>
        </div>
        <div v-else class="text-center text-muted py-3">
            <p>Cargando colecciones...</p>
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
                <div class="border border-secondary bg-black p-3 rounded-3 text-center d-flex flex-column align-items-center justify-content-between position-relative" style="min-height: 350px;">
                    
                    <!-- Botón Favorito -->
                    <button class="btn btn-sm btn-dark position-absolute top-0 end-0 m-2 rounded-circle text-white shadow" @click="marcarFavorito(producto)" title="Añadir a favoritos" style="z-index: 3;">
                        <i class="bi bi-heart"></i>
                    </button>

                    <!-- Imagen con transición fade -->
                    <div 
                        class="bg-white w-100 p-2 rounded-3 d-flex align-items-center justify-content-center overflow-hidden position-relative" 
                        style="height: 190px; cursor: pointer;" 
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

                    <!-- Nombre del producto, Categoría real desde la BD y Precio -->
                    <div class="mt-3 text-start w-100">
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
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import productoService from '@/services/productoService';
import { verificarSesionYAccionar } from '@/utils/auth.js';
import CarritoComponent from '@/components/CarritoComponent.vue';

export default {
  name: 'HomeView',
  components: {
    CarritoComponent
  },
  setup() {
    const router = useRouter();
    const productos = ref([]);
    const colecciones = ref([]);
    const favoritos = ref(JSON.parse(localStorage.getItem('slam_shoes_favoritos')) || []);

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

    const manejarClickUsuario = () => {
      const usuarioGuardado = localStorage.getItem('usuario');
      if (!usuarioGuardado) {
        router.push('/login');
      } else {
        router.push('/profile');
      }
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
        favoritos.value = favs;
        alert(`¡"${producto.nombre}" fue agregado a tus favoritos con éxito!`);
      } else {
        alert(`Este producto ya se encuentra en tus favoritos.`);
      }
    };

    const irAFavoritos = () => {
      const usuarioActivo = verificarSesionYAccionar();
      if (!usuarioActivo) return;

      router.push('/favoritos');
    };

    const totalItemsCarrito = computed(() => {
      const carritoActual = JSON.parse(localStorage.getItem('slam_shoes_cart')) || [];
      return carritoActual.reduce((suma, item) => suma + item.cantidad, 0);
    });

    const totalItemsFavoritos = computed(() => {
      return favoritos.value.length;
    });

    onMounted(() => {
      cargarDatos();
      window.addEventListener('storage', () => {
        favoritos.value = JSON.parse(localStorage.getItem('slam_shoes_favoritos')) || [];
      });
    });

    return {
      productos,
      colecciones,
      manejarClickUsuario,
      irADetalle,
      cargarSegundaImagen,
      restaurarImagen,
      marcarFavorito,
      irAFavoritos,
      totalItemsCarrito,
      totalItemsFavoritos
    };
  }
};
</script>

<style scoped>
.catalog-img {
  max-height: 170px;
  width: 100%;
  object-fit: contain;
}
.fade-transition {
  transition: opacity 0.4s ease-in-out;
}
.collection-card {
  transition: transform 0.3s ease;
}
.collection-card:hover {
  transform: translateY(-5px);
}
</style>