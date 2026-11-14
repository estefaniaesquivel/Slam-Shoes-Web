// Enlaces de los banners: funcionan igual en pruebas (localhost) y desplegados, porque se guardan
// como RUTAS INTERNAS del sitio ("/producto/12") y no como direcciones completas.
// Si el administrador pega la dirección completa de la barra del navegador
// (http://localhost:5173/producto/12) se convierte sola a "/producto/12".
export function normalizarEnlace(texto) {
  const t = String(texto ?? '').trim();
  if (!t) return '';
  if (t.startsWith('/')) return t;
  if (/^https?:\/\//i.test(t)) {
    try {
      const u = new URL(t);
      const local = ['localhost', '127.0.0.1'].includes(u.hostname) || u.hostname === window.location.hostname;
      return local ? `${u.pathname}${u.search}${u.hash}` : t; // dirección externa: se deja completa
    } catch (e) {
      return t;
    }
  }
  return `/${t}`; // "producto/12" -> "/producto/12"
}

export const EJEMPLOS_ENLACE = [
  { ruta: '/producto/12', texto: 'Un producto (el número es su ID)' },
  { ruta: '/marca/3', texto: 'El catálogo de una marca' },
  { ruta: '/jugador/2', texto: 'El panel de un jugador' },
  { ruta: '/coleccion/1', texto: 'Una colección' },
  { ruta: '/categoria/1', texto: 'Una categoría' },
  { ruta: '/ofertas', texto: 'Todas las ofertas' },
  { ruta: '/novedades', texto: 'Novedades' }
];
