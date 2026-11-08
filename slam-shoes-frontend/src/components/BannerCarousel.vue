<template>
  <!-- Carrusel principal. Orden de prioridad: tabla banners > banner_url de colecciones > banners fijos -->
  <div id="primerCarrusel" class="carousel slide my-4 container-fluid px-4" data-bs-ride="carousel" data-bs-interval="3000">
    <div class="carousel-inner">
      <div v-for="(b, i) in slides" :key="b.clave" class="carousel-item" :class="{ active: i === 0 }">
        <component :is="b.enlace ? (b.interno ? 'router-link' : 'a') : 'div'" v-bind="atributosEnlace(b)">
          <img :src="b.imagen" class="d-block w-100 object-fit-cover rounded shadow" style="max-height: 500px;" :alt="b.titulo">
        </component>
      </div>
    </div>
    <button class="carousel-control-prev" type="button" data-bs-target="#primerCarrusel" data-bs-slide="prev">
      <span class="carousel-control-prev-icon bg-secondary rounded p-3" aria-hidden="true"></span>
    </button>
    <button class="carousel-control-next" type="button" data-bs-target="#primerCarrusel" data-bs-slide="next">
      <span class="carousel-control-next-icon bg-secondary rounded p-3" aria-hidden="true"></span>
    </button>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';

const API = 'http://localhost:8080/api';

export default {
  name: 'BannerCarousel',
  setup() {
    const banners = ref([]);
    const colecciones = ref([]);

    onMounted(async () => {
      const [resBanners, resColecciones] = await Promise.all([
        axios.get(`${API}/banners`).catch(() => ({ data: [] })),
        axios.get(`${API}/colecciones`).catch(() => ({ data: [] }))
      ]);
      banners.value = resBanners.data;
      colecciones.value = resColecciones.data;
    });

    const slides = computed(() => {
      if (banners.value.length > 0) {
        return banners.value.map((b) => ({
          clave: `b${b.idBanner}`,
          imagen: b.imagenUrl,
          titulo: b.titulo || 'Banner',
          enlace: b.enlaceDestino || null,
          interno: !!b.enlaceDestino && b.enlaceDestino.startsWith('/')
        }));
      }
      const deColecciones = colecciones.value.filter((c) => c.bannerUrl);
      if (deColecciones.length > 0) {
        return deColecciones.map((c) => ({
          clave: `c${c.idColeccion}`,
          imagen: c.bannerUrl,
          titulo: c.nombre,
          enlace: `/coleccion/${c.idColeccion}`,
          interno: true
        }));
      }
      return [
        { clave: 'f1', imagen: '/Nike JA 3.png', titulo: 'Nike JA 3 - Cobalt Bliss', enlace: null, interno: false },
        { clave: 'f2', imagen: '/promo-halloween.png', titulo: 'Promoción Halloween', enlace: null, interno: false }
      ];
    });

    const atributosEnlace = (b) => {
      if (!b.enlace) return {};
      return b.interno ? { to: b.enlace } : { href: b.enlace, target: '_blank', rel: 'noopener' };
    };

    return { slides, atributosEnlace };
  }
};
</script>
