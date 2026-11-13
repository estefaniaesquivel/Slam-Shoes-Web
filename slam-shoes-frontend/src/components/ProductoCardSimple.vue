<template>
  <!-- Tarjeta del catálogo: imagen 1:1 (cambia a la de orden 2 al pasar el cursor), favorito y etiqueta de oferta -->
  <div class="border border-secondary bg-black p-3 rounded-3 text-center d-flex flex-column align-items-center justify-content-between position-relative h-100">
    <BotonFavorito :producto="producto" />
    <EtiquetaOferta :id-producto="producto.idProducto" />

    <div
      class="bg-white w-100 rounded-3 d-flex align-items-center justify-content-center overflow-hidden position-relative square-box mb-3"
      style="cursor: pointer;"
      @click="irADetalle"
      @mouseenter="mostrarSegunda"
      @mouseleave="imagenActual = producto.imagenUrl"
    >
      <img
        :src="imagenActual || producto.imagenUrl || '/logo-slamshoes-blanco.png'"
        class="img-fluid rounded catalog-img fade-transition"
        :alt="producto.nombre"
      />
    </div>

    <div class="text-start w-100">
      <h6 class="text-white text-truncate mb-1" style="cursor: pointer;" :title="producto.nombre" @click="irADetalle">
        {{ producto.nombre }}
      </h6>
      <p class="text-secondary small mb-1">{{ subtitulo || producto.marca }}</p>
      <PrecioProducto :id-producto="producto.idProducto" :precio="producto.precio" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import BotonFavorito from '@/components/BotonFavorito.vue';
import EtiquetaOferta from '@/components/EtiquetaOferta.vue';
import PrecioProducto from '@/components/PrecioProducto.vue';

const props = defineProps({
  // { idProducto, nombre, marca, precio, imagenUrl }
  producto: { type: Object, required: true },
  // Línea bajo el nombre (por defecto la marca)
  subtitulo: { type: String, default: '' }
});

const router = useRouter();
const imagenActual = ref(props.producto.imagenUrl);
let segundaImagen = null;

const irADetalle = () => router.push(`/producto/${props.producto.idProducto}`);

const mostrarSegunda = async () => {
  if (segundaImagen) { imagenActual.value = segundaImagen; return; }
  try {
    const res = await axios.get(`http://localhost:8080/api/producto-imagenes/producto/${props.producto.idProducto}`);
    const orden2 = res.data.find((i) => i.orden === 2);
    segundaImagen = orden2 ? orden2.urlImagen : props.producto.imagenUrl;
  } catch (e) {
    segundaImagen = props.producto.imagenUrl;
  }
  imagenActual.value = segundaImagen;
};
</script>

<style scoped>
.square-box { width: 100%; aspect-ratio: 1 / 1; }
.catalog-img { width: 100%; height: 100%; object-fit: cover; }
.fade-transition { transition: opacity 0.4s ease-in-out; }
</style>
