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
                
                <a href="#" class="text-light fs-5 text-decoration-none" @click.prevent="irAFavoritos" aria-label="Favoritos">
                    <i class="bi bi-heart"></i>
                </a>

                <!-- ICONO DE USUARIO CONDICIONAL -->
                <a href="#" class="text-light fs-5 text-decoration-none" @click.prevent="manejarClickUsuario" aria-label="Perfil de usuario">
                    <i class="bi bi-person"></i>
                </a>
                
                <!-- Botón de Carrito -->
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
                <div class="border border-secondary bg-black p-3 rounded-3 text-center d-flex flex-column align-items-center justify-content-between position-relative" style="min-height: 380px;">
                    
                    <!-- Botón Favorito -->
                    <button class="btn btn-sm btn-dark position-absolute top-0 end-0 m-2 rounded-circle text-white shadow" @click="marcarFavorito(producto)">
                        <i class="bi bi-heart"></i>
                    </button>

                    <!-- Imagen con transición fade al pasar el cursor para mostrar orden = 2 -->
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

                    <div class="mt-3 text-start w-100">
                        <h6 class="text-white text-truncate" style="cursor: pointer;" @click="irADetalle(producto.idProducto)">
                            {{ producto.nombre }}
                        </h6>
                        <p class="text-secondary small mb-1">{{ producto.marca }}</p>
                        <p class="text-success fw-bold mb-2">${{ producto.precio }} MXN</p>
                        <button class="btn btn-light w-100 btn-sm fw-semibold" @click="agregarAlCarrito(producto)">
                            Añadir al carrito
                        </button>
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

    <!-- Panel lateral del carrito -->
    <div class="offcanvas offcanvas-end text-bg-dark" tabindex="-1" id="offcanvasCarrito" aria-labelledby="offcanvasCarritoLabel">
        <div class="offcanvas-header border-bottom border-secondary">
            <h5 class="offcanvas-title" id="offcanvasCarritoLabel">Carrito de compras</h5>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="offcanvas" aria-label="Cerrar"></button>
        </div>
        <div class="offcanvas-body d-flex flex-column">
            <div id="lista-carrito" class="flex-grow-1 overflow-auto">
                <div v-if="carrito.length === 0" class="text-center text-muted my-4">El carrito está vacío</div>
                <div v-for="(item, index) in carrito" :key="index" class="d-flex justify-content-between align-items-center mb-3 pb-2 border-bottom border-secondary">
                    <div>
                        <h6 class="mb-0 text-white">{{ item.nombre }}</h6>
                        <small class="text-muted">${{ item.precio }} x {{ item.cantidad }}</small>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <span class="fw-bold text-success">${{ item.precio * item.cantidad }}</span>
                        <button class="btn btn-sm btn-outline-danger" @click="eliminarItem(index)"><i class="bi bi-trash"></i></button>
                    </div>
                </div>
            </div>
            <div class="border-top border-secondary pt-3 mt-auto">
                <div class="d-flex justify-content-between fw-bold mb-3">
                    <span>Total:</span>
                    <span class="text-success">${{ totalCarrito }} MXN</span>
                </div>
                <div class="d-grid gap-2">
                    <button class="btn btn-outline-danger btn-sm" @click="vaciarCarrito">Vaciar carrito</button>
                    <button class="btn btn-light" @click="continuarCompra">Continuar compra</button>
                </div>
            </div>
        </div>
    </div>

  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import productoService from '@/services/productoService';
import { verificarSesionYAccionar } from '@/utils/auth.js';

export default {
  name: 'HomeView',
  setup() {
    const router = useRouter();
    const productos = ref([]);
    const colecciones = ref([]);
    const carrito = ref(JSON.parse(localStorage.getItem('slam_shoes_cart')) || []);

    const cargarDatos = async () => {
      try {
        const respuestaProductos = await productoService.obtenerProductos();
        productos.value = respuestaProductos.data.map(p => ({
          ...p,
          imagenActual: p.imagenUrl,
          segundaImagen: null 
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

    const guardarCarritoStorage = () => {
      localStorage.setItem('slam_shoes_cart', JSON.stringify(carrito.value));
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

    const agregarAlCarrito = (producto) => {
      const usuarioActivo = verificarSesionYAccionar();
      if (!usuarioActivo) return;

      const idProd = producto.idProducto || producto.id;
      const itemExistente = carrito.value.find(item => (item.id || item.idProducto) === idProd);
      if (itemExistente) {
        itemExistente.cantidad += 1;
      } else {
        carrito.value.push({
          id: idProd,
          nombre: producto.nombre,
          precio: producto.precio,
          cantidad: 1
        });
      }
      guardarCarritoStorage();
      alert(`¡"${producto.nombre}" se añadió a tu carrito!`);
    };

    const marcarFavorito = (producto) => {
      const usuarioActivo = verificarSesionYAccionar();
      if (!usuarioActivo) return;

      alert(`¡"${producto.nombre}" fue agregado a tus favoritos con éxito!`);
    };

    const irAFavoritos = () => {
      const usuarioActivo = verificarSesionYAccionar();
      if (!usuarioActivo) return;

      alert("Redirigiendo a tu sección de Favoritos...");
    };

    const eliminarItem = (index) => {
      carrito.value.splice(index, 1);
      guardarCarritoStorage();
    };

    const vaciarCarrito = () => {
      carrito.value = [];
      guardarCarritoStorage();
    };

    const totalItemsCarrito = computed(() => {
      return carrito.value.reduce((suma, item) => suma + item.cantidad, 0);
    });

    const totalCarrito = computed(() => {
      return carrito.value.reduce((total, item) => total + (item.precio * item.cantidad), 0);
    });

    const continuarCompra = () => {
      const usuarioActivo = verificarSesionYAccionar();
      if (!usuarioActivo) return;

      alert('¡Proceso de compra simulado con éxito!');
      vaciarCarrito();
    };

    onMounted(() => {
      cargarDatos();
    });

    return {
      productos,
      colecciones,
      carrito,
      manejarClickUsuario,
      irADetalle,
      cargarSegundaImagen,
      restaurarImagen,
      agregarAlCarrito,
      marcarFavorito,
      irAFavoritos,
      eliminarItem,
      vaciarCarrito,
      totalItemsCarrito,
      totalCarrito,
      continuarCompra
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