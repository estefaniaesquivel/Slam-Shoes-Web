<template>
  <div class="bg-dark text-light min-vh-100">

    <!-- Encabezado (Navbar) -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark border-bottom border-secondary">
      <div class="container-fluid px-4">
        <router-link class="navbar-brand" to="/">
          <span class="fw-bold text-white fs-4">SLAM SHOES</span>
        </router-link>
        
        <div class="d-flex align-items-center gap-3">
          <router-link to="/login" class="text-light fs-5"><i class="bi bi-person"></i></router-link>
        </div>
      </div>
    </nav>

    <!-- Carrusel Principal -->
    <div id="primerCarrusel" class="carousel slide my-4 container" data-bs-ride="carousel">
      <div class="carousel-inner">
        <div class="carousel-item active">
          <div class="p-5 bg-secondary text-center rounded">
            <h2>¡Bienvenidos a Slam Shoes!</h2>
            <p>Tu tienda de calzado de básquetbol favorita</p>
          </div>
        </div>
      </div>
    </div>

    <!-- SECCIÓN DINÁMICA: Productos desde Spring Boot / Supabase -->
    <div class="container my-5 text-center">
      <h2 class="mb-4">Catálogo en Línea (Supabase)</h2>
      
      <div class="row g-4">
        <!-- Recorremos la lista de productos que trae el backend -->
        <div class="col-md-4" v-for="producto in productos" :key="producto.idProducto">
          <div class="border border-secondary bg-black p-3 rounded-3 text-center d-flex flex-column align-items-center justify-content-center" style="min-height: 320px;">
            <img :src="producto.imagenUrl" class="img-fluid object-fit-contain mb-3 rounded" style="max-height: 160px;" :alt="producto.nombre" />
            
            <div class="mt-2 text-start w-100">
              <h6 class="text-white">{{ producto.nombre }}</h6>
              <p class="text-success fw-bold">${{ producto.precio }} MXN</p>
              <button class="btn btn-light w-100 btn-sm">Añadir al carrito</button>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import productoService from '@/services/productoService';

export default {
  name: 'HomeView',
  setup() {
    const productos = ref([]);

    const cargarProductos = async () => {
      try {
        const respuesta = await productoService.obtenerProductos();
        productos.value = respuesta.data;
      } catch (error) {
        console.error("Error al cargar los productos de la API:", error);
      }
    };

    onMounted(() => {
      cargarProductos();
    });

    return {
      productos
    };
  }
};
</script>