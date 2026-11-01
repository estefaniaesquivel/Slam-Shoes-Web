<template>
  <div class="bg-dark text-light min-vh-100">

    <!-- Encabezado (Navbar con menús desplegables) -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark border-bottom border-secondary">
        <div class="container-fluid px-4">
            <!-- Logo -->
            <router-link class="navbar-brand" to="/">
                <span class="fw-bold text-white fs-4">SLAM SHOES</span>
            </router-link>
            
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#menuPrincipal" aria-controls="menuPrincipal" aria-expanded="false" aria-label="Toggle navigation">
                <span class="navbar-toggler-icon"></span>
            </button>
            
            <div class="collapse navbar-collapse justify-content-center" id="menuPrincipal">
                <ul class="navbar-nav mb-2 mb-lg-0">
                    <li class="nav-item"><a class="nav-link text-light" href="#">Novedades</a></li>
                    
                    <!-- Menú Marcas -->
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

                    <!-- Menú Por Jugador -->
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
                <router-link to="/login" class="text-light fs-5"><i class="bi bi-person"></i></router-link>
                <a href="#offcanvasCarrito" data-bs-toggle="offcanvas" class="position-relative text-light text-decoration-none fs-5" aria-label="Abrir carrito">
                    <i class="bi bi-bag"></i>
                    <span id="contador-carrito" class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="font-size: 0.6rem;">0</span>
                </a>
            </div>
        </div>
    </nav>

    <!-- Carrusel Principal -->
    <div id="primerCarrusel" class="carousel slide my-4 container" data-bs-ride="carousel" data-bs-interval="3000">
        <div class="carousel-inner rounded">
            <div class="carousel-item active">
                <div class="p-5 bg-black border border-secondary text-center rounded">
                    <h2 class="text-white">Catálogo Oficial Slam Shoes</h2>
                    <p class="text-muted">Explora los modelos exclusivos desde tu base de datos en Supabase</p>
                </div>
            </div>
        </div>
    </div>

    <!-- SECCIÓN DINÁMICA: Productos desde Spring Boot / Supabase -->
    <div class="container my-5">
      <h2 class="mb-4 text-center">[En Tendencia / Catálogo]</h2>
      
      <div class="row g-4">
        <!-- Recorremos la lista de productos que trae el backend -->
        <div class="col-md-4" v-for="producto in productos" :key="producto.idProducto">
          <div class="border border-secondary bg-black p-3 rounded-3 text-center d-flex flex-column align-items-center justify-content-between" style="min-height: 380px;">
            <img :src="producto.imagenUrl" class="img-fluid object-fit-contain mb-3 rounded" style="max-height: 160px;" :alt="producto.nombre" />
            
            <div class="mt-2 text-start w-100">
              <h6 class="text-white">{{ producto.nombre }}</h6>
              <p class="text-secondary small mb-1">{{ producto.marca }}</p>
              <p class="text-success fw-bold">${{ producto.precio }} MXN</p>
              <button 
                class="btn btn-light w-100 btn-sm" 
                @click="agregarAlCarrito(producto)">
                Añadir al carrito
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Pie de página -->
    <footer class="border-top border-secondary bg-dark pt-4 pb-2 mt-5">
        <div class="container text-center">
            <div class="row mb-3">
                <div class="col">
                    <a href="#" class="mx-2 text-secondary text-decoration-none">[Enlace pie 1]</a>
                    <a href="#" class="mx-2 text-secondary text-decoration-none">[Enlace pie 2]</a>
                    <a href="#" class="mx-2 text-secondary text-decoration-none">[Enlace pie 3]</a>
                </div>
            </div>
            <p class="mb-0 text-muted">Slam Shoes Web - Todos los derechos reservados</p>
        </div>
    </footer>

    <!-- Panel lateral del carrito (Offcanvas) -->
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
                        <button class="btn btn-sm btn-outline-danger" @click="eliminarItem(index)">
                            <i class="bi bi-trash"></i>
                        </button>
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
import productoService from '@/services/productoService';

export default {
  name: 'HomeView',
  setup() {
    const productos = ref([]);
    const carrito = ref(JSON.parse(localStorage.getItem('slam_shoes_cart')) || []);

    // Cargar productos desde Spring Boot / Supabase
    const cargarProductos = async () => {
      try {
        const respuesta = await productoService.obtenerProductos();
        productos.value = respuesta.data;
      } catch (error) {
        console.error("Error al cargar los productos de la API:", error);
      }
    };

    // Funciones del carrito
    const guardarCarritoStorage = () => {
      localStorage.setItem('slam_shoes_cart', JSON.stringify(carrito.value));
    };

    const agregarAlCarrito = (producto) => {
      const itemExistente = carrito.value.find(item => (item.idProducto || item.id) === (producto.idProducto || producto.id));
      if (itemExistente) {
        itemExistente.cantidad += 1;
      } else {
        carrito.value.push({
          id: producto.idProducto || producto.id,
          nombre: producto.nombre,
          precio: producto.precio,
          cantidad: 1
        });
      }
      guardarCarritoStorage();
    };

    const eliminarItem = (index) => {
      carrito.value.splice(index, 1);
      guardarCarritoStorage();
    };

    const vaciarCarrito = () => {
      carrito.value = [];
      guardarCarritoStorage();
    };

    const totalCarrito = computed(() => {
      return carrito.value.reduce((total, item) => total + (item.precio * item.cantidad), 0);
    });

    const continuarCompra = () => {
      alert('¡Proceso de compra simulado con éxito!');
      vaciarCarrito();
    };

    onMounted(() => {
      cargarProductos();
    });

    return {
      productos,
      carrito,
      agregarAlCarrito,
      eliminarItem,
      vaciarCarrito,
      totalCarrito,
      continuarCompra
    };
  }
};
</script>