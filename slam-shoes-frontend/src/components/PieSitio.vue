<template>
  <!-- Pie de página (estilo Way of Wade): marca, enlaces por columna y aviso de prototipo escolar -->
  <footer class="pie-sitio w-100 mt-5">
    <div class="container-fluid px-4 pt-5 pb-4">
      <div class="row g-4">
        <!-- Marca y aviso -->
        <div class="col-12 col-md-6 col-lg-3">
          <img src="/logo-slamshoes-blanco.png" alt="Slam Shoes" class="pie-logo mb-3" />
          <p class="pie-texto mb-2">Tienda web de calzado deportivo de baloncesto.</p>
          <span class="badge-prototipo">Prototipo escolar</span>
        </div>

        <!-- Comprar: categorías reales de la BD -->
        <div class="col-6 col-md-3 col-lg-2">
          <h6 class="pie-titulo">Comprar</h6>
          <ul class="pie-lista">
            <li v-for="c in categorias" :key="c.idCategoria">
              <router-link :to="`/categoria/${c.idCategoria}`">{{ c.nombre }}</router-link>
            </li>
            <li><router-link to="/ofertas">Ofertas</router-link></li>
            <li><router-link to="/novedades">Novedades</router-link></li>
          </ul>
        </div>

        <!-- Info -->
        <div class="col-6 col-md-3 col-lg-2">
          <h6 class="pie-titulo">Info</h6>
          <ul class="pie-lista">
            <li><router-link to="/sobre-nosotros">Sobre nosotros</router-link></li>
            <li><router-link :to="{ path: '/sobre-nosotros', hash: '#aviso-legal' }">Aviso legal</router-link></li>
          </ul>
        </div>

        <!-- Aviso de prototipo escolar -->
        <div class="col-12 col-md-6 col-lg-4">
          <h6 class="pie-titulo">Aviso de prototipo escolar</h6>
          <p class="pie-texto mb-0">
            Slam Shoes es un prototipo académico de la UADY, sin fines comerciales. Los productos, pagos y envíos
            son simulados y las marcas e imágenes pertenecen a sus propietarios.
          </p>
        </div>
      </div>
    </div>

    <div class="pie-inferior">
      <div class="container-fluid px-4 py-3 d-flex flex-wrap justify-content-between gap-2">
        <span>Copyright © 2026 Slam Shoes. Prototipo escolar sin fines comerciales.</span>
        <span>Universidad Autónoma de Yucatán</span>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { obtenerCategorias } from '@/utils/catalogos.js';

const categorias = ref([]);
onMounted(async () => {
  try {
    categorias.value = await obtenerCategorias();
  } catch (e) {
    categorias.value = []; // el pie sigue funcionando sin categorías
  }
});
</script>

<style scoped>
.pie-sitio { background-color: #2b2b2b; color: #b5b5b5; border-top: 1px solid #3a3a3a; }
.pie-logo { height: 44px; width: auto; }
.pie-titulo { color: #fff; font-weight: 600; text-transform: uppercase; letter-spacing: 0.02em; margin-bottom: 1rem; }
.pie-texto { font-size: 0.9rem; line-height: 1.5; }
.pie-lista { list-style: none; padding: 0; margin: 0; }
.pie-lista li { margin-bottom: 0.55rem; }
.pie-lista a { color: #b5b5b5; text-decoration: none; font-size: 0.92rem; transition: color 0.2s ease; }
.pie-lista a:hover { color: var(--color-acento); }
.badge-prototipo {
  display: inline-block; padding: 0.25rem 0.7rem; font-size: 0.75rem; font-weight: 600; text-transform: uppercase;
  color: var(--color-acento); border: 1px solid var(--color-acento); border-radius: 2px;
}
.pie-inferior { background-color: #242424; font-size: 0.82rem; }
</style>
