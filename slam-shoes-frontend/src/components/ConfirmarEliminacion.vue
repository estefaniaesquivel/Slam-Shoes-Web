<template>
  <!-- Panel de confirmación para eliminar un producto -->
  <div v-if="producto" class="fondo" @click.self="$emit('cancelar')">
    <div class="panel border border-secondary rounded-3 bg-dark p-4" role="dialog" aria-modal="true">
      <div class="text-center mb-3">
        <i class="bi bi-exclamation-triangle text-danger" style="font-size: 2.5rem;"></i>
        <h5 class="mt-2 mb-1">¿Eliminar este producto?</h5>
      </div>

      <div class="d-flex align-items-center gap-3 bg-black border border-secondary rounded p-2 mb-3">
        <img :src="producto.imagenUrl" class="rounded bg-white" style="width: 56px; height: 56px; object-fit: cover;" :alt="producto.nombre" />
        <div class="overflow-hidden">
          <div class="text-white text-truncate">{{ producto.nombre }}</div>
          <div class="text-secondary small">ID {{ producto.idProducto }}</div>
        </div>
      </div>

      <p class="text-secondary small">
        Se borrarán también sus colores, tallas, imágenes, favoritos y ofertas. Esta acción no se puede deshacer.
      </p>

      <div class="d-flex gap-2">
        <button class="btn btn-outline-light flex-grow-1" @click="$emit('cancelar')" :disabled="eliminando">Cancelar</button>
        <button class="btn btn-danger flex-grow-1" @click="$emit('confirmar')" :disabled="eliminando">
          {{ eliminando ? 'Eliminando...' : 'Sí, eliminar' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ConfirmarEliminacion',
  props: {
    // Producto a eliminar; null = panel cerrado
    producto: { type: Object, default: null },
    eliminando: { type: Boolean, default: false }
  },
  emits: ['confirmar', 'cancelar']
};
</script>

<style scoped>
.fondo {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 1rem;
}
.panel { width: 100%; max-width: 440px; }
</style>
