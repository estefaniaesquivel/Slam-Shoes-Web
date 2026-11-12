<template>
  <!-- Bloques Novedades y Ofertas al estilo Way of Wade: portada con zoom al pasar el cursor y botón que se rellena de rojo -->
  <div class="container-fluid px-4 my-4">
    <div class="row g-2">
      <div class="col-md-6" v-for="t in tiles" :key="t.clave">
        <router-link :to="t.ruta" class="promo-tile">
          <img v-if="t.imagen" :src="t.imagen" class="promo-img" :alt="t.titulo" />
          <div class="promo-contenido">
            <h2 class="promo-titulo">{{ t.titulo }}</h2>
            <span class="promo-boton">COMPRAR</span>
          </div>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script>
// Imágenes fijas del sitio (carpeta public/), igual que el logo.
// Para cambiarlas basta con reemplazar los archivos novedades.webp y ofertas.webp (2100 x 1000 px).
export default {
  name: 'PromoTiles',
  setup() {
    const tiles = [
      { clave: 'novedades', titulo: 'NOVEDADES', ruta: '/novedades', imagen: '/novedades.webp' },
      { clave: 'ofertas', titulo: 'OFERTAS', ruta: '/ofertas', imagen: '/ofertas.webp' }
    ];

    return { tiles };
  }
};
</script>

<style scoped>
.promo-tile {
  position: relative;
  display: block;
  overflow: hidden;
  aspect-ratio: 2.1 / 1;
  background: linear-gradient(135deg, #2b2b2b, #111);
  text-decoration: none;
}
.promo-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.8s ease;
}
/* Zoom suave al pasar el cursor sobre el bloque */
.promo-tile:hover .promo-img {
  transform: scale(1.08);
}
/* Texto en la mitad derecha, centrado */
.promo-contenido {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-left: 45%;
  padding-right: 4%;
  gap: 1.2rem;
}
.promo-titulo {
  margin: 0;
  color: #fff;
  font-weight: 500;
  font-size: clamp(1.4rem, 3.2vw, 3rem);
  letter-spacing: 0.01em;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.5);
}
.promo-boton {
  padding: 0.55rem 1.8rem;
  color: #fff;
  border: 2px solid #fff;
  background: transparent;
  font-size: clamp(0.8rem, 1.1vw, 1rem);
  transition: background-color 0.25s ease, border-color 0.25s ease;
}
/* El botón se rellena de rojo al pasar el cursor encima */
.promo-boton:hover {
  background-color: #e4002b;
  border-color: #e4002b;
}
</style>