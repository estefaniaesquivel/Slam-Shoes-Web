import axios from 'axios';

// ---------------------------------------------------------------------------------------------
// Cliente del Broker para Vue.
//
// El frontend ya NO conoce tablas, repositorios ni quien atiende cada cosa: solo pide
//   "servicio" + "operacion" + "parametros"
// y recibe el resultado. Si mañana cambia cómo se guardan las tallas o cómo se valida el stock,
// el cambio se hace en el proveedor del backend y esta capa no se entera.
//
// Flujo:  Vue -> POST /api/broker/{servicio}/{operacion} -> Broker -> Proveedor -> Broker -> Vue
// ---------------------------------------------------------------------------------------------

const API = 'http://localhost:8080/api/broker';

// Error con el estado que devolvió el Broker (SERVICIO_NO_ENCONTRADO, PARAMETROS_INVALIDOS,
// CONFLICTO, NO_AUTORIZADO, TIEMPO_AGOTADO, BROKER_NO_DISPONIBLE, ...). "message" ya es legible.
export class BrokerError extends Error {
  constructor(mensaje, estado, httpStatus) {
    super(mensaje);
    this.name = 'BrokerError';
    this.estado = estado;
    this.httpStatus = httpStatus;
  }
}

// Identificador único para una solicitud. Si la misma solicitud se manda dos veces con el MISMO id
// (doble clic, reintento de red), el Broker devuelve la respuesta anterior sin ejecutarla otra vez.
export function nuevoIdSolicitud() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID();
  return 'sol-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 12);
}

// Envía una solicitud y devuelve el "resultado" del proveedor. Lanza BrokerError si falla.
export async function enviarSolicitud(servicio, operacion, parametros = {}, idSolicitud = null) {
  try {
    const url = `${API}/${encodeURIComponent(servicio)}/${encodeURIComponent(operacion)}`;
    const res = await axios.post(url, parametros, idSolicitud ? { params: { idSolicitud } } : undefined);
    return res.data.resultado;
  } catch (e) {
    const datos = e.response?.data;
    throw new BrokerError(
      datos?.error || 'No se pudo comunicar con el servidor. Intenta de nuevo.',
      datos?.estado || 'SIN_CONEXION',
      e.response?.status
    );
  }
}

// ---- Operaciones por servicio (el nombre de cada función es el contrato con el backend) ----

export const productosBroker = {
  // Producto + categoría + oferta + tallas + imágenes + estilos del modelo, en UNA sola solicitud
  obtenerDetalle: (idProducto) => enviarSolicitud('productos', 'obtenerDetalle', { idProducto }),
  // Existencias de una talla; con "cantidad" también indica si alcanzan (campo "suficiente")
  consultarStock: (idVariante, cantidad) =>
    enviarSolicitud('productos', 'consultarStock', cantidad ? { idVariante, cantidad } : { idVariante })
};

// "atendidoPor" sirve para evidenciar qué proveedor respondió; no es parte del usuario que se guarda en la sesión
function quitarAtendidoPor(resultado) {
  const copia = { ...resultado };
  delete copia.atendidoPor;
  return copia;
}

export const usuariosBroker = {
  autenticar: async (correo, contrasena) => quitarAtendidoPor(await enviarSolicitud('usuarios', 'autenticar', { correo, contrasena })),
  registrar: async (nombre, correo, contrasena) => quitarAtendidoPor(await enviarSolicitud('usuarios', 'registrar', { nombre, correo, contrasena }))
};

export const pedidosBroker = {
  // idSolicitud opcional: usar el mismo en cada reintento de la misma compra evita pedidos duplicados
  crearPedido: (datos, idSolicitud = null) => enviarSolicitud('pedidos', 'crearPedido', datos, idSolicitud),
  consultarHistorial: (idUsuario) => enviarSolicitud('pedidos', 'consultarHistorial', { idUsuario })
};

// Monitoreo: servicios registrados, métricas y últimas solicitudes del Broker
export async function obtenerEstadoBroker() {
  try {
    return (await axios.get(`${API}/estado`)).data;
  } catch (e) {
    throw new BrokerError(e.response?.data?.error || 'No se pudo consultar el Broker', e.response?.data?.estado || 'SIN_CONEXION', e.response?.status);
  }
}
