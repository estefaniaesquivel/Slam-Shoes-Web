<template>
  <!-- Recuadro con un "+" para elegir una imagen .webp de un tamaño exacto (valida formato y medidas) -->
  <div class="subir-imagen" :style="{ width: anchoVisual + 'px' }">
    <label class="caja" :class="{ 'con-imagen': !!vista }" :style="{ aspectRatio: `${ancho} / ${alto}` }">
      <img v-if="vista" :src="vista" :alt="etiqueta" />
      <template v-else>
        <i class="bi bi-plus-lg fs-3"></i>
        <span class="small text-center px-1">{{ etiqueta }}</span>
      </template>
      <input type="file" accept=".webp,image/webp" class="d-none" @change="elegir" />
    </label>
    <div v-if="mostrarLeyenda" class="leyenda text-secondary">La imagen debe ser de {{ ancho }}x{{ alto }}px</div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue';

const props = defineProps({
  ancho: { type: Number, required: true },
  alto: { type: Number, required: true },
  etiqueta: { type: String, default: 'Agregar imagen' },
  // Imagen ya guardada (se muestra mientras no se elija otra)
  urlActual: { type: String, default: '' },
  anchoVisual: { type: Number, default: 180 },
  mostrarLeyenda: { type: Boolean, default: true }
});
const emit = defineEmits(['seleccionado']);

const preview = ref('');
const vista = computed(() => preview.value || props.urlActual);

function liberar() {
  if (preview.value) URL.revokeObjectURL(preview.value);
  preview.value = '';
}

// Si el padre guarda y cambia la imagen actual, se descarta la vista previa local
watch(() => props.urlActual, liberar);
onBeforeUnmount(liberar);

function medir(archivo) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(archivo);
    const img = new Image();
    img.onload = () => resolve({ ancho: img.naturalWidth, alto: img.naturalHeight, url });
    img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('No se pudo leer la imagen')); };
    img.src = url;
  });
}

async function elegir(evento) {
  const archivo = evento.target.files[0];
  evento.target.value = ''; // permite volver a elegir el mismo archivo
  if (!archivo) return;

  if (!(archivo.type === 'image/webp' || archivo.name.toLowerCase().endsWith('.webp'))) {
    alert('La imagen debe estar en formato .webp');
    return;
  }
  try {
    const { ancho, alto, url } = await medir(archivo);
    if (ancho !== props.ancho || alto !== props.alto) {
      URL.revokeObjectURL(url);
      alert(`La imagen debe medir ${props.ancho}x${props.alto} px (la que elegiste mide ${ancho}x${alto} px).`);
      return;
    }
    liberar();
    preview.value = url;
    emit('seleccionado', archivo);
  } catch (e) {
    alert('No se pudo leer la imagen seleccionada.');
  }
}

// El padre puede pedir descartar la imagen elegida (p. ej. al cancelar una edición)
defineExpose({ limpiar: liberar });
</script>

<style scoped>
.caja {
  width: 100%;
  border: 2px dashed #666;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #aaa;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s ease;
}
.caja:hover { border-color: #fff; color: #fff; }
.caja.con-imagen { border-style: solid; background: #fff; }
.caja img { width: 100%; height: 100%; object-fit: cover; }
.leyenda { font-size: 0.72rem; margin-top: 4px; }
</style>
