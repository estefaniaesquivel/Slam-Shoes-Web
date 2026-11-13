// Paleta de colores y tallas compartidas por el formulario del admin y los filtros del catálogo.
// Los nombres coinciden con lo que se guarda en producto_tallas.color.
export const COLORES = [
  { nombre: 'Naranja', fondo: '#ff5722' },
  { nombre: 'Amarillo', fondo: '#ffc107' },
  { nombre: 'Verde', fondo: '#22c55e' },
  { nombre: 'Azul', fondo: '#6674f5' },
  { nombre: 'Rojo', fondo: '#c81e1e' },
  { nombre: 'Blanco', fondo: '#ffffff', claro: true },
  { nombre: 'Gris', fondo: '#6b7099' },
  { nombre: 'Negro', fondo: '#000000' },
  { nombre: 'Café', fondo: '#6b3a12' },
  { nombre: 'Morado', fondo: '#a855f7' },
  { nombre: 'Rosa', fondo: '#ec4899' },
  { nombre: 'Multicolor', fondo: 'linear-gradient(90deg, #ff5722, #ffc107, #22c55e, #6674f5, #a855f7, #ec4899)', claro: true }
];

// Tallas en cm: 22, 22.5, ... 32
export const TALLAS = Array.from({ length: 21 }, (_, i) => String(22 + i * 0.5));
