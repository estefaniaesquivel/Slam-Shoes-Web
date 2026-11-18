<template>
  <!-- Aquí se inyecta la vista de cada ruta -->
  <router-view />
  <!-- Pie de página compartido por todas las vistas (incluye el aviso de prototipo escolar) -->
  <PieSitio />
</template>

<script>
import PieSitio from '@/components/PieSitio.vue';

export default {
  name: 'App',
  components: { PieSitio }
}
</script>

<!--PARA PRENDER EL SERVIDOR PRIMERO
cd slam-shoes-frontend LUEGO
npm run dev
le pican al link (Local: http://...) que les aparece en la terminal y listo
-->
