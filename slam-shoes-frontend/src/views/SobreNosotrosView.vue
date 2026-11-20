<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal />

    <div class="flex-grow-1 container my-5" style="max-width: 900px;">
      <h1 class="seccion-titulo">Sobre nosotros</h1>
      <p class="lead text-light">
        Slam Shoes es una plataforma web especializada en la exhibición y gestión de calzado deportivo de baloncesto,
        desarrollada íntegramente como un prototipo académico para las asignaturas de Ingeniería de Software en la
        Universidad Autónoma de Yucatán (UADY). Nuestro objetivo es demostrar la implementación de una arquitectura
        distribuida moderna, combinando una experiencia de navegación intuitiva con un sistema robusto de gestión de
        catálogo y órdenes.
      </p>

      <div id="aviso-legal" class="aviso border border-danger rounded-3 p-4 mt-5">
        <h2 class="h4 text-danger mb-3"><i class="bi bi-exclamation-triangle me-2"></i>Aviso Legal y de Prototipo Escolar</h2>
        <ul class="mb-0 ps-3">
          <li class="mb-3">
            <strong>Sin fines comerciales:</strong> Este sitio no opera como un mercado real ni realiza transacciones
            comerciales verdaderas. Todos los productos, inventarios, procesos de compra, pasarelas de pago y envíos
            mostrados en la plataforma son completamente simulados con fines estrictamente de prueba y evaluación
            académica. No se recopilan datos bancarios reales ni se efectúan entregas físicas.
          </li>
          <li>
            <strong>Propiedad intelectual:</strong> Todas las marcas comerciales, logotipos, modelos de calzado, nombres
            de jugadores profesionales e imágenes exhibidas en este catálogo pertenecen a sus respectivos propietarios
            y titulares de derechos. Su uso dentro de Slam Shoes es exclusivamente ilustrativo y educativo, sin ánimo de
            lucro ni afiliación comercial alguna.
          </li>
        </ul>
      </div>
    </div>

    <CarritoComponent />
  </div>
</template>

<script setup>
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import CarritoComponent from '@/components/CarritoComponent.vue';
</script>

<style scoped>
.aviso { background: rgba(255, 193, 7, 0.06); }
</style>
