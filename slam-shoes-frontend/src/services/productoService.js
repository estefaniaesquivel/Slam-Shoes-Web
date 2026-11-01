import axios from 'axios';

// La URL apunta al backend de Spring Boot 
const API_URL = 'http://localhost:8080/api/productos';

export default {
  // Método para obtener todos los productos 
  obtenerProductos() {
    return axios.get(API_URL);
  },

  // Método por si el administrador quiere registrar un nuevo producto
  crearProducto(producto) {
    return axios.post(API_URL, producto);
  }
};