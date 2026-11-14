<template>
  <!-- Alerta de confirmación reutilizable. Con "doble" pide confirmar dos veces (la segunda con el aviso ⚠️) -->
  <div v-if="abierto" class="fondo" @click.self="$emit('cancelar')">
    <div class="panel border border-secondary rounded-3 bg-dark p-4" role="dialog" aria-modal="true">
      <div class="text-center mb-3">
        <div v-if="paso === 2" style="font-size: 2.6rem;">⚠️</div>
        <i v-else class="bi bi-exclamation-triangle text-danger" style="font-size: 2.5rem;"></i>
        <h5 class="mt-2 mb-1">{{ paso === 2 ? tituloDoble : titulo }}</h5>
      </div>

      <div v-if="paso === 1">
        <slot />
      </div>
      <p v-else class="text-secondary small text-center">{{ mensajeDoble }}</p>

      <div class="d-flex gap-2 mt-3">
        <button class="btn btn-outline-light flex-grow-1" :disabled="procesando" @click="$emit('cancelar')">Cancelar</button>
        <button v-if="doble && paso === 1" class="btn btn-danger flex-grow-1" @click="paso = 2">Continuar</button>
        <button v-else class="btn btn-danger flex-grow-1" :disabled="procesando" @click="$emit('confirmar')">
          {{ procesando ? 'Eliminando...' : textoConfirmar }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';

const props = defineProps({
  abierto: { type: Boolean, default: false },
  titulo: { type: String, default: '¿Eliminar?' },
  textoConfirmar: { type: String, default: 'Sí, eliminar' },
  procesando: { type: Boolean, default: false },
  doble: { type: Boolean, default: false },
  tituloDoble: { type: String, default: '¿ESTÁS SEGURO?' },
  mensajeDoble: { type: String, default: '' }
});
defineEmits(['confirmar', 'cancelar']);

const paso = ref(1);
watch(() => props.abierto, (v) => { if (v) paso.value = 1; });
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
.panel { width: 100%; max-width: 480px; }
</style>
