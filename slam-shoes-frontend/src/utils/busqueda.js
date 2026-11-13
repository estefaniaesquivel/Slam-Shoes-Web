// Búsqueda tolerante: sin importar mayúsculas, acentos, espacios ni signos.
// "kyrie5", "Kyrie 5" y "KYRIE  5" encuentran "Nike Kyrie 5 'Bob Esponja'".
export const normalizar = (texto) =>
  String(texto ?? '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9ñ]/g, '');

// Coincide si lo escrito aparece en el nombre, la marca, el modelo o en "marca + nombre"
export function coincideTexto(producto, consulta) {
  const q = normalizar(consulta);
  if (!q) return true;
  return [
    producto.nombre,
    producto.marca,
    producto.modelo,
    `${producto.marca ?? ''} ${producto.nombre ?? ''}`
  ].some((campo) => normalizar(campo).includes(q));
}
