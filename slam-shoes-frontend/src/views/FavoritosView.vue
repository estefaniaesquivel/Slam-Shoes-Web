<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">

    <NavbarPrincipal />

    <!-- Contenido de Favoritos -->
    <div class="container my-5 flex-grow-1">
      <h2 class="mb-4 text-center fw-bold">MIS FAVORITOS</h2>

      <div v-if="favoritos.length === 0" class="text-center text-muted py-5">
        <i class="bi bi-heart display-4 d-block mb-3"></i>
        <h4>No tienes productos en favoritos todavía.</h4>
        <p class="small">Explora nuestro catálogo y guarda los sneakers que más te gusten.</p>
        <router-link to="/" class="btn btn-light mt-3 fw-semibold">Ir al catálogo</router-link>
      </div>

      <div class="row g-4" v-else>
        <div class="col-md-3" v-for="producto in favoritos" :key="producto.idProducto">
          <!-- Tarjeta con posición relativa para contener el botón absoluto de forma limpia -->
          <div class="border border-secondary bg-black p-3 rounded-3 text-center d-flex flex-column align-items-center justify-content-between position-relative" style="min-height: 380px;">
            
            <!-- Botón de eliminar con z-index alto para que nunca quede escondido -->
            <button 
              class="btn btn-sm btn-danger rounded-circle text-white shadow position-absolute" 
              style="top: 10px; right: 10px; z-index: 10; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;"
              @click="removerFavorito(producto.idProducto)" 
              title="Quitar de favoritos"
            >
                <i class="bi bi-trash"></i>
            </button>

            <!-- Imagen con clic al detalle -->
            <div 
                class="bg-white w-100 p-2 rounded-3 d-flex align-items-center justify-content-center overflow-hidden position-relative mt-2" 
                style="height: 190px; cursor: pointer;" 
                @click="irADetalle(producto.idProducto)"
            >
                <img 
                    :src="producto.imagenUrl || '/logo-slamshoes-blanco.png'" 
                    class="img-fluid rounded catalog-img" 
                    :alt="producto.nombre" 
                />
            </div>

            <div class="mt-3 text-start w-100">
                <h6 class="text-white text-truncate" style="cursor: pointer;" @click="irADetalle(producto.idProducto)">
                    {{ producto.nombre }}
                </h6>
                <p class="text-secondary small mb-1">{{ producto.marca }}</p>
                <p class="text-success fw-bold mb-2">${{ producto.precio }} MXN</p>
                <button class="btn btn-light w-100 btn-sm fw-semibold" @click="irADetalle(producto.idProducto)">
                    Ver detalles
                </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <CarritoComponent />

  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { verificarSesionYAccionar } from '@/utils/auth.js';
import { quitarFavorito } from '@/utils/favoritos.js';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import CarritoComponent from '@/components/CarritoComponent.vue';

export default {
  name: 'FavoritosView',
  components: { NavbarPrincipal, CarritoComponent },
  setup() {
    const router = useRouter();
    const favoritos = ref([]);

    const cargarFavoritos = () => {
      const usuarioActivo = verificarSesionYAccionar();
      if (!usuarioActivo) {
        router.push('/login');
        return;
      }

      const listaFavs = JSON.parse(localStorage.getItem('slam_shoes_favoritos')) || [];
      favoritos.value = listaFavs;
    };

    const removerFavorito = (idProducto) => {
      favoritos.value = favoritos.value.filter(p => p.idProducto !== idProducto);
      quitarFavorito(idProducto); // actualiza el navegador y la BD del cliente
      alert("Producto eliminado de tus favoritos.");
    };

    const irADetalle = (id) => {
      router.push(`/producto/${id}`);
    };

    onMounted(() => {
      cargarFavoritos();
    });

    return {
      favoritos,
      removerFavorito,
      irADetalle
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
</style>